package com.topsort.analytics.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * The events API rejects a whole purchase with a 400 when any item breaks these rules, so the SDK
 * has to catch them before the purchase is cached: once it is sent, the purchase is lost.
 */
internal class PurchaseValidationTest {

    private val valid = PurchasedItem(productId = "p1", quantity = 1, unitPrice = 25)

    @Test
    fun `valid items produce no errors`() {
        assertThat(purchaseItemErrors(listOf(valid, valid.copy(quantity = 3)))).isEmpty()
    }

    @Test
    fun `empty item list is rejected`() {
        assertThat(purchaseItemErrors(emptyList())).containsExactly("items must not be empty")
    }

    @Test
    fun `missing unitPrice is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(unitPrice = null))))
            .containsExactly("items[0] (p1): unitPrice is required")
    }

    @Test
    fun `zero unitPrice is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(unitPrice = 0))))
            .containsExactly("items[0] (p1): unitPrice must be greater than 0, was 0")
    }

    @Test
    fun `negative unitPrice is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(unitPrice = -5))))
            .containsExactly("items[0] (p1): unitPrice must be greater than 0, was -5")
    }

    @Test
    fun `zero quantity is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(quantity = 0))))
            .containsExactly("items[0] (p1): quantity must be at least 1, was 0")
    }

    @Test
    fun `negative quantity is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(quantity = -2))))
            .containsExactly("items[0] (p1): quantity must be at least 1, was -2")
    }

    @Test
    fun `empty productId is rejected`() {
        assertThat(purchaseItemErrors(listOf(valid.copy(productId = ""))))
            .containsExactly("items[0] (): productId must not be empty")
    }

    @Test
    fun `every broken item and field is reported, with its index`() {
        val errors = purchaseItemErrors(
            listOf(valid, valid.copy(productId = "p2", quantity = 0, unitPrice = null))
        )

        assertThat(errors).containsExactly(
            "items[1] (p2): quantity must be at least 1, was 0",
            "items[1] (p2): unitPrice is required",
        )
    }
}
