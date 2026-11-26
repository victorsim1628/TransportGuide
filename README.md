# Transport Guide — Europe Trip 2026 (Android)

An Android app (Kotlin + Jetpack Compose) for getting around Rome, Florence/Pisa,
Venice, Milan and Frankfurt/Heidelberg, 29 Nov – 12 Dec 2026. All hotels,
stations, airports and attractions come from `Hotel_Flight_Attraction_v2.xlsx`.

## What it does

| Requirement | How the app does it |
|---|---|
| **(a) Best route** by walking, train, taxi or water-taxi (the water option only appears for trips starting or ending in Venice) | Builds all four options, scores them on time, weather, luggage and disruptions, and marks one **★ BEST**. Unsuitable modes are greyed out with the reason (e.g. no road taxis to St Mark's, water-taxi only in the Venice lagoon). |
| **(b) Distance in km only** | Every distance goes through `Geo.km()`, so the app never shows metres or miles. Walking and taxi distances are real street routes (OpenStreetMap / OSRM). Booked train legs use the rail km from the spreadsheet. |
| **(c) Weather & advice before every trip** | Each "Find best route" fetches the Open-Meteo forecast for the departure hour and gives advice on umbrellas, coats, wind on the lagoon, fog and darkness. Rain and cold also count against walking. |
| **(d) Strikes & protests** in Rome, Florence, Venice, Milan, Frankfurt | Searches news in Italian or German and English (last 14 days) for strikes (*sciopero*, *Streik*) and marches (*corteo*, *Demo*). Each headline is tagged with the modes it affects. Headlines that mention a date you're in that city get 🔴 **ON YOUR DATES**. Matching modes are penalised in the ranking. Buttons link to the official Italian strike calendar (MIT), DB service alerts and ACTV news. |
| **(e) Dropdowns, no typing** | *From* and *To* are dropdowns grouped by city: 🏨 hotels, 🚉 stations, ✈️ airports, 🏛️ attractions, with booking notes such as times and PNRs. You can type to filter. |
| **(f) Box for new places** | *Add a new place*: type a name or address, or paste `lat, lon`, then save it as *From* or *To*. Saved places appear in the dropdowns under 📍 *My places* and can be deleted. |
| **(g) Suggestions at the destination** | Hand-picked sights for each city, plus live Wikipedia landmarks within 1.5 km of the destination, each with its distance in km. Tapping one opens Google Maps directions from the Destination Point to it (walking up to 2 km, public transport beyond); the ⓘ icon opens its Wikipedia article. |

Booked trains (Frecciarossa 8508, 9414, 9728, Regionali to Pisa, EC 150) are recognised: choosing
e.g. *Roma Termini → Firenze S. M. Novella* shows the train, time, coach, seats and PNR.

Each option has *Open step-by-step directions in Google Maps*, for live transit times.

## Data sources (free, no API keys)

| Purpose | Service |
|---|---|
| Walking & driving routes | `routing.openstreetmap.de` (OSRM, © OpenStreetMap contributors) |
| Weather | `api.open-meteo.com` (16-day hourly forecast) |
| Strikes & protests | `news.google.com` RSS search |
| New-place search | `nominatim.openstreetmap.org` |
| Nearby landmarks | `en.wikipedia.org` geosearch |
| Italian / German headlines → British English | Google ML Kit on-device translation (language packs download once, ~30 MB each, then work offline) |

Strike detection reads news headlines, so it can miss things or raise false alarms. Before travel
days, always check the official notices the app links to.

## Build

Requires JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0).

```bash
./gradlew assembleDebug      # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # logic + parser tests (use real API responses in app/src/test/resources)
```

Install on a phone: copy `app-debug.apk` to the device and open it (allow "Install unknown apps"),
or `adb install app/build/outputs/apk/debug/app-debug.apk`.

## Code map

```
app/src/main/java/com/transportguide/app/
  data/TripData.kt        places, booked trains and curated sights from the spreadsheet
  data/CustomPlaces.kt    saved "My places"
  logic/Planner.kt        builds and ranks the four mode options
  logic/WeatherAdvice.kt  forecast -> advice
  logic/DisruptionClassifier.kt  strike/protest relevance, modes affected, trip-date match
  net/Api.kt              all network calls and response parsers
  ui/TripScreen.kt        Compose UI
```
