package com.sentinelbank.core.network.mock

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockInterceptorTest {

    private fun clientWith(interceptor: MockInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(interceptor).build()

    @Test
    fun `returns registered fixture body and status for a matching path and method`() {
        val interceptor = MockInterceptor()
        DefaultBankingFixtures.registerDefaults(interceptor)
        val client = clientWith(interceptor)

        val request = Request.Builder().url("https://api.sentinelbank.example/accounts").get().build()
        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        val body = response.body!!.string()
        assertTrue(body.contains("Everyday Checking"))
        assertTrue(body.contains("acc_1001"))
    }

    @Test
    fun `returns registered fixture for a POST fixture`() {
        val interceptor = MockInterceptor()
        DefaultBankingFixtures.registerDefaults(interceptor)
        val client = clientWith(interceptor)

        val request = Request.Builder()
            .url("https://api.sentinelbank.example/transfer")
            .post("{}".toRequestBody())
            .build()
        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains("SUCCESS"))
    }

    @Test
    fun `returns synthetic 404 when no fixture matches`() {
        val interceptor = MockInterceptor()
        DefaultBankingFixtures.registerDefaults(interceptor)
        val client = clientWith(interceptor)

        val request = Request.Builder().url("https://api.sentinelbank.example/nonexistent").get().build()
        val response = client.newCall(request).execute()

        assertEquals(404, response.code)
    }

    @Test
    fun `does not match a registered path when the method differs`() {
        val interceptor = MockInterceptor()
        interceptor.registerFixture(Regex("^/accounts/?$"), "GET", """{"ok":true}""")
        val client = clientWith(interceptor)

        val request = Request.Builder()
            .url("https://api.sentinelbank.example/accounts")
            .post("{}".toRequestBody())
            .build()
        val response = client.newCall(request).execute()

        assertEquals(404, response.code)
    }
}
