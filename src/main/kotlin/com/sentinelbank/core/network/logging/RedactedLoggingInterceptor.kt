package com.sentinelbank.core.network.logging

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * A logging [Interceptor] that records method, URL, status, and duration for every call, but
 * never logs request/response bodies or the raw value of the `Authorization` header — only that
 * it was present, redacted.
 *
 * Kept intentionally dependency-free (no OkHttp `HttpLoggingInterceptor`) so redaction is
 * guaranteed by construction rather than by configuration.
 */
class RedactedLoggingInterceptor(
    private val logger: (String) -> Unit = { println(it) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authHeader = if (request.header("Authorization") != null) "Authorization: <redacted>" else null

        val startNanos = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (throwable: Throwable) {
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
            logger(
                buildString {
                    append(request.method).append(' ').append(request.url)
                    append(" FAILED after ").append(durationMs).append("ms: ")
                    append(throwable::class.simpleName).append(": ").append(throwable.message)
                    if (authHeader != null) append(" [").append(authHeader).append(']')
                },
            )
            throw throwable
        }
        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)

        logger(
            buildString {
                append(request.method).append(' ').append(request.url)
                append(" -> ").append(response.code)
                append(" (").append(durationMs).append("ms)")
                if (authHeader != null) append(" [").append(authHeader).append(']')
            },
        )

        return response
    }
}
