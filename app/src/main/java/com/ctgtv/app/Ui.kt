package com.ctgtv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val Red = Color(0xFFE50914)
private val Dim = Color(0xFFBBBBBB)

sealed interface Screen {
    object Home : Screen
    data class Browse(val url: String) : Screen
    data class Detail(val movie: Movie) : Screen
    object Settings : Screen
}

@Composable
fun App(onExit: () -> Unit) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val homeState = rememberLazyListState()

    BackHandler {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else onExit()
    }

    when (val s = stack.last()) {
        is Screen.Home -> HomeScreen(
            listState = homeState,
            onOpen = { stack.add(Screen.Detail(it)) },
            onBrowse = { stack.add(Screen.Browse(Store.BASE_URL)) },
            onSettings = { stack.add(Screen.Settings) },
        )
        is Screen.Browse -> BrowseScreen(
            url = s.url,
            onEnter = { stack.add(Screen.Browse(it)) },
            onOpenFile = { stack.add(Screen.Detail(it)) },
        )
        is Screen.Detail -> DetailScreen(
            movie = s.movie,
            onOpenFolder = { stack.add(Screen.Browse(it)) },
        )
        is Screen.Settings -> SettingsScreen()
    }
}

// ---------------------------------------------------------------- shared bits

@Composable
fun FocusButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color.White else Color(0xFF2A2A33))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 18.sp)
    }
}

@Composable
fun FocusRow(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0xFF3A3A48) else Color(0xFF1B1B23))
            .border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 18.sp)
        if (trailing != null) {
            Text(trailing, color = Dim, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun PosterCard(movie: Movie, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val meta by produceState<Meta?>(null, movie.url) { value = MetaRepo.get(movie) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")

    Column(
        Modifier
            .width(150.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .border(3.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(Color(0xFF22222B)),
            contentAlignment = Alignment.Center,
        ) {
            val poster = meta?.poster
            if (poster != null) {
                AsyncImage(
                    model = poster,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    movie.title,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            movie.label,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (focused) Color.White else Dim,
        )
    }
}

@Composable
fun MovieRow(title: String, movies: List<Movie>, onOpen: (Movie) -> Unit) {
    Column {
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(movies, key = { it.url }) { m -> PosterCard(m) { onOpen(m) } }
        }
    }
}

// ---------------------------------------------------------------- home

@Composable
fun HomeScreen(
    listState: LazyListState,
    onOpen: (Movie) -> Unit,
    onBrowse: () -> Unit,
    onSettings: () -> Unit,
) {
    val roots = remember { Store.roots() }
    val recents = remember { Store.recents() }
    val needKey = Store.tmdbKey.isBlank()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("CTG TV", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Red)
                Spacer(Modifier.weight(1f))
                FocusButton("Browse all", onClick = onBrowse)
                FocusButton("Settings", onClick = onSettings)
            }
        }
        if (needKey) {
            item {
                Text(
                    "Tip: add a free TMDB API key in Settings to get posters and descriptions.",
                    color = Dim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
        }
        if (recents.isNotEmpty()) {
            item { MovieRow("Recently played", recents, onOpen) }
        }
        items(roots, key = { it.url }) { r -> LibraryRow(r, onOpen) }
        if (roots.isEmpty()) {
            item {
                Text(
                    "No rows yet. Open Browse all, go into a folder full of movies and choose \"Add this folder to Home\".",
                    color = Dim,
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
        }
    }
}

@Composable
fun LibraryRow(root: Root, onOpen: (Movie) -> Unit) {
    var movies by remember(root.url) { mutableStateOf<List<Movie>?>(null) }
    var error by remember(root.url) { mutableStateOf<String?>(null) }
    var attempt by remember(root.url) { mutableStateOf(0) }

    LaunchedEffect(root.url, attempt) {
        error = null
        try {
            movies = Net.listDir(root.url, force = attempt > 0)
                .filter { it.isDir || Names.isVideo(it.name) }
                .map { it.toMovie() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load"
        }
    }

    val list = movies
    val err = error
    when {
        list != null -> MovieRow(root.name, list, onOpen)
        err != null -> Column(Modifier.padding(horizontal = 48.dp)) {
            Text(root.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Could not load this folder: $err", color = Color(0xFFFF8A80), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            FocusButton("Retry") { attempt++ }
        }
        else -> Column(Modifier.padding(horizontal = 48.dp)) {
            Text(root.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Loading...", color = Dim, fontSize = 14.sp)
        }
    }
}

// ---------------------------------------------------------------- browse

@Composable
fun BrowseScreen(url: String, onEnter: (String) -> Unit, onOpenFile: (Movie) -> Unit) {
    var entries by remember(url) { mutableStateOf<List<DirEntry>?>(null) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    var attempt by remember(url) { mutableStateOf(0) }
    var added by remember(url) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(url, attempt) {
        error = null
        try {
            entries = Net.listDir(url, force = attempt > 0).filter { it.isDir || Names.isVideo(it.name) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load"
        }
    }
    LaunchedEffect(entries) {
        if (!entries.isNullOrEmpty()) {
            delay(200)
            runCatching { focus.requestFocus() }
        }
    }

    val path = remember(url) { Uri.decode(url.removePrefix(Store.BASE_URL)).trim('/').ifEmpty { "/" } }
    val rowName = remember(url) {
        url.trimEnd('/').split('/').takeLast(2).joinToString(" / ") { Uri.decode(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(path, fontSize = 20.sp, color = Dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (url != Store.BASE_URL) {
                FocusButton(if (added) "Added to Home" else "Add this folder to Home") {
                    Store.addRoot(Root(rowName, url))
                    added = true
                }
            }
            if (error != null) FocusButton("Retry") { attempt++ }
        }
        val list = entries
        when {
            error != null -> Text("Could not load: $error", color = Color(0xFFFF8A80))
            list == null -> Text("Loading...", color = Dim)
            list.isEmpty() -> Text("Nothing playable here.", color = Dim)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list, key = { _, e -> e.url }) { i, e ->
                    val mod = if (i == 0) Modifier.focusRequester(focus) else Modifier
                    if (e.isDir) {
                        FocusRow(e.name + "/", modifier = mod) { onEnter(e.url) }
                    } else {
                        FocusRow(e.name, modifier = mod, trailing = fmtSize(e.size).ifEmpty { null }) {
                            onOpenFile(e.toMovie())
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- details

private class Listing(
    val videos: List<DirEntry>,
    val subs: List<DirEntry>,
    val folders: List<DirEntry>,
)

private fun subsFor(movie: Movie, v: DirEntry, l: Listing): List<DirEntry> {
    if (movie.isFolder && l.videos.size == 1) return l.subs
    val base = Names.baseName(v.name)
    return l.subs.filter { it.name.startsWith(base) }
}

private fun play(ctx: Context, movie: Movie, video: DirEntry, subs: List<DirEntry>) {
    Store.pushRecent(movie)
    ctx.startActivity(
        Intent(ctx, PlayerActivity::class.java)
            .putExtra("url", video.url)
            .putExtra("title", movie.label)
            .putStringArrayListExtra("subs", ArrayList(subs.map { it.url }))
    )
}

@Composable
fun DetailScreen(movie: Movie, onOpenFolder: (String) -> Unit) {
    val ctx = LocalContext.current
    val meta by produceState<Meta?>(null, movie.url) { value = MetaRepo.get(movie) }
    var listing by remember(movie.url) { mutableStateOf<Listing?>(null) }
    var error by remember(movie.url) { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(movie.url) {
        val folderUrl = if (movie.isFolder) movie.url else movie.url.substringBeforeLast('/') + "/"
        try {
            val all = Net.listDir(folderUrl)
            val vids = all.filter { !it.isDir && Names.isVideo(it.name) }
            listing = Listing(
                videos = if (movie.isFolder) vids
                else listOf(vids.firstOrNull { it.url == movie.url } ?: DirEntry(movie.rawName, movie.url, false, 0)),
                subs = all.filter { !it.isDir && Names.isSub(it.name) },
                folders = if (movie.isFolder) all.filter { it.isDir } else emptyList(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (movie.isFolder) {
                error = e.message ?: "Could not load folder"
            } else {
                listing = Listing(listOf(DirEntry(movie.rawName, movie.url, false, 0)), emptyList(), emptyList())
            }
        }
    }
    LaunchedEffect(listing) {
        if (listing != null) {
            delay(200)
            runCatching { focus.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val backdrop = meta?.backdrop
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.22f),
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                Modifier
                    .width(200.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF22222B)),
                contentAlignment = Alignment.Center,
            ) {
                val poster = meta?.poster
                if (poster != null) {
                    AsyncImage(
                        model = poster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(movie.title, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    movie.label,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val rating = meta?.rating
                if (rating != null && rating > 0) {
                    Text("TMDB %.1f / 10".format(rating), color = Dim, fontSize = 16.sp)
                }
                val overview = meta?.overview
                if (overview != null) {
                    Text(
                        overview,
                        fontSize = 16.sp,
                        color = Color(0xFFDDDDDD),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))

                val l = listing
                when {
                    error != null -> Text("Could not load: $error", color = Color(0xFFFF8A80))
                    l == null -> Text("Loading files...", color = Dim)
                    l.videos.isEmpty() && l.folders.isEmpty() -> Text("No video files found in this folder.", color = Dim)
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(l.videos, key = { _, v -> v.url }) { i, v ->
                            val pos = Store.getPos(v.url)
                            val label = (if (pos > 0) "Resume from ${fmtTime(pos)}   |   " else "Play   |   ") + v.name
                            FocusRow(
                                label,
                                modifier = if (i == 0) Modifier.focusRequester(focus) else Modifier,
                                trailing = fmtSize(v.size).ifEmpty { null },
                            ) { play(ctx, movie, v, subsFor(movie, v, l)) }
                        }
                        items(l.folders, key = { it.url }) { f ->
                            FocusRow("Open folder: ${f.name}") { onOpenFolder(f.url) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun SettingsScreen() {
    var key by remember { mutableStateOf(Store.tmdbKeyOverride) }
    var roots by remember { mutableStateOf(Store.roots()) }
    var cleared by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Text("TMDB API key (v3) for posters and descriptions", fontSize = 18.sp)
        if (BuildConfig.TMDB_API_KEY.isNotBlank()) {
            Text("A key is already built into this app. Leave the box empty to use it.", color = Dim, fontSize = 14.sp)
        }
        OutlinedTextField(
            value = key,
            onValueChange = {
                key = it
                Store.tmdbKeyOverride = it
                MetaRepo.authFailed = false
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        FocusButton(if (cleared) "Poster cache cleared" else "Clear poster cache") {
            Store.clearMeta()
            MetaRepo.clearMem()
            cleared = true
        }

        Text(
            "Home rows",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (roots.isEmpty()) {
            Text("No rows. Use Browse all to add folders.", color = Dim)
        }
        roots.forEach { r ->
            Row(
                Modifier.fillMaxWidth(0.7f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(r.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                FocusButton("Remove") {
                    Store.removeRoot(r.url)
                    roots = Store.roots()
                }
            }
        }
    }
}
