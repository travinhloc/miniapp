@file:Suppress("LongMethod", "ComplexMethod")

package com.vault.vanishx.data.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single active voice playback for chat bubbles (one message at a time).
 */
object VoicePlaybackBus {
    data class State(
        val messageId: String? = null,
        val playing: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private val activeId = AtomicReference<String?>(null)
    private val generation = AtomicInteger(0)

    fun toggle(context: Context, messageId: String, path: String) {
        if (activeId.get() == messageId && player != null) {
            toggleExisting()
            return
        }
        startNew(context, messageId, path)
    }

    fun stop() {
        generation.incrementAndGet()
        releasePlayer()
        activeId.set(null)
        _state.value = State()
    }

    fun pollPosition() {
        runCatching {
            val p = player ?: return
            if (!p.isPlaying) return
            _state.value = _state.value.copy(
                playing = true,
                positionMs = p.currentPosition.toLong(),
                durationMs = p.duration.toLong().coerceAtLeast(_state.value.durationMs),
            )
        }
    }

    private fun toggleExisting() {
        val p = player ?: return
        runCatching {
            if (p.isPlaying) {
                p.pause()
                _state.value = _state.value.copy(
                    playing = false,
                    positionMs = p.currentPosition.toLong(),
                )
            } else {
                p.start()
                _state.value = _state.value.copy(playing = true)
            }
        }
    }

    private fun startNew(context: Context, messageId: String, path: String) {
        val gen = generation.incrementAndGet()
        releasePlayer()
        activeId.set(null)
        val uri = resolveUri(path)
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setDataSource(context, uri)
            bindPlayerCallbacks(mp, gen, messageId)
            mp.prepareAsync()
        }.onFailure {
            if (generation.get() == gen) stop()
        }
    }

    private fun bindPlayerCallbacks(mp: MediaPlayer, gen: Int, messageId: String) {
        mp.setOnPreparedListener { prepared ->
            if (generation.get() != gen || player !== prepared) {
                runCatching { prepared.release() }
                return@setOnPreparedListener
            }
            activeId.set(messageId)
            _state.value = State(
                messageId = messageId,
                playing = true,
                positionMs = 0L,
                durationMs = prepared.duration.toLong().coerceAtLeast(0L),
            )
            runCatching { prepared.start() }
        }
        mp.setOnCompletionListener {
            if (generation.get() != gen) return@setOnCompletionListener
            _state.value = State(
                messageId = messageId,
                playing = false,
                positionMs = _state.value.durationMs,
                durationMs = _state.value.durationMs,
            )
            if (player === it) {
                releasePlayer()
                activeId.set(null)
            }
        }
        mp.setOnErrorListener { _, _, _ ->
            if (generation.get() == gen) stop()
            true
        }
    }

    private fun releasePlayer() {
        val current = player
        player = null
        runCatching { current?.setOnPreparedListener(null) }
        runCatching { current?.setOnCompletionListener(null) }
        runCatching { current?.setOnErrorListener(null) }
        runCatching { current?.stop() }
        runCatching { current?.release() }
    }

    private fun resolveUri(path: String): Uri {
        val file = File(path)
        return if (file.exists()) Uri.fromFile(file) else Uri.parse(path)
    }
}
