package com.gepsi

import android.app.Application
import android.content.Context
import com.gepsi.data.GepsiDatabase
import com.gepsi.data.GepsiRepository
import com.gepsi.sync.GepsiApi
import com.gepsi.sync.SyncScheduler
import org.osmdroid.config.Configuration
import java.io.File

class GepsiApp : Application() {

    lateinit var repository: GepsiRepository
        private set

    var api: GepsiApi? = null
        private set

    var syncScheduler: SyncScheduler? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val prefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().apply {
            load(this@GepsiApp, prefs)
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(filesDir, "osmdroid/tiles")
            osmdroidBasePath.mkdirs()
            osmdroidTileCache.mkdirs()
        }

        val db = GepsiDatabase.get(this)
        repository = GepsiRepository(db.routeDao(), db.trackPointDao(), db.noteDao())

        val backendUrl = BuildConfig.BACKEND_URL
        if (backendUrl.isNotBlank()) {
            api = GepsiApi.create(backendUrl)
            syncScheduler = SyncScheduler(this)
        }
    }

    companion object {
        @Volatile private var instance: GepsiApp? = null
        fun get(): GepsiApp = instance ?: error("GepsiApp not initialized")
    }
}
