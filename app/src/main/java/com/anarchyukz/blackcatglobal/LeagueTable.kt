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

/** Parse a public standings table; prefer Sky Sports over brittle Google search markup. */
internal fun loadPremierLeagueTable(): List<LeagueTableTeam> {
    val sources = listOf(
        "https://www.skysports.com/premier-league-table",
        "https://www.safc.com/"
    )
    for (source in sources) {
        val parsed = runCatching {
            val document = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(12000)
                .get()
            document.select("tr, .standing-table__row, [class*=tableRow], [class*=standings-row]")
                .mapNotNull { parseLeagueTableRow(it) }
                .distinctBy { it.position }
                .sortedBy { it.position }
                .takeIf { it.size >= 15 && it.any { team -> team.name.contains("Sunderland", true) } }
        }.getOrNull()
        if (!parsed.isNullOrEmpty()) return parsed
    }
    return emptyList()
}

private fun parseLeagueTableRow(row: Element): LeagueTableTeam? {
    val cells = row.select("th, td, .standing-table__cell, [class*=cell]")
    if (cells.size < 8) return null
    val values = cells.map { it.text().replace(Regex("\\s+"), " ").trim() }
    val position = values.firstOrNull()?.toIntOrNull() ?: return null
    if (position !in 1..20) return null
    val name = values.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    val numbers = values.drop(2).map { it.replace("−", "-").replace("+", "").toIntOrNull() }
    if (numbers.size < 8 || numbers.take(6).any { it == null }) return null
    val played = numbers[0] ?: return null
    val goalFor = numbers[4] ?: return null
    val goalAgainst = numbers[5] ?: return null
    val goalDifference = numbers.getOrNull(6) ?: (goalFor - goalAgainst)
    val points = numbers.getOrNull(7) ?: return null
    if (played !in 0..38 || points !in 0..114) return null
    return LeagueTableTeam(position, name, played, goalDifference, points)
}
