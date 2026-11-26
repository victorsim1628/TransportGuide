package com.transportguide.app.net

import com.transportguide.app.data.City
import com.transportguide.app.data.Suggestion
import com.transportguide.app.logic.Disruption
import com.transportguide.app.logic.DisruptionClassifier
import com.transportguide.app.logic.Geo
import com.transportguide.app.logic.RoadRoute
import com.transportguide.app.logic.WeatherAtTrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

/**
 * All network calls. Every service here is free and needs no API key:
 *  - routing.openstreetmap.de   OSRM walking and driving routes (OpenStreetMap)
 *  - api.open-meteo.com         weather forecast
 *  - news.google.com            RSS search for strikes / protests per city
 *  - nominatim.openstreetmap.org  address search for new places
 *  - en.wikipedia.org           landmarks near the destination
 */
object Api {
    private const val UA = "TransportGuide/1.0 (Android travel planner)"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept-Language", "en")
        c.connectTimeout = 12_000
        c.readTimeout = 15_000
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode} from ${c.url.host}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    // ------------------------------------------------------------- routing
    suspend fun route(profile: String, fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): RoadRoute? =
        runCatching {
            val url = "https://routing.openstreetmap.de/routed-$profile/route/v1/driving/" +
                "$fromLon,$fromLat;$toLon,$toLat?overview=false"
            parseOsrm(get(url))
        }.getOrNull()

    fun parseOsrm(json: String): RoadRoute? {
        val o = JSONObject(json)
        if (o.optString("code") != "Ok") return null
        val r = o.getJSONArray("routes").getJSONObject(0)
        val wps = o.getJSONArray("waypoints")
        return RoadRoute(
            km = r.getDouble("distance") / 1000.0,
            minutes = (r.getDouble("duration") / 60.0).roundToInt().coerceAtLeast(1),
            snapMetersFrom = wps.getJSONObject(0).optDouble("distance", 0.0),
            snapMetersTo = wps.getJSONObject(wps.length() - 1).optDouble("distance", 0.0),
        )
    }

    // ------------------------------------------------------------- weather
    suspend fun weather(lat: Double, lon: Double, at: ZonedDateTime): WeatherAtTrip? = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation," +
            "weather_code,wind_speed_10m,is_day&timezone=auto&forecast_days=16"
        parseWeather(get(url), at)
    }.getOrNull()

    /**
     * Picks the forecast hour of the departure. The departure is the phone's time in the phone's
     * time zone; Open-Meteo answers in the place's own time zone, so convert before matching
     * (e.g. a phone still on Singapore time planning a trip in Rome).
     */
    fun parseWeather(json: String, at: ZonedDateTime): WeatherAtTrip? {
        val root = JSONObject(json)
        val h = root.getJSONObject("hourly")
        val times = h.getJSONArray("time")
        val placeZone = runCatching { ZoneId.of(root.getString("timezone")) }.getOrDefault(at.zone)
        val target = at.withZoneSameInstant(placeZone).toLocalDateTime()
            .withMinute(0).withSecond(0).withNano(0)
        var idx = -1
        for (i in 0 until times.length()) {
            if (LocalDateTime.parse(times.getString(i)) == target) { idx = i; break }
        }
        if (idx < 0) return null // beyond the 16-day forecast range
        fun d(k: String) = h.getJSONArray(k).optDouble(idx, 0.0)
        return WeatherAtTrip(
            timeLabel = target.format(DateTimeFormatter.ofPattern("EEE d MMM, HH:00", Locale.UK)) +
                if (placeZone.rules.getOffset(at.toInstant()) != at.offset) " (local time there)" else "",
            temperatureC = d("temperature_2m"),
            apparentC = d("apparent_temperature"),
            precipProbability = d("precipitation_probability").roundToInt(),
            precipitationMm = d("precipitation"),
            windKmh = d("wind_speed_10m"),
            weatherCode = d("weather_code").toInt(),
            isDay = d("is_day") >= 1.0,
        )
    }

    // ------------------------------------------- strikes & protests (news)
    suspend fun disruptions(city: City, sinceDays: Int = 14): List<Disruption> = coroutineScope {
        DisruptionClassifier.queries(city).map { (lang, q) ->
            async {
                runCatching {
                    val (hl, gl) = when (lang) { "it" -> "it" to "IT"; "de" -> "de" to "DE"; else -> "en-GB" to "GB" }
                    val url = "https://news.google.com/rss/search?q=${enc("$q when:${sinceDays}d")}" +
                        "&hl=$hl&gl=$gl&ceid=$gl:${hl.substringBefore('-')}"
                    parseNewsRss(get(url), city, DisruptionClassifier.tripDates(city))
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
            .distinctBy { it.title.lowercase().take(60) }
            .sortedWith(compareByDescending<Disruption> { it.mentionsTripDate }
                .thenByDescending { it.severity.ordinal }
                .thenByDescending { it.publishedMillis })
    }

    fun parseNewsRss(xml: String, city: City, tripDates: List<Pair<Int, Int>>): List<Disruption> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream(Charsets.UTF_8))
        val items = doc.getElementsByTagName("item")
        val out = mutableListOf<Disruption>()
        for (i in 0 until items.length) {
            val e = items.item(i) as Element
            fun tag(n: String) = e.getElementsByTagName(n).item(0)?.textContent?.trim().orEmpty()
            val rawTitle = tag("title")
            val source = tag("source").ifEmpty { rawTitle.substringAfterLast(" - ", "") }
            val title = if (source.isNotEmpty()) rawTitle.removeSuffix(" - $source") else rawTitle
            if (!DisruptionClassifier.isRelevant(title, city)) continue
            val (modes, sev) = DisruptionClassifier.classify(title, city)
            if (modes.isEmpty()) continue
            val published = runCatching {
                ZonedDateTime.parse(tag("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            }.getOrDefault(0L)
            out += Disruption(title, source, tag("link"), published, modes, sev,
                DisruptionClassifier.mentionsAnyDate(title, tripDates))
        }
        return out
    }

    // ------------------------------------------------- new place search
    data class GeoResult(val name: String, val address: String, val lat: Double, val lon: Double)

    suspend fun searchPlace(query: String): List<GeoResult> = runCatching {
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6" +
            "&countrycodes=it,de&q=${enc(query)}"
        val arr = JSONArray(get(url))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val display = o.getString("display_name")
            GeoResult(
                name = o.optString("name").ifBlank { display.substringBefore(",") },
                address = display,
                lat = o.getString("lat").toDouble(),
                lon = o.getString("lon").toDouble(),
            )
        }
    }.getOrDefault(emptyList())

    // ------------------------------------------- landmarks near a place
    suspend fun nearbyLandmarks(lat: Double, lon: Double): List<Suggestion> = runCatching {
        val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&generator=geosearch" +
            "&ggscoord=${enc("$lat|$lon")}&ggsradius=1500&ggslimit=20" +
            "&prop=coordinates|description|pageprops&ppprop=wikibase_item"
        parseWiki(get(url), lat, lon)
    }.getOrDefault(emptyList())

    fun parseWiki(json: String, lat: Double, lon: Double): List<Suggestion> {
        val pages = JSONObject(json).optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
        val boring = Regex("(?i)station|metro|bus|tram|street|via |piazzale|hotel|school|hospital|district|quarter|municipio|rione|bank|company|stop")
        val out = mutableListOf<Suggestion>()
        pages.keys().forEach { k ->
            val p = pages.getJSONObject(k)
            val title = p.optString("title")
            val desc = p.optString("description")
            val c = p.optJSONArray("coordinates")?.optJSONObject(0) ?: return@forEach
            if (boring.containsMatchIn(title) || boring.containsMatchIn(desc)) return@forEach
            val la = c.getDouble("lat"); val lo = c.getDouble("lon")
            out += Suggestion(
                title = title,
                blurb = desc.ifBlank { "Wikipedia landmark" },
                lat = la, lon = lo,
                distanceKm = Geo.haversineKm(lat, lon, la, lo),
                url = "https://en.wikipedia.org/wiki/" + enc(title.replace(' ', '_')),
            )
        }
        return out.sortedBy { it.distanceKm }.take(10)
    }
}
