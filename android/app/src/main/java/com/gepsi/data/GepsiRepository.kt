package com.gepsi.data

import kotlinx.coroutines.flow.Flow

class GepsiRepository(
    private val routes: RouteDao,
    private val points: TrackPointDao,
    private val notes: NoteDao,
) {
    suspend fun startRoute(name: String, startTs: Long): Long =
        routes.insert(Route(name = name, startTs = startTs))

    suspend fun finishRoute(routeId: Long, endTs: Long) {
        routes.finish(routeId, endTs)
    }

    suspend fun appendPoint(point: TrackPoint) {
        points.insert(point)
    }

    suspend fun addNote(note: Note): Long = notes.insert(note)

    suspend fun getRoute(id: Long): Route? = routes.get(id)

    suspend fun allRoutes(): List<Route> = routes.listAll()

    suspend fun existingRouteUuids(): Set<String> = routes.allUuids().toSet()

    suspend fun insertRoute(route: Route): Long = routes.insert(route)

    suspend fun insertPoint(point: TrackPoint): Long = points.insert(point)

    suspend fun pointsForRoute(id: Long): List<TrackPoint> = points.listByRoute(id)

    suspend fun notesForRoute(id: Long): List<Note> = notes.listByRoute(id)

    suspend fun pointCount(id: Long): Int = points.countByRoute(id)

    fun observeRoutes(): Flow<List<Route>> = routes.observeAll()
    fun observePoints(routeId: Long): Flow<List<TrackPoint>> = points.observeByRoute(routeId)
    fun observeNotes(routeId: Long): Flow<List<Note>> = notes.observeByRoute(routeId)

    suspend fun unsyncedRoutes(): List<Route> = routes.unsynced()
    suspend fun unsyncedPoints(routeId: Long): List<TrackPoint> = points.unsynced(routeId)
    suspend fun unsyncedNotes(routeId: Long): List<Note> = notes.unsynced(routeId)
    suspend fun markRouteSynced(id: Long) = routes.markSynced(id)
    suspend fun markPointsSynced(ids: List<Long>) = points.markSynced(ids)
    suspend fun markNoteSynced(id: Long) = notes.markSynced(id)
}
