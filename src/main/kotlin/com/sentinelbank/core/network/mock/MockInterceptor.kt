package com.sentinelbank.core.network.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A local-fixture responder used in place of a real backend.
 *
 * When installed as the outermost [Interceptor] on an [okhttp3.OkHttpClient], it matches every
 * outgoing request's path + HTTP method against a registry of fixtures and answers with a
 * synthetic [Response] built entirely in-process — no socket is ever opened. This lets an app
 * built on `core-network` run fully standalone before a real backend exists.
 *
 * If no fixture matches, a synthetic 404 is returned rather than falling through to the network,
 * so mock mode is always a hermetic sandbox.
 */
class MockInterceptor : Interceptor {

    private data class Fixture(
        val pathPattern: Regex,
        val method: String,
        val responseBody: String,
        val statusCode: Int,
    )

    private val fixtures = CopyOnWriteArrayList<Fixture>()

    /**
     * Registers a fixture that answers any request whose path matches [pathPattern] and whose
     * HTTP method equals [method] (case-insensitive) with [responseBody] and [statusCode].
     *
     * Later registrations take precedence over earlier ones for overlapping patterns, since
     * matching scans in reverse-registration order.
     */
    fun registerFixture(pathPattern: Regex, method: String, responseBody: String, statusCode: Int = 200) {
        fixtures.add(Fixture(pathPattern, method.uppercase(), responseBody, statusCode))
    }

    /** Removes every registered fixture. */
    fun clear() {
        fixtures.clear()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method.uppercase()

        val match = fixtures.lastOrNull { it.method == method && it.pathPattern.containsMatchIn(path) }

        val (status, body) = if (match != null) {
            match.statusCode to match.responseBody
        } else {
            404 to """{"error":"no_mock_fixture_registered","path":"$path","method":"$method"}"""
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message(if (match != null) "OK" else "Not Found")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }
}
