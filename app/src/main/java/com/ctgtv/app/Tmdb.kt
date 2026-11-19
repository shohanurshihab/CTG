package com.ctgtv.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class TmdbAuthException : IOException("Invalid TMDB API key")

object Tmdb {
    /** Returns null when TMDB has no match. Throws on network / key errors. */
    suspend fun lookup(key: String, title: String, year: String?): Meta? = withContext(Dispatchers.IO) {
        search(key, title, year) ?: if (year != null) search(key, title, null) else null
    }

    private fun search(key: String, title: String, year: String?): Meta? {
        val builder = HttpUrl.Builder()
            .scheme("https")
            .host("api.themoviedb.org")
            .addPathSegments("3/search/movie")
            .addQueryParameter("api_key", key)
            .addQueryParameter("query", title)
            .addQueryParameter("include_adult", "false")
        if (year != null) builder.addQueryParameter("year", year)

        Net.client.newCall(Request.Builder().url(builder.build()).build()).execute().use { resp ->
            if (resp.code == 401) throw TmdbAuthException()
            if (!resp.isSuccessful) throw IOException("TMDB HTTP ${resp.code}")
            val results = JSONObject(resp.body?.string() ?: "{}").optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val o = results.getJSONObject(0)

            fun s(k: String): String? = if (o.isNull(k)) null else o.getString(k).takeIf { it.isNotBlank() }
            return Meta(
                found = true,
                title = s("title"),
                overview = s("overview"),
                rating = if (o.isNull("vote_average")) null else o.getDouble("vote_average"),
                poster = s("poster_path")?.let { "https://image.tmdb.org/t/p/w342$it" },
                backdrop = s("backdrop_path")?.let { "https://image.tmdb.org/t/p/w780$it" },
            )
        }
    }
}

/** Memory + disk cache in front of TMDB, with a small concurrency limit. */
object MetaRepo {
    private val mem = ConcurrentHashMap<String, Meta>()
    private val gate = Semaphore(4)

    @Volatile
    var authFailed = false

    fun clearMem() = mem.clear()

    suspend fun get(m: Movie): Meta? {
        mem[m.url]?.let { return if (it.found) it else null }
        Store.getMeta(m.url)?.let {
            mem[m.url] = it
            return if (it.found) it else null
        }
        val key = Store.tmdbKey
        if (key.isBlank() || authFailed) return null

        return gate.withPermit {
            mem[m.url]?.let { return@withPermit if (it.found) it else null }
            try {
                val found = Tmdb.lookup(key, m.title, m.year)
                val meta = found ?: Meta.NONE
                mem[m.url] = meta
                Store.putMeta(m.url, meta)
                found
            } catch (e: CancellationException) {
                throw e
            } catch (e: TmdbAuthException) {
                authFailed = true
                null
            } catch (e: Exception) {
                null // network hiccup: don't cache, try again next time
            }
        }
    }
}
