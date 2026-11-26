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
import com.transportguide.app.logic.DisruptionGroup
import com.transportguide.app.logic.DisruptionSummary
import com.transportguide.app.logic.Geo
import com.transportguide.app.logic.Planner
import com.transportguide.app.logic.RouteOption
import com.transportguide.app.logic.WeatherAdvice
import com.transportguide.app.logic.WeatherAtTrip
import com.transportguide.app.net.Api
import com.transportguide.app.net.Translator
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class TripResult(
    val from: Place,
    val to: Place,
    val options: List<RouteOption>,
    val weather: WeatherAtTrip?,
    val weatherAdvice: List<String>,
    val weatherMissingReason: String?,
    val disruptions: Map<City, List<DisruptionGroup>>,
    val curated: List<Suggestion>,
    val nearby: List<Suggestion>,
)

private const val MAX_EVENTS_PER_CITY = 4

class TripViewModel(app: Application) : AndroidViewModel(app) {
    private val store = CustomPlaces(app)

    var customPlaces by mutableStateOf(store.load()); private set
    val allPlaces get() = TripData.places + customPlaces

    var from by mutableStateOf<Place?>(TripData.byId("h_rome")); private set
    var to by mutableStateOf<Place?>(TripData.byId("x_colosseum")); private set
    /** The system clock, refreshed at the start of every minute. */
    var now by mutableStateOf(LocalDateTime.now()); private set
    /** Set only when the user picks another date/time; null means "leave now". */
    var customDeparture by mutableStateOf<LocalDateTime?>(null); private set
    val departure: LocalDateTime get() = customDeparture ?: now

    private val uiPrefs = app.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
    /** Text size multiplier on top of the phone's own font size setting. */
    var textScale by mutableStateOf(uiPrefs.getFloat("text_scale", 1.0f)); private set

    init {
        viewModelScope.launch {
            while (true) {
                now = LocalDateTime.now()
                delay(60_000L - (System.currentTimeMillis() % 60_000L))
            }
        }
    }

    var loading by mutableStateOf(false); private set
    var result by mutableStateOf<TripResult?>(null); private set

    var searchResults by mutableStateOf<List<Api.GeoResult>>(emptyList()); private set
    var searching by mutableStateOf(false); private set
    var searchMessage by mutableStateOf<String?>(null); private set

    fun chooseFrom(p: Place) { from = p; result = null }
    fun chooseTo(p: Place) { to = p; result = null }
    fun swap() { val f = from; from = to; to = f; result = null }
    fun chooseDeparture(t: LocalDateTime) { customDeparture = t; result = null }
    fun useCurrentTime() { customDeparture = null; now = LocalDateTime.now(); result = null }

    fun chooseTextScale(scale: Float) {
        textScale = scale
        uiPrefs.edit().putFloat("text_scale", scale).apply()
    }

    fun plan() {
        val f = from ?: return
        val t = to ?: return
        loading = true
        now = LocalDateTime.now()
        val leaving = departure.atZone(ZoneId.systemDefault())
        viewModelScope.launch {
            val walk = async { Api.route("foot", f.lat, f.lon, t.lat, t.lon) }
            val car = async { Api.route("car", f.lat, f.lon, t.lat, t.lon) }
            val weather = async { Api.weather(f.lat, f.lon, leaving) }
            val cities = listOf(f.city, t.city).filter { it != City.OTHER }.distinct()
            val news = cities.associateWith { c -> async { Api.disruptions(c) } }
            val curated = TripData.curatedFor(t.city)
                .map { it.copy(distanceKm = Geo.haversineKm(t.lat, t.lon, it.lat, it.lon)) }
                .sortedBy { it.distanceKm }
            val nearby = async { Api.nearbyLandmarks(t.lat, t.lon) }

            val w = weather.await()
            // One entry per event/day with its two most reliable sources; only those are
            // translated to British English (on the phone) and shown.
            val today = LocalDate.now()
            val disruptions = news.mapValues { (_, d) ->
                val groups = DisruptionSummary.summarise(d.await(), today).take(MAX_EVENTS_PER_CITY)
                val translated = Translator.translate(groups.flatMap { it.sources })
                    .associateBy { it.originalTitle ?: it.title }
                groups.map { g ->
                    val src = g.sources.map { translated[it.title] ?: it }
                    g.copy(sources = src, headline = src.first())
                }
            }
            val walkRoute = walk.await()
            // The planner sees each upcoming event once, not every article about it.
            val events = disruptions.values.flatten().map {
                it.headline.copy(modes = it.modes, severity = it.severity, mentionsTripDate = it.mentionsTripDate)
            }
            val options = Planner.plan(f, t, walkRoute, car.await(), w, events)
            val walkKm = options.firstOrNull { it.mode == com.transportguide.app.logic.Mode.WALK }?.km
            val curatedNames = curated.map { it.title.lowercase() }.toSet()

            result = TripResult(
                from = f, to = t, options = options,
                weather = w,
                weatherAdvice = w?.let { WeatherAdvice.advise(it, walkKm) }.orEmpty(),
                weatherMissingReason = if (w == null) {
                    if (leaving.toLocalDateTime().isAfter(LocalDateTime.now().plusDays(15)))
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
