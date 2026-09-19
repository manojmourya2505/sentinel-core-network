package com.sentinelbank.core.network

import com.sentinelbank.core.network.auth.AuthInterceptor
import com.sentinelbank.core.network.logging.RedactedLoggingInterceptor
import com.sentinelbank.core.network.mock.MockInterceptor
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a fully configured [Retrofit] instance for talking to the Sentinel Bank backend.
 *
 * There is no real backend yet, so [mockMode] defaults to `true`: a [MockInterceptor] is
 * installed as the outermost interceptor and short-circuits every request with locally
 * registered fixtures, letting the app run standalone with zero network access. When a real
 * backend exists, callers flip `mockMode = false` and this same factory serves real traffic —
 * including an already-wired (but inactive until [certificatePins] is non-empty) certificate
 * pinning seam.
 */
object ApiClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun create(
        baseUrl: String,
        authTokenProvider: () -> String? = { null },
        mockMode: Boolean = true,
        certificatePins: List<String> = emptyList(),
        mockInterceptor: MockInterceptor = MockInterceptor().also { com.sentinelbank.core.network.mock.DefaultBankingFixtures.registerDefaults(it) },
    ): Retrofit {
        val httpUrl = baseUrl.toHttpUrl()

        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        // Mock mode is checked first: if enabled, it must be the outermost interceptor so it can
        // answer requests before any other interceptor (auth, logging, pinning) touches them.
        if (mockMode) {
            clientBuilder.addInterceptor(mockInterceptor)
        }

        clientBuilder
            .addInterceptor(AuthInterceptor(authTokenProvider))
            .addInterceptor(RedactedLoggingInterceptor())

        // Certificate pinning seam: real, wired, but inactive until pins exist (no real backend
        // or certificate to pin yet). Supplying pins activates it with no other code changes.
        if (certificatePins.isNotEmpty()) {
            val pinnerBuilder = CertificatePinner.Builder()
            certificatePins.forEach { pin ->
                pinnerBuilder.add(httpUrl.host, pin)
            }
            clientBuilder.certificatePinner(pinnerBuilder.build())
        }

        val contentType = "application/json; charset=utf-8".toMediaType()

        return Retrofit.Builder()
            .baseUrl(httpUrl)
            .client(clientBuilder.build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
