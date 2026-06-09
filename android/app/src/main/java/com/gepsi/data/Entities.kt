package com.gepsi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "route")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTs: Long,
    val endTs: Long? = null,
    val synced: Boolean = false,
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
