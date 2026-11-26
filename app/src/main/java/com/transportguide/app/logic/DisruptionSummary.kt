package com.transportguide.app.logic

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One strike/protest event (or one day's news), with its two most reliable reports. */
data class DisruptionGroup(
    /** Date the event happens, when a headline names one. */
    val eventDate: LocalDate?,
    /** Date of the newest report, used when no event date is named. */
    val reportedDate: LocalDate?,
    /** Report from the most reliable outlet; its title is the summary line. */
    val headline: Disruption,
    /** Up to two reports, most reliable first. */
    val sources: List<Disruption>,
    val modes: Set<Mode>,
    val severity: Severity,
    val mentionsTripDate: Boolean,
    /** How many reports were merged into this entry. */
    val reportCount: Int,
)

/**
 * Collapses many headlines about the same day into one entry and keeps the two most
 * reliable sources, so the strike card does not repeat the same news.
 */
object DisruptionSummary {

    /** Higher is more reliable: official operators and agencies, then national and local papers. */
    private val reliability: List<Pair<Int, List<String>>> = listOf(
        10 to listOf("trenitalia", "ferrovie dello stato", "mit.gov", "ministero", "deutsche bahn", "bahn.de",
            "atac", "atm milano", "actv", "avm", "rmv", "vgf", "autolinee toscane", "commissione di garanzia"),
        9 to listOf("ansa", "reuters", "associated press", "ap news", "dpa", "tagesschau", "rai news", "rainews"),
        8 to listOf("corriere della sera", "corriere.it", "la repubblica", "repubblica", "il sole 24 ore",
            "frankfurter allgemeine", "faz", "hessenschau", "hr-inforadio", "süddeutsche", "spiegel", "zeit online",
            "bbc", "the guardian", "financial times"),
        7 to listOf("il post", "sky tg24", "tgcom24", "il messaggero", "la nazione", "corriere fiorentino",
            "il gazzettino", "la nuova venezia", "il giorno", "corriere milano", "milanotoday", "romatoday",
            "firenzetoday", "veneziatoday", "frankfurter rundschau", "fr.de", "frankfurter neue presse",
            "the local", "wanted in rome", "euronews"),
    )

    fun reliabilityOf(source: String): Int {
        val s = source.lowercase()
        return reliability.firstOrNull { (_, names) -> names.any { it in s } }?.first ?: 0
    }

    private val months = mapOf(
        "gennaio" to 1, "januar" to 1, "january" to 1, "jan" to 1,
        "febbraio" to 2, "februar" to 2, "february" to 2, "feb" to 2,
        "marzo" to 3, "märz" to 3, "march" to 3, "mar" to 3,
        "aprile" to 4, "april" to 4, "apr" to 4,
        "maggio" to 5, "mai" to 5, "may" to 5,
        "giugno" to 6, "juni" to 6, "june" to 6, "jun" to 6,
        "luglio" to 7, "juli" to 7, "july" to 7, "jul" to 7,
        "agosto" to 8, "august" to 8, "aug" to 8,
        "settembre" to 9, "september" to 9, "sept" to 9, "sep" to 9,
        "ottobre" to 10, "oktober" to 10, "october" to 10, "okt" to 10, "oct" to 10,
        "novembre" to 11, "november" to 11, "nov" to 11,
        "dicembre" to 12, "dezember" to 12, "december" to 12, "dez" to 12, "dec" to 12,
    )
    private val monthAlt = months.keys.sortedByDescending { it.length }.joinToString("|")
    private val dayMonth = Regex("""\b([0-3]?\d)\.?\s+($monthAlt)\b""")
    private val monthDay = Regex("""\b($monthAlt)\.?\s+([0-3]?\d)\b""")
    private val numeric = Regex("""\b([0-3]?\d)[./]([01]?\d)(?:[./](\d{2,4}))?\b""")

    /** First calendar date a headline names, placed in the year nearest to [today]. */
    fun eventDate(title: String, today: LocalDate): LocalDate? {
        val t = title.lowercase()
        val (d, m) = dayMonth.find(t)?.let { it.groupValues[1].toInt() to months.getValue(it.groupValues[2]) }
            ?: monthDay.find(t)?.let { it.groupValues[2].toInt() to months.getValue(it.groupValues[1]) }
            ?: numeric.find(t)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
            ?: return null
        return listOf(today.year - 1, today.year, today.year + 1)
            .mapNotNull { y -> runCatching { LocalDate.of(y, m, d) }.getOrNull() }
            .minByOrNull { kotlin.math.abs(it.toEpochDay() - today.toEpochDay()) }
    }

    fun summarise(items: List<Disruption>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<DisruptionGroup> {
        fun reported(d: Disruption) =
            if (d.publishedMillis > 0) Instant.ofEpochMilli(d.publishedMillis).atZone(zone).toLocalDate() else null

        return items
            .map { it to eventDate(it.originalTitle ?: it.title, today) }
            // Events that are already over are no longer useful.
            .filter { (_, date) -> date == null || !date.isBefore(today) }
            .groupBy { (d, date) -> date?.let { "E$it" } ?: "R${reported(d)}:${d.severity}" }
            .map { (_, pairs) ->
                val reports = pairs.map { it.first }.sortedWith(
                    compareByDescending<Disruption> { reliabilityOf(it.source) }
                        .thenByDescending { it.severity.ordinal }
                        .thenByDescending { it.publishedMillis })
                val sources = reports.distinctBy { it.source.lowercase() }.take(2)
                DisruptionGroup(
                    eventDate = pairs.first().second,
                    reportedDate = reports.mapNotNull { reported(it) }.maxOrNull(),
                    headline = sources.first(),
                    sources = sources,
                    modes = reports.flatMap { it.modes }.toSet(),
                    severity = reports.maxOf { it.severity },
                    mentionsTripDate = reports.any { it.mentionsTripDate },
                    reportCount = reports.size,
                )
            }
            .sortedWith(
                compareByDescending<DisruptionGroup> { it.mentionsTripDate }
                    .thenBy { it.eventDate == null }
                    .thenBy { it.eventDate }
                    .thenByDescending { it.severity.ordinal }
                    .thenByDescending { it.reportedDate })
    }
}
