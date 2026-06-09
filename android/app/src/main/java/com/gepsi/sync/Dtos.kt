package com.gepsi.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RouteDto(
    val client_id: Long,
    val name: String,
    val start_ts: Long,
    val end_ts: Long?,
)

@JsonClass(generateAdapter = true)
data class PointDto(
    val client_id: Long,
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val accuracy_m: Float,
)

@JsonClass(generateAdapter = true)
data class PointBatchDto(val points: List<PointDto>)

@JsonClass(generateAdapter = true)
data class NoteMetaDto(
    val client_id: Long,
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val text: String?,
)

@JsonClass(generateAdapter = true)
data class GenericAck(val inserted: Int? = null, val total: Int? = null)
