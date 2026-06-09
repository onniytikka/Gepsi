package com.gepsi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Route::class, TrackPoint::class, Note::class],
    version = 1,
    exportSchema = false,
)
abstract class GepsiDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: GepsiDatabase? = null

        fun get(context: Context): GepsiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GepsiDatabase::class.java,
                    "gepsi.db",
                ).build().also { instance = it }
            }
    }
}
