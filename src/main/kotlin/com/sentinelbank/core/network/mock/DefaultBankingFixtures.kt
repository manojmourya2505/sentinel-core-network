package com.sentinelbank.core.network.mock

/**
 * A small starter set of realistic fixtures for [MockInterceptor], demonstrating the pattern the
 * app layer is expected to extend with its own, richer fixtures for its actual API surface.
 */
object DefaultBankingFixtures {

    private val accountsBody = """
        [
          {
            "id": "acc_1001",
            "nickname": "Everyday Checking",
            "type": "CHECKING",
            "balanceCents": 284310,
            "currency": "USD"
          },
          {
            "id": "acc_1002",
            "nickname": "High-Yield Savings",
            "type": "SAVINGS",
            "balanceCents": 1520045,
            "currency": "USD"
          }
        ]
    """.trimIndent()

    private val transferSuccessBody = """
        {
          "status": "SUCCESS",
          "transferId": "trf_20260919_0001",
          "message": "Transfer completed."
        }
    """.trimIndent()

    /** Registers the default example fixtures onto [interceptor]. */
    fun registerDefaults(interceptor: MockInterceptor) {
        interceptor.registerFixture(
            pathPattern = Regex("^/accounts/?$"),
            method = "GET",
            responseBody = accountsBody,
            statusCode = 200,
        )
        interceptor.registerFixture(
            pathPattern = Regex("^/transfer/?$"),
            method = "POST",
            responseBody = transferSuccessBody,
            statusCode = 200,
        )
    }
}
