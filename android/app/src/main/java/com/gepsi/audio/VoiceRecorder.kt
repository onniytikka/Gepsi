package com.gepsi.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(): File {
        if (recorder != null) error("Recorder already running")
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")
        currentFile = file

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        return file
    }

    fun stop(): File? {
        val rec = recorder ?: return null
        val file = currentFile
        try {
            rec.stop()
        } catch (_: RuntimeException) {
            file?.delete()
            recorder = null
            currentFile = null
            return null
        } finally {
            rec.reset()
            rec.release()
            recorder = null
        }
        currentFile = null
        return file
    }

    fun cancel() {
        recorder?.runCatching { stop() }
        recorder?.runCatching { reset() }
        recorder?.release()
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}
