package com.anarchyukz.blackcatglobal

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal data class LeagueTableTeam(
    val position: Int,
    val name: String,
    val played: Int,
    val goalDifference: Int,
    val points: Int
)

/**
 * Read the current Premier League standings from Google's public search table first,
 * with the official Sunderland AFC site as a fallback. This is a display-only scrape:
 * Google or the site can change its HTML, so callers handle an empty result gracefully.
 */
internal fun loadPremierLeagueTable(): List<LeagueTableTeam> {
    val sources = listOf(
        "https://www.google.com/search?q=Premier+League+table",
        "https://www.safc.com/"
    )
    for (source in sources) {
        val parsed = runCatching {
            val document = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(12000)
                .get()
            val rows = document.select("table tr")
            rows.mapNotNull { row -> parseLeagueTableRow(row) }
                .distinctBy { it.position }
                .sortedBy { it.position }
                .takeIf { it.size >= 10 }
        }.getOrNull()
        if (!parsed.isNullOrEmpty()) return parsed
    }
    return emptyList()
}

private fun parseLeagueTableRow(row: Element): LeagueTableTeam? {
    val cells = row.select("th, td")
    if (cells.size < 8) return null
    val values = cells.map { it.text().replace(Regex("\\s+"), " ").trim() }
    val position = values.firstOrNull()?.toIntOrNull() ?: return null
    if (position !in 1..20) return null

    // Club names sometimes live in image alt text rather than visible cell text.
    val nameCell = cells.getOrNull(1) ?: return null
    val name = nameCell.text().replace(Regex("\\s+"), " ").trim().ifBlank {
        nameCell.selectFirst("img")?.attr("alt").orEmpty().trim()
    }
    if (name.isBlank() || name.length > 45 || name.all { it.isDigit() }) return null

    // Expected order after position and club: played, won, drawn, lost, GF, GA, GD, points.
    val numbers = values.drop(2).map { it.replace("+", "").toIntOrNull() }
    if (numbers.size < 8 || numbers.take(6).any { it == null }) return null
    val played = numbers[0] ?: return null
    val goalFor = numbers[4] ?: return null
    val goalAgainst = numbers[5] ?: return null
    val goalDifference = numbers.getOrNull(6) ?: (goalFor - goalAgainst)
    val points = numbers.getOrNull(7) ?: return null
    if (played !in 0..38 || points !in 0..114) return null
    return LeagueTableTeam(position, name, played, goalDifference, points)
}
