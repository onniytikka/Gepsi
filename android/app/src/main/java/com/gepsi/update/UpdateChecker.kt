package com.gepsi.update

import com.gepsi.BuildConfig
import com.gepsi.exchange.packageMoshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// Serialized with Moshi reflection — no @JsonClass (codegen is not applied).
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
)

/** Checks the published version manifest on GitHub against the running build. */
object UpdateChecker {
    private const val VERSION_URL =
        "https://github.com/onniytikka/Gepsi/raw/main/apk/version.json"

    /** Returns the newer version if one is published, null otherwise (or on any error). */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient()
            client.newCall(Request.Builder().url(VERSION_URL).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string() ?: return@runCatching null
                val info = packageMoshi.adapter(UpdateInfo::class.java).fromJson(body)
                info?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
            }
        }.getOrNull()
    }
}
