package com.gepsi.exchange

import android.content.Context
import android.net.Uri
import com.gepsi.data.GepsiRepository
import com.gepsi.data.Note
import com.gepsi.data.Route
import com.gepsi.data.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

class InvalidPackageException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ImportPreview(
    val cachedZip: File,
    val manifest: PackageManifest,
    val newRoutes: List<RouteExport>,
    val duplicateRouteNames: List<String>,
) {
    val pointCount: Int get() = newRoutes.sumOf { it.points.size }
    val noteCount: Int get() = newRoutes.sumOf { it.notes.size }
    val voiceCount: Int get() = newRoutes.sumOf { r -> r.notes.count { it.audioFile != null } }
}

data class ImportResult(
    val routesImported: Int,
    val pointsImported: Int,
    val notesImported: Int,
)

/** Reads a .gepsi package and inserts its routes as new local rows, deduplicating by route uuid. */
class PackageImporter(
    private val context: Context,
    private val repo: GepsiRepository,
) {
    /**
     * Copies the content stream to cache immediately (the sender's URI grant is
     * transient and must not be held across the confirmation dialog), then parses
     * and partitions routes into new vs already-imported.
     */
    suspend fun preview(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val importDir = File(context.cacheDir, "import").apply {
            deleteRecursively()
            mkdirs()
        }
        val cached = File(importDir, "incoming.gepsi")
        runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                cached.outputStream().use { input.copyTo(it) }
            }
        }.getOrElse { throw InvalidPackageException("Could not read the shared file", it) }

        val manifest = runCatching {
            ZipFile(cached).use { zip ->
                val entry = zip.getEntry(PACKAGE_MANIFEST_NAME)
                    ?: throw InvalidPackageException("Not a Gepsi package")
                val json = zip.getInputStream(entry).bufferedReader().readText()
                packageMoshi.adapter(PackageManifest::class.java).fromJson(json)
                    ?: throw InvalidPackageException("Empty manifest")
            }
        }.getOrElse {
            if (it is InvalidPackageException) throw it
            throw InvalidPackageException("Not a valid Gepsi package", it)
        }

        if (manifest.schemaVersion > PACKAGE_SCHEMA_VERSION) {
            throw InvalidPackageException("Package was made with a newer Gepsi version — update the app")
        }

        val existing = repo.existingRouteUuids()
        val (dupes, fresh) = manifest.routes.partition { it.uuid in existing }
        ImportPreview(
            cachedZip = cached,
            manifest = manifest,
            newRoutes = fresh,
            duplicateRouteNames = dupes.map { it.name },
        )
    }

    suspend fun commit(preview: ImportPreview): ImportResult = withContext(Dispatchers.IO) {
        var routes = 0
        var points = 0
        var notesCount = 0
        val voiceDir = File(context.filesDir, "voice").apply { mkdirs() }

        ZipFile(preview.cachedZip).use { zip ->
            for (export in preview.newRoutes) {
                val newRouteId = repo.insertRoute(
                    Route(
                        name = export.name,
                        startTs = export.startTs,
                        endTs = export.endTs,
                        synced = false,
                        uuid = export.uuid,
                    )
                )
                routes++
                for (p in export.points) {
                    repo.insertPoint(
                        TrackPoint(routeId = newRouteId, lat = p.lat, lon = p.lon, ts = p.ts, accuracyM = p.accuracyM)
                    )
                    points++
                }
                for (n in export.notes) {
                    val voicePath = n.audioFile?.let { extractVoice(zip, it, voiceDir) }
                    repo.addNote(
                        Note(routeId = newRouteId, lat = n.lat, lon = n.lon, ts = n.ts, text = n.text, voicePath = voicePath)
                    )
                    notesCount++
                }
            }
        }
        preview.cachedZip.delete()
        ImportResult(routesImported = routes, pointsImported = points, notesImported = notesCount)
    }

    private fun extractVoice(zip: ZipFile, entryName: String, voiceDir: File): String? {
        // Zip-slip guard: only accept plain relative entries under audio/.
        if (entryName.contains("..") || entryName.startsWith("/") || entryName.contains("\\")) return null
        val entry = zip.getEntry(entryName) ?: return null
        val target = File(voiceDir, "${UUID.randomUUID()}.m4a")
        if (!target.canonicalPath.startsWith(voiceDir.canonicalPath + File.separator)) return null
        return runCatching {
            zip.getInputStream(entry).use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            target.absolutePath
        }.getOrNull()
    }
}
