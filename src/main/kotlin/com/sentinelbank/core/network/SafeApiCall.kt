package com.sentinelbank.core.network

import com.sentinelbank.core.common.AppError
import com.sentinelbank.core.common.Result
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs [block] and maps any failure into [com.sentinelbank.core.common.AppError.Network],
 * wrapped in core-common's [Result] type, so callers never need to catch networking exceptions
 * themselves.
 *
 * - [IOException] (no connectivity, timeout, DNS failure, or — in mock mode — an unmatched
 *   fixture bubbling up as a transport failure) maps to a network error.
 * - Retrofit's [HttpException] (a non-2xx HTTP response) maps to a network error carrying the
 *   HTTP status in its message.
 * - Any other unexpected [Exception] is also captured as a network error rather than propagating,
 *   since from the caller's perspective it still represents "the call did not succeed."
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (io: IOException) {
        Result.Error(AppError.Network(message = io.message ?: "Network I/O error", cause = io))
    } catch (http: HttpException) {
        Result.Error(
            AppError.Network(
                message = "HTTP ${http.code()}: ${http.message()}",
                cause = http,
            ),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (unexpected: Exception) {
        Result.Error(AppError.Network(message = unexpected.message ?: "Unexpected network error", cause = unexpected))
    }
}
