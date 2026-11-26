package com.transportguide.app.data

import android.content.Context
import com.transportguide.app.logic.Geo
import org.json.JSONArray
import org.json.JSONObject

/** Stores the places the user typed in, so they appear in the dropdowns next time. */
class CustomPlaces(context: Context) {
    private val prefs = context.getSharedPreferences("custom_places", Context.MODE_PRIVATE)

    fun load(): List<Place> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Place(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    city = runCatching { City.valueOf(o.getString("city")) }.getOrDefault(City.OTHER),
                    category = Category.CUSTOM,
                    lat = o.getDouble("lat"),
                    lon = o.getDouble("lon"),
                    address = o.optString("address"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(name: String, lat: Double, lon: Double, address: String): Place {
        val place = Place(
            id = "c_" + System.currentTimeMillis(),
            name = name,
            city = Geo.nearestCity(lat, lon),
            category = Category.CUSTOM,
            lat = lat, lon = lon, address = address,
        )
        save(load() + place)
        return place
    }

    fun remove(id: String) = save(load().filterNot { it.id == id })

    private fun save(list: List<Place>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("city", it.city.name)
                put("lat", it.lat); put("lon", it.lon); put("address", it.address)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object { const val KEY = "places" }
}
