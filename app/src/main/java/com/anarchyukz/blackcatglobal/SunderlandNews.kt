package com.anarchyukz.blackcatglobal

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class SunderlandNewsArticle(
    val title: String,
    val summary: String,
    val source: String,
    val published: String,
    val url: String,
    val imageUrl: String?
) {
    val publishedTimestamp: Long get() = parsePublishedTimestamp(published)
}

private data class NewsRssFeed(val source: String, val url: String)

// Keep Roker Report first so its Sunderland-specific coverage wins when duplicate
// headlines are syndicated by more than one feed. Articles are still ordered by date.
private val sunderlandRssFeeds = listOf(
    NewsRssFeed("Roker Report", "https://rokerreport.sbnation.com/rss/current.xml"),
    NewsRssFeed("ChronicleLive · Sunderland AFC", "https://www.chroniclelive.co.uk/sport/football/sunderland-afc?service=rss"),
    NewsRssFeed("Sunderland Echo Sport", "https://www.sunderlandecho.com/sport/football/sunderland-afc/rss"),
    NewsRssFeed(
        "Sunderland AFC News",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("Sunderland AFC football news", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Sunderland AFC · latest",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("(Sunderland AFC OR SAFC OR \"Black Cats\") football", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Sunderland AFC · transfers",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("Sunderland AFC transfer news", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Roker Report · search",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("site:rokerreport.sbnation.com Sunderland", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "ChronicleLive · Sunderland AFC",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("site:chroniclelive.co.uk/sport/football/sunderland-afc", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Sunderland Echo · football",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("site:sunderlandecho.com/sport/football/sunderland-afc", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed("ChronicleLive · Sunderland", "https://www.chroniclelive.co.uk/all-about/sunderland?service=rss"),
    NewsRssFeed("Sunderland Echo", "https://www.sunderlandecho.com/rss"),
    NewsRssFeed("The Northern Echo", "https://www.thenorthernecho.co.uk/news/rss/"),
    // Google News RSS searches fill the local-news section when publisher RSS endpoints
    // are blocked, moved, or return HTML instead of XML. These remain RSS-only sources.
    NewsRssFeed(
        "Local News · Sunderland",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("Sunderland local news -football -Sunderland AFC", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Local News · Tyne and Wear",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("Tyne and Wear local news Newcastle South Shields Gateshead", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    ),
    NewsRssFeed(
        "Local News · County Durham",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("County Durham local news", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    )
)

/**
 * RSS-only news build: publisher HTML is intentionally never scraped.
 * Strict RSS/Atom reader. It deliberately rejects HTML responses and never scrapes
 * publisher webpages as a fallback. A broken/non-feed URL contributes zero articles.
 */
internal fun loadSunderlandNews(): List<SunderlandNewsArticle> {
    val executor = java.util.concurrent.Executors.newFixedThreadPool(8)
    return try {
        val tasks = sunderlandRssFeeds.map { feed ->
            executor.submit(java.util.concurrent.Callable {
                runCatching { readRssFeed(feed) }.getOrDefault(emptyList())
            })
        }
        tasks.flatMap { task ->
            runCatching { task.get(14, java.util.concurrent.TimeUnit.SECONDS) }
                .getOrDefault(emptyList())
        }.distinctBy { it.url }.sortedByDescending { it.publishedTimestamp }.take(80)
    } finally {
        executor.shutdownNow()
    }
}

private fun readRssFeed(feed: NewsRssFeed): List<SunderlandNewsArticle> {
    val connection = (URL(feed.url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "BlackCatGlobal/1.0 (RSS reader; Android)")
        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
    }

    return try {
        val status = connection.responseCode
        require(status in 200..299) { "RSS request failed with HTTP $status" }
        connection.inputStream.use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(stream, null)

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && event != XmlPullParser.START_TAG) {
                event = parser.next()
            }
            require(event == XmlPullParser.START_TAG) { "Empty RSS response" }
            val rootName = parser.name.substringAfter(':').lowercase()
            require(rootName == "rss" || rootName == "feed" || rootName == "rdf") {
                "Response is not RSS/Atom XML (root=$rootName)"
            }

            val articles = mutableListOf<SunderlandNewsArticle>()
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                val name = parser.name.substringAfter(':').lowercase()
                if (name == "item" || name == "entry") {
                    parseFeedItem(parser, name, feed.source)?.let(articles::add)
                }
            }
            articles
        }
    } finally {
        connection.disconnect()
    }
}

private fun parseFeedItem(
    parser: XmlPullParser,
    itemTag: String,
    fallbackSource: String
): SunderlandNewsArticle? {
    var title = ""
    var summary = ""
    var source = fallbackSource
    var published = ""
    var link = ""
    var image: String? = null

    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.END_TAG &&
            parser.name.substringAfter(':').equals(itemTag, ignoreCase = true)
        ) break
        if (parser.eventType != XmlPullParser.START_TAG) continue

        val tag = parser.name.substringAfter(':').lowercase()
        // Media RSS often stores thumbnails in namespaced media:content or
        // media:thumbnail elements. XmlPullParser exposes the local tag name,
        // so inspect the URL attribute before treating "content" as article text.
        val mediaUrl = parser.getAttributeValue(null, "url")
            ?: parser.getAttributeValue("http://search.yahoo.com/mrss/", "url")
        if (tag in setOf("enclosure", "thumbnail", "content") &&
            !mediaUrl.isNullOrBlank() &&
            (mediaUrl.startsWith("https://", true) || mediaUrl.startsWith("http://", true))
        ) {
            if (image == null) image = mediaUrl
            skipXmlElement(parser)
            continue
        }

        when (tag) {
            "link" -> {
                val href = parser.getAttributeValue(null, "href").orEmpty().trim()
                val rel = parser.getAttributeValue(null, "rel").orEmpty()
                val text = readXmlText(parser).trim()
                if (href.isNotBlank() && (rel.isBlank() || rel == "alternate")) link = href
                else if (link.isBlank() && text.isNotBlank()) link = text
            }
            "title" -> title = readXmlText(parser).trim()
            "description", "summary", "content", "encoded" -> {
                val value = readXmlText(parser).trim()
                // Some publisher RSS feeds put the article thumbnail only inside the
                // description/content HTML rather than a Media RSS tag. Extract that
                // URL from the feed payload itself; do not fetch or scrape article pages.
                if (image == null) image = extractRssHtmlImage(value)
                if (summary.isBlank() && value.isNotBlank()) summary = value
            }
            "pubdate", "published", "updated", "date" -> {
                val value = readXmlText(parser).trim()
                if (published.isBlank()) published = value
            }
            "source" -> {
                val value = readXmlText(parser).trim()
                if (value.isNotBlank()) source = value
            }
            "enclosure", "thumbnail" -> skipXmlElement(parser)
            else -> skipXmlElement(parser)
        }
    }

    val cleanTitle = title.trim()
    val cleanUrl = link.trim()
    if (cleanTitle.isBlank() ||
        !(cleanUrl.startsWith("https://", true) || cleanUrl.startsWith("http://", true))
    ) return null

    return SunderlandNewsArticle(
        title = cleanTitle,
        summary = summary.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim().ifBlank { cleanTitle },
        source = source.ifBlank { fallbackSource },
        published = published,
        url = cleanUrl,
        imageUrl = image?.takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }
    )
}

/**
 * Extract the first plausible article image from HTML embedded inside an RSS item.
 * Handles regular and lazy-loaded image attributes plus Open Graph-style image tags.
 * This reads only the RSS item content and never requests the publisher's webpage.
 */
private fun extractRssHtmlImage(html: String): String? {
    if (html.isBlank()) return null
    val patterns = listOf(
        Regex("""(?is)<img\\b[^>]*?\\b(?:src|data-src|data-original|data-lazy-src)\\s*=\\s*["']([^"']+)["']"""),
        Regex("""(?is)<source\\b[^>]*?\\bsrcset\\s*=\\s*["']([^"']+)["']"""),
        Regex("""(?is)<meta\\b[^>]*?\\b(?:property|name)\\s*=\\s*["'](?:og:image|twitter:image)["'][^>]*?\\bcontent\\s*=\\s*["']([^"']+)["']""")
    )
    for (pattern in patterns) {
        val match = pattern.find(html) ?: continue
        val candidate = match.groupValues[1].substringBefore(',').trim().substringBefore(' ')
        if (candidate.startsWith("https://", true) || candidate.startsWith("http://", true)) {
            val lower = candidate.lowercase()
            if (!lower.contains("pixel") && !lower.contains("spacer") && !lower.contains("logo")) {
                return candidate.replace("&amp;", "&")
            }
        }
    }
    return null
}

/** Parse RSS dates as real timestamps; sorting the raw date strings is not chronological. */
private fun parsePublishedTimestamp(value: String): Long {
    val date = value.trim()
    if (date.isBlank()) return 0L
    val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm zzz",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd"
    )
    for (pattern in patterns) {
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(date, position)
        if (parsed != null && position.index == date.length) return parsed.time
    }
    return 0L
}

/** Reads text/CDATA inside an XML element without parsing it as HTML. */
private fun readXmlText(parser: XmlPullParser): String {
    val targetDepth = parser.depth
    val result = StringBuilder()
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF ->
                result.append(parser.text.orEmpty())
            XmlPullParser.END_TAG -> if (parser.depth == targetDepth) return result.toString()
        }
    }
    return result.toString()
}

private fun skipXmlElement(parser: XmlPullParser) {
    val targetDepth = parser.depth
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.END_TAG && parser.depth == targetDepth) return
    }
}
