package com.anarchyukz.blackcatglobal

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
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
    NewsRssFeed(
        "ChronicleLive",
        "https://www.chroniclelive.co.uk/all-about/sunderland?service=rss"
    ),
    NewsRssFeed(
        "ChronicleLive · Sunderland AFC",
        "https://www.chroniclelive.co.uk/sport/football/sunderland-afc?service=rss"
    ),
    NewsRssFeed(
        "Roker Report",
        "https://rokerreport.sbnation.com/rss/current.xml"
    ),
    NewsRssFeed(
        "The Northern Echo",
        "https://www.thenorthernecho.co.uk/news/rss/"
    ),
    NewsRssFeed(
        "Sunderland Wire",
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode("site:sunderland.theclubwire.com Sunderland", StandardCharsets.UTF_8.name()) +
            "&hl=en-GB&gl=GB&ceid=GB:en"
    )
)

/** Loads headlines exclusively from RSS/Atom feeds. Failed feeds are skipped independently. */
internal fun loadSunderlandNews(): List<SunderlandNewsArticle> {
    return sunderlandRssFeeds.flatMap { feed ->
        runCatching {
            val document = Jsoup.connect(feed.url)
                .userAgent("BlackCatGlobal/1.0 (Android RSS reader)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .timeout(10_000)
                .ignoreContentType(true)
                .get()

            val items = document.select("item, entry")
            items.mapNotNull { item ->
                val title = item.selectFirst("title")?.text()?.trim().orEmpty()
                val linkElement = item.selectFirst("link[rel=alternate]") ?: item.selectFirst("link")
                val url = linkElement?.attr("href")?.takeIf { it.isNotBlank() }
                    ?: linkElement?.text()?.trim().orEmpty()
                if (title.isBlank() || url.isBlank() ||
                    (!url.startsWith("https://") && !url.startsWith("http://"))
                ) return@mapNotNull null

                val rawSummary = item.selectFirst("description, summary, content, encoded")?.let {
                    it.text().ifBlank { it.html() }
                }.orEmpty()
                val summaryDoc = Jsoup.parse(rawSummary, Parser.htmlParser())
                val summary = summaryDoc.text().trim().ifBlank { title }
                val feedImage = item.selectFirst("enclosure[url]")?.attr("url")
                    ?: item.children().firstOrNull {
                        it.tagName().substringAfter(':').lowercase() in setOf("content", "thumbnail") &&
                            it.hasAttr("url")
                    }?.attr("url")
                val image = feedImage?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                    ?: summaryDoc.selectFirst("img")?.let { it.attr("abs:src").ifBlank { it.attr("src") } }
                        ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

                SunderlandNewsArticle(
                    title = title,
                    summary = summary,
                    source = item.selectFirst("source")?.text()?.trim().orEmpty().ifBlank { feed.source },
                    published = item.selectFirst("pubDate, published, updated, date")?.text()?.trim().orEmpty(),
                    url = url,
                    imageUrl = image
                )
            }
        }.getOrDefault(emptyList())
    }
        .distinctBy { it.url }
        .sortedByDescending { it.published }
        .take(60)
}
