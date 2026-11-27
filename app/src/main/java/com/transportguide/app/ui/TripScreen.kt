package com.transportguide.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transportguide.app.data.Category
import com.transportguide.app.data.City
import com.transportguide.app.data.Place
import com.transportguide.app.data.Suggestion
import com.transportguide.app.logic.DisruptionGroup
import com.transportguide.app.logic.DisruptionSummary
import com.transportguide.app.logic.Geo
import com.transportguide.app.logic.MapsLinks
import com.transportguide.app.logic.Mode
import com.transportguide.app.logic.RouteOption
import com.transportguide.app.logic.Severity
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(vm: TripViewModel = viewModel()) {
    val base = LocalDensity.current
    // The chosen text size multiplies the phone's own font-size setting.
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * vm.textScale)) {
        TripContent(vm)
    }
}

private val TEXT_SIZES = listOf("Small" to 0.85f, "Normal" to 1.0f, "Large" to 1.15f, "Extra large" to 1.3f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripContent(vm: TripViewModel) {
    val ctx = LocalContext.current
    var sizeMenu by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text("Andiamo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Europe 2026", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { sizeMenu = true }) { Icon(Icons.Filled.FormatSize, "Text size") }
                    DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }) {
                        Text("Text size", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        TEXT_SIZES.forEach { (label, scale) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale) },
                                leadingIcon = {
                                    if (vm.textScale == scale) Icon(Icons.Filled.Check, null)
                                    else Spacer(Modifier.size(24.dp))
                                },
                                onClick = { vm.chooseTextScale(scale); sizeMenu = false },
                            )
                        }
                    }
                }
            },
        )
    }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PlacePicker("From (Originating Point)", vm.from, vm.allPlaces) { vm.chooseFrom(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = vm::swap) { Icon(Icons.Filled.SwapVert, "Swap") }
                }
                PlacePicker("To (Destination Point)", vm.to, vm.allPlaces) { vm.chooseTo(it) }
            }
            item { DepartureRow(vm) }
            item {
                Button(
                    onClick = vm::plan, enabled = vm.from != null && vm.to != null && !vm.loading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    if (vm.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Find best route · check weather & strikes")
                }
            }
            vm.result?.let { r ->
                item { WeatherCard(r) }
                item { DisruptionCard(r.disruptions, r.visitDate, ctx) }
                item { SectionTitle("Routes: ${r.from.name} → ${r.to.name}") }
                r.options.forEach { opt -> item { RouteCard(opt, r.from, r.to, ctx) } }
                item { SuggestionsCard(r.to, r.curated, r.nearby, ctx) }
            }
            item { AddPlaceCard(vm) }
            if (vm.customPlaces.isNotEmpty()) item { MyPlacesCard(vm) }
            item {
                Text(
                    "Routes © OpenStreetMap contributors (OSRM) · Weather: Open-Meteo · News: Google News · " +
                        "Landmarks: Wikipedia. Distances are shown in km only. Always confirm strikes with the official notices.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ pickers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlacePicker(label: String, selected: Place?, places: List<Place>, onPick: (Place) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val shown = places.filter {
        filter.isBlank() || it.name.contains(filter, true) || it.city.label.contains(filter, true) ||
            it.category.label.contains(filter, true)
    }.sortedWith(compareBy<Place>({ it.city.ordinal }, { it.category.ordinal }, { it.name }))

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (expanded) filter else selected?.let { "${icon(it.category)} ${it.display}" } ?: "",
            onValueChange = { filter = it; expanded = true },
            label = { Text(label) },
            placeholder = { Text("Tap to choose, or type to filter") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false; filter = "" }) {
            var lastCity: City? = null
            shown.forEach { p ->
                if (p.city != lastCity) {
                    lastCity = p.city
                    Text(
                        p.city.label.uppercase(), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
                    )
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${icon(p.category)} ${p.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val sub = p.note.ifBlank { p.address }
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    onClick = { onPick(p); expanded = false; filter = "" },
                )
            }
        }
    }
}

private fun icon(c: Category) = when (c) {
    Category.HOTEL -> "🏨"; Category.STATION -> "🚉"; Category.AIRPORT -> "✈️"
    Category.ATTRACTION -> "🏛️"; Category.TRANSIT -> "🚏"; Category.CUSTOM -> "📍"
}

@Composable
private fun DepartureRow(vm: TripViewModel) {
    val ctx = LocalContext.current
    val t = vm.departure
    val live = vm.customDeparture == null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(
                if (live) "Leaving now" else "Planned departure",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline,
            )
            Text(t.format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.UK)), fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = {
            DatePickerDialog(ctx, { _, y, m, d ->
                TimePickerDialog(ctx, { _, h, min ->
                    vm.chooseDeparture(java.time.LocalDateTime.of(y, m + 1, d, h, min))
                }, t.hour, t.minute, true).show()
            }, t.year, t.monthValue - 1, t.dayOfMonth).apply {
                // Way back to the live system clock (replaces the old "Now" button).
                setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Leave now") { _, _ -> vm.useCurrentTime() }
            }.show()
        }) { Icon(Icons.Filled.EditCalendar, "Change departure time", tint = MaterialTheme.colorScheme.primary) }
    }
}

// ------------------------------------------------------------------ results

@Composable
private fun SectionTitle(s: String) =
    Text(s, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
private fun WeatherCard(r: TripResult) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("Weather & advice · ${r.from.city.label}")
            val w = r.weather
            if (w == null) Text(r.weatherMissingReason ?: "No forecast.")
            else {
                Text("${w.timeLabel} — ${w.description}, ${w.temperatureC.toInt()}°C (feels ${w.apparentC.toInt()}°C)")
                Text("Rain ${w.precipProbability}% · ${"%.1f".format(w.precipitationMm)} mm · wind ${w.windKmh.toInt()} km/h",
                    style = MaterialTheme.typography.bodySmall)
                r.weatherAdvice.forEach { Text(it) }
            }
        }
    }
}

@Composable
private fun DisruptionCard(byCity: Map<City, List<DisruptionGroup>>, visit: java.time.LocalDate, ctx: Context) {
    val anyWarn = byCity.values.flatten().any { it.eventDate == visit }
    val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    Card(colors = CardDefaults.cardColors(
        containerColor = if (anyWarn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val last = visit.plusDays(DisruptionSummary.DAYS_AFTER_VISIT.toLong())
            SectionTitle("Strikes & protests · ${visit.format(dayFmt)} – ${last.format(dayFmt)}")
            Text("Your visit day and the 2 days after. One line per day, from the two most reliable sources.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            byCity.forEach { (city, groups) ->
                Text(city.label, fontWeight = FontWeight.SemiBold)
                if (groups.isEmpty()) Text("✅ No strikes or protests reported for these dates.", style = MaterialTheme.typography.bodySmall)
                groups.forEach { g ->
                    val onVisitDay = g.eventDate == visit
                    val kind = if (g.severity == Severity.WARNING) "Strike" else "Protest"
                    val badge = if (onVisitDay) "🔴 $kind on your visit day" else "🟠 $kind"
                    val day = g.eventDate?.format(dayFmt).orEmpty()
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("$badge · $day", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(g.headline.title, style = MaterialTheme.typography.bodyMedium)
                        Text("Affects: ${g.modes.joinToString { it.label }}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            g.sources.forEach { src ->
                                Text("${src.source.ifBlank { "Source" }} ↗",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { open(ctx, src.link) }.padding(vertical = 4.dp))
                            }
                        }
                        if (g.reportCount > g.sources.size) Text(
                            "+${g.reportCount - g.sources.size} similar reports merged",
                            style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (city.country == "IT") AssistChip(onClick = { open(ctx, "https://scioperi.mit.gov.it/mit2/public/scioperi") },
                        label = { Text("Official IT strike calendar") })
                    if (city.country == "DE") AssistChip(onClick = { open(ctx, "https://www.bahn.de/service/fahrplaene/aktuell") },
                        label = { Text("DB service alerts") })
                    if (city == City.VENICE) AssistChip(onClick = { open(ctx, "https://actv.avmspa.it/en/content/news") },
                        label = { Text("ACTV news") })
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RouteCard(o: RouteOption, from: Place, to: Place, ctx: Context) {
    val colors = when {
        o.best -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        !o.available -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else -> CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${o.mode.emoji}  ${o.mode.label}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (o.best) Text("★ BEST", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                if (!o.available) Text("Not suitable", color = MaterialTheme.colorScheme.outline)
            }
            if (o.available) Text(
                "${Geo.km(o.km)} · about ${Geo.minutes(o.minutes)}" + if (o.isEstimate) " (estimate)" else "",
                style = MaterialTheme.typography.titleLarge,
            )
            o.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            o.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (o.available) Text(
                "Open step-by-step directions in Google Maps",
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().clickable { openMaps(ctx, from, to, o.mode) }.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SuggestionsCard(to: Place, curated: List<Suggestion>, nearby: List<Suggestion>, ctx: Context) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("While you're at ${to.name}…")
            Text("Tap a place for Google Maps directions from ${to.name}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            if (curated.isNotEmpty()) {
                Text("Worth considering in ${to.city.label}", fontWeight = FontWeight.SemiBold)
                curated.forEach { s -> SuggestionRow(s, to, ctx) }
            }
            if (nearby.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Landmarks within 1.5 km (Wikipedia)", fontWeight = FontWeight.SemiBold)
                nearby.forEach { s -> SuggestionRow(s, to, ctx) }
            }
            if (curated.isEmpty() && nearby.isEmpty()) Text("No suggestions found nearby.")
        }
    }
}

@Composable
private fun SuggestionRow(s: Suggestion, from: Place, ctx: Context) {
    val walk = (s.distanceKm ?: Geo.haversineKm(from.lat, from.lon, s.lat, s.lon)) <= MapsLinks.WALK_LIMIT_KM
    // Plain clickable row (a pill-shaped TextButton clipped letters). Tap = directions from the Destination Point.
    Row(
        Modifier.fillMaxWidth()
            .clickable { open(ctx, MapsLinks.toSuggestion(from.lat, from.lon, s.lat, s.lon, from.mapsQuery(), s.mapsName)) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("🏛️ ${s.title}" + (s.distanceKm?.let { " · ${Geo.km(it)}" } ?: ""), fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(s.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Icon(
            if (walk) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Filled.DirectionsTransit,
            contentDescription = if (walk) "Walking directions" else "Public transport directions",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp).size(22.dp),
        )
        s.url?.let { url ->
            IconButton(onClick = { open(ctx, url) }) {
                Icon(Icons.Outlined.Info, "Read about it on Wikipedia", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ------------------------------------------------------------- new places

@Composable
private fun AddPlaceCard(vm: TripViewModel) {
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AddLocationAlt, null); Spacer(Modifier.width(8.dp)); SectionTitle("Add a new place")
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Name or address (e.g. Galleria Borghese Rome)") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
                trailingIcon = { IconButton(onClick = { vm.search(query) }) { Icon(Icons.Filled.Search, "Search") } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                label = { Text("Save as (optional nickname)") }, modifier = Modifier.fillMaxWidth(),
            )
            if (vm.searching) CircularProgressIndicator(Modifier.size(22.dp))
            vm.searchMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            vm.searchResults.forEach { r ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(r.name, fontWeight = FontWeight.SemiBold)
                    Text(r.address, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.saveSearchResult(r, name, false); query = ""; name = "" }) { Text("Save & use as From") }
                        Button(onClick = { vm.saveSearchResult(r, name, true); query = ""; name = "" }) { Text("as To") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPlacesCard(vm: TripViewModel) {
    Card {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("My places")
            vm.customPlaces.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Text("📍 ${p.display}") }
                    IconButton(onClick = { vm.deleteCustom(p) }) { Icon(Icons.Filled.Delete, "Delete", tint = Color.Gray) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ helpers

private fun open(ctx: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Directions go straight to the Google Maps app; without it, fall back to the browser.
    if (url.startsWith("https://maps.google.com/")) {
        val viaApp = runCatching { ctx.startActivity(Intent(intent).setPackage("com.google.android.apps.maps")) }
        if (viaApp.isSuccess) return
    }
    runCatching { ctx.startActivity(intent) }
}

private fun openMaps(ctx: Context, from: Place, to: Place, mode: Mode) {
    open(ctx, MapsLinks.directions(from.lat, from.lon, to.lat, to.lon, MapsLinks.modeFor(mode), from.mapsQuery(), to.mapsQuery()))
}
