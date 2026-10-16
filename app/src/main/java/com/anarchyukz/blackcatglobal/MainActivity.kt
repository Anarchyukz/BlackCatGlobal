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
import androidx.compose.foundation.layout.statusBarsPadding
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

private data class LineupTeam(
    val name: String,
    val formation: String?,
    val starters: List<String>,
    val substitutes: List<String>
)

private data class DashboardData(
    val fixture: String?,
    val kickoff: String?,
    val previousFixture: String?,
    val previousKickoff: String?,
    val previousCupFixture: String?,
    val previousCupKickoff: String?,
    val fixtureSource: String,
    val guides: List<GuideResult>,
    val searchResults: List<WebResult>,
    val lineups: List<LineupTeam>,
    val squadPlayers: List<String>,
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
    "Live Soccer TV" to "https://www.livesoccertv.com/teams/england/sunderland/",
    "FourFourTwo" to "https://www.fourfourtwo.com/features"
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
            modifier = Modifier.fillMaxSize().statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Sunderland AFC wordmark supplied by the user, rendered as red monospace text.
            Text(
                text = """
  ____                  _           _                 _      _      _____ ____ 
 / ___| _   _ _ __   __| | ___ _ __| | __ _ _ __   __| |    / \    |  ___/ ___|
 \___ \| | | | '_ \ / _`|/ _ \ '__| |/ _`| '_ \ / _`|   / _ \   | |_ | |    
  ___) | |_| | | | | (_| |  __/ |  | | (_| | | | | (_| |  / ___ \ _|  _|| |___ 
 |____/ \__,_|_| |_|\__,_|\___|_|  |_|\__,_|_| |_|\__,_| /_/   \_(_)_|(_)\_____|
                                                                               
""".trimIndent(),
                color = CatRed,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 2.4.sp,
                lineHeight = 2.4.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
                Spacer(Modifier.height(30.dp))
                Text("TEAM LINE-UPS", fontSize = 12.sp, color = CatRed,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.4.sp)
                Spacer(Modifier.height(12.dp))
                if (data?.lineups.isNullOrEmpty()) {
                    Text(
                        "Confirmed starting line-ups are not available yet.",
                        color = Muted, fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 21.sp
                    )
                    if (!data?.squadPlayers.isNullOrEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("PREMIER LEAGUE SQUAD", color = Color.White,
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Registered first-team squad • not the match-day starting XI",
                            color = Muted, fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(data!!.squadPlayers.joinToString("\n"), color = Color.White,
                            fontSize = 14.sp, lineHeight = 21.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                } else {
                    data!!.lineups.forEach { team ->
                        Spacer(Modifier.height(8.dp))
                        Text(team.name.uppercase(), color = Color.White,
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        team.formation?.let {
                            Text("Formation: $it", color = Muted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text("STARTING XI", color = CatRed, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(team.starters.joinToString("\n"), color = Color.White,
                            fontSize = 14.sp, lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        if (team.substitutes.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("SUBSTITUTES", color = CatRed, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            Text(team.substitutes.joinToString("\n"), color = Muted,
                                fontSize = 13.sp, lineHeight = 19.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            Spacer(Modifier.height(24.dp))
            data?.previousFixture?.let { previous ->
                Spacer(Modifier.height(20.dp))
                Text("PREVIOUS PREMIER LEAGUE MATCH", fontSize = 11.sp, color = Muted,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(7.dp))
                Text(previous, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                data.previousKickoff?.let { previousTime ->
                    Spacer(Modifier.height(4.dp))
                    Text(previousTime, color = Muted, fontSize = 12.sp)
                }
            }
            data?.previousCupFixture?.let { previous ->
                Spacer(Modifier.height(20.dp))
                Text("PREVIOUS CUP / EUROPE MATCH", fontSize = 11.sp, color = Muted,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(7.dp))
                Text(previous, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                data.previousCupKickoff?.let { previousTime ->
                    Spacer(Modifier.height(4.dp))
                    Text(previousTime, color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Listings refresh automatically", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Black Cat Global provides information about TV channels and broadcasters showing Sunderland matches. It does not stream, host or provide the matches themselves. Listings are for information only and may not always be accurate or available in every region.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
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

    // Load saved previous-match details before refreshing them from results pages.
    val prefs = context.getSharedPreferences("black_cat_global", android.content.Context.MODE_PRIVATE)
    var previousFixture = prefs.getString("previous_fixture", null)
    var previousKickoff = prefs.getString("previous_kickoff_label", null)
    var previousCupFixture = prefs.getString("previous_cup_fixture", null)
    var previousCupKickoff = prefs.getString("previous_cup_kickoff_label", null)
    val now = System.currentTimeMillis()

    // Fetch completed results separately: TV listings generally only show upcoming fixtures,
    // so they cannot reliably supply the previous match.
    try {
        val resultsDoc = Jsoup.connect("https://www.11v11.com/teams/sunderland/option/fixtures/")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(12000)
            .get()
        val resultRows = resultsDoc.select("tr").mapNotNull { row ->
            val text = row.text().replace(Regex("\\s+"), " ").trim()
            if (!text.contains("Sunderland", ignoreCase = true) ||
                !Regex("\\b\\d{1,2}\\s*[-–:]\\s*\\d{1,2}\\b").containsMatchIn(text)) return@mapNotNull null
            val dateMatch = Regex("\\b\\d{1,2}\\s+[A-Za-z]{3,9}\\s+20\\d{2}\\b").find(text)
                ?: Regex("\\b\\d{1,2}\\s+[A-Za-z]{3,9}\\b").find(text)
                ?: return@mapNotNull null
            val dateText = dateMatch.value
            val yearText = if (Regex("\\b20\\d{2}\\b").containsMatchIn(dateText)) dateText else "$dateText 2026"
            val millis = try {
                val patterns = listOf("d MMMM yyyy", "d MMM yyyy", "d MMMM", "d MMM")
                patterns.firstNotNullOfOrNull { pattern ->
                    try {
                        val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.UK)
                        parser.isLenient = false
                        parser.parse(yearText)?.time
                    } catch (_: Exception) { null }
                }
            } catch (_: Exception) { null } ?: return@mapNotNull null
            if (millis > now) return@mapNotNull null
            val competition = row.select("td,th").joinToString(" ") { it.text() }
            val resultText = text.replace(dateText, "").trim()
            Triple(resultText, dateText, millis) to competition
        }.distinctBy { it.first.first.lowercase() }
        // Keep the existing previous-match behaviour unchanged.
        val latestResult = resultRows.maxByOrNull { it.first.third }?.first
        if (latestResult != null) {
            previousFixture = latestResult.first
            previousKickoff = latestResult.second
            prefs.edit().putString("previous_fixture", previousFixture)
                .putString("previous_kickoff_label", previousKickoff).apply()
        }
        // Use 11v11's full match-record table for cup/European competitions.
        // This is intentionally separate from the existing Premier League previous-match source.
        val allMatchesDoc = Jsoup.connect("https://www.11v11.com/teams/sunderland/tab/matches/")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(12000)
            .get()
        val cupResults = allMatchesDoc.select("tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 5) return@mapNotNull null
            val text = row.text().replace(Regex("\\s+"), " ").trim()
            if (!text.contains("Sunderland", ignoreCase = true)) return@mapNotNull null
            val competition = cells.last()?.text()?.trim().orEmpty()
            if (!Regex("(?i)FA Cup|EFL Cup|League Cup|Carabao Cup|Champions League|Europa League|Conference League|UEFA|EFL Trophy").containsMatchIn(competition)) return@mapNotNull null
            val dateText = cells.first()?.text()?.trim().orEmpty()
            if (!Regex("\\b\\d{1,2}\\s+[A-Za-z]{3,9}\\s+20\\d{2}\\b").containsMatchIn(dateText)) return@mapNotNull null
            val millis = try {
                val parser = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.UK)
                parser.isLenient = false
                parser.parse(dateText)?.time
            } catch (_: Exception) { null } ?: return@mapNotNull null
            if (millis > now) return@mapNotNull null
            val match = cells.getOrNull(1)?.text()?.trim().orEmpty()
            val score = cells.getOrNull(3)?.text()?.trim().orEmpty()
            val resultText = listOf(match, score, competition).filter { it.isNotBlank() }.joinToString(" • ")
            Triple(resultText, dateText, millis)
        }
        val latestCupResult = cupResults.maxByOrNull { it.third }
        if (latestCupResult != null) {
            previousCupFixture = latestCupResult.first
            previousCupKickoff = latestCupResult.second
            prefs.edit().putString("previous_cup_fixture", previousCupFixture)
                .putString("previous_cup_kickoff_label", previousCupKickoff).apply()
        }
    } catch (_: Exception) {
        // Keep the last saved result if the results site is temporarily unavailable.
    }
    if (previousFixture.isNullOrBlank()) {
        // Safe first-launch fallback for the latest completed Sunderland match as of 15 Oct 2026.
        previousFixture = "Sunderland 0–2 Brighton"
        previousKickoff = "Sat 10 Oct"
        prefs.edit().putString("previous_fixture", previousFixture)
            .putString("previous_kickoff_label", previousKickoff).apply()
    }

    val searchResults = if (fixture != null) fetchWorldwideSearchResults(fixture) else emptyList()
    val lineups = if (fixture != null) fetchLineups(fixture) else emptyList()
    val squadPlayers = fetchPremierLeagueSquad()

    // Keep the selected fixture pinned from kick-off until the end of its 90-minute window.
    // This prevents guide pages rolling forward to the next fixture while the game is in progress.
    // Also look for a past Sunderland fixture in the pages on first launch,
    // so the previous-match section does not depend on the app having been open before.
    val pastFixtures = docs.flatMap { (_, doc) ->
        doc.select("a").mapNotNull { anchor ->
            val title = anchor.text().replace(Regex("\\s+"), " ").trim()
            if (title.length !in 8..100 ||
                !title.contains("Sunderland", ignoreCase = true) ||
                !Regex("(?i)\\b(vs?\\.?|versus)\\b").containsMatchIn(title)) {
                return@mapNotNull null
            }
            val nearby = findFixtureContainer(anchor)?.text()?.replace(Regex("\\s+"), " ").orEmpty()
            val dateLabel = extractKickoff(nearby) ?: return@mapNotNull null
            val dateMillis = parseKickoffMillis(dateLabel) ?: return@mapNotNull null
            if (dateMillis >= now - 90L * 60L * 1000L) return@mapNotNull null
            Triple(title, dateLabel, dateMillis)
        }
    }.distinctBy { it.first.lowercase() }
    val newestPast = pastFixtures.maxByOrNull { it.third }
    if (newestPast != null &&
        (previousFixture.isNullOrBlank() ||
            (parseKickoffMillis(previousKickoff.orEmpty()) ?: 0L) < newestPast.third)) {
        previousFixture = newestPast.first
        previousKickoff = newestPast.second
        prefs.edit().putString("previous_fixture", previousFixture)
            .putString("previous_kickoff_label", previousKickoff).apply()
    }
    val pinnedFixture = prefs.getString("pinned_fixture", null)
    val pinnedKickoff = prefs.getLong("pinned_kickoff", 0L)
    val pinnedUntil = pinnedKickoff + 90L * 60L * 1000L

    // A fixture is "current" from its scheduled kick-off until at least 90 minutes later.
    // Never replace it with the next listing while this window is still active.
    if (!pinnedFixture.isNullOrBlank() && pinnedKickoff > 0L && now < pinnedUntil) {
        fixture = pinnedFixture
        kickoff = prefs.getString("pinned_kickoff_label", kickoff)
        fixtureSource = "Current match held until full-time window"
    } else {
        // The previous match's 90-minute window has ended: clear it before selecting the next fixture.
        if (pinnedKickoff > 0L && now >= pinnedUntil) {
            previousFixture = pinnedFixture ?: previousFixture
            previousKickoff = prefs.getString("pinned_kickoff_label", previousKickoff)
            prefs.edit()
                .putString("previous_fixture", previousFixture)
                .putString("previous_kickoff_label", previousKickoff)
                .remove("pinned_fixture").remove("pinned_kickoff")
                .remove("pinned_kickoff_label").apply()
        }

        val parsedKickoff = kickoff?.let { parseKickoffMillis(it) }
        if (!fixture.isNullOrBlank() && parsedKickoff != null &&
            parsedKickoff - now <= 7L * 24L * 60L * 60L * 1000L &&
            parsedKickoff > now - 90L * 60L * 1000L) {
            prefs.edit()
                .putString("pinned_fixture", fixture)
                .putLong("pinned_kickoff", parsedKickoff)
                .putString("pinned_kickoff_label", kickoff)
                .apply()
        }
    }

    return DashboardData(
        fixture = fixture,
        kickoff = kickoff,
        previousFixture = previousFixture,
        previousKickoff = previousKickoff,
        previousCupFixture = previousCupFixture,
        previousCupKickoff = previousCupKickoff,
        fixtureSource = fixtureSource,
        guides = guides,
        searchResults = searchResults,
        lineups = lineups,
        squadPlayers = squadPlayers,
        errors = errors
    )
}


/**
 * Reads Sunderland's current squad numbers from the official Premier League website.
 * This is only a squad fallback; it must never be presented as the confirmed match-day XI.
 */
private fun fetchPremierLeagueSquad(): List<String> {
    return try {
        val doc = Jsoup.connect(
            "https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season"
        )
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(12000)
            .get()

        val heading = doc.select("h1, h2, h3, h4")
            .firstOrNull { it.text().trim().equals("Sunderland", ignoreCase = true) }
            ?: return emptyList()

        val players = linkedSetOf<String>()
        var element = heading.nextElementSibling()
        while (element != null && !element.tagName().matches(Regex("h[1-4]"))) {
            val text = element.text().replace(Regex("\\s+"), " ").trim()
            if (text.isNotBlank()) {
                // Squad-number article lines start with a shirt number followed by a player name.
                Regex("""^(\d{1,3})\s+(.+)$""").matchEntire(text)?.let { match ->
                    players.add("${match.groupValues[1]}  ${match.groupValues[2]}")
                }
            }
            element = element.nextElementSibling()
        }
        players.toList()
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * ESPN's public scoreboard/summary endpoints expose confirmed match line-ups without an API key.
 * If the teams have not announced them yet, this returns an empty list and the screen says so.
 */
private fun fetchLineups(fixture: String): List<LineupTeam> {
    val sides = fixture.split(Regex("(?i)\\s+(?:v|vs|versus)\\.?\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (sides.size < 2) return emptyList()

    val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    val from = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(now.time)
    now.add(java.util.Calendar.DAY_OF_YEAR, 45)
    val to = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(now.time)

    val candidateEvents = mutableListOf<org.json.JSONObject>()
    for (league in listOf("eng.1", "eng.2")) {
        try {
            val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$league/scoreboard?dates=$from-$to&limit=100"
            val jsonText = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(10000)
                .execute().body()
            val events = org.json.JSONObject(jsonText).optJSONArray("events") ?: continue
            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                val competitors = event.optJSONObject("competitions")?.optJSONArray("competitors")
                    ?: event.optJSONArray("competitors")
                val names = mutableListOf<String>()
                if (competitors != null) for (j in 0 until competitors.length()) {
                    val team = competitors.optJSONObject(j)?.optJSONObject("team")
                    val teamName = team?.optString("displayName").orEmpty()
                    if (teamName.isNotBlank()) names.add(teamName)
                }
                val eventName = event.optString("name", "")
                val allNames = (names + eventName).joinToString(" ")
                if (allNames.contains("Sunderland", ignoreCase = true) &&
                    sides.all { side ->
                        val cleaned = side.replace(Regex("(?i)\\b(afc|fc)\\b"), "").trim()
                        cleaned.length < 4 || allNames.contains(cleaned, ignoreCase = true) ||
                            cleaned.split(" ").firstOrNull()?.let { allNames.contains(it, ignoreCase = true) } == true
                    }) {
                    candidateEvents.add(event)
                }
            }
        } catch (_: Exception) {
            // Try the other English league; lineup data may not be available from every source.
        }
    }

    val event = candidateEvents.firstOrNull() ?: return emptyList()
    val eventId = event.optString("id").takeIf { it.isNotBlank() } ?: return emptyList()
    return try {
        val summaryUrl = "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/summary?event=$eventId"
        var summaryText = Jsoup.connect(summaryUrl).ignoreContentType(true)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(10000).execute().body()
        var parsed = parseEspnLineups(summaryText)
        if (parsed.isEmpty()) {
            // Championship events may need the matching league summary endpoint.
            summaryText = Jsoup.connect("https://site.api.espn.com/apis/site/v2/sports/soccer/eng.2/summary?event=$eventId")
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(10000).execute().body()
            parsed = parseEspnLineups(summaryText)
        }
        parsed
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseEspnLineups(jsonText: String): List<LineupTeam> {
    return try {
        val root = org.json.JSONObject(jsonText)
        val boxscore = root.optJSONObject("boxscore") ?: return emptyList()
        val teams = boxscore.optJSONArray("players") ?: return emptyList()
        val result = mutableListOf<LineupTeam>()
        for (i in 0 until teams.length()) {
            val teamObj = teams.optJSONObject(i) ?: continue
            val teamName = teamObj.optJSONObject("team")?.optString("displayName").orEmpty()
            if (teamName.isBlank()) continue
            val starters = linkedSetOf<String>()
            val substitutes = linkedSetOf<String>()
            val formation = teamObj.optString("formation").takeIf { it.isNotBlank() && it != "null" }
            val statsGroups = teamObj.optJSONArray("statistics")
            if (statsGroups != null) {
                for (g in 0 until statsGroups.length()) {
                    val group = statsGroups.optJSONObject(g) ?: continue
                    val athletes = group.optJSONArray("athletes") ?: continue
                    for (a in 0 until athletes.length()) {
                        val entry = athletes.optJSONObject(a) ?: continue
                        val athlete = entry.optJSONObject("athlete") ?: entry
                        val name = athlete.optString("displayName",
                            athlete.optString("fullName", athlete.optString("name", ""))).trim()
                        if (name.isBlank()) continue
                        val jersey = athlete.opt("jersey")?.toString()?.takeIf { it.isNotBlank() && it != "null" }
                        val position = athlete.optJSONObject("position")?.optString("abbreviation")
                            ?.takeIf { !it.isNullOrBlank() && it != "null" }
                        val label = listOfNotNull(jersey?.let { "$it." }, name, position?.let { "($it)" })
                            .joinToString(" ")
                        val isStarter = entry.optBoolean("starter", athlete.optBoolean("starter", false))
                        if (isStarter) starters.add(label) else substitutes.add(label)
                    }
                }
            }
            if (starters.isNotEmpty()) {
                result.add(LineupTeam(teamName, formation, starters.toList(), substitutes.toList()))
            }
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
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
