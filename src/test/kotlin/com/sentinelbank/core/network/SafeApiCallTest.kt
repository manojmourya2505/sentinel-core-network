package com.sentinelbank.core.network

import com.sentinelbank.core.common.AppError
import com.sentinelbank.core.common.Result
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SafeApiCallTest {

    @Test
    fun `maps a successful block to Result Success`() = runTest {
        val result = safeApiCall { "ok" }

        assertIs<Result.Success<String>>(result)
        assertEquals("ok", result.data)
    }

    @Test
    fun `maps a thrown IOException to Result Error with AppError Network`() = runTest {
        val result = safeApiCall<String> { throw IOException("connection reset") }

        assertIs<Result.Error>(result)
        val error = result.error
        assertIs<AppError.Network>(error)
        assertTrue(error.message.contains("connection reset"))
    }

    @Test
    fun `maps a thrown HttpException to Result Error with AppError Network`() = runTest {
        val httpException = HttpException(Response.error<Any>(500, "boom".toResponseBody()))

        val result = safeApiCall<String> { throw httpException }

        assertIs<Result.Error>(result)
        val error = result.error
        assertIs<AppError.Network>(error)
        assertTrue(error.message.contains("500"))
    }
}
