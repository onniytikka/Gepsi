package com.gepsi.exchange

import android.content.Context
import com.gepsi.BuildConfig
import com.gepsi.data.GepsiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Bundles every route, track point, note and voice file into a single shareable .gepsi (zip) file. */
class PackageExporter(
    private val context: Context,
    private val repo: GepsiRepository,
) {
    suspend fun exportAll(): File = withContext(Dispatchers.IO) {
        val voiceFiles = mutableMapOf<String, File>() // zip entry name -> source file

        val routeExports = repo.allRoutes().map { route ->
            val notes = repo.notesForRoute(route.id).map { note ->
                val audioEntry = note.voicePath
                    ?.let(::File)
                    ?.takeIf { it.exists() }
                    ?.let { f ->
                        val entry = "audio/${f.name}"
                        voiceFiles[entry] = f
                        entry
                    }
                NoteExport(lat = note.lat, lon = note.lon, ts = note.ts, text = note.text, audioFile = audioEntry)
            }
            RouteExport(
                uuid = route.uuid,
                name = route.name,
                startTs = route.startTs,
                endTs = route.endTs,
                points = repo.pointsForRoute(route.id).map {
                    PointExport(lat = it.lat, lon = it.lon, ts = it.ts, accuracyM = it.accuracyM)
                },
                notes = notes,
            )
        }

        val manifest = PackageManifest(
            schemaVersion = PACKAGE_SCHEMA_VERSION,
            packageId = UUID.randomUUID().toString(),
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            routes = routeExports,
        )
        val json = packageMoshi.adapter(PackageManifest::class.java).toJson(manifest)

        val exportDir = File(context.cacheDir, "export").apply {
            deleteRecursively()
            mkdirs()
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        val outFile = File(exportDir, "gepsi-export-$stamp.gepsi")

        ZipOutputStream(outFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(PACKAGE_MANIFEST_NAME))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((entryName, file) in voiceFiles) {
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        outFile
    }
}
