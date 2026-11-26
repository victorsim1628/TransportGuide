package com.transportguide.app.data

enum class Category(val label: String) {
    HOTEL("Hotel"),
    STATION("Train station"),
    AIRPORT("Airport"),
    ATTRACTION("Attraction"),
    TRANSIT("Transport hub"),
    CUSTOM("My places"),
}

enum class City(val label: String, val lat: Double, val lon: Double, val country: String) {
    ROME("Rome", 41.8967, 12.4822, "IT"),
    FLORENCE("Florence", 43.7696, 11.2558, "IT"),
    PISA("Pisa", 43.7228, 10.4017, "IT"),
    VENICE("Venice", 45.4408, 12.3155, "IT"),
    MILAN("Milan", 45.4642, 9.1900, "IT"),
    FRANKFURT("Frankfurt", 50.1109, 8.6821, "DE"),
    HEIDELBERG("Heidelberg", 49.3988, 8.6724, "DE"),
    OTHER("Other", 0.0, 0.0, ""),
}

data class Place(
    val id: String,
    val name: String,
    val city: City,
    val category: Category,
    val lat: Double,
    val lon: Double,
    val address: String = "",
    /** Booking or ticket details copied from the trip spreadsheet. */
    val note: String = "",
) {
    val display: String get() = "$name · ${city.label}"
}
