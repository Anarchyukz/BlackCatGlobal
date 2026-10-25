package com.anarchyukz.blackcatglobal

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

// Lightweight in-app browser ad/tracker filter. This only blocks known advertising
// and tracking hosts; publisher article content and first-party page requests are left alone.
private val blockedAdHosts = setOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com",
    "adservice.google.com", "pagead2.googlesyndication.com", "adnxs.com",
    "adsrvr.org", "adform.net", "adroll.com", "advertising.com",
    "taboola.com", "outbrain.com", "mgid.com", "revcontent.com",
    "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com",
    "openx.net", "openx.com", "casalemedia.com", "smartadserver.com",
    "adcolony.com", "applovin.com", "unityads.unity3d.com",
    "moatads.com", "amazon-adsystem.com", "adnxs-simple.com",
    "scorecardresearch.com", "quantserve.com", "hotjar.com",
    "branch.io", "app-measurement.com", "connect.facebook.net"
)

private fun isBlockedAdRequest(rawUrl: String?): Boolean {
    val host = runCatching { Uri.parse(rawUrl ?: return false).host?.lowercase() }.getOrNull()
        ?: return false
    return blockedAdHosts.any { host == it || host.endsWith(".$it") }
}

private const val AD_CLEANUP_SCRIPT = """
(function() {
  const styleId = 'blackcat-ad-cleanup';
  if (!document.getElementById(styleId)) {
    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = [
      '[id*="google_ads"], [id^="ad-slot"], [id^="ad-container"],',
      '[class*="advertisement"], [class*="advertising"], [class*="ad-banner"],',
      '[class*="ad-container"], [class*="ad-slot"], [class*="sponsor-widget"],',
      'ins.adsbygoogle, iframe[src*="doubleclick"], iframe[src*="googlesyndication"]',
      '{display:none!important;visibility:hidden!important;max-height:0!important;min-height:0!important;pointer-events:none!important;}'
    ].join('');
    (document.head || document.documentElement).appendChild(style);
  }
})();
"""

private fun formatPodcastTime(milliseconds: Long): String {
    val total = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

private data class PodcastEpisode(val title: String, val date: String, val audioUrl: String, val duration: String = "")

private data class PodcastFeed(val name: String, val rssUrl: String, val appleId: String? = null)

private val podcastFeeds = listOf(
    PodcastFeed("Wise Men Say", "https://feeds.acast.com/public/shows/1354251d-765d-4d91-b940-b43f7fac9d73"),
    PodcastFeed("Haway The Podcast", "https://podcasts.apple.com/gb/podcast/haway-the-podcast-sunderland-afc/id1800684854", "1800684854"),
    PodcastFeed("WTF Sunderland", "https://feed.podbean.com/whatthefalkpod/feed.xml"),
    PodcastFeed("The Roar", "https://anchor.fm/s/d3c3478/podcast/rss")
)

private suspend fun loadPodcastEpisodes(feed: PodcastFeed): List<PodcastEpisode> = withContext(Dispatchers.IO) {
    runCatching {
        val rssUrl = if (feed.appleId != null) {
            // Apple lookup provides the publisher's actual RSS URL; audio still streams directly.
            val json = Jsoup.connect("https://itunes.apple.com/lookup?id=${feed.appleId}")
                .ignoreContentType(true).userAgent("BlackCatGlobal/1.0").timeout(12000).execute().body()
            org.json.JSONObject(json).optJSONArray("results")?.optJSONObject(0)?.optString("feedUrl")
                ?.takeIf { it.startsWith("http") } ?: feed.rssUrl
        } else feed.rssUrl
        val xml = Jsoup.connect(rssUrl).ignoreContentType(true).parser(org.jsoup.parser.Parser.xmlParser())
            .userAgent("BlackCatGlobal/1.0").timeout(15000).get()
        xml.select("item").mapNotNull { item ->
            val enclosure = item.selectFirst("enclosure[url]")?.attr("url")
                ?: item.selectFirst("media|content[url]")?.attr("url")
                ?: return@mapNotNull null
            val title = item.selectFirst("title")?.text()?.trim().orEmpty().ifBlank { "Podcast episode" }
            val date = item.selectFirst("pubDate")?.text()?.trim().orEmpty()
            val duration = item.selectFirst("itunes|duration")?.text()?.trim().orEmpty()
            PodcastEpisode(title, date, enclosure, duration)
        }.take(30)
    }.getOrDefault(emptyList())
}

private val CatRed = Color(0xFFE53945)
private val DeepBackground = Color(0xFF101114)
private val Panel = Color(0xFF1B1D22)
private val Muted = Color(0xFFB7BAC3)

@Composable
private fun BlackCatAppWithSplash() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800L)
        showSplash = false
    }
    Box(Modifier.fillMaxSize()) {
        if (showSplash) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF09090B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bcg),
                        contentDescription = "BlackCat Global artwork",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text("BlackCat Global", color = Color.White, fontWeight = FontWeight.Black,
                        fontSize = 28.sp, letterSpacing = 0.2.sp)
                    Text("SUNDERLAND AFC", color = Color(0xFFFFA2A8), fontSize = 11.sp,
                        letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CatRed, strokeWidth = 2.dp)
                }
            }
        } else {
            BlackCatApp()
        }
    }
}

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
    val substitutes: List<String>,
    val status: String = "CONFIRMED",
    val source: String = "ESPN"
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
    "Sky Sports Main Event", "Sky Sports Premier League", "Sky Sports Ultra HDR",
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
                BlackCatAppWithSplash()
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
    var selectedTab by remember { mutableIntStateOf(5) }
    var selectedPodcast by remember { mutableIntStateOf(0) }
    var newsLoading by remember { mutableStateOf(true) }
    var newsArticles by remember { mutableStateOf<List<SunderlandNewsArticle>>(emptyList()) }
    var newsCategory by remember { mutableStateOf("Sunderland AFC") }
    var newsRefreshToken by remember { mutableIntStateOf(0) }
    var readArticleUrls by remember {
        mutableStateOf(context.getSharedPreferences("blackcat_news", android.content.Context.MODE_PRIVATE)
            .getStringSet("read_urls", emptySet()).orEmpty().toSet())
    }
    var readerArticle by remember { mutableStateOf<SunderlandNewsArticle?>(null) }
    var readerContent by remember { mutableStateOf("") }
    var readerLoading by remember { mutableStateOf(false) }
    var readerUnavailable by remember { mutableStateOf(false) }
    var readerBrowserUrl by remember { mutableStateOf<String?>(null) }
    var readerWebView by remember { mutableStateOf<WebView?>(null) }
    var leagueLoading by remember { mutableStateOf(true) }
    var leagueTeams by remember { mutableStateOf<List<LeagueTableTeam>>(emptyList()) }
    var leagueRefreshToken by remember { mutableIntStateOf(0) }
    var fixturesLoading by remember { mutableStateOf(true) }
    var upcomingFixtures by remember { mutableStateOf<List<SunderlandFixture>>(emptyList()) }
    var worldwideSearchResults by remember { mutableStateOf<List<WebResult>>(emptyList()) }
    var worldwideSearchLoading by remember { mutableStateOf(false) }
    var lineupTeams by remember { mutableStateOf<List<LineupTeam>>(emptyList()) }
    var lineupsLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAllChannels by remember { mutableStateOf(false) }
    var analyticsSnapshotLogged by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<GitHubUpdate?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = readerArticle != null) {
        if (readerBrowserUrl != null) {
            if (readerWebView?.canGoBack() == true) {
                readerWebView?.goBack()
            } else {
                readerBrowserUrl = null
                readerWebView = null
            }
        } else {
            readerArticle = null
            readerContent = ""
            readerUnavailable = false
        }
    }

    LaunchedEffect(newsRefreshToken) {
        newsLoading = true
        val firstLoad = withContext(Dispatchers.IO) { runCatching { loadSunderlandNews() }.getOrDefault(emptyList()) }
        if (firstLoad.isNotEmpty()) newsArticles = firstLoad
        newsLoading = false
        while (true) {
            kotlinx.coroutines.delay(5 * 60_000L)
            newsLoading = true
            val refreshed = withContext(Dispatchers.IO) { runCatching { loadSunderlandNews() }.getOrDefault(emptyList()) }
            if (refreshed.isNotEmpty()) newsArticles = refreshed
            newsLoading = false
        }
    }

    LaunchedEffect(leagueRefreshToken) {
        leagueLoading = true
        fixturesLoading = true
        val initial = withContext(Dispatchers.IO) {
            Pair(
                runCatching { loadPremierLeagueTable() }.getOrDefault(emptyList()),
                runCatching { loadSunderlandFixtures() }.getOrDefault(emptyList())
            )
        }
        if (initial.first.isNotEmpty()) leagueTeams = initial.first
        if (initial.second.isNotEmpty()) upcomingFixtures = initial.second
        leagueLoading = false
        fixturesLoading = false
        while (true) {
            kotlinx.coroutines.delay(5 * 60_000L)
            val refreshed = withContext(Dispatchers.IO) {
                Pair(
                    runCatching { loadPremierLeagueTable() }.getOrDefault(emptyList()),
                    runCatching { loadSunderlandFixtures() }.getOrDefault(emptyList())
                )
            }
            if (refreshed.first.isNotEmpty()) leagueTeams = refreshed.first
            if (refreshed.second.isNotEmpty()) upcomingFixtures = refreshed.second
        }
    }

    LaunchedEffect(refreshToken) {
        loading = true
        dashboard = withContext(Dispatchers.IO) { loadDashboard(context) }
        loading = false
        while (true) {
            kotlinx.coroutines.delay(5 * 60_000L)
            loading = true
            dashboard = withContext(Dispatchers.IO) { loadDashboard(context) }
            loading = false
        }
    }

    val data = dashboard
    // Keep the TV finder tied to the TV-guide fixture, as it was before cross-competition
    // fixtures were wired into channel discovery. Cup/European fixtures remain in League.
    val nextSunderlandFixture = upcomingFixtures.minByOrNull { it.sortTime }

    fun selectedFixtureForLiveData(): String? {
        if (data?.fixtureSource?.contains("Current match", true) == true && !data.fixture.isNullOrBlank()) {
            return data.fixture
        }
        return data?.fixture
    }

    // Keep the channel search independent from line-up lookup, but query the same
    // TV-guide fixture that the working version used.
    LaunchedEffect(selectedFixtureForLiveData(), refreshToken) {
        val target = selectedFixtureForLiveData()
        if (target.isNullOrBlank()) {
            worldwideSearchResults = emptyList()
            lineupTeams = emptyList()
            worldwideSearchLoading = false
            lineupsLoading = false
        } else {
            worldwideSearchLoading = true
            lineupsLoading = true
            val channelJob = async(Dispatchers.IO) {
                runCatching { fetchWorldwideSearchResults(target) }.getOrDefault(emptyList())
            }
            val lineupJob = async(Dispatchers.IO) {
                runCatching { fetchLineups(target) }.getOrDefault(emptyList())
            }
            lineupTeams = lineupJob.await()
            lineupsLoading = false
            worldwideSearchResults = channelJob.await()
            worldwideSearchLoading = false
        }
        while (true) {
            kotlinx.coroutines.delay(5 * 60_000L)
            val target = selectedFixtureForLiveData() ?: continue
            worldwideSearchLoading = true
            lineupsLoading = true
            val channelJob = async(Dispatchers.IO) {
                runCatching { fetchWorldwideSearchResults(target) }.getOrDefault(emptyList())
            }
            val lineupJob = async(Dispatchers.IO) {
                runCatching { fetchLineups(target) }.getOrDefault(emptyList())
            }
            worldwideSearchResults = channelJob.await()
            worldwideSearchLoading = false
            lineupTeams = lineupJob.await()
            lineupsLoading = false
        }
    }

    // Restore the previously working TV fallback: combine guide results, targeted
    // search results and the match-specific fallback list. Keep the last good set
    // visible if a source temporarily returns an empty response.
    val freshChannels = remember(data, worldwideSearchResults) {
        val fixture = data?.fixture.orEmpty()
        val fallbackChannels = if (fixture.isNotBlank()) {
            curatedBroadcastResults(fixture).flatMap { it.channels }
        } else emptyList()
        (data?.guides.orEmpty().flatMap { it.channels } +
            worldwideSearchResults.flatMap { it.channels } +
            fallbackChannels)
            .map { canonicalChannelName(it.trim()) }
            .filter { it.isNotBlank() && !isStreamingPlatformNotChannel(it) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
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
    val regionalFallbackResults = remember(data?.fixture) {
        data?.fixture?.takeIf { it.contains("Sunderland", true) }
            ?.let { curatedBroadcastResults(it) }.orEmpty()
    }
    val filteredSearchResults = remember(worldwideSearchResults, regionalFallbackResults, searchQuery) {
        (worldwideSearchResults + regionalFallbackResults)
            .distinctBy { (it.title + "|" + it.region).lowercase() }
            .filter { searchQuery.isBlank() ||
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
                        Triple("News", Icons.Filled.Article, 5),
                        Triple("TV", Icons.Filled.SportsSoccer, 0),
                        Triple("League", Icons.Filled.Public, 1),
                        Triple("Podcasts", Icons.Filled.Headphones, 2),
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
                        Image(
                            painter = painterResource(id = R.drawable.bcg),
                            contentDescription = "BlackCat Global logo",
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("BlackCat Global", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 0.1.sp, maxLines = 1)
                            Text("SUNDERLAND AFC", color = Color(0xFFFFA2A8), fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    androidx.compose.material3.IconButton(onClick = { refreshToken++; newsRefreshToken++; leagueRefreshToken++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh listings", tint = if (loading) CatRed else Muted)
                    }
                }

                if (loading && selectedTab == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF282027))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                color = CatRed,
                                strokeWidth = 2.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "SEARCHING MATCH LISTINGS",
                                    color = Color(0xFFFFA2A8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "Checking public TV guides and worldwide sources…",
                                    color = Muted,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                when (selectedTab) {
                    5 -> {
                        val visibleNewsArticles = remember(newsArticles, newsCategory) {
                            // Hide pirate-stream spam from news results without suppressing ordinary
                            // match previews, reports, transfer stories or legitimate broadcaster news.
                            fun isFootballStreamSpam(article: SunderlandNewsArticle): Boolean {
                                val title = article.title.lowercase()
                                val summary = article.summary.lowercase()
                                val url = article.url.lowercase()
                                val source = article.source.lowercase()
                                val text = "$title $summary"
                                val suspiciousHosts = listOf(
                                    "totalsportek", "footybite", "stream2watch", "crackstreams",
                                    "livetv.sx", "vipbox", "buffstreams", "rojadirecta",
                                    "hesgoal", "vipleague", "soccerstreams", "sportshub"
                                )
                                if (suspiciousHosts.any { url.contains(it) || source.contains(it) }) return true
                                val spamPhrases = listOf(
                                    "football streams", "soccer streams", "free live stream",
                                    "free football stream", "watch football live free",
                                    "watch live online free", "live streaming links",
                                    "free streaming links", "watch matches for free",
                                    "unofficial stream", "pirated stream", "pirate stream",
                                    "live match stream links"
                                )
                                if (spamPhrases.any { text.contains(it) }) return true
                                // Generic "watch live" alone is not enough to block a legitimate
                                // broadcaster article; require a free-stream/link cue as well.
                                val streamCue = Regex(
                                    "(?i)\\b(streams?|streaming|watch live)\\b"
                                ).containsMatchIn(text)
                                val illegalCue = Regex(
                                    "(?i)\\b(free|unofficial|pirate[sd]?|illegal|links?|no sign[- ]?up)\\b"
                                ).containsMatchIn(text)
                                return streamCue && illegalCue &&
                                    Regex("(?i)\\b(football|soccer|premier league|sunderland|match)\\b").containsMatchIn(text)
                            }
                            fun isSunderlandFootball(article: SunderlandNewsArticle): Boolean {
                                val title = article.title
                                val source = article.source
                                val url = article.url.lowercase()
                                // Feed/domain classification is more reliable than matching broad words
                                // in article summaries, which caused local stories to leak into AFC news.
                                if (source.contains("Roker Report", true) ||
                                    url.contains("rokerreport.sbnation.com") ||
                                    url.contains("/sport/football/sunderland-afc") ||
                                    source.contains("ChronicleLive · Sunderland AFC", true) ||
                                    source.contains("Sunderland Echo Sport", true)) return true
                                val searchable = "$title ${article.summary}"
                                val clubMention = Regex(
                                    "(?i)\\b(Sunderland AFC|SAFC|Black Cats|Sunderland|Régis Le Bris|Roker Report|Wilson Isidor|Dan Neil|Chris Rigg)\\b"
                                ).containsMatchIn(searchable)
                                val footballContext = Regex(
                                    "(?i)\\b(football|transfer|team news|match report|starting XI|line[- ]?up|fixture|Premier League|manager|striker|goal|kick-off|kickoff|league|cup tie|signing|contract)\\b"
                                ).containsMatchIn(searchable)
                                val knownAfcPublisher = source.contains("Roker Report", true) ||
                                    url.contains("rokerreport.sbnation.com") ||
                                    url.contains("/sport/football/sunderland-afc") ||
                                    source.contains("ChronicleLive · Sunderland AFC", true) ||
                                    source.contains("Sunderland Echo Sport", true)
                                return knownAfcPublisher || (clubMention && footballContext)
                            }
                            fun isLocalPublisher(article: SunderlandNewsArticle): Boolean {
                                val source = article.source.lowercase()
                                val url = article.url.lowercase()
                                return source.contains("chroniclelive") || source.contains("sunderland echo") ||
                                    source.contains("northern echo") || source.contains("local news") ||
                                    source.contains("bbc") || source.contains("itv") ||
                                    source.contains("shieldsgazette") || source.contains("shields gazette") ||
                                    url.contains("chroniclelive.co.uk") || url.contains("sunderlandecho.com") ||
                                    url.contains("northernecho.co.uk") || url.contains("bbc.co.uk") ||
                                    url.contains("itv.com") || url.contains("shieldsgazette.com")
                            }
                            when (newsCategory) {
                                "Sunderland AFC" -> newsArticles.filter { !isFootballStreamSpam(it) && isSunderlandFootball(it) }
                                    .sortedWith(
                                        compareByDescending<SunderlandNewsArticle> { it.publishedTimestamp }
                                            .thenBy {
                                                if (it.source.contains("Roker Report", true) ||
                                                    it.url.contains("rokerreport.sbnation.com", true)) 0 else 1
                                            }
                                    )
                                "Local News" -> // Local coverage can include Sunderland AFC and other local football stories too.
                                // Keep local-publisher filtering and the illegal-stream spam filter, but do not exclude football articles.
                                newsArticles.filter { !isFootballStreamSpam(it) && isLocalPublisher(it) }
                                    .sortedByDescending { it.publishedTimestamp }
                                else -> newsArticles.filter { !isFootballStreamSpam(it) && isSunderlandFootball(it) }
                                    .sortedByDescending { it.publishedTimestamp }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("SUNDERLAND NEWS", color = CatRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.4.sp)
                                Text("RSS FEEDS · AUTO-REFRESH EVERY 5 MIN", color = Muted, fontSize = 9.sp)
                            }
                        }
                        Text(
                            "Choose Sunderland AFC coverage or news from across the local area.",
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Sunderland AFC", "Local News").forEach { category ->
                                Button(
                                    onClick = { newsCategory = category },
                                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (newsCategory == category) CatRed else Color(0xFF292B31),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 12.sp)
                                }
                            }
                        }
                        if (newsLoading && newsArticles.isEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CatRed, strokeWidth = 2.dp)
                                Text("Loading RSS feeds…", color = Muted, fontSize = 12.sp)
                            }
                        } else if (visibleNewsArticles.isEmpty()) {
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Panel)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        if (newsArticles.isEmpty()) "No RSS headlines available" else "No headlines in this section",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (newsArticles.isEmpty()) "The feeds may be temporarily unavailable. Use the refresh icon at the top of the screen to try again." else "There are no articles in this section yet. Use the refresh icon at the top of the screen to reload the feeds.",
                                        color = Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            visibleNewsArticles.forEach { article ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Panel)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            article.imageUrl?.let { imageUrl ->
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = article.title,
                                                    modifier = Modifier.width(112.dp).height(84.dp),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(article.source.uppercase(), color = CatRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(article.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp)
                                                Text(article.published, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Text(article.summary, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("OPEN ARTICLE ↗", color = Color(0xFFFF8B93), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    0 -> {
                        // Use the exact user-supplied banner asset, cropped to a compact card.
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.banner),
                                contentDescription = "Local takeaway discounts",
                                modifier = Modifier.fillMaxWidth().height(132.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(8.dp).background(CatRed, RoundedCornerShape(50)))
                            Text(if (loading) "UPDATING MATCH CENTRE" else "MATCH CENTRE", color = CatRed, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.8.sp)
                            Spacer(Modifier.weight(1f))
                            Text("AUTO REFRESH · 5 MIN", color = Muted, fontSize = 9.sp)
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
                                Text("UP NEXT", color = Color(0xFFFFA2A8), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                                // The home screen uses the dated fixture schedule, not the TV-guide
                                // page's first Sunderland match link (which can point to a later game).
                                FixtureMatchup(
                                    fixture = nextSunderlandFixture?.let { scheduled ->
                                        if (scheduled.venue.equals("Home", true)) {
                                            "Sunderland vs ${scheduled.opponent}"
                                        } else {
                                            "${scheduled.opponent} vs Sunderland"
                                        }
                                    } ?: if (loading) "Finding Sunderland's next match…" else "Fixture unavailable"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = CatRed, modifier = Modifier.size(19.dp))
                                    Text(
                                        nextSunderlandFixture?.date ?: data?.kickoff
                                            ?: "Kick-off time will appear when confirmed",
                                        color = Muted, fontSize = 13.sp, lineHeight = 18.sp
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(color = Color(0xFF4B343A))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("WORLDWIDE TV", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                                        Text("${channels.size} channel${if (channels.size == 1) "" else "s"} found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    androidx.compose.material3.TextButton(onClick = { selectedTab = 0 }) {
                                        Text("View channels  ›", color = Color(0xFFFF8B93), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        SectionHeader("BROADCAST SNAPSHOT", "Names found in public listings and web results")
                        if (channels.isEmpty()) {
                            InfoPanel(
                                if (loading || worldwideSearchLoading) "Checking worldwide listings" else "No verified channel matches found yet",
                                "Checking the selected fixture across regional listings. Use the single refresh icon at the top to retry; streaming-only services are kept out of the TV-channel list."
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                channels.take(if (showAllChannels) channels.size else 6).forEachIndexed { index, channel -> ChannelRow(channel, index + 1) }
                                if (channels.size > 6) androidx.compose.material3.TextButton(onClick = { showAllChannels = !showAllChannels }) {
                                    Text(if (showAllChannels) "Show fewer channels" else "See all ${channels.size} channels", color = Color(0xFFFF8B93))
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
                        SectionHeader("PREMIER LEAGUE TABLE", "2026/27 standings · public table source")
                        if (leagueLoading && leagueTeams.isEmpty()) {
                            InfoPanel("Loading league table…", "Getting the latest Premier League standings.")
                        } else if (leagueTeams.isEmpty()) {
                            InfoPanel("League table unavailable", "The standings source did not return a table. Tap refresh to try again.")
                            androidx.compose.material3.TextButton(onClick = { leagueRefreshToken++ }) {
                                Text("Try again", color = Color(0xFFFF8B93))
                            }
                        } else {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("POS", Modifier.width(32.dp), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("CLUB", Modifier.weight(1f), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("P", Modifier.width(22.dp), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Text("GD", Modifier.width(34.dp), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Text("PTS", Modifier.width(30.dp), color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF3A3D45))
                                    leagueTeams.forEach { team ->
                                        val isSunderland = team.name.contains("Sunderland", true)
                                        Row(
                                            Modifier.fillMaxWidth().background(if (isSunderland) Color(0xFF3A1820) else Color.Transparent).padding(vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(team.position.toString(), Modifier.width(32.dp), color = if (isSunderland) Color(0xFFFFA2A8) else Muted, fontSize = 11.sp, fontWeight = if (isSunderland) FontWeight.ExtraBold else FontWeight.Normal)
                                            Text(team.name, Modifier.weight(1f), color = if (isSunderland) Color.White else Color(0xFFE6E6E6), fontSize = 11.sp, fontWeight = if (isSunderland) FontWeight.ExtraBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(team.played.toString(), Modifier.width(22.dp), color = Muted, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Text(if (team.goalDifference > 0) "+${team.goalDifference}" else team.goalDifference.toString(), Modifier.width(34.dp), color = Muted, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            Text(team.points.toString(), Modifier.width(30.dp), color = if (isSunderland) Color(0xFFFFA2A8) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                        }
                                        androidx.compose.material3.HorizontalDivider(color = Color(0xFF303239), thickness = 0.5.dp)
                                    }
                                }
                            }
                            Text("P = played · GD = goal difference · PTS = points. Standings refresh automatically every five minutes.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                            androidx.compose.material3.TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.skysports.com/premier-league-table"))) }) {
                                Text("Open full league table ↗", color = Color(0xFFFF8B93))
                            }
                        }
                        SectionHeader("NEXT SUNDERLAND MATCHES", "Premier League and any European or cup fixtures · nearest kick-offs first")
                        if (fixturesLoading && upcomingFixtures.isEmpty()) {
                            InfoPanel("Loading fixtures…", "Checking Sunderland's public fixture listings.")
                        } else if (upcomingFixtures.isEmpty()) {
                            InfoPanel("Fixtures temporarily unavailable", "The fixture source did not return readable upcoming matches. Tap refresh to try again.")
                            androidx.compose.material3.TextButton(onClick = { leagueRefreshToken++ }) {
                                Text("Retry fixtures", color = Color(0xFFFF8B93))
                            }
                        } else {
                            val closestKickoff = upcomingFixtures.minOfOrNull { it.sortTime }
                            val closestMatches = if (closestKickoff == null) emptyList() else upcomingFixtures.filter {
                                kotlin.math.abs(it.sortTime - closestKickoff) <= 48L * 60L * 60L * 1000L
                            }
                            val displayedFixtures = if (closestMatches.isNotEmpty()) closestMatches else upcomingFixtures.take(2)
                            Text("Showing the next scheduled match(es) across all competitions.", color = Muted, fontSize = 11.sp)
                            displayedFixtures.forEach { fixture ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixture.url)))
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Panel)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(fixture.competition.uppercase(), color = CatRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                            Text(if (fixture.venue == "Home") "Sunderland vs ${fixture.opponent}" else "${fixture.opponent} vs Sunderland", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("${fixture.date} · ${fixture.venue}", color = Muted, fontSize = 11.sp)
                                        }
                                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open fixture", tint = Muted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        SectionHeader("SUNDERLAND PODCASTS", "The latest voices, reactions and debate from Wearside")
                        val podcasts = listOf(
                            Triple("Wise Men Say", "4.5 / 5  ·  2–3 times weekly", "The longest-running Sunderland AFC podcast, with a strong community feel."),
                            Triple("Haway The Podcast", "4.8 / 5  ·  Daily (short/medium)", "Instant post-match reactions and daily updates."),
                            Triple("WTF Sunderland", "4.5 / 5  ·  Weekly", "Opposition fan previews and player interviews."),
                            Triple("The Roar", "4.1 / 5  ·  Weekly", "Sunderland Echo writers discuss the latest news, views and debate.")
                        )
                        val podcastUrls = listOf(
                            "https://shows.acast.com/wisemensay",
                            "https://app.sport-social.co.uk/podcast/haway-the-podcast",
                            "https://whatthefalkpod.podbean.com/",
                            "https://podcasts.apple.com/gb/podcast/the-roar-podcast-sunderland-echo/id1476471017"
                        )
                        podcasts.forEachIndexed { index, podcast ->
                            Card(
                                Modifier.fillMaxWidth().clickable { selectedPodcast = index },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = if (selectedPodcast == index) Color(0xFF382027) else Panel)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(44.dp).background(if (index == 0) CatRed else Color(0xFF30343D), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(podcast.first, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(podcast.second, color = Color(0xFFFFB45E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(podcast.third, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                                        if (index == 0) Text("FEATURED PODCAST", color = Color(0xFFFFA2A8), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                    Icon(Icons.Filled.Headphones, contentDescription = if (selectedPodcast == index) "Selected podcast" else "Select ${podcast.first}", tint = Color(0xFFFFA2A8))
                                }
                            }
                        }
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
                        var episodes by remember { mutableStateOf<List<PodcastEpisode>>(emptyList()) }
                        var feedLoading by remember { mutableStateOf(true) }
                        var feedError by remember { mutableStateOf(false) }
                        var activeEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
                        val player = remember(context) { ExoPlayer.Builder(context).build() }
                        var isPlaying by remember { mutableStateOf(false) }
                        var playbackPosition by remember { mutableStateOf(0L) }
                        var playbackDuration by remember { mutableStateOf(0L) }
                        DisposableEffect(player) {
                            val listener = object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                                override fun onPlaybackStateChanged(state: Int) {
                                    playbackPosition = player.currentPosition.coerceAtLeast(0L)
                                    playbackDuration = player.duration.takeIf { it > 0 } ?: 0L
                                }
                            }
                            player.addListener(listener)
                            onDispose { player.removeListener(listener); player.release() }
                        }
                        LaunchedEffect(selectedPodcast) {
                            feedLoading = true
                            feedError = false
                            episodes = loadPodcastEpisodes(podcastFeeds[selectedPodcast])
                            feedError = episodes.isEmpty()
                            feedLoading = false
                        }
                        LaunchedEffect(isPlaying) {
                            while (isPlaying) {
                                playbackPosition = player.currentPosition.coerceAtLeast(0L)
                                playbackDuration = player.duration.takeIf { it > 0 } ?: 0L
                                kotlinx.coroutines.delay(500)
                            }
                        }
                        Text("NATIVE PLAYER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF252027))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(activeEpisode?.title ?: "Choose an episode below", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Text(podcasts[selectedPodcast].first, color = Color(0xFFFFA2A8), fontSize = 11.sp)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    androidx.compose.material3.FilledTonalButton(onClick = {
                                        if (player.mediaItemCount > 0) {
                                            if (player.isPlaying) player.pause() else player.play()
                                        } else if (episodes.isNotEmpty()) {
                                            activeEpisode = episodes.first()
                                            player.setMediaItem(MediaItem.fromUri(episodes.first().audioUrl))
                                            player.prepare()
                                            player.play()
                                        }
                                    }, enabled = player.mediaItemCount > 0 || episodes.isNotEmpty()) {
                                        Icon(if (isPlaying) Icons.Filled.History else Icons.Filled.SportsSoccer, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isPlaying) "Pause" else "Play")
                                    }
                                    androidx.compose.material3.FilledTonalButton(
                                        onClick = {
                                            if (player.mediaItemCount > 0) player.seekTo((player.currentPosition - 30_000L).coerceAtLeast(0L))
                                        },
                                        enabled = player.mediaItemCount > 0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text("−30s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    androidx.compose.material3.FilledTonalButton(
                                        onClick = {
                                            if (player.mediaItemCount > 0) player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                                        },
                                        enabled = player.mediaItemCount > 0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text("−10s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    androidx.compose.material3.FilledTonalButton(
                                        onClick = {
                                            if (player.mediaItemCount > 0) {
                                                val target = player.currentPosition + 10_000L
                                                val duration = player.duration
                                                player.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
                                            }
                                        },
                                        enabled = player.mediaItemCount > 0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text("+10s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    androidx.compose.material3.FilledTonalButton(
                                        onClick = {
                                            if (player.mediaItemCount > 0) {
                                                val target = player.currentPosition + 30_000L
                                                val duration = player.duration
                                                player.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
                                            }
                                        },
                                        enabled = player.mediaItemCount > 0,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text("+30s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${formatPodcastTime(playbackPosition)} / ${formatPodcastTime(playbackDuration)}", color = Muted, fontSize = 11.sp)
                                    androidx.compose.material3.Slider(
                                        value = if (playbackDuration > 0) playbackPosition.toFloat().coerceIn(0f, playbackDuration.toFloat()) else 0f,
                                        onValueChange = { playbackPosition = it.toLong() },
                                        onValueChangeFinished = { player.seekTo(playbackPosition) },
                                        valueRange = 0f..(playbackDuration.coerceAtLeast(1L).toFloat())
                                    )
                                }
                                }
                            }
                        Text("LATEST EPISODES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        when {
                            feedLoading -> InfoPanel("Loading episodes…", "Fetching the podcast RSS feed.")
                            feedError -> InfoPanel("Episodes unavailable", "This podcast feed could not be loaded right now. Try again later.")
                            else -> episodes.forEach { episode ->
                                Card(Modifier.fillMaxWidth().clickable {
                                    activeEpisode = episode
                                    player.setMediaItem(MediaItem.fromUri(episode.audioUrl))
                                    player.prepare()
                                    player.play()
                                }, shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (activeEpisode?.audioUrl == episode.audioUrl) Color(0xFF382027) else Panel)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Filled.Headphones, contentDescription = "Play episode", tint = Color(0xFFFFA2A8), modifier = Modifier.size(24.dp))
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(episode.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            Text(listOf(episode.date, episode.duration.takeIf { it.isNotBlank() }?.let { "${it} min" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFFFFA2A8))
                                    }
                                }
                            }
                        }
                        Text("Audio streams directly in BlackCat Global. You can leave the tab and use Android's media controls while listening.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
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
                                if ((candidate.startsWith("https://", true) || candidate.startsWith("http://", true)) &&
                                    (name.equals(team, true) || name.contains(team, true) || team.contains(name, true))) {
                                    badge = candidate.replaceFirst("http://", "https://", ignoreCase = true)
                                    break
                                }
                            }
                            if (badge == null && teams.length() > 0) {
                                // Only use a fallback result when its team name actually matches;
                                // the API can return similarly named clubs ahead of the requested team.
                                val matching = (0 until teams.length()).mapNotNull { teams.optJSONObject(it) }
                                    .firstOrNull { item ->
                                        val name = item.optString("strTeam")
                                        name.equals(team, true) || name.contains(team, true) || team.contains(name, true)
                                    }
                                val candidate = matching?.optString("strBadge").orEmpty()
                                    .ifBlank { matching?.optString("strTeamBadge").orEmpty() }
                                badge = candidate.takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }
                                    ?.replaceFirst("http://", "https://", ignoreCase = true)
                            }
                        }
                        // Stable Premier League fallback for Leeds if TheSportsDB has no badge.
                        if (badge.isNullOrBlank() && team.contains("Leeds", true)) {
                            badge = "https://assets.football-logos.cc/logos/england/256x256/leeds-united.26dba8f5.png"
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamCrest(badgeUrls.getOrNull(0), parts[0], Modifier.size(64.dp))
                Text(
                    parts[0],
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("VS", color = Color(0xFFFFA2A8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamCrest(badgeUrls.getOrNull(1), parts[1], Modifier.size(64.dp))
                Text(
                    parts[1],
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        Text(fixture, color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TeamCrest(url: String?, teamName: String, modifier: Modifier = Modifier) {
    var imageFailed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier.background(Color(0xFF30262B), RoundedCornerShape(12.dp)).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = url,
                contentDescription = "$teamName crest",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onError = { imageFailed = true }
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
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
    }
    val lower = channel.lowercase()
    val brand = when {
        "sky" in lower -> "sky\\nsports"
        "tnt" in lower -> "TNT\\nSPORTS"
        "bein" in lower -> "beIN\\nSPORTS"
        "espn" in lower -> "ESPN"
        "bbc" in lower -> "BBC"
        "itv" in lower -> "itv"
        "supersport" in lower -> "SUPER\\nSPORT"
        "dazn" in lower -> "DAZN"
        "canal" in lower -> "CANAL+"
        "optus" in lower -> "optus\\nsport"
        "peacock" in lower -> "peacock"
        "fox" in lower -> "FOX\\nSPORTS"
        "sportsnet" in lower -> "SN"
        else -> channel.split(" ").take(2).joinToString(" ") { it.take(5) }.uppercase()
    }
    val brandColor = when {
        "sky" in lower -> Color(0xFF1265B5)
        "tnt" in lower -> Color(0xFFB719C8)
        "bein" in lower -> Color(0xFF6B3CA0)
        "espn" in lower || "fox" in lower -> Color(0xFFB8202F)
        "bbc" in lower -> Color(0xFF30343B)
        "dazn" in lower -> Color.Black
        "canal" in lower -> Color(0xFF263F9D)
        else -> Color(0xFF18354C)
    }
    // Dedicated broadcaster logo artwork (not browser favicons). Wikimedia Commons
    // Special:FilePath resolves each maintained logo asset to its image file.
    val logoUrl = when {
        "sky sports+" in lower || "sky sports plus" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Sky_Sports_%2B_-_Logo_2025.svg"
        "sky" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Sky_Sports_2025.svg"
        "tnt" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/TNT_Sports_%282023%29.svg"
        "bein" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/BeIN_Sports_logo.svg"
        "espn" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/ESPN_wordmark.svg"
        "bbc" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/BBC_Sport_logo.svg"
        "itv" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/ITV_logo.svg"
        "dazn" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/DAZN_Logo.svg"
        "canal" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Canal%2B_logo.svg"
        "optus" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Optus_Sport_logo.svg"
        "peacock" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Peacock_TV_logo.svg"
        "fox" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Fox_Sports_logo.svg"
        "sportsnet" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Sportsnet_logo.svg"
        "nbc" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/NBC_Sports_logo.svg"
        "cbs" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/CBS_Sports_logo.svg"
        "paramount" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Paramount%2B_logo.svg"
        "prime" in lower || "amazon" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/Prime_Video_logo.svg"
        "fubo" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/FuboTV_logo.svg"
        "supersport" in lower ->
            "https://commons.wikimedia.org/wiki/Special:FilePath/SuperSport_logo.svg"
        else -> null
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111820))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
            Box(Modifier.size(width = 92.dp, height = 48.dp).background(Color.White, RoundedCornerShape(5.dp)).padding(4.dp),
                contentAlignment = Alignment.Center) {
                if (logoUrl != null && !logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        imageLoader = imageLoader,
                        contentDescription = "$channel logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        onError = { logoFailed = true }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(brandColor, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text(brand, color = Color.White, fontSize = if (brand.length > 8) 10.sp else 13.sp,
                            lineHeight = 13.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(channel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("WORLDWIDE LISTING  •  $number", color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Filled.OpenInNew, contentDescription = "Channel listing", tint = Color(0xFF91A8C8), modifier = Modifier.size(19.dp))
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
            val fixtureTeams = f.split(Regex("(?i)\\s+v(?:s)?\\.?\\s+|\\s+versus\\s+"))
                .map { it.trim() }.filter { it.isNotBlank() }
            doc.select("a").firstOrNull { a ->
                val t = a.text().replace(Regex("\\s+"), " ").trim()
                val normalizedText = t.lowercase()
                val teamsMatch = fixtureTeams.size >= 2 && fixtureTeams.all { team ->
                    if (team.equals("Sunderland", true)) normalizedText.contains("sunderland")
                    else if (normalizedText.contains(team.lowercase())) true
                    else team.split(Regex("\\s+"))
                        .filter { it.length >= 4 && it.lowercase() !in setOf("united", "city", "town", "athletic", "rovers", "county") }
                        .any { word -> normalizedText.contains(word.lowercase()) }
                }
                t.contains("Sunderland", true) &&
                    Regex("(?i)\\b(vs?\\.?|versus)\\b").containsMatchIn(t) &&
                    teamsMatch && t.length < 120
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
        val fixtureSpecificChannels = if (name == "TV Guide UK" && anchor != null) {
            extractFixtureSpecificChannels(anchor.absUrl("href"), fixture)
        } else emptyList()
        // TV Guide UK is the source for the headline channel list: if its exact match
        // page cannot be verified, do not substitute a nearby card that may be another game.
        val channels = if (name == "TV Guide UK") fixtureSpecificChannels else preciseChannels
            .distinctBy { it.lowercase() }
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

    // Search results and line-ups load in their own background task from the UI.
    // Keep dashboard scraping independent so slow international sources cannot block it.
    val searchResults = emptyList<WebResult>()
    val lineups = emptyList<LineupTeam>()
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
        .map { it.trim() }.filter { it.isNotBlank() }
    if (sides.size < 2) return emptyList()

    fun normalizeTeam(value: String): String = value.lowercase()
        .replace(Regex("\\b(afc|fc|united|city|town)\\b"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ").trim()
    fun sameTeam(expected: String, actual: String): Boolean {
        val e = normalizeTeam(expected)
        val a = normalizeTeam(actual)
        if (e.isBlank() || a.isBlank()) return false
        return e == a || e.startsWith(a) || a.startsWith(e) ||
            e.split(" ").filter { it.length >= 4 }.any { word -> a.split(" ").contains(word) }
    }

    val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    val from = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(now.time)
    now.add(java.util.Calendar.DAY_OF_YEAR, 45)
    val to = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(now.time)

    val candidates = mutableListOf<Pair<String, org.json.JSONObject>>()
    for (league in listOf("eng.1", "eng.2")) {
        try {
            val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$league/scoreboard?dates=$from-$to&limit=100"
            val body = Jsoup.connect(url).ignoreContentType(true)
                .userAgent("BlackCatGlobal-Android").timeout(10000).execute().body()
            val events = org.json.JSONObject(body).optJSONArray("events") ?: continue
            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                val competitors = event.optJSONObject("competitions")?.optJSONArray("competitors")
                    ?: event.optJSONArray("competitors")
                val names = mutableListOf<String>()
                if (competitors != null) for (j in 0 until competitors.length()) {
                    val team = competitors.optJSONObject(j)?.optJSONObject("team")
                    team?.optString("displayName")?.takeIf { it.isNotBlank() }?.let(names::add)
                    team?.optString("shortDisplayName")?.takeIf { it.isNotBlank() }?.let(names::add)
                }
                if (names.any { it.contains("Sunderland", true) } &&
                    sides.all { expected -> names.any { actual -> sameTeam(expected, actual) } }) {
                    candidates.add(league to event)
                }
            }
        } catch (_: Exception) { }
    }

    val eventPair = candidates.minByOrNull { (_, event) ->
        runCatching { java.time.Instant.parse(event.optString("date")).toEpochMilli() }.getOrDefault(Long.MAX_VALUE)
    }
    if (eventPair != null) {
        val (league, event) = eventPair
        val eventId = event.optString("id").takeIf { it.isNotBlank() }
        if (eventId != null) {
            for (summaryLeague in listOf(league, "eng.1", "eng.2").distinct()) {
                val parsed = try {
                    val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$summaryLeague/summary?event=$eventId"
                    val body = Jsoup.connect(url).ignoreContentType(true)
                        .userAgent("BlackCatGlobal-Android").timeout(10000).execute().body()
                    parseEspnLineups(body)
                } catch (_: Exception) { emptyList() }
                if (parsed.size >= 2 && parsed.all { it.starters.isNotEmpty() }) {
                    return parsed.map { it.copy(status = "CONFIRMED", source = "ESPN") }
                }
            }
        }
    }
    return fetchPredictedLineups(fixture, sides)
}

private fun fetchPredictedLineups(fixture: String, sides: List<String>): List<LineupTeam> {
    val userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    fun norm(value: String): String = normalizeLineupTeam(value)
        .replace(Regex("\\b(afc|fc|united|city|town)\\b"), "")
        .replace(Regex("\\s+"), " ").trim()

    fun parseLikelyPage(url: String): List<LineupTeam> {
        val doc = Jsoup.connect(url).userAgent(userAgent).timeout(15000).get()
        val body = doc.body().text().replace(Regex("\\s+"), " ")
        val result = mutableListOf<LineupTeam>()
        for (side in sides) {
            val sideWords = norm(side).split(" ").filter { it.length >= 3 }
            val teamHeading = Regex("(?i)([A-Za-zÀ-ÿ.'’&-]+(?:\\s+[A-Za-zÀ-ÿ.'’&-]+){0,4})\\s+predicted lineup")
                .findAll(body).firstOrNull { m ->
                    val heading = norm(m.groupValues[1])
                    heading == norm(side) || (sideWords.isNotEmpty() && sideWords.all { heading.contains(it) })
                } ?: continue

            val afterHeading = teamHeading.range.last + 1
            val nextTeamHeading = Regex("(?i)[A-Za-zÀ-ÿ.'’&-]+(?:\\s+[A-Za-zÀ-ÿ.'’&-]+){0,4}\\s+predicted lineup")
                .find(body, afterHeading)
            val section = body.substring(afterHeading, nextTeamHeading?.range?.first ?: minOf(body.length, afterHeading + 3500))
            val formation = Regex("(?i)Formation\\s+([0-9]+(?:-[0-9]+){1,3})").find(section)?.groupValues?.get(1)
            val likelyIndex = section.indexOf("Likely XI", ignoreCase = true)
            val confirmedIndex = section.indexOf("Confirmed XI", ignoreCase = true)
            val listStart = when {
                likelyIndex >= 0 -> likelyIndex + "Likely XI".length
                confirmedIndex >= 0 -> confirmedIndex + "Confirmed XI".length
                else -> continue
            }
            // Keep the numbered player list: the "Projected from..." sentence comes before it.
            val listText = section.substring(listStart)
                .substringBefore("Team news")
                .substringBefore("Injuries")
                .substringBefore("Unavailable")
                .substringBefore("Doubtful")
            // The page text can flatten list items differently across mobile/desktop HTML.
            // First extract numbered shirt-number entries, then fall back to numbered names
            // without relying on the position abbreviation being present in the text.
            val players = Regex("(?i)(?:^|\\s)\\d+\\.\\s+\\d+\\s+(.+?)\\s+[GDMF](?=\\s+\\d+\\.\\s+\\d+\\s+|\\s*$)")
                .findAll(listText)
                .map { it.groupValues[1].trim().trimEnd(',', '.') }
                .filter { it.length in 2..60 && !it.contains("Projected from", true) }
                .distinct()
                .take(11)
                .toList()
            if (players.size >= 8) {
                val isConfirmed = confirmedIndex >= 0 && (likelyIndex < 0 || confirmedIndex < likelyIndex)
                result.add(LineupTeam(side, formation, players, emptyList(), if (isConfirmed) "CONFIRMED" else "PREDICTED", "Likely Lineups"))
            }
        }
        return result
    }

    val directUrls = buildList {
        if (fixture.contains("Sunderland", true) && fixture.contains("Leeds", true)) {
            // Try both current fixture-specific sources before doing a search.
            add("https://likelylineups.com/sunderland-vs-leeds-united-lineups/")
        }
        val query = "\"$fixture\" lineups site:likelylineups.com"
        runCatching {
            val search = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=8")
                .userAgent(userAgent).timeout(10000).get()
            search.select("a[href]").mapNotNull { anchor ->
                val raw = anchor.absUrl("href")
                val resolved = if (raw.contains("google.com/url", true)) Uri.parse(raw).getQueryParameter("q") ?: raw else raw
                resolved.takeIf { it.contains("likelylineups.com/", true) && it.contains("lineups", true) && !it.contains("google.com", true) }
            }.forEach { if (it !in this) add(it) }
        }
    }
    for (url in directUrls) {
        val parsed = runCatching { parseLikelyPage(url) }.getOrDefault(emptyList())
        if (parsed.size == sides.size && parsed.all { it.starters.size >= 8 }) return parsed
    }

    // Reliable textual fallback: Sports Mole's team-news articles use semicolon-separated XIs.
    return try {
        // Prefer a known, current fixture article so Google search markup or anti-bot
        // changes cannot prevent the predicted XI from loading for this match.
        val directArticle = if (fixture.contains("Sunderland", true) && fixture.contains("Leeds", true)) {
            "https://www.sportsmole.co.uk/football/sunderland/team-news/sunderland-vs-leeds-injury-suspension-list-predicted-xis_607603.html"
        } else null
        val articleUrl = directArticle ?: run {
            val query = "\"$fixture\" predicted XI site:sportsmole.co.uk/football"
            val search = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=8")
                .userAgent(userAgent).timeout(10000).get()
            search.select("a[href]").mapNotNull { anchor ->
                val raw = anchor.absUrl("href")
                val resolved = if (raw.contains("google.com/url", true)) Uri.parse(raw).getQueryParameter("q") ?: raw else raw
                resolved.takeIf { it.contains("sportsmole.co.uk/football/", true) && it.contains("team-news", true) && !it.contains("google.com", true) }
            }.firstOrNull()
        } ?: return emptyList()
        val articleText = Jsoup.connect(articleUrl).userAgent(userAgent).timeout(12000).get().body().text().replace(Regex("\\s+"), " ")
        val labels = Regex("(?i)Sports Mole\\s*['’]s\\s*predicted XI:")
        val matches = labels.findAll(articleText).toList()
        val xIs = matches.mapIndexedNotNull { index, match ->
            val end = matches.getOrNull(index + 1)?.range?.first ?: minOf(articleText.length, match.range.last + 350)
            articleText.substring(match.range.last + 1, end)
                .substringBefore("Our prediction")
                .substringBefore("Our verdict")
                .trim().trimStart(':', ' ')
                .split(Regex("\\s*;\\s*"))
                .map(String::trim)
                .filter { it.length in 2..60 }
                .takeIf { it.size >= 8 }
        }
        if (xIs.size >= 2 && sides.size >= 2) sides.take(2).mapIndexed { index, side ->
            LineupTeam(side, null, xIs[index].take(11), emptyList(), "PREDICTED", "Sports Mole")
        } else emptyList()
    } catch (_: Exception) { emptyList() }
}

private fun normalizeLineupTeam(value: String): String = value.lowercase()
    .replace(Regex("\\b(afc|fc|united|city|town)\\b"), "")
    .replace(Regex("[^a-z0-9 ]"), " ")
    .replace(Regex("\\s+"), " ").trim()

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

/**
 * Reads broadcaster names from the exact TVGuide.co.uk fixture page rather than
 * inheriting channels from search-engine snippets or unrelated matches.
 */
private fun extractFixtureSpecificChannels(url: String, fixture: String?): List<String> {
    if (!url.startsWith("https://www.tvguide.co.uk/", ignoreCase = true) ||
        !url.contains("/sport/football/fixtures/", ignoreCase = true) ||
        fixture.isNullOrBlank()) return emptyList()
    return try {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(9000)
            .get()
        val body = doc.body().text().replace(Regex("\\s+"), " ").trim()
        val sides = fixture.split(Regex("(?i)\\s+(?:v|vs|versus)\\.?\\s+"))
            .map { it.trim() }.filter { it.length > 2 }
        if (sides.size < 2 || sides.any { !body.contains(it, ignoreCase = true) }) return emptyList()
        val markers = listOf("Broadcast details", "TV channel", "Live TV coverage")
        val markerIndex = markers.map { body.indexOf(it, ignoreCase = true) }
            .filter { it >= 0 }.minOrNull() ?: return emptyList()
        val section = body.substring(markerIndex, (markerIndex + 900).coerceAtMost(body.length))
        val terms = (broadcasterTerms + listOf(
            "TNT Sports 1", "TNT Sports 2", "TNT Sports 3", "TNT Sports 4",
            "Sky Sports Main Event", "Sky Sports Football", "Sky Sports+",
            "BBC One", "BBC Two", "ITV1", "ITV4"
        )).distinct().sortedByDescending { it.length }
        val matches = terms.filter { section.contains(it, ignoreCase = true) }
        matches.filter { candidate ->
            matches.none { other ->
                !other.equals(candidate, ignoreCase = true) &&
                    other.length > candidate.length &&
                    other.startsWith(candidate, ignoreCase = true)
            }
        }.map { canonicalChannelName(it) }
            .filter { it.isNotBlank() && !isStreamingPlatformNotChannel(it) }
            .distinctBy { it.lowercase() }
    } catch (_: Exception) {
        emptyList()
    }
}

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
    if (!hasSunderland) return emptyList()
    if (!hasBournemouth) return worldwideBroadcastDirectory(fixture)

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

/**
 * Broad international linear-channel leads for Sunderland fixtures other than the
 * one legacy match-specific set above. These are not fixture-confirmed: regional
 * schedules and rights can vary, so the UI labels them as channels to check.
 */
private fun worldwideBroadcastDirectory(fixture: String): List<WebResult> {
    val matchLabel = fixture.replace(Regex("\\s+"), " ").trim()
    val broadcasts = listOf(
        "United Kingdom / Ireland" to listOf("Sky Sports+"),
        "United States" to listOf("USA Network", "NBCSN"),
        "Brazil" to listOf("ESPN Brasil", "TNT Sports Brasil", "BandSports"),
        "France" to listOf("Canal+ Live 2", "Canal+ Sport"),
        "Germany / Austria" to listOf("Sky Sport HD 9", "Sky Sport Premier League"),
        "Nordics" to listOf("V Sport 1", "V Sport Premier League"),
        "Bulgaria" to listOf("Nova Sport Bulgaria"),
        "Bosnia and Herzegovina" to listOf("Arena Sport 5"),
        "Poland" to listOf("Canal+ Extra 5"),
        "Czech Republic / Slovakia" to listOf("Canal+ Sport 5"),
        "Portugal" to listOf("Sport TV"),
        "Greece" to listOf("Nova Sports Start"),
        "Iceland" to listOf("SÝN Sport 5"),
        "South Africa" to listOf("SuperSport Premier League", "MáXimo 360"),
        "India" to listOf("Star Sports"),
        "Malaysia" to listOf("Astro Premier League 3"),
        "Singapore" to listOf("Hub Premier 3"),
        "New Zealand" to listOf("Sky Sport Premier League"),
        "Middle East / North Africa" to listOf("beIN SPORTS")
    )
    return broadcasts.map { (region, channels) ->
        WebResult(
            title = "$matchLabel — $region",
            snippet = "Regional linear-channel candidates: ${channels.joinToString(", ")}. This is a broadcaster directory, not confirmation that this exact fixture is scheduled; check the local channel guide.",
            source = "International broadcasters (check fixture)",
            region = region,
            channels = channels
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
        "Portugal" to "\"$fixture\" Sport TV Portugal Premier League transmissão",
        "Mexico / Central America" to "\"$fixture\" Sky Sports Mexico Fox Sports Centroamérica Premier League transmisión",
        "Argentina / Chile" to "\"$fixture\" ESPN Argentina Chile Disney+ Premium fútbol Premier League",
        "Bulgaria" to "\"$fixture\" Nova Sport Bulgaria Premier League TV",
        "Balkans" to "\"$fixture\" Arena Sport Sportklub Premier League TV Serbia Croatia Bosnia",
        "Czech Republic / Slovakia" to "\"$fixture\" Canal+ Sport 5 Premier League Czech Slovakia",
        "Hungary / Romania" to "\"$fixture\" Match4 Premier League Hungary Digi Sport Romania",
        "Sub-Saharan Africa" to "\"$fixture\" SuperSport Premier League South Africa Nigeria Ghana channel",
        "Japan" to "\"$fixture\" Premier League Japan TV channel U-NEXT",
        "South Korea" to "\"$fixture\" Premier League South Korea TV channel Coupang Play",
        "China" to "\"$fixture\" Premier League China TV channel Migu",
        "Southeast Asia" to "\"$fixture\" Premier League Malaysia Singapore Thailand Vietnam TV channel",
        "Middle East" to "\"$fixture\" Premier League beIN SPORTS channel Qatar Saudi Arabia UAE"
    )
    val agent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"
    // Query regions concurrently: sequential Google + Bing requests could take several
    // minutes and previously delayed the line-up screen as well as the TV listings.
    val executor = java.util.concurrent.Executors.newFixedThreadPool(10)
    val futures = queries.map { (region, query) ->
        executor.submit(java.util.concurrent.Callable {
            val local = mutableListOf<WebResult>()
            try {
                val google = Jsoup.connect("https://www.google.com/search?q=" + enc(query) + "&num=6")
                    .userAgent(agent).timeout(4500).get()
                google.select("div.MjjYud, div.g, div.tF2Cxc").forEach { block ->
                    val title = block.selectFirst("h3")?.text()?.trim().orEmpty()
                    val snippet = block.selectFirst("div.VwiC3b, div[data-sncf], span.aCOpRe, div.IsZvec")?.text()?.trim().orEmpty()
                    if (title.isNotBlank()) {
                        val combined = "$title $snippet"
                        val found = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }
                            .distinctBy { it.lowercase() }
                        local.add(WebResult(title, snippet, "Google", region, found))
                    }
                }
            } catch (_: Exception) { }
            try {
                val bing = Jsoup.connect("https://www.bing.com/search?q=" + enc(query) + "&count=6")
                    .userAgent(agent).timeout(4500).get()
                bing.select("li.b_algo").forEach { block ->
                    val title = block.selectFirst("h2")?.text()?.trim().orEmpty()
                    val snippet = block.selectFirst("p")?.text()?.trim().orEmpty()
                    if (title.isNotBlank()) {
                        val combined = "$title $snippet"
                        val found = broadcasterTerms.filter { combined.contains(it, ignoreCase = true) }
                            .distinctBy { it.lowercase() }
                        local.add(WebResult(title, snippet, "Bing", region, found))
                    }
                }
            } catch (_: Exception) { }
            local
        })
    }
    val results = mutableListOf<WebResult>()
    try {
        futures.forEach { future ->
            runCatching { results.addAll(future.get(22, java.util.concurrent.TimeUnit.SECONDS)) }
        }
    } finally {
        executor.shutdownNow()
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
