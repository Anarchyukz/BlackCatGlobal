package com.anarchyukz.blackcatglobal

import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class SunderlandNewsArticle(
    val title: String,
    val summary: String,
    val source: String,
    val published: String,
    val url: String,
    val imageUrl: String?
)

internal fun loadSunderlandNews(): List<SunderlandNewsArticle> {
    val query = URLEncoder.encode("Sunderland AFC football", StandardCharsets.UTF_8.toString())
    val feedUrl = "https://news.google.com/rss/search?q=$query&hl=en-GB&gl=GB&ceid=GB:en"
    val document = Jsoup.connect(feedUrl)
        .userAgent("BlackCatGlobal/1.0 (Android news reader)")
        .timeout(12_000)
        .ignoreContentType(true)
        .get()

    return document.select("item").mapNotNull { item ->
        val title = item.selectFirst("title")?.text()?.trim().orEmpty()
        val url = item.selectFirst("link")?.text()?.trim().orEmpty()
        if (title.isBlank() || url.isBlank()) return@mapNotNull null

        val descriptionHtml = item.selectFirst("description")?.text().orEmpty()
        val descriptionDoc = Jsoup.parse(descriptionHtml)
        val summary = descriptionDoc.text().trim().ifBlank { title }
        val image = descriptionDoc.selectFirst("img")?.attr("src")?.takeIf {
            it.startsWith("https://") || it.startsWith("http://")
        }
        SunderlandNewsArticle(
            title = title,
            summary = summary,
            source = item.selectFirst("source")?.text()?.trim().orEmpty().ifBlank { "Google News" },
            published = item.selectFirst("pubDate")?.text()?.trim().orEmpty(),
            url = url,
            imageUrl = image
        )
    }.distinctBy { it.url }.take(30)
}


/**
 * Extracts readable text already served on a publisher's public page.
 * Deliberately does not use hidden APIs, scripts, or other techniques to defeat a paywall.
 * Returns null when the page looks blocked, subscription-gated, or too incomplete to read.
 */
internal fun loadPublicArticleText(url: String): String? {
    if (!url.startsWith("https://") && !url.startsWith("http://")) return null

    val document = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-GB,en;q=0.9")
        .timeout(15_000)
        .followRedirects(true)
        .get()

    val finalUrl = document.location().ifBlank { url }
    val finalHost = finalUrl.substringAfter("://").substringBefore('/').lowercase()
    val title = document.title().lowercase()
    val pageText = document.text().lowercase()

    val consentPage = finalHost.contains("consent.google.") ||
        finalHost.contains("consent.yahoo.") ||
        title.contains("before you continue") ||
        title.contains("privacy notice") ||
        (pageText.contains("we use cookies") && pageText.contains("accept all") &&
            document.select("article, [itemprop=articleBody], .article-body, .story-body, .entry-content").isEmpty())
    if (consentPage) return null

    // Publishers often embed the full, public article body as JSON-LD even when their
    // visible page is assembled by JavaScript. Read only structured article text; do not
    // execute scripts, call private endpoints, or attempt to bypass subscription checks.
    val paywallSignals = listOf(
        "subscribe to continue", "subscribe to read", "to continue reading",
        "unlock this article", "already a subscriber", "sign in to read",
        "register to continue reading", "you have reached your limit",
        "you've reached your limit", "subscriber-only", "subscribers only",
        "this article is for subscribers", "read the full story with a subscription"
    )
    val appearsGated = paywallSignals.any { pageText.contains(it) }

    fun cleanText(raw: String): String {
        return Jsoup.parse(raw).text()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\\n")
            .trim()
    }

    val structuredBodies = document.select("script[type=application/ld+json]").mapNotNull { script ->
        val raw = script.data().ifBlank { script.html() }
        // JSON-LD is parsed as data, never executed. Extract Article/NewsArticle articleBody.
        val articleBodyMatches = Regex(
            "\"articleBody\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).findAll(raw).mapNotNull { match ->
            runCatching {
                val encoded = "\"" + match.groupValues[1] + "\""
                org.json.JSONTokener(encoded).nextValue() as? String
            }.getOrNull()
        }.toList()
        articleBodyMatches.maxByOrNull { it.length }
    }.filter { !it.isNullOrBlank() }.map { cleanText(it!!) }
        .filter { it.length >= 350 }

    if (!appearsGated) {
        structuredBodies.maxByOrNull { it.length }?.let { return it }
    }

    // Prefer genuine article containers. Clone before stripping presentation, adverts,
    // cookie overlays, related stories, newsletter forms and social widgets.
    val candidates = document.select(
        "[itemprop=articleBody], article, main, .article-body, .article__body, " +
            ".story-body, .story__body, .entry-content, .post-content, [class*=article-body], [class*=story-content]"
    ).filter { it.text().length >= 250 }
    val candidate = candidates.maxByOrNull { element ->
        element.select("p").sumOf { it.text().length } +
            if (element.hasAttr("itemprop") && element.attr("itemprop").contains("articleBody")) 500 else 0
    } ?: document.body() ?: return null

    val readable = candidate.clone()
    readable.select(
        "script, style, noscript, nav, header, footer, aside, form, button, iframe, dialog, " +
            "[role=dialog], [aria-modal=true], [aria-label*=cookie i], [aria-label*=consent i], " +
            "[class*=cookie i], [id*=cookie i], [class*=consent i], [id*=consent i], " +
            "[class*=privacy i], [id*=privacy i], [class*=overlay i], [id*=overlay i], " +
            "[class*=modal i], [id*=modal i], [class*=advert i], [id*=advert i], " +
            "[class*=newsletter i], [class*=related i], [class*=recommend i], [class*=social i], " +
            "[class*=paywall i], [id*=paywall i], [class*=subscribe i], [id*=subscribe i]"
    ).remove()

    val cookieSignals = listOf(
        "we use cookies", "this website uses cookies", "we use cookies and similar",
        "cookie preferences", "manage your consent", "accept all cookies",
        "accept cookies", "reject all", "your privacy choices", "privacy preference center",
        "allow all cookies", "consent to the use of cookies", "by continuing to browse"
    )
    val paragraphs = readable.select("h1, h2, h3, p, blockquote")
        .map { it.text().replace(Regex("\\s+"), " ").trim() }
        .filter { paragraph ->
            paragraph.length >= 35 &&
                cookieSignals.none { signal -> paragraph.lowercase().contains(signal) } &&
                !paragraph.lowercase().matches(Regex("^(accept|reject|manage|settings|cookie settings|privacy choices)(\\s+all)?$"))
        }
        .distinct()
    val text = paragraphs.joinToString("\\n\\n").trim()

    val mostlyConsent = cookieSignals.count { pageText.contains(it) } >= 2 && text.length < 900
    if (mostlyConsent || (appearsGated && text.length < 1800)) return null
    if (text.length >= 350 && paragraphs.size >= 3) return text

    // Some publishers only expose a useful public description in Open Graph metadata.
    // Return it only as a clearly short excerpt, not as if it were the full story.
    if (!appearsGated) {
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("meta[name=description]")?.attr("content")?.takeIf { it.isNotBlank() }
        val excerpt = description?.let(::cleanText).orEmpty()
        if (excerpt.length >= 80) return "EXCERPT ONLY\\n\\n$excerpt"
    }
    return null
}
