package com.anarchyukz.blackcatglobal

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.analytics.FirebaseAnalytics
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
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

private data class WebResult(val title: String, val snippet: String, val source: String, val region: String, val channels: List<String>)

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
    // UK and Ireland
    "Sky Sports", "Sky Sports+", "TNT Sports", "Premier Sports", "Viaplay Sports",
    "BBC One", "BBC Two", "ITV1", "ITV4", "LFCTV",
    // North and South America
    "ESPN", "ESPN+", "ESPN Deportes", "Fox Sports", "FOX Deportes", "CBS Sports Network",
    "NBC Sports", "USA Network", "Telemundo", "Universo", "TUDN", "Sportsnet", "TSN",
    "beIN SPORTS", "Canal+", "Claro Sports", "TNT Sports Brasil", "BandSports",
    // Europe
    "Ziggo Sport", "Sport TV", "Eleven Sports", "Arena Sport", "Sportklub",
    "Movistar Plus+", "DAZN", "RTL", "Canal+ Sport", "Sky Sport Austria",
    // Africa and Middle East
    "SuperSport", "DStv", "SABC Sport", "Canal+ Afrique", "beIN SPORTS MENA", "TOD",
    // Asia and Oceania
    "JioHotstar", "Star Sports", "Sony Sports Network", "Astro SuperSport",
    "SPOTV", "TrueVisions", "Optus Sport", "Sky Sport NZ", "Stan Sport",
    // Match-specific international channels that are easy to miss in broad searches
    "Sky Sport HD 9", "Sky Sport Premier League", "Canal+ Live 2", "Canal+ Extra 5",
    "V Sport 1", "V Sport Premier League", "NBCSN Extra", "NBCSN", "NBC",
    "Canal+ App", "Sky Go", "WOW", "NOW Sports", "NOW Sports Membership",
    // Common broadcaster families / listings and streaming services
    "HBO Max", "Peacock", "Paramount+", "Setanta Sports", "Disney+", "HBO MAX",
    "Fubo TV Canada", "FuboTV", "Fubo", "Optus Sport", "Viaplay", "DAZN",
    "Stan Sport", "JioHotstar", "Sky Sports App", "Canal+ streaming", "ESPN+",
    "TOD", "StarHub TV", "SuperSport", "TNT Sports", "USA Network", "Sky Sports+"
)

private val guidePages = listOf(
    "TV Guide UK" to "https://www.tvguide.co.uk/sport/football/sunderland",
    "Where's The Match" to "https://www.wheresthematch.com/Football/Sunderland.asp",
    "Live Soccer TV" to "https://www.livesoccertv.com/teams/england/sunderland/",
    "EntertainmentBox" to "https://entertainmentbox.com/football/sunderland",
    "FourFourTwo" to "https://www.fourfourtwo.com/features"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAnalytics.getInstance(this).logEvent("dashboard_open", Bundle().apply {
            putString("screen_name", "main_dashboard")
        })
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
    var refreshToken by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var analyticsSnapshotLogged by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<GitHubUpdate?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshToken) {
        loading = true
        dashboard = withContext(Dispatchers.IO) { loadDashboard(context) }
        loading = false
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            loading = true
            dashboard = withContext(Dispatchers.IO) { loadDashboard(context) }
            loading = false
        }
    }

    val data = dashboard
    val freshChannels = remember(data) {
        (data?.guides.orEmpty().flatMap { it.channels } + data?.searchResults.orEmpty().flatMap { it.channels })
            .map { canonicalChannelName(it.trim()) }
            .filter { it.isNotBlank() && !isStreamingPlatformNotChannel(it) }
            .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    }
    // Keep the last non-empty set visible while a refresh runs or if a guide temporarily
    // returns no results. Replace it only when a later refresh finds broadcaster results.
    var lastKnownChannels by remember { mutableStateOf<List<String>>(emptyList()) }
    val channels = if (freshChannels.isNotEmpty()) freshChannels else lastKnownChannels
    LaunchedEffect(freshChannels) {
        if (freshChannels.isNotEmpty()) lastKnownChannels = freshChannels
    }
    val filteredChannels = remember(channels, searchQuery) {
        channels.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val filteredGuides = remember(data, searchQuery) {
        data?.guides.orEmpty().filter { searchQuery.isBlank() ||
            it.name.contains(searchQuery, true) || it.fixture.orEmpty().contains(searchQuery, true) ||
            it.context.orEmpty().contains(searchQuery, true) ||
            it.channels.any { channel -> channel.contains(searchQuery, true) } }
    }
    val filteredSearchResults = remember(data, searchQuery) {
        data?.searchResults.orEmpty().filter { searchQuery.isBlank() ||
            it.title.contains(searchQuery, true) || it.snippet.contains(searchQuery, true) ||
            it.channels.any { channel -> channel.contains(searchQuery, true) } }
    }

    LaunchedEffect(data) {
        if (data != null && !analyticsSnapshotLogged) {
            analyticsSnapshotLogged = true
            FirebaseAnalytics.getInstance(context).logEvent("dashboard_data_loaded", Bundle().apply {
                putBoolean("fixture_available", !data.fixture.isNullOrBlank())
                putInt("broadcaster_count", channels.size)
                putBoolean("lineups_available", data.lineups.isNotEmpty())
                putBoolean("squad_available", data.squadPlayers.isNotEmpty())
                putBoolean("previous_result_available", !data.previousFixture.isNullOrBlank())
            })
        }
    }

    Surface(Modifier.fillMaxSize(), color = DeepBackground) {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            containerColor = DeepBackground,
            bottomBar = {
                androidx.compose.material3.NavigationBar(containerColor = Panel, contentColor = Color.White) {
                    val items = listOf(
                        Triple("Match", Icons.Filled.SportsSoccer, 0),
                        Triple("TV", Icons.Filled.Public, 1),
                        Triple("Team", Icons.Filled.Groups, 2),
                        Triple("Results", Icons.Filled.History, 3),
                        Triple("Search", Icons.Filled.Search, 4)
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = selectedTab == item.third,
                            onClick = { selectedTab = item.third },
                            icon = { Icon(item.second, contentDescription = item.first) },
                            label = { Text(item.first, fontSize = 10.sp, maxLines = 1) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = CatRed, selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF3B2027), unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.background(CatRed, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text("BC", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("BLACK CAT GLOBAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 0.7.sp, maxLines = 1)
                            Text("SUNDERLAND AFC", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        }
                    }
                    androidx.compose.material3.IconButton(onClick = { refreshToken++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh listings", tint = if (loading) CatRed else Muted)
                    }
                }

                when (selectedTab) {
                    0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(8.dp).background(CatRed, RoundedCornerShape(50)))
                            Text(if (loading) "UPDATING MATCH CENTRE" else "MATCH CENTRE", color = CatRed, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.8.sp)
                            Spacer(Modifier.weight(1f))
                            Text("AUTO REFRESH · 60 SEC", color = Muted, fontSize = 9.sp)
                        }
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                            Column(
                                Modifier.fillMaxWidth().background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3A1820), Color(0xFF21191F), Panel))
                                ).padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("NEXT FIXTURE", color = Color(0xFFFFA2A8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                                FixtureMatchup(
                                    fixture = data?.fixture ?: if (loading) "Finding Sunderland's next match…" else "Fixture unavailable"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = CatRed, modifier = Modifier.size(19.dp))
                                    Text(data?.kickoff ?: "Kick-off time will appear when confirmed", color = Muted, fontSize = 13.sp)
                                }
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF4B343A))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("WORLDWIDE TV", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                                        Text("${channels.size} channel${if (channels.size == 1) "" else "s"} found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    androidx.compose.material3.TextButton(onClick = { selectedTab = 1 }) {
                                        Text("View channels  ›", color = Color(0xFFFF8B93), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        SectionHeader("BROADCAST SNAPSHOT", "Names found in public listings and web results")
                        if (channels.isEmpty()) {
                            InfoPanel(if (loading) "Checking worldwide listings" else "No channels found yet",
                                "Listings can vary by country and source. Use the refresh icon above to check again.")
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                channels.take(6).forEachIndexed { index, channel -> ChannelRow(channel, index + 1) }
                                if (channels.size > 6) androidx.compose.material3.TextButton(onClick = { selectedTab = 1 }) {
                                    Text("See all ${channels.size} channels", color = Color(0xFFFF8B93))
                                }
                            }
                        }
                        data?.previousFixture?.let {
                            SectionHeader("LAST LEAGUE MATCH", data.previousKickoff.orEmpty())
                            InfoPanel(it, "Previous Premier League fixture")
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:blackcatglobal@proton.me")).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Black Cat Global sponsorship enquiry")
                                }
                                try {
                                    context.startActivity(emailIntent)
                                } catch (_: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "No email app is installed.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF252027))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(Modifier.background(Color(0xFF4A2029), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                    Icon(Icons.Filled.Public, contentDescription = null, tint = Color(0xFFFFA2A8))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("SUPPORT BLACK CAT GLOBAL", color = Color(0xFFFFA2A8), fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                                    Text("Want to sponsor the app?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Click for details", color = Muted, fontSize = 12.sp)
                                }
                                Icon(Icons.Filled.OpenInNew, contentDescription = "Sponsor details", tint = Color(0xFFFF8B93))
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://windscribe.com/yo/qxtwefau")))
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF20232A))
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    Modifier.size(36.dp).background(Color(0xFF263B50), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Public, contentDescription = null, tint = Color(0xFF72C7FF), modifier = Modifier.size(21.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("WINDscribe VPN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Want more privacy online? Check out Windscribe via my referral.", color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
                                }
                                Icon(Icons.Filled.OpenInNew, contentDescription = "Open Windscribe referral", tint = Color(0xFF72C7FF), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    1 -> {
                        SectionHeader("WORLDWIDE TV CHANNELS", "Actual named channels found in public match listings")
                        if (channels.isEmpty()) {
                            InfoPanel(if (loading) "Searching listings…" else "No broadcaster names found",
                                "Try refreshing later. A listing is not a guarantee that a channel will show the match in your region.")
                        } else channels.forEachIndexed { index, channel -> ChannelRow(channel, index + 1) }

                        val streamingServices = remember(data) {
                            data?.searchResults.orEmpty().flatMap { it.channels }
                                .filter { isStreamingService(it) }
                                .map { canonicalChannelName(it) }
                                .distinctBy { it.lowercase() }
                                .sortedBy { it.lowercase() }
                        }
                        SectionHeader("STREAMING SERVICES", "Apps and online services listed separately from TV channels")
                        if (streamingServices.isEmpty()) {
                            InfoPanel("No streaming services identified yet",
                                "The app searches for match-specific service listings as well as TV channels.")
                        } else streamingServices.forEachIndexed { index, service -> ChannelRow(service, index + 1) }

                    }
                    2 -> {
                        SectionHeader("TEAM & LINE-UPS", "Confirmed match-day line-ups when published")
                        if (data?.lineups.isNullOrEmpty()) {
                            InfoPanel("Line-ups not available yet",
                                "Starting XIs are usually published closer to kick-off. The registered squad below is not a confirmed match-day team.")
                            if (!data?.squadPlayers.isNullOrEmpty()) {
                                SectionHeader("REGISTERED PREMIER LEAGUE SQUAD", "Squad numbers · 2026/27")
                                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                    Text(data!!.squadPlayers.joinToString("\n"), Modifier.fillMaxWidth().padding(16.dp), color = Color.White, fontSize = 14.sp, lineHeight = 23.sp)
                                }
                            }
                        } else data!!.lineups.forEach { team ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(team.name.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    team.formation?.let { Text("Formation  $it", color = Muted, fontSize = 12.sp) }
                                    Text("STARTING XI", color = CatRed, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.5.sp)
                                    team.starters.forEachIndexed { index, player ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text((index + 1).toString().padStart(2, '0'), color = CatRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(player, color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                    if (team.substitutes.isNotEmpty()) {
                                        androidx.compose.material3.HorizontalDivider(color = Color(0xFF34363C))
                                        Text("SUBSTITUTES", color = Muted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)
                                        Text(team.substitutes.joinToString("\n"), color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        SectionHeader("RECENT RESULTS", "Latest result information available from public sources")
                        data?.previousFixture?.let { previous ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("PREVIOUS LEAGUE MATCH", color = CatRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                                    Text(previous, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(data.previousKickoff ?: "Date unavailable", color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                        data?.previousCupFixture?.let { previous ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("PREVIOUS CUP / EUROPE MATCH", color = CatRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                                    Text(previous, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(data.previousCupKickoff ?: "Date unavailable", color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                        if (data?.previousFixture.isNullOrBlank() && data?.previousCupFixture.isNullOrBlank())
                            InfoPanel("No recent results found", "The app will retry when it refreshes its public sources.")
                    }
                    else -> {
                        SectionHeader("SEARCH LISTINGS", "Filter the results currently loaded in the app")
                        androidx.compose.material3.OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
                            placeholder = { Text("Channel or guide name", color = Muted) }, shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatRed, unfocusedBorderColor = Color(0xFF3A3D45),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = CatRed
                            )
                        )
                        SectionHeader("MATCHING CHANNELS", "${filteredChannels.size} result${if (filteredChannels.size == 1) "" else "s"}")
                        if (filteredChannels.isEmpty()) InfoPanel("No matching channels", "Try a different term or clear the search. This filters loaded results; it does not search the whole internet.")
                        else filteredChannels.forEachIndexed { index, channel -> ChannelRow(channel, index + 1) }
                        SectionHeader("MATCHING SOURCES", "${filteredGuides.size + filteredSearchResults.size} results")
                        filteredGuides.forEach { guide ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(guide.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(guide.context ?: "Sunderland fixture listing", color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 4)
                                    androidx.compose.material3.TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(guide.url))) }) {
                                        Text("Open source  ↗", color = Color(0xFFFF8B93))
                                    }
                                }
                            }
                        }
                        filteredSearchResults.forEach { result ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(result.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(result.snippet, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                                    Text("Source: ${result.source} · Search area: ${result.region}", color = CatRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17191D))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = CatRed, modifier = Modifier.size(17.dp))
                            Text("BLACK CAT GLOBAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("v${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 10.sp)
                        }
                        Text("Listings refresh every 60 seconds. Broadcaster results may be incomplete or unconfirmed. This app does not host or stream matches.",
                            color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        Button(
                            onClick = {
                                if (!checkingUpdate) {
                                    checkingUpdate = true
                                    updateMessage = null
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) { checkForGitHubUpdate() }
                                        checkingUpdate = false
                                        updateInfo = result
                                        updateMessage = when {
                                            result == null -> "Couldn't check for updates. Check your internet connection and try again."
                                            result.versionCode > BuildConfig.VERSION_CODE -> null
                                            else -> "You're using the latest version."
                                        }
                                        FirebaseAnalytics.getInstance(context).logEvent("update_check", Bundle().apply {
                                            putBoolean("update_available", result != null && result.versionCode > BuildConfig.VERSION_CODE)
                                        })
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !checkingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = CatRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (checkingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (checkingUpdate) "Checking for updates…" else "Check for updates", fontWeight = FontWeight.Bold)
                        }
                        updateMessage?.let { Text(it, color = Muted, fontSize = 12.sp) }
                    }
                }
            }
        }
    }

    if (updateInfo != null && updateInfo!!.versionCode > BuildConfig.VERSION_CODE) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update available") },
            text = { Text("Black Cat Global ${updateInfo!!.tag} is available. Your current version is ${BuildConfig.VERSION_NAME}. Open the APK download page to install the update.") },
            confirmButton = {
                TextButton(onClick = {
                    val url = updateInfo!!.downloadUrl
                    updateInfo = null
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }) { Text("Download update") }
            },
            dismissButton = { TextButton(onClick = { updateInfo = null }) { Text("Not now") } }
        )
    }
}


@Composable
private fun FixtureMatchup(fixture: String) {
    val parts = remember(fixture) {
        Regex("(?i)\\s+(?:v\\.?|vs\\.?|versus)\\s+")
            .split(fixture.trim())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(2)
    }
    var badgeUrls by remember(fixture) { mutableStateOf<List<String?>>(emptyList()) }

    LaunchedEffect(parts) {
        badgeUrls = if (parts.size == 2) {
            withContext(Dispatchers.IO) {
                parts.map { team ->
                    try {
                        val query = enc(team.removeSuffix(" AFC").removeSuffix(" FC").trim())
                        val doc = Jsoup.connect("https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t=$query")
                            .userAgent("BlackCatGlobal-Android")
                            .timeout(7000)
                            .ignoreContentType(true)
                            .get()
                        val teams = doc.select("body").text().let { org.json.JSONObject(it).optJSONArray("teams") }
                        var badge: String? = null
                        if (teams != null) {
                            for (i in 0 until teams.length()) {
                                val item = teams.optJSONObject(i) ?: continue
                                val name = item.optString("strTeam")
                                val candidate = item.optString("strBadge").ifBlank { item.optString("strTeamBadge") }
                                if (candidate.startsWith("https://") &&
                                    (name.equals(team, true) || name.contains(team, true) || team.contains(name, true))) {
                                    badge = candidate
                                    break
                                }
                            }
                            if (badge == null && teams.length() > 0) {
                                val first = teams.optJSONObject(0)
                                val candidate = first?.optString("strBadge").orEmpty().ifBlank { first?.optString("strTeamBadge").orEmpty() }
                                badge = candidate.takeIf { it.startsWith("https://") }
                            }
                        }
                        badge
                    } catch (_: Exception) { null }
                }
            }
        } else emptyList()
    }

    if (parts.size == 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TeamCrest(badgeUrls.getOrNull(0), parts[0], Modifier.size(40.dp))
            Text(
                parts[0],
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text("VS", color = Color(0xFFFFA2A8), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(
                parts[1],
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            TeamCrest(badgeUrls.getOrNull(1), parts[1], Modifier.size(40.dp))
        }
    } else {
        Text(fixture, color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TeamCrest(url: String?, teamName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF30262B), RoundedCornerShape(12.dp)).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "$teamName crest",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Filled.SportsSoccer, contentDescription = "$teamName crest unavailable",
                tint = Muted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
        if (!subtitle.isNullOrBlank()) Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun InfoPanel(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ChannelRow(channel: String, number: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.background(Color(0xFF352027), RoundedCornerShape(9.dp)).padding(horizontal = 9.dp, vertical = 7.dp)) {
                Text(number.toString().padStart(2, '0'), color = Color(0xFFFF8B93), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(channel, Modifier.weight(1f), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("FOUND", color = Muted, fontSize = 9.sp, letterSpacing = 0.8.sp)
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
        val context = container?.text()?.replace(Regex("\\s+"), " ")?.take(900)
        // TV-guide sites often put the fixture and broadcaster in sibling cards, not the
        // same small HTML container. Search progressively larger fixture-related text too.
        val nearbyText = extractBroadcastersNearFixture(doc, anchor, fixture)
        val linkText = container?.select("a")?.map { it.text().trim() }.orEmpty()
        val haystack = (context.orEmpty() + " " + linkText.joinToString(" ") + " " + nearbyText)
        val preciseChannels = broadcasterTerms.filter { term -> haystack.contains(term, ignoreCase = true) }
            .distinctBy { canonicalChannelName(it).lowercase() }
        // Some listing pages render broadcaster labels outside the match card. If the
        // local card extraction misses them, use the guide's own Sunderland listings
        // as a fallback instead of leaving the app's channel tab completely blank.
        val pageText = doc.body().text().replace(Regex("\\s+"), " ")
        // Do not scan the entire page for channel names: that can pick up unrelated
        // competitions, generic streaming-device references, or site-wide promotions.
        // Only show broadcaster names found in the fixture's own nearby listing text.
        val channels = preciseChannels
        GuideResult(
            name = name,
            fixture = anchor?.text()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotBlank() },
            context = context?.takeIf { it.isNotBlank() } ?: nearbyText.take(420).takeIf { it.isNotBlank() },
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

    // Google often publishes the scheduled UK kick-off when TV guides omit it.
    if (kickoff.isNullOrBlank() && !fixture.isNullOrBlank()) {
        kickoff = fetchKickoffFromGoogle(fixture)
        if (!kickoff.isNullOrBlank()) fixtureSource = "Google match listing"
    }

    val searchResults = (if (fixture != null) fetchWorldwideSearchResults(fixture) else emptyList()) +
        (if (fixture != null) curatedBroadcastResults(fixture) else emptyList())
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

/**
 * Extract broadcaster names even when a guide's HTML separates the match card from
 * its channel label. It deliberately starts with the fixture's own ancestors and only
 * falls back to a short nearby body-text window.
 */
private fun extractBroadcastersNearFixture(
    doc: org.jsoup.nodes.Document,
    anchor: org.jsoup.nodes.Element?,
    fixture: String?
): String {
    var node: org.jsoup.nodes.Element? = anchor
    repeat(9) {
        val current = node ?: return@repeat
        val text = current.text().replace(Regex("\\s+"), " ").trim()
        if (text.length in 20..2600 &&
            broadcasterTerms.any { term -> text.contains(term, ignoreCase = true) }) {
            return text
        }
        node = current.parent()
    }

    val body = doc.body().text().replace(Regex("\\s+"), " ").trim()
    val candidates = listOfNotNull(
        anchor?.text()?.takeIf { it.isNotBlank() },
        fixture?.takeIf { it.isNotBlank() },
        "Sunderland"
    )
    for (candidate in candidates) {
        val index = body.indexOf(candidate, ignoreCase = true)
        if (index >= 0) {
            val start = (index - 900).coerceAtLeast(0)
            val end = (index + candidate.length + 1300).coerceAtMost(body.length)
            val nearby = body.substring(start, end)
            if (broadcasterTerms.any { term -> nearby.contains(term, ignoreCase = true) }) return nearby
        }
    }
    return ""
}

private fun fetchKickoffFromGoogle(fixture: String): String? {
    val query = "\"${fixture}\" kick-off time date UK"
    return try {
        val doc = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=8")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(9000)
            .get()
        doc.select("div.MjjYud").firstNotNullOfOrNull { block ->
            val text = listOf(block.selectFirst("h3")?.text().orEmpty(),
                block.selectFirst("div.VwiC3b, div[data-sncf], span.aCOpRe")?.text().orEmpty(),
                block.text()).joinToString(" ")
            extractKickoff(text) ?: extractGoogleKickoff(text)
        }
    } catch (_: Exception) { null }
}

private fun extractGoogleKickoff(text: String): String? {
    val clean = text.replace(Regex("\\s+"), " ")
    val dateMatch = Regex("(?i)\\b(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?,?\\s+([A-Za-z]{3,9})\\s+(\\d{1,2})(?:,?\\s+20\\d{2})?\\b").find(clean)
        ?: return null
    val timeMatch = Regex("(?i)\\b(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(AM|PM)\\b").find(clean)
        ?: return null
    val hour = timeMatch.groupValues[1].toInt()
    val minute = timeMatch.groupValues[2].ifBlank { "00" }
    val suffix = timeMatch.groupValues[3].uppercase()
    val converted = when {
        suffix == "AM" && hour == 12 -> 0
        suffix == "PM" && hour != 12 -> hour + 12
        else -> hour
    }
    return "${dateMatch.groupValues[1].take(3)} ${dateMatch.groupValues[3]} ${dateMatch.groupValues[2].take(3)} • %02d:%s".format(converted, minute)
}

// Structured, match-specific broadcaster data supplied for the Sunderland v Bournemouth example.
 // These are presented as leads, not independently verified rights confirmations.
private fun curatedBroadcastResults(fixture: String): List<WebResult> {
    val hasSunderland = fixture.contains("Sunderland", ignoreCase = true)
    val hasBournemouth = fixture.contains("Bournemouth", ignoreCase = true)
    if (!hasSunderland || !hasBournemouth) return emptyList()

    val matchLabel = "Bournemouth v Sunderland"
    // Match-specific channel names from international fixture listings. Streaming apps are
    // retained where no separate linear channel was specified; availability must be checked locally.
    val broadcasts = listOf(
        "United Kingdom" to listOf("Sky Sports+"),
        "Ireland" to listOf("Sky Sports+"),
        "United States" to listOf("NBCSN", "Peacock"),
        "Canada" to listOf("DAZN Canada", "FuboTV Canada"),
        "Caribbean" to listOf("Disney+ Premium Caribbean"),
        "Argentina" to listOf("Disney+ Premium Argentina"),
        "Brazil" to listOf("Disney+ Premium Brazil"),
        "Chile" to listOf("Disney+ Premium Chile"),
        "Colombia / Venezuela" to listOf("Disney+ Premium Sur"),
        "Bulgaria" to listOf("Nova Sport Bulgaria"),
        "Bosnia and Herzegovina" to listOf("Arena Sport 5"),
        "Czech Republic" to listOf("Canal+ Sport 5", "OnePlay"),
        "Slovakia" to listOf("Canal+ Sport 5", "Voyo"),
        "France" to listOf("Canal+ Live 2", "myCANAL"),
        "Spain" to listOf("DAZN Spain"),
        "Portugal" to listOf("DAZN Portugal"),
        "Netherlands" to listOf("Viaplay Netherlands"),
        "Denmark" to listOf("Viaplay Denmark"),
        "Norway" to listOf("Viaplay Norway"),
        "Sweden" to listOf("V Sport 1", "Viaplay Sweden"),
        "Finland" to listOf("Elisa Viihde Viaplay"),
        "Iceland" to listOf("SÝN Sport 5"),
        "Poland" to listOf("Canal+ Extra 5", "CANAL+ online"),
        "Hungary" to listOf("Match4"),
        "Germany / Austria" to listOf("Sky Sport HD 9", "Sky Go", "WOW"),
        "Greece" to listOf("Nova Sports Start"),
        "Estonia" to listOf("Go3 Extra Sports Estonia"),
        "Latvia" to listOf("Go3 Extra Sports Latvia"),
        "Lithuania" to listOf("Go3 Extra Sports Lithuania"),
        "Ukraine" to listOf("Setanta Sports+"),
        "South Africa" to listOf("MáXimo 360", "DStv App"),
        "India" to listOf("JioHotstar"),
        "Bangladesh / Pakistan" to listOf("Toffee Live", "myco"),
        "Indonesia" to listOf("Vidio"),
        "Japan" to listOf("U-NEXT"),
        "South Korea" to listOf("Coupang Play"),
        "Hong Kong / Macau" to listOf("M Plus Live"),
        "Malaysia" to listOf("Astro Premier League 3", "Astro Go"),
        "Singapore" to listOf("Hub Premier 3", "StarHub TV+"),
        "Thailand / Cambodia / Laos" to listOf("MONOMAX"),
        "Vietnam" to listOf("FPT Play"),
        "China" to listOf("Migu"),
        "Australia" to listOf("Stan Sport"),
        "Turkey" to listOf("beIN CONNECT Turkey", "TOD"),
        "Middle East / North Africa" to listOf("beIN SPORTS CONNECT", "TOD"),
        "Worldwide / regional listings" to listOf("V Sport Premier League 3", "Canal+ Sport 5")
    )
    return broadcasts.map { (region, platforms) ->
        WebResult(
            title = "$matchLabel — $region",
            snippet = "Listed channel(s) / service(s): ${platforms.joinToString(", ")}. These are match-specific listing leads, not a guarantee of carriage in every package; check the broadcaster's local schedule.",
            source = "International match TV listings (unverified)",
            region = region,
            channels = platforms
        )
    }
}

private fun fetchWorldwideSearchResults(fixture: String): List<WebResult> {
    // Search match-specific listings by region, not just general Premier League rights pages.
    // Search areas are labels for the query, not claims that every result is confirmed in that territory.
    val queries = listOf(
        "Worldwide" to "\"$fixture\" international TV broadcast channels worldwide football",
        "Worldwide" to "\"$fixture\" where to watch live TV listings broadcaster by country",
        "UK" to "\"$fixture\" Sky Sports+ Sky Sports Football UK TV listing",
        "USA" to "\"$fixture\" Peacock NBC Sports USA broadcast schedule",
        "Canada" to "\"$fixture\" Fubo Canada Premier League match TV schedule",
        "Australia" to "\"$fixture\" Optus Sport Australia broadcast",
        "New Zealand" to "\"$fixture\" Sky Sport NZ Premier League TV listing",
        "Germany / Austria" to "\"$fixture\" Sky Sport HD 9 Sky Go WOW Deutschland Österreich",
        "France" to "\"$fixture\" Canal+ Live 2 Canal+ Premier League diffusion",
        "Spain" to "\"$fixture\" DAZN España Premier League partido dónde ver",
        "Netherlands" to "\"$fixture\" Viaplay Nederland Premier League wedstrijd",
        "Nordics" to "\"$fixture\" V Sport 1 V Sport Premier League Viaplay Sweden Norway Denmark",
        "Italy" to "\"$fixture\" NOW Italia Premier League partita TV",
        "Poland" to "\"$fixture\" Canal+ Extra 5 Canal+ Online Premier League",
        "Africa" to "\"$fixture\" SuperSport Premier League TV listing",
        "Middle East" to "\"$fixture\" beIN SPORTS MENA TOD Premier League broadcast",
        "India" to "\"$fixture\" JioHotstar Star Sports India Premier League broadcast",
        "Asia" to "\"$fixture\" StarHub TV SPOTV Premier League channel",
        "Brazil" to "\"$fixture\" ESPN Brasil TNT Sports Brasil BandSports transmissão",
        "Portugal" to "\"$fixture\" Sport TV Portugal Premier League transmissão"
    )
    val results = mutableListOf<WebResult>()
    val agent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    for ((region, query) in queries) {
        try {
            val google = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=6")
                .userAgent(agent)
                .timeout(5500)
                .get()
            val blocks = google.select("div.MjjYud, div.g, div.tF2Cxc")
            blocks.forEach { block ->
                val title = block.selectFirst("h3")?.text()?.trim().orEmpty()
                val snippet = block.selectFirst("div.VwiC3b, div[data-sncf], span.aCOpRe, div.IsZvec")?.text()?.trim().orEmpty()
                if (title.isNotBlank()) {
                    val combined = "$title $snippet"
                    val found = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }
                        .distinctBy { it.lowercase() }
                    results.add(WebResult(title, snippet, "Google", region, found))
                }
            }
        } catch (_: Exception) { }

        try {
            val bing = Jsoup.connect("https://www.bing.com/search?q=" + enc(query) + "&count=6")
                .userAgent(agent)
                .timeout(5500)
                .get()
            bing.select("li.b_algo").forEach { block ->
                val title = block.selectFirst("h2")?.text()?.trim().orEmpty()
                val snippet = block.selectFirst("p")?.text()?.trim().orEmpty()
                if (title.isNotBlank()) {
                    val combined = "$title $snippet"
                    val found = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }
                        .distinctBy { it.lowercase() }
                    results.add(WebResult(title, snippet, "Bing", region, found))
                }
            }
        } catch (_: Exception) { }
    }

    // Keep a larger set because each regional query can reveal a different official listing.
    return results.distinctBy { (it.title + "|" + it.region).lowercase() }.take(100)
}

private data class GitHubUpdate(val tag: String, val versionCode: Int, val downloadUrl: String)

private fun checkForGitHubUpdate(): GitHubUpdate? {
    return try {
        val body = Jsoup.connect("https://api.github.com/repos/Anarchyukz/BlackCatGlobal/releases/latest")
            .ignoreContentType(true)
            .header("Accept", "application/vnd.github+json")
            .userAgent("BlackCatGlobal-Android")
            .timeout(12000)
            .execute().body()
        val release = org.json.JSONObject(body)
        val tag = release.optString("tag_name", "")
        val versionCode = Regex("(\\d+)$").find(tag)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val assets = release.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url", "").takeIf { it.startsWith("https://") }
                if (apkUrl != null) break
            }
        }
        val url = apkUrl ?: release.optString("html_url", "").takeIf { it.startsWith("https://") } ?: return null
        GitHubUpdate(tag, versionCode, url)
    } catch (_: Exception) {
        null
    }
}

private fun isStreamingService(raw: String): Boolean {
    val value = raw.trim().lowercase()
    // "Viaplay Sports" is a branded TV channel; plain Viaplay is the streaming service.
    if (value.startsWith("viaplay sports")) return false
    return listOf(
        "peacock", "fubo", "optus sport", "viaplay", "dazn", "stan sport",
        "jiohotstar", "paramount+", "disney+", "hbo max", "now", "sky go",
        "sky sports app", "canal+ app", "canal+ streaming", "wow", "tod",
        "starhub tv", "espn+", "amazon prime", "prime video", "apple tv"
    ).any { value == it || value.startsWith("$it ") || value.endsWith(" $it") }
}

private fun isStreamingPlatformNotChannel(raw: String): Boolean {
    // Services are shown in a separate section; keep actual linear TV channels here.
    // Sky Sports+ and named channels such as NBCSN Extra remain eligible.
    return isStreamingService(raw)
}

private fun canonicalChannelName(raw: String): String {
    // Merge spelling/branding aliases across guide sites and search engines.
    return when (raw.trim().lowercase()) {
        "hbo max" -> "HBO Max"
        "bein sports" -> "beIN SPORTS"
        "fubotv" -> "Fubo"
        "fubo tv canada" -> "Fubo"
        "sky sports+" -> "Sky Sports+"
        "sky sport hd 9" -> "Sky Sport HD 9"
        "canal+ live 2" -> "Canal+ Live 2"
        "canal+ extra 5" -> "Canal+ Extra 5"
        else -> raw.trim()
    }
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
