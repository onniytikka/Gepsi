package com.gepsi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert
    suspend fun insert(route: Route): Long

    @Update
    suspend fun update(route: Route)

    @Query("UPDATE route SET endTs = :endTs WHERE id = :id")
    suspend fun finish(id: Long, endTs: Long)

    @Query("UPDATE route SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM route WHERE id = :id")
    suspend fun get(id: Long): Route?

    @Query("SELECT * FROM route ORDER BY startTs DESC")
    fun observeAll(): Flow<List<Route>>

    @Query("SELECT * FROM route WHERE synced = 0")
    suspend fun unsynced(): List<Route>
}

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(point: TrackPoint): Long

    @Query("SELECT * FROM track_point WHERE routeId = :routeId ORDER BY ts ASC")
    fun observeByRoute(routeId: Long): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_point WHERE routeId = :routeId ORDER BY ts ASC")
    suspend fun listByRoute(routeId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_point WHERE synced = 0 AND routeId = :routeId")
    suspend fun unsynced(routeId: Long): List<TrackPoint>

    @Query("UPDATE track_point SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM track_point WHERE routeId = :routeId")
    suspend fun countByRoute(routeId: Long): Int
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Query("SELECT * FROM note WHERE routeId = :routeId ORDER BY ts ASC")
    fun observeByRoute(routeId: Long): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE routeId = :routeId ORDER BY ts ASC")
    suspend fun listByRoute(routeId: Long): List<Note>

    @Query("SELECT * FROM note WHERE synced = 0 AND routeId = :routeId")
    suspend fun unsynced(routeId: Long): List<Note>

    @Query("UPDATE note SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
