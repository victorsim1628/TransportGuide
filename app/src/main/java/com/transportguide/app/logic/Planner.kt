package com.transportguide.app.logic

import com.transportguide.app.data.BookedTrain
import com.transportguide.app.data.Category
import com.transportguide.app.data.City
import com.transportguide.app.data.Place
import com.transportguide.app.data.TripData
import kotlin.math.roundToInt

/** A road or footpath route from the router (OSRM). */
data class RoadRoute(val km: Double, val minutes: Int, val snapMetersFrom: Double, val snapMetersTo: Double)

/**
 * Builds the four mode options, applies weather and disruption penalties,
 * and marks the best one. No network access here, so it is unit-tested.
 */
object Planner {

    fun plan(
        from: Place,
        to: Place,
        walk: RoadRoute?,
        car: RoadRoute?,
        weather: WeatherAtTrip?,
        disruptions: List<Disruption>,
    ): List<RouteOption> {
        val straight = Geo.haversineKm(from.lat, from.lon, to.lat, to.lon)
        val luggage = isLuggageTransfer(from, to)
        val bothInLagoon = Geo.inVeniceLagoon(from.lat, from.lon) && Geo.inVeniceLagoon(to.lat, to.lon)
        val active = disruptions.filter { it.severity != Severity.INFO }

        val veniceTrip = from.city == City.VENICE || to.city == City.VENICE || bothInLagoon
        val options = listOfNotNull(
            walkOption(straight, walk, weather, luggage, active),
            trainOption(from, to, straight, car, bothInLagoon, luggage, active),
            taxiOption(from, to, straight, car, bothInLagoon, luggage, active),
            // Water-taxi / vaporetto is hidden completely outside Venice.
            if (veniceTrip) waterOption(from, to, straight, bothInLagoon, weather, active) else null,
        )
        val best = options.filter { it.available }.minByOrNull { it.score }
        return options
            .map { if (it === best) it.copy(best = true) else it }
            .sortedWith(compareByDescending<RouteOption> { it.available }.thenBy { it.score })
    }

    fun isLuggageTransfer(from: Place, to: Place): Boolean {
        val ends = setOf(from.category, to.category)
        return Category.HOTEL in ends && (Category.STATION in ends || Category.AIRPORT in ends)
    }

    fun bookedTrain(from: Place, to: Place): BookedTrain? =
        TripData.bookedTrains.firstOrNull { it.fromId == from.id && it.toId == to.id }

    // ---------------------------------------------------------------- walking
    private fun walkOption(
        straight: Double, walk: RoadRoute?, w: WeatherAtTrip?, luggage: Boolean, d: List<Disruption>,
    ): RouteOption {
        val km = walk?.km ?: (straight * 1.3)
        val min = walk?.minutes ?: (km / 4.5 * 60).roundToInt()
        val notes = mutableListOf<String>()
        val warns = mutableListOf<String>()
        var score = min.toDouble()
        val available = km <= 8.0
        if (!available) warns += "Too far to walk (${Geo.km(km)})."
        if (km > 3) score += 30
        if (w != null) {
            if (WeatherAdvice.isSevere(w)) { score += 60; warns += "Severe weather for walking." }
            else if (WeatherAdvice.isWet(w)) { score += 12 * km; warns += "Rain likely — umbrellas needed." }
            if (w.apparentC <= 3 && km > 1.5) { score += 10; warns += "Cold for a long walk." }
        }
        if (luggage && km > 0.8) { score += 40; warns += "Long walk with suitcases." }
        if (km <= 1.2) notes += "Short and easy — often quicker than waiting for transport."
        applyDisruptions(Mode.WALK, d, warns).let { score += it }
        if (walk == null) notes += "Estimated: walking router not reachable."
        return RouteOption(Mode.WALK, km, min, available, walk == null, notes, warns, score)
    }

    // ---------------------------------------------------------- train / metro
    private fun trainOption(
        from: Place, to: Place, straight: Double, car: RoadRoute?, bothInLagoon: Boolean,
        luggage: Boolean, d: List<Disruption>,
    ): RouteOption {
        val notes = mutableListOf<String>()
        val warns = mutableListOf<String>()
        val booked = bookedTrain(from, to)
        val (km, min) = when {
            booked != null -> {
                notes += "Booked: ${booked.train} · ${booked.date} ${booked.departs} → ${booked.arrives}"
                notes += booked.details
                booked.km to minutesBetween(booked.departs, booked.arrives)
            }
            straight > 30 -> {
                notes += "Intercity rail estimate — check Trenitalia / Italo / DB for times."
                val k = straight * 1.2
                k to (k / 110 * 60 + 25).roundToInt()
            }
            else -> {
                val k = car?.km ?: (straight * 1.3)
                notes += localTransitHint(from, to)
                k to (8 + k / 18 * 60 + 6).roundToInt()
            }
        }
        var available = true
        if (bothInLagoon) {
            available = straight > 0.3 && isPeopleMoverPair(from, to)
            if (!available) notes.clear()
            if (!available) notes += "No trains, metro or road buses inside historic Venice — see Water-taxi / Vaporetto."
            else notes.add(0, "People Mover Tronchetto ↔ Piazzale Roma (about 3 min).")
        }
        if (straight < 0.8 && booked == null) { available = false; notes += "Too short for public transport." }
        var score = min.toDouble() + if (booked != null) -30 else 0
        if (luggage && booked == null) score += 8
        score += applyDisruptions(Mode.TRAIN, d, warns)
        return RouteOption(Mode.TRAIN, km, min, available, booked == null, notes, warns, score)
    }

    // ------------------------------------------------------------------- taxi
    private fun taxiOption(
        from: Place, to: Place, straight: Double, car: RoadRoute?, bothInLagoon: Boolean,
        luggage: Boolean, d: List<Disruption>,
    ): RouteOption {
        val notes = mutableListOf<String>()
        val warns = mutableListOf<String>()
        val km = car?.km ?: (straight * 1.35)
        val min = ((car?.minutes ?: (km / 25 * 60).roundToInt()) * 1.3 + 5).roundToInt()
        var available = true
        // Venice: cars reach only Piazzale Roma / Tronchetto. OSRM snaps far away for canal-side points.
        val veniceEnd = from.city == City.VENICE || to.city == City.VENICE
        if (veniceEnd && car != null && (car.snapMetersFrom > 150 || car.snapMetersTo > 150)) {
            available = false
            notes += "Road taxis cannot reach this point in Venice — use a water-taxi."
        }
        if (bothInLagoon && !isPeopleMoverPair(from, to)) {
            available = false
            if (notes.isEmpty()) notes += "Road taxis cannot reach this point in Venice — use a water-taxi."
        }
        if (available) notes += taxiTip(from, to)
        var score = min.toDouble() + 8 // cost
        if (luggage) { score -= 6; notes += "Easiest with 4 people and luggage." }
        score += applyDisruptions(Mode.TAXI, d, warns)
        return RouteOption(Mode.TAXI, km, min, available, car == null, notes, warns, score)
    }

    // -------------------------------------------------------------- water
    private fun waterOption(
        from: Place, to: Place, straight: Double, bothInLagoon: Boolean, w: WeatherAtTrip?, d: List<Disruption>,
    ): RouteOption {
        val notes = mutableListOf<String>()
        val warns = mutableListOf<String>()
        val km = straight * 1.35
        val min = (km / 16 * 60 + 10).roundToInt()
        if (!bothInLagoon) {
            notes += "Only available within the Venice lagoon."
            return RouteOption(Mode.WATER_TAXI, km, min, false, true, notes, warns, Double.MAX_VALUE)
        }
        notes += "Private water-taxi: about €70–€130 per boat (up to 4–6 people with bags)."
        notes += "Vaporetto (ACTV): line 1 slow along the Grand Canal, line 2 faster; single ticket ≈ €9.50, 24 h pass ≈ €25."
        if (to.id == "h_venice" || from.id == "h_venice") notes += "Tronchetto stop: lines 2 and the People Mover."
        var score = min.toDouble() + 6
        if (w != null) {
            if (w.windKmh >= 40) { score += 60; warns += "Strong wind — boat services may be suspended." }
            if (w.weatherCode in setOf(45, 48)) { score += 15; warns += "Fog slows boats." }
        }
        score += applyDisruptions(Mode.WATER_TAXI, d, warns)
        return RouteOption(Mode.WATER_TAXI, km, min, true, true, notes, warns, score)
    }

    // ---------------------------------------------------------------- helpers
    /**
     * [d] holds only events on the visit day (the caller filters by date). Each affected mode
     * gets one short warning line, not the news headlines.
     */
    private fun applyDisruptions(mode: Mode, d: List<Disruption>, warns: MutableList<String>): Double {
        val hits = d.filter { mode in it.modes }
        if (hits.isEmpty()) return 0.0
        val strike = hits.any { it.severity == Severity.WARNING }
        warns += if (strike) "⚠️ Strike on your visit day: ${impact(mode, true)}"
        else "⚠️ Protest or disruption on your visit day: ${impact(mode, false)}"
        return if (strike) 80.0 else 20.0
    }

    private fun impact(mode: Mode, strike: Boolean) = when (mode) {
        Mode.TRAIN -> if (strike) "trains, metro and buses may not run." else "trains, metro and buses may be delayed or diverted."
        Mode.TAXI -> if (strike) "taxis may be hard to find." else "roads may be closed or congested."
        Mode.WALK -> "some streets and squares may be closed."
        Mode.WATER_TAXI -> if (strike) "vaporetti may not run; private water-taxis usually do." else "boat services may be disrupted."
    }

    private fun isPeopleMoverPair(from: Place, to: Place): Boolean {
        val pm = setOf("h_venice", "t_piazzale_roma")
        return from.id in pm && to.id in pm
    }

    private fun localTransitHint(from: Place, to: Place): String = when {
        from.id == "a_fco" || to.id == "a_fco" -> "Leonardo Express FCO ↔ Roma Termini (32 min, about €14 pp), then taxi or bus."
        from.id == "a_fra" || to.id == "a_fra" -> "S-Bahn S8/S9 Hauptwache ↔ Airport; Terminal 3 needs the airport shuttle."
        setOf(from.id, to.id) == setOf("h_milan", "s_milano_centrale") -> "Metro M2 Loreto ↔ Centrale, 2 stops (about 10 min door to door)."
        setOf(from.id, to.id) == setOf("h_frankfurt", "s_frankfurt_hbf") -> "U-Bahn U4/U5 or S-Bahn, about 15 min door to door."
        from.city == City.ROME -> "Rome: Metro A/B or ATAC buses; buy tickets at tabacchi or tap a contactless card."
        from.city == City.FLORENCE -> "Florence: Tramway T1/T2 or Autolinee Toscane buses; the centre is compact."
        from.city == City.MILAN -> "Milan: Metro M1–M5 — tap a contactless card at the gates."
        from.city == City.FRANKFURT -> "Frankfurt: U-Bahn / S-Bahn / tram (RMV); Deutschlandticket valid."
        else -> "Local public transport."
    }

    private fun taxiTip(from: Place, to: Place): String = when {
        from.id == "a_fco" || to.id == "a_fco" -> "Fixed taxi fare FCO ↔ inside the Aurelian Walls (about €55–60, up to 4 people)."
        from.city == City.ROME -> "Use the white licensed taxis; FreeNow / itTaxi apps work in Rome."
        from.city == City.MILAN -> "Milan: taxis queue at Centrale; app: itTaxi / FreeNow."
        from.city == City.FRANKFURT -> "Frankfurt: Taxi Frankfurt or FreeNow; cards accepted."
        from.city == City.FLORENCE -> "Florence: call 055 4242 / 055 4390 or use the itTaxi app."
        else -> "Licensed taxi rank or app."
    }

    private fun minutesBetween(a: String, b: String): Int {
        fun m(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        return (m(b) - m(a)).let { if (it < 0) it + 24 * 60 else it }
    }
}
