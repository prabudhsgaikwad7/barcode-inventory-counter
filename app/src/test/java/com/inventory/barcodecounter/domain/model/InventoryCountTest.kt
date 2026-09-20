package com.inventory.barcodecounter.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryCountTest {

    @Test
    fun isExpired_returnsTrueWhenExpiryBeforeToday() {
        val count = InventoryCount(
            barcode = "1",
            productName = "Item",
            expectedQuantity = 1,
            actualQuantity = 1,
            expiryDate = LocalDate.of(2020, 1, 1),
            status = CountStatus.PENDING,
            updatedAtMillis = 0,
        )

        assertTrue(count.isExpired(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun isExpired_returnsFalseWhenExpiryIsTodayOrLater() {
        val today = LocalDate.of(2026, 6, 1)
        val count = InventoryCount(
            barcode = "1",
            productName = "Item",
            expectedQuantity = 1,
            actualQuantity = 1,
            expiryDate = today,
            status = CountStatus.PENDING,
            updatedAtMillis = 0,
        )

        assertFalse(count.isExpired(today))
    }
}
