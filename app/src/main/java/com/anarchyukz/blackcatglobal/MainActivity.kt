package com.anarchyukz.blackcatglobal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val CatRed = Color(0xFFE53945)
private val DeepBackground = Color(0xFF101114)
private val Panel = Color(0xFF1B1D22)
private val Muted = Color(0xFFB7BAC3)

private data class GuideResult(
    val name: String,
    val fixture: String?,
    val context: String?,
    val channels: List<String>,
    val url: String
)

private data class WebResult(val title: String, val snippet: String, val source: String, val channels: List<String>)

private data class DashboardData(
    val fixture: String?,
    val kickoff: String?,
    val fixtureSource: String,
    val guides: List<GuideResult>,
    val searchResults: List<WebResult>,
    val errors: List<String>
)

private val broadcasterTerms = listOf(
    "Sky Sports", "TNT Sports", "HBO Max", "DAZN", "ESPN", "beIN SPORTS", "beIN Sports",
    "Stan Sport", "SuperSport", "Disney+", "Peacock", "NBC", "USA Network", "Canal+",
    "Setanta Sports", "Paramount+", "JioHotstar", "Star Sports", "Viaplay", "Amazon Prime",
    "Prime Video", "Apple TV", "Fubo", "Sport TV", "Ziggo Sport", "Eleven Sports",
    "Movistar", "Arena Sport", "DStv", "TOD", "LFCTV", "NOW", "NOW TV", "HBO MAX", "Sky Sports+"
)

private val guidePages = listOf(
    "TV Guide UK" to "https://www.tvguide.co.uk/sport/football/sunderland",
    "Where's The Match" to "https://www.wheresthematch.com/Football/Sunderland.asp",
    "Live Soccer TV" to "https://www.livesoccertv.com/teams/england/sunderland/"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = CatRed,
                onPrimary = Color.White,
                background = DeepBackground,
                surface = Panel,
                onSurface = Color.White
            )) {
                BlackCatApp()
            }
        }
    }
}

@Composable
private fun BlackCatApp() {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var dashboard by remember { mutableStateOf<DashboardData?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            loading = true
            dashboard = withContext(Dispatchers.IO) { loadDashboard(context) }
            loading = false
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val data = dashboard
    val channels = remember(data) {
        (data?.guides.orEmpty().flatMap { it.channels } +
            data?.searchResults.orEmpty().flatMap { it.channels })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.background(CatRed, RoundedCornerShape(22.dp)).padding(17.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White,
                    modifier = Modifier.width(34.dp).height(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("BLACK CAT GLOBAL", fontSize = 23.sp, fontWeight = FontWeight.Black,
                color = Color.White, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(32.dp))

            if (loading && data == null) {
                CircularProgressIndicator(color = CatRed)
                Spacer(Modifier.height(16.dp))
                Text("Finding the next Sunderland match…", color = Muted, fontSize = 15.sp)
            } else {
                Text("SUNDERLAND", fontSize = 12.sp, color = CatRed,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.4.sp)
                Spacer(Modifier.height(12.dp))
                Text(data?.fixture ?: "Fixture unavailable",
                    fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                data?.kickoff?.let {
                    Spacer(Modifier.height(9.dp))
                    Text(it, color = Muted, fontSize = 14.sp)
                }
                Spacer(Modifier.height(30.dp))
                Text("WATCH ON", fontSize = 12.sp, color = CatRed,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.4.sp)
                Spacer(Modifier.height(12.dp))
                if (channels.isEmpty()) {
                    Text(
                        if (loading) "Checking worldwide TV listings…" else "TV channels not confirmed yet",
                        color = Muted, fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Text(channels.joinToString("  •  "), color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 29.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Listings refresh automatically", color = Muted, fontSize = 12.sp)
        }
    }
}

private fun loadDashboard(context: android.content.Context): DashboardData {
    val errors = mutableListOf<String>()
    val docs = guidePages.mapNotNull { (name, url) ->
        try {
            name to Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(12000)
                .get()
        } catch (_: Exception) {
            errors += name
            null
        }
    }

    val fixtureGuide = docs.firstOrNull { it.first == "TV Guide UK" }
        ?: docs.firstOrNull { it.first == "Where's The Match" }
        ?: docs.firstOrNull { it.first == "Live Soccer TV" }

    var fixture: String? = null
    var kickoff: String? = null
    var fixtureSource = "Public TV guide"
    if (fixtureGuide != null) {
        val (name, doc) = fixtureGuide
        val fixtureAnchor = doc.select("a").firstOrNull { a ->
            val t = a.text().replace(Regex("\\s+"), " ").trim()
            t.contains("Sunderland", ignoreCase = true) &&
                Regex("(?i)\\b(vs?\\.?|versus)\\b").containsMatchIn(t) &&
                t.length < 100
        }
        if (fixtureAnchor != null) {
            fixture = fixtureAnchor.text().replace(Regex("\\s+"), " ").trim()
            val container = findFixtureContainer(fixtureAnchor)
            kickoff = container?.text()?.let { extractKickoff(it) }
            fixtureSource = name
        } else {
            fixture = extractFixtureFromText(doc.body().text())
            if (fixture != null) fixtureSource = name
        }
    }

    val guides = docs.map { (name, doc) ->
        val anchor = fixture?.let { f ->
            doc.select("a").firstOrNull { a ->
                val t = a.text().replace(Regex("\\s+"), " ").trim()
                t.contains("Sunderland", true) &&
                    (Regex("(?i)\\b(vs?\\.?|versus)\\b").containsMatchIn(t) || t.contains("Sunderland", true)) &&
                    (f.split(Regex("(?i)\\s+v(?:s)?\\.?\\s+")).any { part ->
                        part.length > 2 && t.contains(part.trim(), true)
                    } || t.equals(f, true))
            }
        } ?: doc.select("a").firstOrNull { it.text().contains("Sunderland", true) && it.text().length < 100 }

        val container = anchor?.let { findFixtureContainer(it) }
        val context = container?.text()?.replace(Regex("\\s+"), " ")?.take(420)
        val linkText = container?.select("a")?.map { it.text().trim() }.orEmpty()
        val haystack = (context.orEmpty() + " " + linkText.joinToString(" "))
        val channels = broadcasterTerms.filter { term -> haystack.contains(term, ignoreCase = true) }.distinct()
        GuideResult(
            name = name,
            fixture = anchor?.text()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() },
            context = context?.takeIf { it.isNotBlank() },
            channels = channels,
            url = guidePages.first { it.first == name }.second
        )
    }

    val searchResults = if (fixture != null) fetchWorldwideSearchResults(fixture) else emptyList()

    // Keep the selected fixture pinned from kick-off until the end of its 90-minute window.
    // This prevents guide pages rolling forward to the next fixture while the game is in progress.
    val prefs = context.getSharedPreferences("black_cat_global", android.content.Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val pinnedFixture = prefs.getString("pinned_fixture", null)
    val pinnedKickoff = prefs.getLong("pinned_kickoff", 0L)
    val pinnedUntil = pinnedKickoff + 90L * 60L * 1000L
    if (!pinnedFixture.isNullOrBlank() && pinnedKickoff > 0L && now in pinnedKickoff until pinnedUntil) {
        fixture = pinnedFixture
        kickoff = prefs.getString("pinned_kickoff_label", kickoff)
        fixtureSource = "Pinned for match duration"
    } else {
        val parsedKickoff = kickoff?.let { parseKickoffMillis(it) }
        if (!fixture.isNullOrBlank() && parsedKickoff != null && parsedKickoff > now &&
            parsedKickoff - now <= 7L * 24L * 60L * 60L * 1000L) {
            prefs.edit()
                .putString("pinned_fixture", fixture)
                .putLong("pinned_kickoff", parsedKickoff)
                .putString("pinned_kickoff_label", kickoff)
                .apply()
        } else if (pinnedKickoff > 0L && now >= pinnedUntil) {
            prefs.edit().remove("pinned_fixture").remove("pinned_kickoff")
                .remove("pinned_kickoff_label").apply()
        }
    }

    return DashboardData(
        fixture = fixture,
        kickoff = kickoff,
        fixtureSource = fixtureSource,
        guides = guides,
        searchResults = searchResults,
        errors = errors
    )
}

private fun fetchWorldwideSearchResults(fixture: String): List<WebResult> {
    val query = "\"$fixture\" football TV channel broadcaster worldwide"
    val results = mutableListOf<WebResult>()

    try {
        val google = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=10")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(10000)
            .get()
        google.select("div.MjjYud").forEach { block ->
            val title = block.selectFirst("h3")?.text()?.trim().orEmpty()
            val snippet = block.selectFirst("div.VwiC3b, div[data-sncf], span.aCOpRe")?.text()?.trim().orEmpty()
            if (title.isNotBlank() && snippet.isNotBlank()) {
                val combined = "$title $snippet"
                val channels = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }.distinct()
                results.add(WebResult(title, snippet, "Google", channels))
            }
        }
    } catch (_: Exception) { }

    try {
        val bing = Jsoup.connect("https://www.bing.com/search?q=" + enc(query) + "&count=10")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(10000)
            .get()
        bing.select("li.b_algo").forEach { block ->
            val title = block.selectFirst("h2")?.text()?.trim().orEmpty()
            val snippet = block.selectFirst("p")?.text()?.trim().orEmpty()
            if (title.isNotBlank() && snippet.isNotBlank()) {
                val combined = "$title $snippet"
                val channels = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }.distinct()
                results.add(WebResult(title, snippet, "Bing", channels))
            }
        }
    } catch (_: Exception) { }

    return results.distinctBy { it.title.lowercase() }.take(12)
}

private fun findFixtureContainer(anchor: org.jsoup.nodes.Element): org.jsoup.nodes.Element? {
    var node: org.jsoup.nodes.Element? = anchor
    repeat(7) {
        val current = node ?: return null
        val text = current.text().replace(Regex("\\s+"), " ").trim()
        if (text.contains("Sunderland", true) && text.length in 35..520) return current
        node = current.parent()
    }
    return anchor.parent()
}

private fun extractFixtureFromText(text: String): String? {
    val clean = text.replace(Regex("\\s+"), " ")
    val awayPattern = Regex("(?i)([A-Za-z][A-Za-z .&'-]{1,45}?)(?:\\s*vs?\\.?\\s*|\\s+versus\\s*)(Sunderland)")
    val homePattern = Regex("(?i)(Sunderland)(?:\\s*vs?\\.?\\s*|\\s+versus\\s*)([A-Za-z][A-Za-z .&'-]{1,45}?)")
    val away = awayPattern.find(clean)
    if (away != null) return "${away.groupValues[1].trim()} vs ${away.groupValues[2].trim()}"
    val home = homePattern.find(clean)
    if (home != null) return "${home.groupValues[1].trim()} vs ${home.groupValues[2].trim()}"
    return null
}

private fun extractKickoff(text: String): String? {
    val clean = text.replace(Regex("\\s+"), " ")
    val date = Regex("(?i)\\b(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?\\s+\\d{1,2}\\s+[A-Za-z]{3,9}\\b").find(clean)?.value
    val time = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b").find(clean)?.value
    return listOfNotNull(date, time).joinToString(" • ").ifBlank { "" }.takeIf { it.isNotBlank() }
}

private fun parseKickoffMillis(label: String): Long? {
    val patterns = listOf(
        "EEE d MMM • HH:mm", "EEEE d MMMM • HH:mm",
        "EEE d MMM HH:mm", "EEEE d MMMM HH:mm",
        "d MMM • HH:mm", "d MMM HH:mm"
    )
    for (pattern in patterns) {
        try {
            val formatter = java.text.SimpleDateFormat(pattern, java.util.Locale.UK)
            formatter.timeZone = java.util.TimeZone.getTimeZone("Europe/London")
            formatter.isLenient = false
            val parsed = formatter.parse(label) ?: continue
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/London"))
            calendar.time = parsed
            if (!label.contains(Regex("\\b20\\d{2}\\b"))) {
                calendar.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance(
                    java.util.TimeZone.getTimeZone("Europe/London")).get(java.util.Calendar.YEAR))
            }
            return calendar.timeInMillis
        } catch (_: Exception) { }
    }
    return null
}

private fun enc(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
