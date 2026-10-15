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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val CatRed = Color(0xFFE53945)
private val DeepBackground = Color(0xFF101114)
private val Panel = Color(0xFF1B1D22)
private val Muted = Color(0xFFB7BAC3)

data class SearchSource(val title: String, val description: String, val url: (String) -> String)

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
    var fixture by remember { mutableStateOf("") }
    var matchDate by remember { mutableStateOf("") }
    var competition by remember { mutableStateOf("Premier League") }
    var country by remember { mutableStateOf("Worldwide — all countries") }
    var officialOnly by remember { mutableStateOf(true) }
    var hasSearched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    val sources = remember(officialOnly) {
        buildList {
            add(SearchSource("Sunderland AFC", "Club announcements and match broadcast guides") { q ->
                "https://www.google.com/search?q=" + enc("site:safc.com $q TV channel broadcast")
            })
            add(SearchSource("Live Soccer TV", "International TV and streaming listings") { q ->
                "https://www.livesoccertv.com/search/?q=" + enc(q)
            })
            add(SearchSource("Premier League", "Official competition and broadcaster information") { q ->
                "https://www.google.com/search?q=" + enc("site:premierleague.com $q broadcast TV")
            })
            add(SearchSource("Worldwide web search", "Search broadly for country-by-country coverage") { q ->
                "https://www.google.com/search?q=" + enc("$q official broadcaster TV channel streaming")
            })
            add(SearchSource("Bing web search", "A second search index to help find additional listings") { q ->
                "https://www.bing.com/search?q=" + enc("$q official broadcaster TV channel streaming")
            })
            if (!officialOnly) {
                add(SearchSource("Additional public listings", "Broader results; verify the source before relying on it") { q ->
                    "https://www.google.com/search?q=" + enc("$q international football TV listings")
                })
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Text("Find the match. Find the channel.", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Search worldwide for legal TV broadcasters and authorised streaming services showing Sunderland AFC.",
                        color = Muted, fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = fixture,
                        onValueChange = { fixture = it; error = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Fixture or opponent *") },
                        placeholder = { Text("e.g. Sunderland vs Newcastle") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = matchDate,
                        onValueChange = { matchDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Match date (optional)") },
                        placeholder = { Text("e.g. 25 October 2026") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = competition,
                        onValueChange = { competition = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Competition (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Region") },
                        placeholder = { Text("Worldwide — all countries") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = officialOnly, onCheckedChange = { officialOnly = it })
                        Column {
                            Text("Prioritise official sources", fontWeight = FontWeight.SemiBold)
                            Text("Recommended for legal viewing information", color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (error) {
                        Text("Enter a fixture or opponent to start your search.", color = CatRed, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            if (fixture.isBlank()) {
                                error = true
                            } else {
                                hasSearched = true
                                val query = buildString {
                                    append("Sunderland AFC ")
                                    append(fixture.trim())
                                    if (matchDate.isNotBlank()) append(" ").append(matchDate.trim())
                                    if (competition.isNotBlank()) append(" ").append(competition.trim())
                                    append(" ")
                                    append(country.trim().ifBlank { "worldwide all countries" })
                                }
                                // Open the first source immediately; remaining sources are shown below.
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sources.first().url(query))))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CatRed)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("FIND WORLDWIDE CHANNELS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (hasSearched) {
                Text("SEARCH SOURCES", fontSize = 13.sp, color = CatRed, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Text("Open each source to compare its current listings. Search results are not automatically verified by this app.", color = Muted, fontSize = 13.sp)
                val query = buildString {
                    append("Sunderland AFC ")
                    append(fixture.trim())
                    if (matchDate.isNotBlank()) append(" ").append(matchDate.trim())
                    if (competition.isNotBlank()) append(" ").append(competition.trim())
                    append(" ")
                    append(country.trim().ifBlank { "worldwide all countries" })
                }
                sources.forEach { source ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(source.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(source.description, color = Muted, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url(query))))
                            }) {
                                Text("OPEN", color = CatRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("How it works", fontWeight = FontWeight.Bold)
                        Text("1. Enter a Sunderland fixture.\n2. Search official and international listing sources.\n3. Open the results and confirm the broadcaster for the country you need.", color = Muted, fontSize = 13.sp)
                    }
                }
            }

            Text(
                "Legal viewing only • No streams hosted • No automatic background searches",
                color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
    }
}

private fun enc(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
