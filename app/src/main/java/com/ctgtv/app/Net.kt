package com.ctgtv.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object Net {
    const val UA =
        "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", UA).build())
        }
        .build()

    private val cache = ConcurrentHashMap<String, List<DirEntry>>()

    /** Lists one nginx "Index of /..." page. Results are cached in memory. */
    suspend fun listDir(url: String, force: Boolean = false): List<DirEntry> {
        val u = if (url.endsWith("/")) url else "$url/"
        if (!force) cache[u]?.let { return it }
        val result = withContext(Dispatchers.IO) {
            client.newCall(Request.Builder().url(u).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                parse(resp.body?.string() ?: "", u)
            }
        }
        cache[u] = result
        return result
    }

    private fun parse(html: String, base: String): List<DirEntry> {
        val doc = Jsoup.parse(html, base)
        val out = ArrayList<DirEntry>()
        for (a in doc.select("a[href]")) {
            val href = a.attr("href")
            if (href.isEmpty() || href.startsWith("?") || href.startsWith("#")) continue
            if (href.startsWith("../") || href == "./" || href.startsWith("/")) continue
            val abs = a.absUrl("href")
            if (abs.isEmpty() || abs == base) continue
            // an absolute link must stay inside the current folder
            if (href.contains("://") && !abs.startsWith(base)) continue

            val path = href.substringBefore('?')
            val isDir = path.endsWith("/")
            val name = Uri.decode(path.trimEnd('/')).substringAfterLast('/')
            if (name.isEmpty()) continue

            // nginx prints "date time size" right after the link
            val tail = (a.nextSibling() as? TextNode)?.text()?.trim().orEmpty()
            val size = if (isDir) 0L else parseSize(tail.split(Regex("\\s+")).lastOrNull().orEmpty())
            out.add(DirEntry(name, abs, isDir, size))
        }
        return out
    }

    private fun parseSize(s: String): Long {
        if (s.isEmpty() || s == "-") return 0L
        s.toLongOrNull()?.let { return it }
        val n = s.dropLast(1).toDoubleOrNull() ?: return 0L
        val mult = when (s.last().uppercaseChar()) {
            'K' -> 1024.0
            'M' -> 1024.0 * 1024
            'G' -> 1024.0 * 1024 * 1024
            else -> 0.0
        }
        return (n * mult).toLong()
    }
}
