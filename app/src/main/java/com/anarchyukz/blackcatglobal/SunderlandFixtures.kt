package com.anarchyukz.blackcatglobal

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class SunderlandFixture(
    val date: String,
    val opponent: String,
    val competition: String,
    val venue: String,
    val sortTime: Long,
    val url: String
)

/** Reads future Sunderland fixtures from public fixture listings, without inventing fallback dates. */
internal fun loadSunderlandFixtures(): List<SunderlandFixture> {
    val sources = listOf(
        "https://www.safc.com/",
        "https://www.skysports.com/sunderland-scores-fixtures"
    )
    val now = System.currentTimeMillis()
    for (source in sources) {
        val parsed = runCatching {
            val doc = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(12000)
                .get()
            doc.select("a, li, article, [class*=fixture], [class*=match]")
                .mapNotNull { parseSunderlandFixture(it, source, now) }
                .distinctBy { it.opponent.lowercase() + "|" + it.date + "|" + it.competition }
                .sortedBy { it.sortTime }
                .take(12)
        }.getOrNull().orEmpty()
        if (parsed.isNotEmpty()) return parsed
    }
    return emptyList()
}

private fun parseSunderlandFixture(element: Element, source: String, now: Long): SunderlandFixture? {
    val text = element.text().replace(Regex("\\s+"), " ").trim()
    if (text.length !in 20..600 || !text.contains("Sunderland", true)) return null
    val dateMatch = Regex("(?i)\\b(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(?:\\s+(20\\d{2}))?\\b").find(text) ?: return null
    val day = dateMatch.groupValues[1].toIntOrNull() ?: return null
    val month = dateMatch.groupValues[2].replaceFirstChar { it.uppercase() }
    val london = TimeZone.getTimeZone("Europe/London")
    val calendar = java.util.Calendar.getInstance(london)
    val year = dateMatch.groupValues[3].toIntOrNull() ?: calendar.get(java.util.Calendar.YEAR)
    val timeText = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(text)?.value ?: "23:59"
    val millis = runCatching {
        SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK).apply {
            timeZone = london
            isLenient = false
        }.parse("$day $month $year $timeText")?.time
    }.getOrNull() ?: return null
    if (millis < now - 60_000L) return null
    val matchup = Regex("(?i)(Sunderland(?: AFC)?\\s+(?:vs?\\.?|v)\\s+([^|,–-]{2,45})|([^|,–-]{2,45})\\s+(?:vs?\\.?|v)\\s+Sunderland)").find(text) ?: return null
    val home = matchup.groupValues[1].isNotBlank()
    val opponent = (if (home) matchup.groupValues[2] else matchup.groupValues[3]).trim()
        .removeSuffix(" AFC").trim()
    if (opponent.isBlank() || opponent.contains("Sunderland", true)) return null
    val competition = when {
        Regex("(?i)Europa League|UEFA").containsMatchIn(text) -> "UEFA Europa League"
        Regex("(?i)FA Cup").containsMatchIn(text) -> "FA Cup"
        Regex("(?i)EFL Cup|Carabao|League Cup").containsMatchIn(text) -> "EFL Cup"
        Regex("(?i)Premier League").containsMatchIn(text) -> "Premier League"
        else -> "Sunderland fixture"
    }
    val dateLabel = SimpleDateFormat("EEE d MMM · HH:mm", Locale.UK).apply { timeZone = london }.format(Date(millis))
    val link = if (element.tagName() == "a") element else element.selectFirst("a[href]")
    val url = link?.absUrl("href")?.takeIf { it.startsWith("https://") } ?: source
    return SunderlandFixture(dateLabel, opponent, competition, if (home) "Home" else "Away", millis, url)
}
