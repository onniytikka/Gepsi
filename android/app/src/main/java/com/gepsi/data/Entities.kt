package com.gepsi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "route",
    indices = [Index(value = ["uuid"], unique = true)],
)
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTs: Long,
    val endTs: Long? = null,
    val synced: Boolean = false,
    // Stable identity across devices, used to deduplicate package imports.
    @ColumnInfo(defaultValue = "") val uuid: String = UUID.randomUUID().toString(),
)

@Entity(
    tableName = "track_point",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("routeId")],
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val accuracyM: Float,
    val synced: Boolean = false,
)

@Entity(
    tableName = "note",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("routeId")],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val lat: Double,
    val lon: Double,
    val ts: Long,
    val text: String? = null,
    val voicePath: String? = null,
    val synced: Boolean = false,
)
