package com.transportguide.app.data

/**
 * Places and bookings taken from Hotel_Flight_Attraction_v2.xlsx
 * (sheets: Hotel Stays, Flight Details, Train Details, Attractions).
 * Coordinates are approximate building/entrance positions.
 */
object TripData {

    val places: List<Place> = listOf(
        // ---------------- Hotels ----------------
        Place("h_rome", "Little Queen Suite", City.ROME, Category.HOTEL, 41.89480, 12.47700,
            "Via Florida, 20, Rome, Lazio, 00186", "29 Nov – 2 Dec · 3 nights"),
        Place("h_florence", "B&B Hotel Firenze City Center", City.FLORENCE, Category.HOTEL, 43.76960, 11.26920,
            "Viale Giovanni Amendola, 34, Florence, 50121", "2 – 5 Dec · 3 nights"),
        Place("h_venice", "B&B HOTEL Venezia Laguna", City.VENICE, Category.HOTEL, 45.44050, 12.30630,
            "Isola Nova del Tronchetto, 16, Venice, 30135", "5 – 7 Dec · 2 nights"),
        Place("h_milan", "Palazzo Loreto Hotel Milano", City.MILAN, Category.HOTEL, 45.48740, 9.21640,
            "Viale Monza, 7, Milan, 20125", "7 – 9 Dec · 2 nights"),
        Place("h_frankfurt", "Flemings Selection Hotel Frankfurt", City.FRANKFURT, Category.HOTEL, 50.11700, 8.68170,
            "Eschenheimer Tor 2, 60318 Frankfurt", "9 – 12 Dec · 3 nights"),

        // ---------------- Train stations ----------------
        Place("s_roma_termini", "Roma Termini", City.ROME, Category.STATION, 41.90100, 12.50180,
            "Piazza dei Cinquecento, Rome", "Frecciarossa 8508 · 2 Dec 09:50 → Firenze SMN · PNR ED29EN",
            mapsName = "Roma Termini station, Rome"),
        Place("s_firenze_smn", "Firenze S. M. Novella", City.FLORENCE, Category.STATION, 43.77640, 11.24800,
            "Piazza della Stazione, Florence", "Regionale 18361 · 3 Dec 09:53 → Pisa · FR 9414 · 5 Dec 11:20 → Venezia",
            mapsName = "Firenze Santa Maria Novella station, Florence"),
        Place("s_pisa_srossore", "Pisa S. Rossore", City.PISA, Category.STATION, 43.72380, 10.39150,
            "Via Andrea Pisano, Pisa", "Regionale 18399 · 3 Dec 16:46 → Firenze SMN",
            mapsName = "Pisa San Rossore station, Pisa"),
        Place("s_venezia_slucia", "Venezia S. Lucia", City.VENICE, Category.STATION, 45.44100, 12.32100,
            "Fondamenta Santa Lucia, Venice", "Frecciarossa 9728 · 7 Dec 11:48 → Milano Centrale · PNR EFCYKN",
            mapsName = "Venezia Santa Lucia station, Venice"),
        Place("s_milano_centrale", "Milano Centrale", City.MILAN, Category.STATION, 45.48620, 9.20470,
            "Piazza Duca d'Aosta, Milan", "EuroCity EC 150 · 9 Dec 11:10 → Frankfurt Hbf · Order 655370800984",
            mapsName = "Milano Centrale station, Milan"),
        Place("s_frankfurt_hbf", "Frankfurt (Main) Hbf", City.FRANKFURT, Category.STATION, 50.10710, 8.66380,
            "Am Hauptbahnhof, Frankfurt", "EC 150 arrives 9 Dec 18:46, platform 4",
            mapsName = "Frankfurt (Main) Hauptbahnhof, Frankfurt"),
        Place("s_heidelberg_hbf", "Heidelberg Hbf", City.HEIDELBERG, Category.STATION, 49.40360, 8.67570,
            "Willy-Brandt-Platz, Heidelberg", "Day trip 11 Dec",
            mapsName = "Heidelberg Hauptbahnhof, Heidelberg"),

        // ---------------- Airports ----------------
        Place("a_fco", "Rome Fiumicino Airport (FCO) T3", City.ROME, Category.AIRPORT, 41.80030, 12.23890,
            "Fiumicino", "EY 85 lands 29 Nov 05:40",
            mapsName = "Fiumicino Airport Terminal 3, Rome"),
        Place("a_fra", "Frankfurt Airport (FRA) T3", City.FRANKFURT, Category.AIRPORT, 50.02980, 8.58200,
            "Frankfurt am Main", "WY 118 departs 12 Dec 20:30 from Terminal 3",
            mapsName = "Frankfurt Airport Terminal 3, Frankfurt"),

        // ---------------- Transport hubs ----------------
        Place("t_piazzale_roma", "Piazzale Roma", City.VENICE, Category.TRANSIT, 45.43810, 12.31840,
            "Piazzale Roma, Venice", "Bus / taxi terminus, People Mover to Tronchetto",
            mapsName = "Piazzale Roma, Venice"),

        // ---------------- Attractions ----------------
        Place("x_colosseum", "Colosseum / Forum-Palatine", City.ROME, Category.ATTRACTION, 41.89020, 12.49220,
            "Piazza del Colosseo, Rome", "Mon 30 Nov 12:00 · Booking OCO5098278",
            mapsName = "Colosseum, Rome"),
        Place("x_pantheon", "Pantheon", City.ROME, Category.ATTRACTION, 41.89860, 12.47690,
            "Piazza della Rotonda, Rome", "Tue 1 Dec · not booked yet"),
        Place("x_trevi", "Trevi Fountain", City.ROME, Category.ATTRACTION, 41.90090, 12.48330,
            "Piazza di Trevi, Rome", "Tue 1 Dec · no ticket needed"),
        Place("x_duomo_fi", "Duomo & Brunelleschi's Dome", City.FLORENCE, Category.ATTRACTION, 43.77310, 11.25600,
            "Piazza del Duomo, Florence", "Wed 2 Dec · Dome climb 16:30 sharp · bags to Piazza Duomo 38/r",
            mapsName = "Cattedrale di Santa Maria del Fiore, Florence"),
        Place("x_pisa_tower", "Leaning Tower of Pisa", City.PISA, Category.ATTRACTION, 43.72300, 10.39660,
            "Piazza del Duomo, Pisa", "Thu 3 Dec 12:00 · Ticket 2337627158-01",
            mapsName = "Leaning Tower of Pisa, Pisa"),
        Place("x_accademia", "David — Galleria dell'Accademia", City.FLORENCE, Category.ATTRACTION, 43.77680, 11.25860,
            "Via Ricasoli, 58/60, Florence", "Fri 4 Dec 10:30 · Order 24469879",
            mapsName = "Galleria dell'Accademia, Florence"),
        Place("x_rialto", "Rialto Bridge & Grand Canal", City.VENICE, Category.ATTRACTION, 45.43800, 12.33590,
            "Rialto, Venice", "Sat 5 Dec · free",
            mapsName = "Rialto Bridge, Venice"),
        Place("x_san_marco", "St Mark's Basilica & Square", City.VENICE, Category.ATTRACTION, 45.43450, 12.33970,
            "Piazza San Marco, Venice", "Sun 6 Dec · not booked yet",
            mapsName = "St Mark's Basilica, Venice"),
        Place("x_last_supper", "The Last Supper (Santa Maria delle Grazie)", City.MILAN, Category.ATTRACTION, 45.46600, 9.17100,
            "Piazza di Santa Maria delle Grazie, 2, Milan", "Tue 8 Dec · not booked yet",
            mapsName = "Cenacolo Vinciano, Milan"),
        Place("x_romerberg", "Römerberg Christmas Market", City.FRANKFURT, Category.ATTRACTION, 50.11060, 8.68210,
            "Römerberg, Frankfurt", "Wed 9 Dec · free",
            mapsName = "Römerberg, Frankfurt"),
        Place("x_stadel", "Städel Museum", City.FRANKFURT, Category.ATTRACTION, 50.10310, 8.67410,
            "Schaumainkai 63, Frankfurt", "Thu 10 Dec · not booked yet"),
        Place("x_main_tower", "Main Tower", City.FRANKFURT, Category.ATTRACTION, 50.11250, 8.67200,
            "Neue Mainzer Str. 52-58, Frankfurt", "Thu 10 Dec"),
        Place("x_heidelberg_castle", "Heidelberg Castle", City.HEIDELBERG, Category.ATTRACTION, 49.41060, 8.71530,
            "Schlosshof 1, Heidelberg", "Fri 11 Dec · day trip",
            mapsName = "Heidelberg Castle, Heidelberg"),
    )

    /** Hand-picked "also worth seeing" ideas per city, used alongside live Wikipedia results. */
    val curated: Map<City, List<Suggestion>> = mapOf(
        City.ROME to listOf(
            Suggestion("Piazza Navona", "Baroque square with Bernini's Fountain of the Four Rivers", 41.8992, 12.4731),
            Suggestion("Spanish Steps", "135 steps up to Trinità dei Monti; great at dusk", 41.9060, 12.4828),
            Suggestion("Largo di Torre Argentina", "Ruins where Caesar was killed, now a cat sanctuary (by your hotel)", 41.8955, 12.4768),
            Suggestion("Altare della Patria", "Vittoriano monument with a rooftop lift and panoramic terrace", 41.8946, 12.4831),
            Suggestion("Campo de' Fiori", "Morning market square, lively in the evening", 41.8956, 12.4722),
            Suggestion("St Peter's Basilica", "Vatican basilica and dome climb", 41.9022, 12.4539),
            Suggestion("Trastevere", "Cobbled lanes and trattorias across the Tiber", 41.8894, 12.4700),
        ),
        City.FLORENCE to listOf(
            Suggestion("Ponte Vecchio", "Medieval bridge lined with goldsmiths", 43.7680, 11.2531),
            Suggestion("Piazza della Signoria & Palazzo Vecchio", "Open-air sculpture gallery and town hall", 43.7696, 11.2558, mapsName = "Piazza della Signoria, Florence"),
            Suggestion("Uffizi Gallery", "Botticelli, Leonardo, Caravaggio", 43.7678, 11.2553),
            Suggestion("Piazzale Michelangelo", "Best panorama of the city at sunset", 43.7629, 11.2650),
            Suggestion("Basilica di Santa Croce", "Tombs of Michelangelo and Galileo (near your hotel)", 43.7686, 11.2622),
            Suggestion("Mercato Centrale", "Food hall for lunch near San Lorenzo", 43.7764, 11.2532),
        ),
        City.PISA to listOf(
            Suggestion("Pisa Cathedral & Baptistery", "Piazza dei Miracoli ensemble", 43.7233, 10.3958, mapsName = "Pisa Cathedral, Pisa"),
            Suggestion("Camposanto Monumentale", "Monumental cemetery with frescoes", 43.7240, 10.3947),
            Suggestion("Piazza dei Cavalieri", "Renaissance square of the Scuola Normale", 43.7194, 10.4004),
        ),
        City.VENICE to listOf(
            Suggestion("Doge's Palace", "Gothic palace and the Bridge of Sighs", 45.4337, 12.3404),
            Suggestion("Campanile di San Marco", "Bell tower with lagoon views (lift)", 45.4341, 12.3390),
            Suggestion("Gallerie dell'Accademia", "Venetian painting: Titian, Tintoretto", 45.4312, 12.3283),
            Suggestion("Santa Maria della Salute", "Baroque basilica at the mouth of the Grand Canal", 45.4307, 12.3346),
            Suggestion("Libreria Acqua Alta", "Quirky bookshop with gondola shelves", 45.4377, 12.3428),
            Suggestion("Burano", "Colourful fishing island (vaporetto 12)", 45.4853, 12.4166),
        ),
        City.MILAN to listOf(
            Suggestion("Duomo di Milano & rooftop", "Gothic cathedral — walk the roof terraces", 45.4641, 9.1919, mapsName = "Duomo di Milano, Milan"),
            Suggestion("Galleria Vittorio Emanuele II", "19th-century glass-roofed arcade", 45.4659, 9.1900),
            Suggestion("Teatro alla Scala", "World-famous opera house and museum", 45.4674, 9.1895),
            Suggestion("Castello Sforzesco", "Castle with Michelangelo's Rondanini Pietà", 45.4705, 9.1795),
            Suggestion("Navigli canals", "Canal-side bars for aperitivo", 45.4520, 9.1760, mapsName = "Naviglio Grande, Milan"),
            Suggestion("Brera district & Pinacoteca", "Art gallery and charming streets", 45.4719, 9.1880, mapsName = "Pinacoteca di Brera, Milan"),
        ),
        City.FRANKFURT to listOf(
            Suggestion("Kaiserdom St. Bartholomäus", "Imperial cathedral with tower climb", 50.1107, 8.6852),
            Suggestion("Eiserner Steg", "Iron footbridge with skyline views", 50.1083, 8.6827),
            Suggestion("Alte Oper", "Rebuilt 1880 opera house", 50.1158, 8.6719),
            Suggestion("Kleinmarkthalle", "Indoor food market for lunch", 50.1129, 8.6837),
            Suggestion("Goethe House", "Birthplace of Johann Wolfgang von Goethe", 50.1110, 8.6776),
            Suggestion("Sachsenhausen", "Apple-wine taverns south of the river", 50.1035, 8.6880),
        ),
        City.HEIDELBERG to listOf(
            Suggestion("Old Bridge (Alte Brücke)", "18th-century bridge with the bridge monkey", 49.4136, 8.7105, mapsName = "Alte Brücke, Heidelberg"),
            Suggestion("Philosophenweg", "Hillside walk with castle views", 49.4165, 8.7030),
            Suggestion("Hauptstraße", "One of Europe's longest pedestrian streets", 49.4111, 8.7040),
        ),
    )

    /** Curated sights for a city; each gets a Google Maps name ("Title, City" unless overridden). */
    fun curatedFor(city: City): List<Suggestion> =
        curated[city].orEmpty().map { it.copy(mapsName = it.mapsName ?: "${it.title}, ${city.label}") }

    /** Train legs booked in the spreadsheet (Train Details sheet). */
    val bookedTrains: List<BookedTrain> = listOf(
        BookedTrain("2 Dec", "Frecciarossa 8508", "s_roma_termini", "s_firenze_smn", "09:50", "11:27", 261.0, "Coach 6 · 11A, 11B, 12A, 12B · PNR ED29EN"),
        BookedTrain("3 Dec", "Regionale 18361", "s_firenze_smn", "s_pisa_srossore", "09:53", "11:11", 80.0, "No reserved seats · Receipt 2042368582"),
        BookedTrain("3 Dec", "Regionale 18399", "s_pisa_srossore", "s_firenze_smn", "16:46", "18:08", 80.0, "No reserved seats · Receipt 2042368582"),
        BookedTrain("5 Dec", "Frecciarossa 9414", "s_firenze_smn", "s_venezia_slucia", "11:20", "13:36", 257.0, "Coach 6 · 15C, 15D, 16C, 16D · PNR EE2YU5"),
        BookedTrain("7 Dec", "Frecciarossa 9728", "s_venezia_slucia", "s_milano_centrale", "11:48", "14:15", 278.0, "Coach 4 · 5A, 5B, 6A, 6B · PNR EFCYKN"),
        BookedTrain("9 Dec", "EuroCity EC 150", "s_milano_centrale", "s_frankfurt_hbf", "11:10", "18:46", 675.0, "Coach 2 · seats 11, 12, 13, 18 · Order 655370800984"),
    )

    fun byId(id: String): Place? = places.firstOrNull { it.id == id }
}

data class Suggestion(
    val title: String,
    val blurb: String,
    val lat: Double,
    val lon: Double,
    val distanceKm: Double? = null,
    val url: String? = null,
    /** Name for Google Maps; null (e.g. Wikipedia results) means use exact coordinates. */
    val mapsName: String? = null,
)

data class BookedTrain(
    val date: String,
    val train: String,
    val fromId: String,
    val toId: String,
    val departs: String,
    val arrives: String,
    val km: Double,
    val details: String,
)
