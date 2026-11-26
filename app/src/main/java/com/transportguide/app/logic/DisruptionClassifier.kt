package com.transportguide.app.logic

import com.transportguide.app.data.City

/**
 * Decides which transport modes a strike/protest headline affects.
 * Keywords cover Italian, German and English news.
 */
object DisruptionClassifier {

    private val trainWords = listOf(
        "treno", "treni", "ferrovi", "trenitalia", "italo", "trenord", "frecciarossa", "fs ", "rfi",
        "bahn", "zug", "züge", "gdl", "evg", "s-bahn", "fernverkehr", "deutsche bahn", "db ",
        "railway", "rail ", "train",
    )
    private val localTransitWords = listOf(
        "metro", "metropolitana", " bus", "autobus", "tram ", "tram,", "tram:", "tramvia", "straßenbahn",
        "atac", "cotral", "atm ", "atm,", "autolinee toscane", "mezzi pubblici", "trasporto pubblico", " tpl", "trasporti",
        "u-bahn", "vgf", "rmv", "öpnv", "nahverkehr", "ver.di", "verdi-streik",
        "public transport", "subway",
    )
    private val taxiWords = listOf("taxi", "tassisti", "ncc", "uber")
    private val waterWords = listOf("vaporetto", "vaporetti", "actv", "motoscafi", "lancioni", "gondol", "water taxi", "traghett")
    private val strikeWords = listOf("sciopero", "scioperi", "streik", "warnstreik", "strike", "agitazione", "fermo")
    private val protestWords = listOf(
        "corteo", "manifestazione", "presidio", "protesta", "proteste", "blocco", "blocchi",
        " demo ", " demos", "demo-", "demonstration", "kundgebung", "protest", " march ", "rally", "chiusura strade", "sperrung",
    )
    private val generalStrike = listOf("sciopero generale", "generalstreik", "general strike")
    /** Walk-outs by pupils or climate groups are marches, not transport strikes. */
    private val notTransportStrike = listOf("schulstreik", "jugendstreik", "klimastreik", "bildungsstreik", "school strike", "studenti")

    /** Words that tie a headline to the city, or make it nationwide. Headlines with neither are dropped. */
    private val localTerms = mapOf(
        City.ROME to listOf("roma", "rome", "atac", "cotral", "fiumicino", "lazio"),
        City.FLORENCE to listOf("firenze", "florence", "tramvia", "toscana", "tuscany", "autolinee toscane", "pisa"),
        City.PISA to listOf("pisa", "toscana", "tuscany"),
        City.VENICE to listOf("venezia", "venice", "actv", "avm", "mestre", "veneto", "vaporett"),
        City.MILAN to listOf("milano", "milan", "atm", "trenord", "lombardia", "malpensa", "linate"),
        City.FRANKFURT to listOf("frankfurt", "rmv", "vgf", "hessen", "rhein-main", "offenbach"),
        City.HEIDELBERG to listOf("heidelberg", "rnv", "baden-württemberg", "mannheim"),
    )
    private val nationalTerms = mapOf(
        "IT" to listOf("generale", "nazionale", "italia", "trenitalia", "italo", "ferrovie", "fs ", "rfi", "scioperi di dicembre", "treni"),
        "DE" to listOf("bundesweit", "deutschlandweit", "deutsche bahn", " db ", "db-", "gdl", "evg", "generalstreik", "fernverkehr"),
    )

    fun isRelevant(title: String, city: City): Boolean {
        val t = " " + title.lowercase() + " "
        return localTerms[city].orEmpty().any { it in t } || nationalTerms[city.country].orEmpty().any { it in t }
    }

    fun classify(title: String, city: City): Pair<Set<Mode>, Severity> {
        val t = " " + title.lowercase() + " "
        val modes = mutableSetOf<Mode>()
        val isStrike = strikeWords.any { it in t } && notTransportStrike.none { it in t }
        val isProtest = protestWords.any { it in t } || notTransportStrike.any { it in t }

        if (trainWords.any { it in t }) modes += Mode.TRAIN
        if (localTransitWords.any { it in t }) modes += Mode.TRAIN
        if (taxiWords.any { it in t }) modes += Mode.TAXI
        if (waterWords.any { it in t }) {
            modes += Mode.WATER_TAXI
            if ("actv" in t) modes += Mode.TRAIN // ACTV also runs buses / People Mover
        }
        // Taxis rarely join a general strike, so they stay the fallback; trains, buses and vaporetti do stop.
        if (generalStrike.any { it in t }) {
            modes += Mode.TRAIN
            if (city == City.VENICE) modes += Mode.WATER_TAXI
        }

        // A march or road block slows taxis and buses and closes streets to pedestrians.
        if (isProtest && modes.isEmpty()) {
            modes += setOf(Mode.TAXI, Mode.TRAIN, Mode.WALK)
            if (city == City.VENICE) modes += Mode.WATER_TAXI
        }

        val severity = when {
            isStrike && modes.isNotEmpty() -> Severity.WARNING
            isProtest -> Severity.WATCH
            modes.isNotEmpty() -> Severity.WATCH
            else -> Severity.INFO
        }
        return modes to severity
    }

    /** Headlines are checked for trip dates, e.g. "2 dicembre", "9. Dezember", "Dec 9", "30/11". */
    fun mentionsAnyDate(title: String, dates: List<Pair<Int, Int>>): Boolean {
        val t = title.lowercase()
        return dates.any { (d, m) ->
            val names = MONTHS[m] ?: return@any false
            Regex("""\b$d\.?\s*($names)\b""").containsMatchIn(t) ||
                Regex("""\b($names)\.?\s*$d\b""").containsMatchIn(t) ||
                Regex("""\b0?$d[./]0?$m\b""").containsMatchIn(t)
        }
    }

    private val MONTHS = mapOf(
        11 to "novembre|november|nov",
        12 to "dicembre|dezember|december|dec|dez",
    )

    /** Days (day, month) the family is in each city, from the Hotel Stays sheet. */
    fun tripDates(city: City): List<Pair<Int, Int>> = when (city) {
        City.ROME -> listOf(29 to 11, 30 to 11, 1 to 12, 2 to 12)
        City.FLORENCE -> (2..5).map { it to 12 }
        City.PISA -> listOf(3 to 12)
        City.VENICE -> (5..7).map { it to 12 }
        City.MILAN -> (7..9).map { it to 12 }
        City.FRANKFURT -> (9..12).map { it to 12 }
        City.HEIDELBERG -> listOf(11 to 12)
        City.OTHER -> emptyList()
    }

    /** Search queries per city, local language first. */
    fun queries(city: City): List<Pair<String, String>> = when (city.country) {
        "IT" -> listOf(
            "it" to "${italianName(city)} sciopero trasporti",
            "it" to "${italianName(city)} sciopero taxi OR treni OR metro OR bus",
            "it" to "${italianName(city)} corteo OR manifestazione OR protesta",
            "en" to "${city.label} strike OR protest transport",
        ) + if (city == City.VENICE) listOf("it" to "Venezia ACTV vaporetti sciopero") else emptyList()
        "DE" -> listOf(
            "de" to "${city.label} Streik Bahn OR S-Bahn OR U-Bahn OR ÖPNV",
            "de" to "${city.label} Demonstration OR Kundgebung OR Sperrung",
            "de" to "${city.label} Taxi Streik",
            "en" to "${city.label} strike OR protest transport",
        )
        else -> emptyList()
    }

    fun italianName(city: City) = when (city) {
        City.ROME -> "Roma"
        City.FLORENCE -> "Firenze"
        City.VENICE -> "Venezia"
        City.MILAN -> "Milano"
        else -> city.label
    }
}
