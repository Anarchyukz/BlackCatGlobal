package com.anarchyukz.blackcatglobal

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
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

private data class NewsRssFeed(val source: String, val url: String)

private val sunderlandRssFeeds = listOf(
    NewsRssFeed("ChronicleLive", "https://www.chroniclelive.co.uk/all-about/sunderland?service=rss"),
    NewsRssFeed("ChronicleLive · Sunderland AFC", "https://www.chroniclelive.co.uk/sport/football/sunderland-afc?service=rss"),
    NewsRssFeed("Roker Report", "https://rokerreport.sbnation.com/rss/current.xml"),
    NewsRssFeed("The Northern Echo", "https://www.thenorthernecho.co.uk/news/rss/"),
    NewsRssFeed(
        "Sunderland Wire",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("site:sunderland.theclubwire.com Sunderland", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    )
)

/**
 * Strict RSS/Atom reader. It deliberately rejects HTML responses and never scrapes
 * publisher webpages as a fallback. A broken/non-feed URL contributes zero articles.
 */
internal fun loadSunderlandNews(): List<SunderlandNewsArticle> =
    sunderlandRssFeeds.flatMap { feed ->
        runCatching { readRssFeed(feed) }.getOrDefault(emptyList())
    }.distinctBy { it.url }.sortedByDescending { it.published }.take(60)

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
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        connection.inputStream.use { parser.setInput(it, null) }

        var event = parser.eventType
        var rootName = ""
        while (event != XmlPullParser.END_DOCUMENT && event != XmlPullParser.START_TAG) {
            event = parser.next()
        }
        require(event == XmlPullParser.START_TAG) { "Empty RSS response" }
        rootName = parser.name.substringAfter(':').lowercase()
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
            "enclosure", "thumbnail" -> {
                val value = parser.getAttributeValue(null, "url")
                    ?: parser.getAttributeValue("http://search.yahoo.com/mrss/", "url")
                if (!value.isNullOrBlank()) image = value
                skipXmlElement(parser)
            }
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
