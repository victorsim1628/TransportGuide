package com.transportguide.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transportguide.app.data.City
import com.transportguide.app.data.CustomPlaces
import com.transportguide.app.data.Place
import com.transportguide.app.data.Suggestion
import com.transportguide.app.data.TripData
import com.transportguide.app.logic.Disruption
import com.transportguide.app.logic.Geo
import com.transportguide.app.logic.Planner
import com.transportguide.app.logic.RouteOption
import com.transportguide.app.logic.WeatherAdvice
import com.transportguide.app.logic.WeatherAtTrip
import com.transportguide.app.net.Api
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class TripResult(
    val from: Place,
    val to: Place,
    val options: List<RouteOption>,
    val weather: WeatherAtTrip?,
    val weatherAdvice: List<String>,
    val weatherMissingReason: String?,
    val disruptions: Map<City, List<Disruption>>,
    val curated: List<Suggestion>,
    val nearby: List<Suggestion>,
)

class TripViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CustomPlaces(app)

    var customPlaces by mutableStateOf(store.load()); private set
    val allPlaces get() = TripData.places + customPlaces

    var from by mutableStateOf<Place?>(TripData.byId("h_rome")); private set
    var to by mutableStateOf<Place?>(TripData.byId("x_colosseum")); private set
    var departure by mutableStateOf(LocalDateTime.now().plusMinutes(15)); private set

    var loading by mutableStateOf(false); private set
    var result by mutableStateOf<TripResult?>(null); private set

    var searchResults by mutableStateOf<List<Api.GeoResult>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    var searchMessage by mutableStateOf<String?>(null); private set

    fun chooseFrom(p: Place) { from = p; result = null }
    fun chooseTo(p: Place) { to = p; result = null }
    fun swap() { val f = from; from = to; to = f; result = null }
    fun chooseDeparture(t: LocalDateTime) { departure = t; result = null }

    fun plan() {
        val f = from ?: return
        val t = to ?: return
        loading = true
        viewModelScope.launch {
            val walk = async { Api.route("foot", f.lat, f.lon, t.lat, t.lon) }
            val car = async { Api.route("car", f.lat, f.lon, t.lat, t.lon) }
            val weather = async { Api.weather(f.lat, f.lon, departure) }
            val cities = listOf(f.city, t.city).filter { it != City.OTHER }.distinct()
            val news = cities.associateWith { c -> async { Api.disruptions(c) } }
            val curated = TripData.curated[t.city].orEmpty()
                .map { it.copy(distanceKm = Geo.haversineKm(t.lat, t.lon, it.lat, it.lon)) }
                .sortedBy { it.distanceKm }
            val nearby = async { Api.nearbyLandmarks(t.lat, t.lon) }

            val w = weather.await()
            val disruptions = news.mapValues { it.value.await() }
            val walkRoute = walk.await()
            val options = Planner.plan(f, t, walkRoute, car.await(), w, disruptions.values.flatten())
            val walkKm = options.firstOrNull { it.mode == com.transportguide.app.logic.Mode.WALK }?.km
            val curatedNames = curated.map { it.title.lowercase() }.toSet()

            result = TripResult(
                from = f, to = t, options = options,
                weather = w,
                weatherAdvice = w?.let { WeatherAdvice.advise(it, walkKm) }.orEmpty(),
                weatherMissingReason = if (w == null) {
                    if (departure.isAfter(LocalDateTime.now().plusDays(15)))
                        "Forecast is available up to 16 days ahead — check again closer to the date."
                    else "Weather service not reachable — check your connection and try again."
                } else null,
                disruptions = disruptions,
                curated = curated,
                nearby = nearby.await().filter { s -> curatedNames.none { it in s.title.lowercase() || s.title.lowercase() in it } },
            )
            loading = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        // Accept "41.9009, 12.4833" directly.
        val coords = Regex("""^\s*(-?\d+(\.\d+)?)\s*,\s*(-?\d+(\.\d+)?)\s*$""").find(query)
        if (coords != null) {
            searchResults = listOf(Api.GeoResult("Pinned location", query.trim(),
                coords.groupValues[1].toDouble(), coords.groupValues[3].toDouble()))
            return
        }
        searching = true; searchMessage = null
        viewModelScope.launch {
            searchResults = Api.searchPlace(query)
            if (searchResults.isEmpty()) searchMessage = "No match. Try adding the city, e.g. \"Galleria Borghese Rome\"."
            searching = false
        }
    }

    fun saveSearchResult(r: Api.GeoResult, customName: String, asDestination: Boolean) {
        val p = store.add(customName.ifBlank { r.name }, r.lat, r.lon, r.address)
        customPlaces = store.load()
        if (asDestination) chooseTo(p) else chooseFrom(p)
        searchResults = emptyList()
        searchMessage = "Saved \"${p.name}\" to My places."
    }

    fun deleteCustom(p: Place) {
        store.remove(p.id); customPlaces = store.load()
        if (from?.id == p.id) from = null
        if (to?.id == p.id) to = null
    }
}
