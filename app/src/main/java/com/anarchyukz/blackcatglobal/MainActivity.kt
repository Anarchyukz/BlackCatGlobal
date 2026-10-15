package com.anarchyukz.blackcatglobal

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.TextButton
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

private data class DashboardData(
    val fixture: String?,
    val kickoff: String?,
    val fixtureSource: String,
    val guides: List<GuideResult>,
    val errors: List<String>
)

private val broadcasterTerms = listOf(
    "Sky Sports", "TNT Sports", "HBO Max", "DAZN", "ESPN", "beIN SPORTS", "beIN Sports",
    "Stan Sport", "SuperSport", "Disney+", "Peacock", "NBC", "USA Network", "Canal+",
    "Setanta Sports", "Paramount+", "JioHotstar", "Star Sports", "Viaplay", "Amazon Prime",
    "Prime Video", "Apple TV", "Fubo", "Sport TV", "Ziggo Sport", "Eleven Sports",
    "Movistar", "Arena Sport", "DStv", "TOD", "LFCTV"
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
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var dashboard by remember { mutableStateOf<DashboardData?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        dashboard = withContext(Dispatchers.IO) { loadDashboard() }
        loading = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.background(CatRed, RoundedCornerShape(15.dp)).padding(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("BLACK CAT", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.2.sp)
                    Text("GLOBAL", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CatRed, letterSpacing = 4.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Public, contentDescription = "Worldwide", tint = CatRed)
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sunderland's next game", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Fixtures and TV listings are checked from public football guides. No fixture details to enter.", color = Muted, fontSize = 13.sp)
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = CatRed, modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                            Text("Checking fixture and TV guides…", color = Muted)
                        }
                    } else {
                        val data = dashboard
                        Text(data?.fixture ?: "Fixture not available right now", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        data?.kickoff?.let { Text(it, color = CatRed, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        Text("Fixture source: " + (data?.fixtureSource ?: "Sunderland AFC / football guides"), color = Muted, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { refreshKey++ },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CatRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("REFRESH FIXTURE & CHANNELS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("TV CHANNELS & WORLDWIDE GUIDES", fontSize = 13.sp, color = CatRed, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            Text("Listings are taken from public guide pages where available. Broadcasters can vary by country and some matches are not televised in every territory.", color = Muted, fontSize = 13.sp)

            val results = dashboard?.guides.orEmpty()
            if (!loading && results.isEmpty()) {
                Text("No guide results could be read just now. Open a guide below to check its latest listing.", color = Muted, fontSize = 14.sp)
            }
            results.forEach { result ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(result.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (result.fixture != null) Text(result.fixture, fontWeight = FontWeight.SemiBold)
                        if (!result.channels.isNullOrEmpty()) {
                            Text("Channels found: " + result.channels.joinToString(", "), color = Color.White, fontSize = 14.sp)
                        } else {
                            Text(result.context ?: "No channel name could be reliably extracted; open the guide to confirm.", color = Muted, fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                        }) {
                            Text("OPEN GUIDE", color = CatRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Search worldwide listings", fontWeight = FontWeight.Bold)
                    Text("These searches use the detected next fixture and can find country-by-country broadcaster pages when the guide sites publish them.", color = Muted, fontSize = 13.sp)
                    val fixture = dashboard?.fixture ?: "Sunderland AFC next match"
                    TextButton(onClick = {
                        val query = enc("\"$fixture\" worldwide TV channel broadcaster Live Soccer TV")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")))
                    }) { Text("GOOGLE WORLDWIDE TV SEARCH", color = CatRed, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = {
                        val query = enc("\"$fixture\" TV listings international broadcaster")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bing.com/search?q=$query")))
                    }) { Text("BING WORLDWIDE TV SEARCH", color = CatRed, fontWeight = FontWeight.Bold) }
                }
            }

            Text("Legal viewing information only • No streams hosted • No background polling", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

private fun loadDashboard(): DashboardData {
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

    return DashboardData(
        fixture = fixture,
        kickoff = kickoff,
        fixtureSource = fixtureSource,
        guides = guides,
        errors = errors
    )
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
    val patterns = listOf(
        Regex("(?i)([A-Za-z][A-Za-z .&'-]{1,45}\\s+(?:vs?\\.?|versus)\\s+Sunderland)"),
        Regex("(?i)(Sunderland\\s+(?:vs?\\.?|versus)\\s+[A-Za-z][A-Za-z .&'-]{1,45})")
    )
    return patterns.firstNotNullOfOrNull { regex -> regex.find(clean)?.groupValues?.getOrNull(1)?.trim() }
}

private fun extractKickoff(text: String): String? {
    val clean = text.replace(Regex("\\s+"), " ")
    val date = Regex("(?i)\\b(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?\\s+\\d{1,2}\\s+[A-Za-z]{3,9}\\b").find(clean)?.value
    val time = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b").find(clean)?.value
    return listOfNotNull(date, time).joinToString(" • ").ifBlank { "" }.takeIf { it.isNotBlank() }
}

private fun enc(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
