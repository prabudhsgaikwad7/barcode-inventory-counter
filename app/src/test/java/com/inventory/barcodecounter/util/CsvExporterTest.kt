package com.inventory.barcodecounter.util

import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {

    @Test
    fun toCsv_includesHeaderAndEscapedValues() {
        val counts = listOf(
            InventoryCount(
                id = 1,
                barcode = "111",
                productName = "Bread, sliced",
                expectedQuantity = 5,
                actualQuantity = 4,
                expiryDate = LocalDate.of(2026, 1, 15),
                status = CountStatus.PENDING,
                updatedAtMillis = 100,
            ),
        )

        val csv = CsvExporter.toCsv(counts)

        assertTrue(csv.startsWith("barcode,product_name,expected_qty,actual_qty,difference,expiry_date,status,expired"))
        assertTrue(csv.contains("\"Bread, sliced\""))
        assertTrue(csv.contains("2026-01-15"))
    }
}
