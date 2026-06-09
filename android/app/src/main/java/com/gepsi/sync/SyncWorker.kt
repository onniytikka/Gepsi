package com.gepsi.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gepsi.GepsiApp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val noteMetaAdapter = moshi.adapter(NoteMetaDto::class.java)

    override suspend fun doWork(): Result {
        val app = GepsiApp.get()
        val repo = app.repository
        val api = app.api ?: return Result.success()

        return try {
            for (route in repo.unsyncedRoutes()) {
                if (route.endTs == null) continue
                api.createRoute(
                    RouteDto(
                        client_id = route.id,
                        name = route.name,
                        start_ts = route.startTs,
                        end_ts = route.endTs,
                    )
                )

                val pts = repo.unsyncedPoints(route.id)
                if (pts.isNotEmpty()) {
                    val batch = PointBatchDto(pts.map {
                        PointDto(
                            client_id = it.id,
                            lat = it.lat,
                            lon = it.lon,
                            ts = it.ts,
                            accuracy_m = it.accuracyM,
                        )
                    })
                    api.postPoints(route.id, batch)
                    repo.markPointsSynced(pts.map { it.id })
                }

                for (note in repo.unsyncedNotes(route.id)) {
                    val meta = NoteMetaDto(
                        client_id = note.id,
                        lat = note.lat,
                        lon = note.lon,
                        ts = note.ts,
                        text = note.text,
                    )
                    val metaBody = noteMetaAdapter.toJson(meta)
                        .toRequestBody("application/json".toMediaType())
                    val voicePart = note.voicePath?.let { path ->
                        val file = File(path)
                        if (!file.exists()) null else {
                            val rb = file.asRequestBody("audio/mp4".toMediaType())
                            MultipartBody.Part.createFormData("voice", file.name, rb)
                        }
                    }
                    api.postNote(route.id, metaBody, voicePart)
                    repo.markNoteSynced(note.id)
                }

                repo.markRouteSynced(route.id)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed; will retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
