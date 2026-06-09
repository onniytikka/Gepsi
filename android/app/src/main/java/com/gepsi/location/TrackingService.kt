package com.gepsi.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gepsi.GepsiApp
import com.gepsi.MainActivity
import com.gepsi.R
import com.gepsi.data.TrackPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class TrackingService : Service() {

    private lateinit var client: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeRouteId = AtomicLong(0L)

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val rid = activeRouteId.get()
            if (rid == 0L) return
            for (loc in result.locations) {
                persistPoint(rid, loc)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        client = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent.getStringExtra(EXTRA_ROUTE_NAME) ?: "Walk")
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart(routeName: String) {
        if (activeRouteId.get() != 0L) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Missing ACCESS_FINE_LOCATION; aborting tracking start")
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            val repo = GepsiApp.get().repository
            val id = repo.startRoute(routeName, System.currentTimeMillis())
            activeRouteId.set(id)
            TrackingState.setActive(id)
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        try {
            client.requestLocationUpdates(req, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "requestLocationUpdates denied", e)
            stopSelf()
        }
    }

    private fun handleStop() {
        val rid = activeRouteId.getAndSet(0L)
        client.removeLocationUpdates(callback)
        TrackingState.setActive(null)
        if (rid != 0L) {
            scope.launch {
                GepsiApp.get().repository.finishRoute(rid, System.currentTimeMillis())
                GepsiApp.get().syncScheduler?.enqueueNow()
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persistPoint(routeId: Long, loc: Location) {
        scope.launch {
            GepsiApp.get().repository.appendPoint(
                TrackPoint(
                    routeId = routeId,
                    lat = loc.latitude,
                    lon = loc.longitude,
                    ts = if (loc.time > 0) loc.time else System.currentTimeMillis(),
                    accuracyM = if (loc.hasAccuracy()) loc.accuracy else 0f,
                )
            )
        }
    }

    private fun buildNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.tracking_channel_desc)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(ch)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.gepsi.action.START_TRACKING"
        const val ACTION_STOP = "com.gepsi.action.STOP_TRACKING"
        const val EXTRA_ROUTE_NAME = "route_name"
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "gepsi_tracking"
        private const val TAG = "TrackingService"

        fun startIntent(ctx: Context, routeName: String): Intent =
            Intent(ctx, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ROUTE_NAME, routeName)
            }

        fun stopIntent(ctx: Context): Intent =
            Intent(ctx, TrackingService::class.java).apply { action = ACTION_STOP }
    }
}
