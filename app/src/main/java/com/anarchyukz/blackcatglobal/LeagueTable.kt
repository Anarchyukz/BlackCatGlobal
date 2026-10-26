package com.anarchyukz.blackcatglobal

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale

internal data class LeagueTableTeam(
    val position: Int,
    val name: String,
    val played: Int,
    val goalDifference: Int,
    val points: Int
)

/**
 * ESPN's public standings JSON is the primary source; Sky Sports HTML is a fallback.
 * Return an empty list only when neither source yields a credible 20-team PL table.
 */
internal fun loadPremierLeagueTable(): List<LeagueTableTeam> {
    // Soccer standings live under ESPN's /apis/v2/ route. The /apis/site/v2/
    // route is only a stub for this endpoint and can return no table at all.
    val espnUrls = listOf(
        "https://site.api.espn.com/apis/v2/sports/soccer/eng.1/standings",
        "https://site.web.api.espn.com/apis/v2/sports/soccer/eng.1/standings"
    )
    val espn = espnUrls.firstNotNullOfOrNull { url ->
        runCatching {
            val body = Jsoup.connect(url)
                .userAgent("BlackCatGlobal/1.0 (Android)")
                .timeout(15000)
                .ignoreContentType(true)
                .execute().body()
            val root = JSONObject(body)
            val entries = mutableListOf<JSONObject>()
            collectStandingEntries(root, entries)
            entries.mapIndexedNotNull { index, entry -> parseEspnEntry(entry, index + 1) }
                .distinctBy { it.name }
                .sortedBy { it.position }
                .takeIf { table ->
                    table.size >= 15 && table.any { team -> team.name.contains("Sunderland", true) }
                }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }
    if (!espn.isNullOrEmpty()) return espn

    val sky = runCatching {
        val document = Jsoup.connect("https://www.skysports.com/premier-league-table")
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
            .timeout(15000)
            .get()
        val rows = (document.select("tr") + document.select(".standing-table__row"))
            .distinct()
        rows.mapNotNull { parseLeagueTableRow(it) }
            .distinctBy { it.position }
            .sortedBy { it.position }
            .takeIf { it.size >= 15 && it.any { team -> team.name.contains("Sunderland", true) } }
    }.getOrNull()
    return sky.orEmpty()
}

private fun collectStandingEntries(value: Any?, out: MutableList<JSONObject>) {
    when (value) {
        is JSONObject -> {
            val team = value.optJSONObject("team")
            val stats = value.optJSONArray("stats")
            if (team != null && stats != null && team.optString("displayName").isNotBlank()) {
                out.add(value)
            } else {
                val keys = value.keys()
                while (keys.hasNext()) collectStandingEntries(value.opt(keys.next()), out)
            }
        }
        is JSONArray -> for (i in 0 until value.length()) collectStandingEntries(value.opt(i), out)
    }
}

private fun parseEspnEntry(entry: JSONObject, fallbackPosition: Int): LeagueTableTeam? {
    val team = entry.optJSONObject("team") ?: return null
    val name = team.optString("displayName").ifBlank { team.optString("name") }.trim()
    if (name.isBlank()) return null
    val stats = entry.optJSONArray("stats") ?: return null
    val values = mutableMapOf<String, Double>()
    for (i in 0 until stats.length()) {
        val stat = stats.optJSONObject(i) ?: continue
        val key = listOf("name", "abbreviation", "shortDisplayName", "displayName")
            .map { stat.optString(it) }.firstOrNull { it.isNotBlank() }?.lowercase(Locale.UK).orEmpty()
        val raw = stat.opt("value")
        val number = when (raw) {
            is Number -> raw.toDouble()
            else -> raw?.toString()?.toDoubleOrNull()
                ?: stat.optString("displayValue").replace("+", "").replace("−", "-").toDoubleOrNull()
        }
        if (key.isNotBlank() && number != null) values[key] = number
    }
    fun stat(vararg keys: String): Int? {
        val found = keys.firstNotNullOfOrNull { key ->
            values.entries.firstOrNull { it.key.contains(key) }?.value
        }
        return found?.toInt()
    }
    val played = stat("gamesplayed", "played", "gp") ?: return null
    val points = stat("points", "pts") ?: return null
    val gd = stat("pointdifferential", "goaldifference", "goal difference", "gd") ?: 0
    val position = stat("rank", "position", "teamrank") ?: fallbackPosition
    if (position !in 1..30 || played !in 0..38 || points !in 0..114) return null
    return LeagueTableTeam(position, name, played, gd, points)
}

private fun parseLeagueTableRow(row: Element): LeagueTableTeam? {
    val tableCells = row.select("th, td")
    val cells = if (tableCells.size >= 8) tableCells else {
        row.children().filter { it.classNames().any { name -> name.contains("cell", true) } }
    }
    if (cells.size < 8) return null
    val values = cells.map { it.text().replace(Regex("\\s+"), " ").trim() }
    val position = values.firstOrNull()?.toIntOrNull() ?: return null
    if (position !in 1..20) return null
    val name = values.drop(1).firstOrNull { value ->
        value.isNotBlank() && value.any(Char::isLetter) && !value.contains("played", true)
    } ?: return null
    val nameIndex = values.indexOf(name)
    val numbers = values.drop(nameIndex + 1).map { it.replace("−", "-").replace("+", "").toIntOrNull() }
    if (numbers.size < 8 || numbers.take(6).any { it == null }) return null
    val played = numbers[0] ?: return null
    val goalFor = numbers[4] ?: return null
    val goalAgainst = numbers[5] ?: return null
    val goalDifference = numbers.getOrNull(6) ?: (goalFor - goalAgainst)
    val points = numbers.getOrNull(7) ?: return null
    if (played !in 0..38 || points !in 0..114) return null
    return LeagueTableTeam(position, name, played, goalDifference, points)
}
