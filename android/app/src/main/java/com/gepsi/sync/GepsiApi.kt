package com.gepsi.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GepsiApi {

    @POST("api/routes")
    suspend fun createRoute(@Body route: RouteDto)

    @POST("api/routes/{client_id}/points/batch")
    suspend fun postPoints(
        @Path("client_id") clientId: Long,
        @Body batch: PointBatchDto,
    ): GenericAck

    @Multipart
    @POST("api/routes/{client_id}/notes")
    suspend fun postNote(
        @Path("client_id") clientId: Long,
        @Part("meta") meta: RequestBody,
        @Part voice: MultipartBody.Part?,
    )

    companion object {
        fun create(baseUrl: String): GepsiApi {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(log)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GepsiApi::class.java)
        }
    }
}
