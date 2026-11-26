package com.transportguide.app.logic

/** Converts common American spellings, as produced by machine translation, to British English. */
object BritishEnglish {
    private val words = mapOf(
        "center" to "centre", "centers" to "centres", "centered" to "centred",
        "theater" to "theatre", "theaters" to "theatres",
        "color" to "colour", "colors" to "colours", "colorful" to "colourful",
        "labor" to "labour", "neighbor" to "neighbour", "neighborhood" to "neighbourhood",
        "neighborhoods" to "neighbourhoods", "favorite" to "favourite", "honor" to "honour",
        "canceled" to "cancelled", "canceling" to "cancelling", "traveled" to "travelled",
        "traveler" to "traveller", "travelers" to "travellers", "traveling" to "travelling",
        "organize" to "organise", "organized" to "organised", "organizes" to "organises",
        "organizing" to "organising", "organization" to "organisation", "organizations" to "organisations",
        "organizer" to "organiser", "organizers" to "organisers",
        "mobilize" to "mobilise", "mobilized" to "mobilised", "mobilization" to "mobilisation",
        "authorize" to "authorise", "authorized" to "authorised", "authorization" to "authorisation",
        "apologize" to "apologise", "realize" to "realise", "recognize" to "recognise",
        "criticize" to "criticise", "criticized" to "criticised", "program" to "programme",
        "license" to "licence", "defense" to "defence", "offense" to "offence",
        "gray" to "grey", "subway" to "metro", "subways" to "metros",
        "railroad" to "railway", "railroads" to "railways",
        "sidewalk" to "pavement", "sidewalks" to "pavements", "downtown" to "city centre",
    )
    private val pattern = Regex("""\b(${words.keys.sortedByDescending { it.length }.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

    fun convert(text: String): String = pattern.replace(text) { m ->
        val found = m.value
        val british = words[found.lowercase()] ?: return@replace found
        when {
            found.all { !it.isLetter() || it.isUpperCase() } -> british.uppercase()
            found[0].isUpperCase() -> british.replaceFirstChar { it.uppercase() }
            else -> british
        }
    }
}
