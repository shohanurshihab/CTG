package com.ctgtv.app

import org.json.JSONObject

data class Root(val name: String, val url: String)

data class DirEntry(val name: String, val url: String, val isDir: Boolean, val size: Long) {
    fun toMovie(): Movie {
        val (t, y) = Names.parse(name, isDir)
        return Movie(t, y, url, isDir, name)
    }
}

/** A movie is either a folder (containing the video file) or a video file itself. */
data class Movie(
    val title: String,
    val year: String?,
    val url: String,
    val isFolder: Boolean,
    val rawName: String,
) {
    val label: String get() = if (year != null) "$title ($year)" else title

    fun toJson(): JSONObject = JSONObject()
        .put("t", title)
        .put("y", year ?: JSONObject.NULL)
        .put("u", url)
        .put("f", isFolder)
        .put("r", rawName)

    companion object {
        fun fromJson(o: JSONObject) = Movie(
            o.getString("t"),
            if (o.isNull("y")) null else o.getString("y"),
            o.getString("u"),
            o.getBoolean("f"),
            o.getString("r"),
        )
    }
}

/** Poster / description info from TMDB. [found] = false means "looked up, nothing found". */
data class Meta(
    val found: Boolean,
    val title: String?,
    val overview: String?,
    val rating: Double?,
    val poster: String?,
    val backdrop: String?,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("found", found)
        .put("title", title ?: JSONObject.NULL)
        .put("overview", overview ?: JSONObject.NULL)
        .put("rating", rating ?: JSONObject.NULL)
        .put("poster", poster ?: JSONObject.NULL)
        .put("backdrop", backdrop ?: JSONObject.NULL)

    companion object {
        val NONE = Meta(false, null, null, null, null, null)

        private fun str(o: JSONObject, k: String): String? = if (o.isNull(k)) null else o.getString(k)

        fun fromJson(o: JSONObject) = Meta(
            o.optBoolean("found", false),
            str(o, "title"),
            str(o, "overview"),
            if (o.isNull("rating")) null else o.getDouble("rating"),
            str(o, "poster"),
            str(o, "backdrop"),
        )
    }
}

object Names {
    val VIDEO = setOf("mkv", "mp4", "avi", "m4v", "mov", "webm", "ts", "wmv", "mpg", "mpeg")
    val SUBS = setOf("srt", "vtt", "ass", "ssa")

    fun ext(name: String) = name.substringAfterLast('.', "").lowercase()
    fun isVideo(name: String) = ext(name) in VIDEO
    fun isSub(name: String) = ext(name) in SUBS
    fun baseName(name: String) = name.substringBeforeLast('.', name)

    private val yearRe = Regex("(?<![0-9])((?:19[2-9]|20[0-3])[0-9])(?![0-9])")
    private val qualityRe = Regex(
        "(?i)\\b(2160p|1080p|720p|480p|4k|web-?rip|web-?dl|blu-?ray|brrip|bdrip|hdrip|hdtv|dvdrip|x264|x265|h264|hevc|aac)\\b"
    )

    /** "9.Bullets.2022.1080p.WEBRip.x264 [DDN]" -> ("9 Bullets", "2022") */
    fun parse(raw: String, isDir: Boolean): Pair<String, String?> {
        val s = if (!isDir && isVideo(raw)) baseName(raw) else raw
        val cleaned = s.replace('.', ' ').replace('_', ' ')
        val m = yearRe.findAll(cleaned).firstOrNull { it.range.first > 0 }
        val year = m?.value
        val cut = m?.range?.first ?: qualityRe.find(cleaned)?.range?.first ?: cleaned.length
        var title = cleaned.substring(0, cut)
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("[()\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-')
        if (title.isBlank()) title = raw
        return title to year
    }
}

fun fmtTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun fmtSize(b: Long): String = when {
    b <= 0 -> ""
    b >= (1L shl 30) -> "%.1f GB".format(b / 1073741824.0)
    b >= (1L shl 20) -> "%d MB".format(b shr 20)
    else -> "%d KB".format(b shr 10)
}
