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
