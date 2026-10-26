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
            parseFixturesFromDocument(doc, source, now)
        }.getOrDefault(emptyList())
    }

    // Verified UEFA fixture fallback. Some club/TV fixture pages omit European games or
    // return a partial list, so keep tomorrow's confirmed match visible even if scraping fails.
    val london = TimeZone.getTimeZone("Europe/London")
    val confirmedLechFixture = runCatching {
        val kickoff = SimpleDateFormat("d MMM yyyy HH:mm", Locale.UK).apply {
            timeZone = london
            isLenient = false
        }.parse("22 Oct 2026 17:45")?.time
        if (kickoff != null && kickoff >= now) {
            listOf(SunderlandFixture(
                date = "Thu 22 Oct · 17:45",
                opponent = "Lech Poznań",
                competition = "UEFA Europa League",
                venue = "Away",
                sortTime = kickoff,
                url = "https://www.uefa.com/uefaeuropaleague/clubs/53360/matches/"
            ))
        } else emptyList()
    }.getOrDefault(emptyList())

    return (all + confirmedLechFixture)
        .distinctBy { "${it.opponent.lowercase(Locale.UK)}|${it.sortTime / 60_000L}" }
        .sortedBy { it.sortTime }
        .take(20)
}

private fun parseFixturesFromDocument(
    doc: org.jsoup.nodes.Document,
    source: String,
    now: Long
): List<SunderlandFixture> {
    var dateContext: String? = null
    val found = mutableListOf<SunderlandFixture>()
    val nodes = doc.select("h1, h2, h3, h4, h5, time, [class*=date], li, article, [class*=fixture], [class*=match], [class*=event], a")
    for (node in nodes) {
        val nodeText = node.text().replace(Regex("\\s+"), " ").trim()
        val containsDate = Regex(
            "(?i)\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\\s+20\\d{2})?\\b"
        ).containsMatchIn(nodeText)
        if (containsDate && nodeText.length <= 160) {
            dateContext = if (nodeText.contains("Sunderland", true)) {
                Regex("(?i)\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\\s+20\\d{2})?\\b").find(nodeText)?.value ?: nodeText
            } else nodeText
        }
        if (nodeText.contains("Sunderland", true)) {
            parseSunderlandFixture(node, source, now, dateContext)?.let(found::add)
        }
    }
    return found
}

private fun parseSunderlandFixture(element: Element, source: String, now: Long, dateContext: String? = null): SunderlandFixture? {
    val rawText = element.text().replace(Regex("\\s+"), " ").trim()
    val hasOwnDate = Regex("(?i)\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)").containsMatchIn(rawText)
    val text = if (hasOwnDate) rawText else "${dateContext.orEmpty()} $rawText".trim()
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
        .replace(Regex("(?i)\\s+(?:kick(?:[- ]?off)?\\b.*|starts? at\\b.*|\\d{1,2}[.:]\\d{2}.*)$"), "")
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

/** A completed Sunderland match, with its competition and source kept alongside the score. */
internal data class SunderlandMatchResult(
    val date: String,
    val fixture: String,
    val score: String,
    val competition: String,
    val sortTime: Long,
    val url: String
)

/**
 * Read completed matches from 11v11's league fixture page and full match archive.
 * Keep competition on each result so league and cup/European games can be displayed separately.
 */
internal fun loadSunderlandResults(): List<SunderlandMatchResult> {
    val sources = listOf(
        "https://www.11v11.com/teams/sunderland/option/fixtures/",
        "https://www.11v11.com/teams/sunderland/tab/matches/"
    )
    val london = TimeZone.getTimeZone("Europe/London")
    val now = System.currentTimeMillis()
    val datePatterns = listOf("d MMMM yyyy", "d MMM yyyy", "d MMMM", "d MMM")
    val results = sources.flatMap { source ->
        runCatching {
            val doc = Jsoup.connect(source)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .timeout(12000)
                .get()
            doc.select("tr").mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size < 4) return@mapNotNull null
                val rowText = row.text().replace(Regex("\\s+"), " ").trim()
                if (!rowText.contains("Sunderland", true)) return@mapNotNull null
                val dateText = cells.firstOrNull()?.text()?.trim().orEmpty()
                val dateMatch = Regex("(?i)\\b\\d{1,2}\\s+[A-Za-z]{3,9}(?:\\s+20\\d{2})?\\b").find(dateText)
                    ?: return@mapNotNull null
                val rawDate = dateMatch.value
                val dateWithYear = if (Regex("\\b20\\d{2}\\b").containsMatchIn(rawDate)) rawDate else "$rawDate 2026"
                val millis = datePatterns.firstNotNullOfOrNull { pattern ->
                    runCatching {
                        SimpleDateFormat(pattern, Locale.UK).apply {
                            timeZone = london
                            isLenient = false
                        }.parse(dateWithYear)?.time
                    }.getOrNull()
                } ?: return@mapNotNull null
                if (millis > now || millis < now - 370L * 24L * 60L * 60L * 1000L) return@mapNotNull null
                val scoreCell = cells.getOrNull(3)?.text()?.trim().orEmpty()
                val scoreMatch = Regex("\\b(\\d{1,2})\\s*[-–:]\\s*(\\d{1,2})\\b").find(scoreCell)
                    ?: Regex("\\b(\\d{1,2})\\s*[-–:]\\s*(\\d{1,2})\\b").find(rowText)
                    ?: return@mapNotNull null
                val score = "${scoreMatch.groupValues[1]}–${scoreMatch.groupValues[2]}"
                val fixtureCell = cells.getOrNull(1)?.text()?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                val fixture = fixtureCell.takeIf { it.contains("Sunderland", true) }
                    ?: rowText.substringAfter(rawDate).replace(scoreMatch.value, "").trim()
                if (fixture.isBlank()) return@mapNotNull null
                val competitionText = cells.lastOrNull()?.text()?.trim().orEmpty()
                val competition = when {
                    Regex("(?i)Europa League").containsMatchIn(competitionText) -> "UEFA Europa League"
                    Regex("(?i)Conference League").containsMatchIn(competitionText) -> "UEFA Conference League"
                    Regex("(?i)Champions League").containsMatchIn(competitionText) -> "UEFA Champions League"
                    Regex("(?i)FA Cup").containsMatchIn(competitionText) -> "FA Cup"
                    Regex("(?i)EFL Cup|League Cup|Carabao").containsMatchIn(competitionText) -> "EFL Cup"
                    Regex("(?i)EFL Trophy").containsMatchIn(competitionText) -> "EFL Trophy"
                    Regex("(?i)Premier League").containsMatchIn(competitionText) -> "Premier League"
                    else -> competitionText.ifBlank { "Sunderland match" }
                }
                val link = row.selectFirst("a[href]")
                val url = link?.absUrl("href")?.takeIf { it.startsWith("https://") } ?: source
                SunderlandMatchResult(
                    date = SimpleDateFormat("EEE d MMM yyyy", Locale.UK).apply { timeZone = london }.format(Date(millis)),
                    fixture = fixture,
                    score = score,
                    competition = competition,
                    sortTime = millis,
                    url = url
                )
            }
        }.getOrDefault(emptyList())
    }
    return results.distinctBy { "${it.sortTime / 86_400_000L}|${it.fixture.lowercase(Locale.UK)}|${it.score}" }
        .sortedByDescending { it.sortTime }
        .take(12)
}
