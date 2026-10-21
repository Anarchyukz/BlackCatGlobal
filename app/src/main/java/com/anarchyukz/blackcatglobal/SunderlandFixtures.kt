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

/**
 * Merge multiple public fixture sources. The official club homepage can omit European
 * fixtures, so never stop after the first source returns a partial list.
 */
internal fun loadSunderlandFixtures(): List<SunderlandFixture> {
    val sources = listOf(
        "https://www.skysports.com/sunderland-scores-fixtures",
        "https://www.safc.com/"
    )
    val now = System.currentTimeMillis()
    val all = sources.flatMap { source ->
        runCatching {
            val doc = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(15000)
                .get()
            doc.select("a, li, article, [class*=fixture], [class*=match], [class*=event]").mapNotNull {
                parseSunderlandFixture(it, source, now)
            }
        }.getOrDefault(emptyList())
    }
    return all
        .distinctBy { "${it.opponent.lowercase(Locale.UK)}|${it.sortTime / 60_000L}|${it.competition}" }
        .sortedBy { it.sortTime }
        .take(20)
}

private fun parseSunderlandFixture(element: Element, source: String, now: Long): SunderlandFixture? {
    val text = element.text().replace(Regex("\\s+"), " ").trim()
    if (text.length !in 15..900 || !text.contains("Sunderland", true)) return null

    val dateMatch = Regex(
        "(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+" +
            "(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)" +
            "(?:\\s+(20\\d{2}))?\\b"
    ).find(text) ?: return null
    val day = dateMatch.groupValues[1].toIntOrNull() ?: return null
    val month = dateMatch.groupValues[2].take(3).replaceFirstChar { it.uppercase() }
    val london = TimeZone.getTimeZone("Europe/London")
    val currentCalendar = java.util.Calendar.getInstance(london)
    var year = dateMatch.groupValues[3].toIntOrNull() ?: currentCalendar.get(java.util.Calendar.YEAR)

    val time24 = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(text)
    val time12 = Regex("(?i)\\b(\\d{1,2})[.:]([0-5]\\d)\\s*(am|pm)\\b").find(text)
        ?: Regex("(?i)\\b(\\d{1,2})\\s*(am|pm)\\b").find(text)
    val hour: Int
    val minute: Int
    if (time24 != null) {
        hour = time24.groupValues[1].toInt()
        minute = time24.groupValues[2].toInt()
    } else if (time12 != null) {
        var h = time12.groupValues[1].toInt()
        minute = time12.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val meridiem = time12.groupValues.last().lowercase(Locale.UK)
        if (meridiem == "pm" && h != 12) h += 12
        if (meridiem == "am" && h == 12) h = 0
        hour = h
    } else {
        hour = 23
        minute = 59
    }

    var millis = runCatching {
        SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK).apply {
            timeZone = london
            isLenient = false
        }.parse("$day $month $year $hour:$minute")?.time
    }.getOrNull() ?: return null
    // Fixture pages often omit the year. Treat a clearly past date as next season/year.
    if (dateMatch.groupValues[3].isBlank() && millis < now - 2L * 24L * 60L * 60L * 1000L) {
        year++
        millis = runCatching {
            SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK).apply {
                timeZone = london
                isLenient = false
            }.parse("$day $month $year $hour:$minute")?.time
        }.getOrNull() ?: return null
    }
    if (millis < now - 60_000L) return null

    val matchup = Regex(
        "(?i)(Sunderland(?: AFC)?\\s+(?:vs?\\.?|v)\\s+([^|,–—-]{2,55})|([^|,–—-]{2,55}?)\\s+(?:vs?\\.?|v)\\s+Sunderland(?: AFC)?)"
    ).find(text) ?: return null
    val home = matchup.groupValues[1].isNotBlank()
    val opponent = (if (home) matchup.groupValues[2] else matchup.groupValues[3])
        .replace(Regex("(?i)\\s+(kick[- ]?off|starts? at|\\d{1,2}[.:]\\d{2}.*)$"), "")
        .trim().trimEnd('.', ':')
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
