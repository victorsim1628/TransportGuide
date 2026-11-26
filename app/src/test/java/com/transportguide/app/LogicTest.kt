package com.transportguide.app

import com.transportguide.app.data.City
import com.transportguide.app.data.TripData
import com.transportguide.app.logic.Disruption
import com.transportguide.app.logic.DisruptionClassifier
import com.transportguide.app.logic.Geo
import com.transportguide.app.logic.Mode
import com.transportguide.app.logic.Planner
import com.transportguide.app.logic.RoadRoute
import com.transportguide.app.logic.Severity
import com.transportguide.app.logic.WeatherAdvice
import com.transportguide.app.logic.WeatherAtTrip
import com.transportguide.app.net.Api
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private fun res(name: String) = javaClass.classLoader!!.getResource(name)!!.readText()
    private fun p(id: String) = TripData.byId(id)!!

    private val dry = WeatherAtTrip("x", 12.0, 11.0, 5, 0.0, 8.0, 1, true)
    private val rainy = WeatherAtTrip("x", 8.0, 5.0, 85, 2.5, 15.0, 63, true)

    @Test fun kmFormatting() {
        assertEquals("1.7 km", Geo.km(1.678))
        assertEquals("261 km", Geo.km(261.0))
    }

    @Test fun nearestCityAndLagoon() {
        assertEquals(City.VENICE, Geo.nearestCity(45.4405, 12.3063))
        assertEquals(City.FRANKFURT, Geo.nearestCity(50.03, 8.58))
        assertTrue(Geo.inVeniceLagoon(45.4345, 12.3397))
        assertFalse(Geo.inVeniceLagoon(45.4862, 9.2047))
    }

    @Test fun allSpreadsheetPlacesSitInTheirCity() {
        TripData.places.filter { it.city != City.OTHER }.forEach {
            assertEquals(it.name, it.city, Geo.nearestCity(it.lat, it.lon))
        }
    }

    @Test fun parsesRealOsrmResponse() {
        val r = Api.parseOsrm(res("osrm_foot.json"))!!
        assertEquals(1.678, r.km, 0.01)
        assertEquals(22, r.minutes)
    }

    @Test fun shortDryWalkInRomeWinsOverTaxi() {
        val walk = Api.parseOsrm(res("osrm_foot.json"))
        val car = RoadRoute(2.4, 8, 10.0, 10.0)
        val opts = Planner.plan(p("h_rome"), p("x_colosseum"), walk, car, dry, emptyList())
        assertEquals(Mode.WALK, opts.first { it.best }.mode)
        assertFalse(opts.first { it.mode == Mode.WATER_TAXI }.available)
    }

    @Test fun rainPushesAwayFromWalking() {
        val walk = RoadRoute(2.9, 38, 5.0, 5.0)
        val car = RoadRoute(3.4, 11, 5.0, 5.0)
        val opts = Planner.plan(p("h_rome"), p("s_roma_termini"), walk, car, rainy, emptyList())
        assertTrue(opts.first { it.best }.mode != Mode.WALK)
    }

    @Test fun veniceHotelToSanMarcoUsesWaterAndBlocksRoadTaxi() {
        val car = Api.parseOsrm(res("osrm_car_venice.json"))!!
        val opts = Planner.plan(p("h_venice"), p("x_san_marco"), RoadRoute(3.9, 50, 5.0, 5.0), car, dry, emptyList())
        assertFalse(opts.first { it.mode == Mode.TAXI }.available)
        assertFalse(opts.first { it.mode == Mode.TRAIN }.available)
        assertEquals(Mode.WATER_TAXI, opts.first { it.best }.mode)
    }

    @Test fun bookedTrainIsUsedForIntercityLeg() {
        val opts = Planner.plan(p("s_roma_termini"), p("s_firenze_smn"), null, null, dry, emptyList())
        val train = opts.first { it.mode == Mode.TRAIN }
        assertTrue(train.best)
        assertEquals(261.0, train.km, 0.1)
        assertEquals(97, train.minutes)
        assertFalse(opts.first { it.mode == Mode.WALK }.available)
    }

    @Test fun taxiStrikeOnTripDateMovesBestAwayFromTaxi() {
        val walk = RoadRoute(2.9, 38, 5.0, 5.0)
        val car = RoadRoute(3.4, 11, 5.0, 5.0)
        val strike = Disruption("Roma, sciopero dei taxi il 2 dicembre", "Test", "", 0, setOf(Mode.TAXI), Severity.WARNING, true)
        val before = Planner.plan(p("h_rome"), p("s_roma_termini"), walk, car, rainy, emptyList())
        val after = Planner.plan(p("h_rome"), p("s_roma_termini"), walk, car, rainy, listOf(strike))
        assertEquals(Mode.TAXI, before.first { it.best }.mode)
        assertTrue(after.first { it.best }.mode != Mode.TAXI)
        assertTrue(after.first { it.mode == Mode.TAXI }.warnings.any { "sciopero" in it })
    }

    @Test fun classifierRecognisesModesAndDates() {
        val (m1, s1) = DisruptionClassifier.classify("Sciopero treni Trenitalia e Italo venerdì", City.ROME)
        assertTrue(Mode.TRAIN in m1); assertEquals(Severity.WARNING, s1)
        val (m2, _) = DisruptionClassifier.classify("Venezia, sciopero ACTV: vaporetti a rischio", City.VENICE)
        assertTrue(Mode.WATER_TAXI in m2)
        val (m3, s3) = DisruptionClassifier.classify("Gen-Z-Demos in Frankfurt: Hier kommt es heute zu ÖPNV-Einschränkungen", City.FRANKFURT)
        assertTrue(Mode.TRAIN in m3); assertEquals(Severity.WATCH, s3)
        val (m4, _) = DisruptionClassifier.classify("New restaurant opens near the Duomo", City.MILAN)
        assertTrue(m4.isEmpty())
        val (m5, _) = DisruptionClassifier.classify("Milano, fermata del bus spostata", City.MILAN)
        assertTrue(Mode.TRAIN in m5) // transit topic, but not a strike
        assertTrue(DisruptionClassifier.mentionsAnyDate("Sciopero taxi il 2 dicembre", DisruptionClassifier.tripDates(City.ROME)))
        assertTrue(DisruptionClassifier.mentionsAnyDate("Warnstreik am 10. Dezember", DisruptionClassifier.tripDates(City.FRANKFURT)))
        assertFalse(DisruptionClassifier.mentionsAnyDate("Sciopero il 21 dicembre", DisruptionClassifier.tripDates(City.ROME)))
        assertTrue(DisruptionClassifier.mentionsAnyDate("Corteo 30/11 a Roma", DisruptionClassifier.tripDates(City.ROME)))
    }

    @Test fun headlinesFoundInLiveRunAreClassifiedSensibly() {
        // General strike: trains and buses stop, taxis remain the fallback.
        val (g, gs) = DisruptionClassifier.classify("Sciopero generale del 4 dicembre 2026, si fermano anche i mezzi pubblici?", City.FLORENCE)
        assertEquals(setOf(Mode.TRAIN), g); assertEquals(Severity.WARNING, gs)
        assertTrue(DisruptionClassifier.mentionsAnyDate("Sciopero generale del 4 dicembre 2026", DisruptionClassifier.tripDates(City.FLORENCE)))
        val (v, _) = DisruptionClassifier.classify("Venezia, sciopero nazionale 4 dicembre: possibili disagi Actv, AVM e VeLa", City.VENICE)
        assertTrue(Mode.WATER_TAXI in v)
        // School walk-outs are marches, not transport strikes.
        val (_, ss) = DisruptionClassifier.classify("Schulstreik gegen Wehrpflicht in Frankfurt: Protest im Regen", City.FRANKFURT)
        assertEquals(Severity.WATCH, ss)
        // Saarbahn is in Saarbrücken, not Frankfurt.
        assertFalse(DisruptionClassifier.isRelevant("Massiver Tarifstreit: Streik bei der Saarbahn und kein Ende", City.FRANKFURT))
        assertTrue(DisruptionClassifier.isRelevant("Trasporti, gli scioperi di dicembre: quando si fermano treni, bus e aerei", City.MILAN))
        assertTrue(DisruptionClassifier.isRelevant("GDL kündigt bundesweiten Streik an", City.FRANKFURT))
        // "tramite" (Italian for "via") must not look like a tram.
        assertTrue(DisruptionClassifier.classify("Biglietti acquistabili tramite app a Roma", City.ROME).first.isEmpty())
    }

    @Test fun generalStrikeOnFlorenceDayAvoidsTramAndBus() {
        val strike = Disruption("Sciopero generale del 4 dicembre", "Test", "", 0, setOf(Mode.TRAIN), Severity.WARNING, true)
        val opts = Planner.plan(p("h_florence"), p("s_firenze_smn"), RoadRoute(2.8, 35, 5.0, 5.0),
            RoadRoute(3.1, 10, 5.0, 5.0), rainy, listOf(strike))
        assertEquals(Mode.TAXI, opts.first { it.best }.mode)
    }

    @Test fun parsesRealGoogleNewsRss() {
        val list = Api.parseNewsRss(res("gnews_frankfurt.xml"), City.FRANKFURT, DisruptionClassifier.tripDates(City.FRANKFURT))
        assertTrue("expected transport-relevant items", list.isNotEmpty())
        list.forEach {
            assertTrue(it.title.isNotBlank()); assertTrue(it.link.startsWith("https://"))
            assertTrue(it.publishedMillis > 0); assertFalse(it.title.endsWith(" - ${it.source}"))
        }
    }

    @Test fun parsesRealWikipediaGeosearch() {
        val list = Api.parseWiki(res("wiki_colosseum.json"), 41.8902, 12.4922)
        assertTrue(list.any { it.title == "Colosseum" })
        assertNotNull(list.first().distanceKm)
        assertTrue(list.zipWithNext().all { (a, b) -> a.distanceKm!! <= b.distanceKm!! })
    }

    @Test fun weatherAdviceFlagsRainAndLongWalk() {
        val advice = WeatherAdvice.advise(rainy, 3.0)
        assertTrue(advice.any { "rain" in it.lowercase() })
        assertTrue(advice.any { "not recommended" in it })
        assertTrue(WeatherAdvice.advise(dry, 1.0).none { "not recommended" in it })
    }

    @Test fun parsesOpenMeteoHour() {
        val json = """{"timezone":"Europe/Rome","hourly":{"time":["2026-12-02T08:00","2026-12-02T09:00"],
            "temperature_2m":[7.1,8.4],"apparent_temperature":[4.0,5.2],"precipitation_probability":[20,70],
            "precipitation":[0.0,1.2],"weather_code":[3,61],"wind_speed_10m":[9.0,14.5],"is_day":[1,1]}}"""
        val rome = java.time.ZoneId.of("Europe/Rome")
        val w = Api.parseWeather(json, java.time.ZonedDateTime.of(2026, 12, 2, 9, 30, 0, 0, rome))!!
        assertEquals(70, w.precipProbability)
        assertEquals("Light rain", w.description)
        // Phone still on Singapore time: 16:30 SGT is 09:30 in Rome, so the 09:00 Rome hour is used.
        val sgt = java.time.ZonedDateTime.of(2026, 12, 2, 16, 30, 0, 0, java.time.ZoneId.of("Asia/Singapore"))
        val w2 = Api.parseWeather(json, sgt)!!
        assertEquals(70, w2.precipProbability)
        assertTrue(w2.timeLabel.contains("09:00"))
        assertTrue(w2.timeLabel.contains("local time there"))
    }
}
