package com.ctgtv.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var view: PlayerView
    private var url: String = ""
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val u = intent.getStringExtra("url")
        if (u == null) {
            finish()
            return
        }
        url = u

        view = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            controllerShowTimeoutMs = 4000
        }
        setContentView(view)
    }

    override fun onStart() {
        super.onStart()
        if (url.isNotEmpty()) startPlayback()
    }

    private fun startPlayback() {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Net.UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)

        val p = ExoPlayer.Builder(this, DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        val subs = (intent.getStringArrayListExtra("subs") ?: arrayListOf<String>()).map { s ->
            val mime = when (Names.ext(s.substringBefore('?'))) {
                "vtt" -> MimeTypes.TEXT_VTT
                "ass", "ssa" -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(s))
                .setMimeType(mime)
                .setLanguage("en")
                .setLabel(Uri.decode(s.substringAfterLast('/')))
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }

        val item = MediaItem.Builder()
            .setUri(url)
            .setSubtitleConfigurations(subs)
            .build()

        p.setMediaItem(item, Store.getPos(url))
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                showError(error)
            }
        })
        p.prepare()
        p.playWhenReady = true

        view.player = p
        player = p
    }

    private fun savePosition() {
        val p = player ?: return
        val dur = p.duration
        val pos = p.currentPosition
        if (dur != C.TIME_UNSET && dur > 0 && pos > dur - 60_000) {
            Store.setPos(url, 0) // finished
        } else if (pos > 15_000) {
            Store.setPos(url, pos)
        }
    }

    override fun onStop() {
        super.onStop()
        savePosition()
        if (::view.isInitialized) view.player = null
        player?.release()
        player = null
    }

    private fun showError(e: PlaybackException) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Can't play this file")
            .setMessage(
                "${e.errorCodeName}\n\nThis TV may not support the video or audio format. " +
                    "You can try an external player such as VLC or MX Player."
            )
            .setPositiveButton("Open external player") { _, _ -> openExternal() }
            .setNegativeButton("Close") { _, _ -> finish() }
            .show()
    }

    private fun openExternal() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*"))
        } catch (e: Exception) {
            Toast.makeText(this, "No external player installed", Toast.LENGTH_LONG).show()
        }
    }

    private fun hint(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).also { it.show() }
    }

    /** With the controls hidden, left/right on the remote seek 10 seconds. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val p = player
        if (p != null && !view.isControllerFullyVisible && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    p.seekBack()
                    hint("<<  ${fmtTime(p.currentPosition)}")
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    p.seekForward()
                    hint(">>  ${fmtTime(p.currentPosition)}")
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
