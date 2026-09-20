package com.inventory.barcodecounter.domain

import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InventoryMergePolicyTest {

    @Test
    fun merge_sumsActualQuantityAndKeepsEarliestExpiry() {
        val existing = InventoryCount(
            id = 1,
            barcode = "123",
            productName = "Milk",
            expectedQuantity = 10,
            actualQuantity = 4,
            expiryDate = LocalDate.of(2026, 12, 1),
            status = CountStatus.PENDING,
            updatedAtMillis = 1,
        )
        val incoming = InventoryMergePolicy.buildNewCount(
            barcode = "123",
            productName = "Organic Milk",
            expectedQuantity = 10,
            actualQuantity = 3,
            expiryDate = LocalDate.of(2026, 10, 1),
            nowMillis = 2,
        )

        val merged = InventoryMergePolicy.merge(existing, incoming)

        assertEquals(7, merged.actualQuantity)
        assertEquals("Organic Milk", merged.productName)
        assertEquals(LocalDate.of(2026, 10, 1), merged.expiryDate)
        assertEquals(2, merged.updatedAtMillis)
    }

    @Test
    fun merge_fallbackToExistingNameIfIncomingIsBlank() {
        val existing = InventoryCount(
            id = 1,
            barcode = "123",
            productName = "Milk 1L",
            expectedQuantity = 10,
            actualQuantity = 4,
            expiryDate = LocalDate.of(2026, 12, 1),
            status = CountStatus.PENDING,
            updatedAtMillis = 1,
        )
        val incoming = InventoryMergePolicy.buildNewCount(
            barcode = "123",
            productName = "  ",
            expectedQuantity = 10,
            actualQuantity = 2,
            expiryDate = LocalDate.of(2026, 12, 1),
            nowMillis = 2,
        )

        val merged = InventoryMergePolicy.merge(existing, incoming)

        assertEquals("Milk 1L", merged.productName)
        assertEquals(6, merged.actualQuantity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun merge_throwsIfBarcodesDoNotMatch() {
        val existing = InventoryCount(
            id = 1,
            barcode = "123",
            productName = "Milk",
            expectedQuantity = 10,
            actualQuantity = 4,
            expiryDate = LocalDate.of(2026, 12, 1),
            status = CountStatus.PENDING,
            updatedAtMillis = 1,
        )
        val incoming = InventoryMergePolicy.buildNewCount(
            barcode = "999",
            productName = "Bread",
            expectedQuantity = 5,
            actualQuantity = 1,
            expiryDate = LocalDate.of(2026, 12, 1),
            nowMillis = 2,
        )

        InventoryMergePolicy.merge(existing, incoming)
    }

    @Test(expected = IllegalArgumentException::class)
    fun merge_throwsIfExistingCountIsSubmitted() {
        val existing = InventoryCount(
            id = 1,
            barcode = "123",
            productName = "Milk",
            expectedQuantity = 10,
            actualQuantity = 4,
            expiryDate = LocalDate.of(2026, 12, 1),
            status = CountStatus.SUBMITTED,
            updatedAtMillis = 1,
        )
        val incoming = InventoryMergePolicy.buildNewCount(
            barcode = "123",
            productName = "Milk",
            expectedQuantity = 10,
            actualQuantity = 1,
            expiryDate = LocalDate.of(2026, 12, 1),
            nowMillis = 2,
        )

        InventoryMergePolicy.merge(existing, incoming)
    }

    @Test
    fun buildNewCount_trimsBarcodeAndSetsPendingStatus() {
        val count = InventoryMergePolicy.buildNewCount(
            barcode = "  9876543210  ",
            productName = "Bread",
            expectedQuantity = 20,
            actualQuantity = 15,
            expiryDate = LocalDate.of(2026, 8, 15),
            nowMillis = 1000,
        )

        assertEquals("9876543210", count.barcode)
        assertEquals(CountStatus.PENDING, count.status)
        assertEquals(1000, count.updatedAtMillis)
        assertEquals(-5, count.quantityDifference)
    }
}
