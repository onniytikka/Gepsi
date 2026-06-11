package com.gepsi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Route::class, TrackPoint::class, Note::class],
    version = 2,
    exportSchema = false,
)
abstract class GepsiDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: GepsiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE route ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                // Backfill before creating the unique index.
                db.execSQL(
                    "UPDATE route SET uuid = lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) " +
                        "|| '-' || lower(hex(randomblob(2))) || '-' || lower(hex(randomblob(2))) " +
                        "|| '-' || lower(hex(randomblob(6)))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_route_uuid ON route (uuid)")
            }
        }

        fun get(context: Context): GepsiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GepsiDatabase::class.java,
                    "gepsi.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
