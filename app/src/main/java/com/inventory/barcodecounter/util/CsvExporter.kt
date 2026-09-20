package com.inventory.barcodecounter.util

import com.inventory.barcodecounter.domain.model.InventoryCount
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toCsv(counts: List<InventoryCount>): String {
        val header = "barcode,product_name,expected_qty,actual_qty,difference,expiry_date,status,expired"
        val rows = counts.map { count ->
            listOf(
                escape(count.barcode),
                escape(count.productName),
                count.expectedQuantity.toString(),
                count.actualQuantity.toString(),
                count.quantityDifference.toString(),
                count.expiryDate.format(dateFormatter),
                count.status.name,
                count.isExpired().toString(),
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun escape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
