package com.gepsi.exchange

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Serialized with reflection (KotlinJsonAdapterFactory) — Moshi codegen is not
// applied in this project, so these must NOT use @JsonClass.

const val PACKAGE_SCHEMA_VERSION = 1
const val PACKAGE_MANIFEST_NAME = "manifest.json"

data class PackageManifest(
    val schemaVersion: Int,
    val packageId: String,
    val exportedAt: Long,
    val appVersion: String,
    val routes: List<RouteExport>,
)

data class RouteExport(
    val uuid: String,
    val name: String,
    val startTs: Long,
    val endTs: Long?,
    val points: List<PointExport>,
    val notes: List<NoteExport>,
)

data class PointExport(
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val accuracyM: Float,
)

data class NoteExport(
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val text: String?,
    val audioFile: String?,
)

val packageMoshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
