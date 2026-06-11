package com.gepsi.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * App-scoped voice memo player. Living outside the activity, it keeps playing
 * across configuration changes (rotation); the UI just re-collects [state].
 */
class AudioPlaybackController {

    data class State(
        val path: String? = null,
        val playing: Boolean = false,
        val positionMs: Int = 0,
        val durationMs: Int = 0,
        val error: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player = MediaPlayer()
    private var pollJob: Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Loads [path] if not already loaded. No-op when the same memo is current, so rotation doesn't restart playback. */
    fun prepare(path: String) {
        if (_state.value.path == path && !_state.value.error) return
        stop()
        val ok = runCatching {
            if (!File(path).exists()) error("missing file")
            player.reset()
            player.setDataSource(path)
            player.prepare()
            player.setOnCompletionListener {
                pollJob?.cancel()
                _state.value = _state.value.copy(playing = false, positionMs = _state.value.durationMs)
            }
        }.isSuccess
        _state.value = if (ok) {
            State(path = path, durationMs = player.duration)
        } else {
            State(path = path, error = true)
        }
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.path == null || s.error) return
        runCatching {
            if (s.playing) {
                player.pause()
                pollJob?.cancel()
                _state.value = s.copy(playing = false, positionMs = player.currentPosition)
            } else {
                if (s.positionMs >= s.durationMs) player.seekTo(0)
                player.start()
                _state.value = s.copy(playing = true)
                startPolling()
            }
        }
    }

    fun seekTo(ms: Int) {
        val s = _state.value
        if (s.path == null || s.error) return
        runCatching {
            player.seekTo(ms.coerceIn(0, s.durationMs))
            _state.value = s.copy(positionMs = ms.coerceIn(0, s.durationMs))
        }
    }

    /** Stops playback and unloads the current memo. */
    fun stop() {
        pollJob?.cancel()
        runCatching {
            if (player.isPlaying) player.stop()
            player.reset()
        }
        _state.value = State()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                runCatching {
                    _state.value = _state.value.copy(positionMs = player.currentPosition)
                }
                delay(200)
            }
        }
    }
}
