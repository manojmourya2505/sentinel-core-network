package com.sentinelbank.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds an `Authorization: Bearer <token>` header to every outgoing request when
 * [authTokenProvider] returns a non-null token. Requests are left untouched when no token is
 * available (e.g. signed-out state).
 */
class AuthInterceptor(
    private val authTokenProvider: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authTokenProvider()

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
