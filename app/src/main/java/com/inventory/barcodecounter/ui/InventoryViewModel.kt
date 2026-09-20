package com.inventory.barcodecounter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inventory.barcodecounter.data.repository.InventoryRepository
import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import com.inventory.barcodecounter.domain.model.ProductInfo
import com.inventory.barcodecounter.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class RecordsTab {
    PENDING,
    SUBMITTED,
    ALL,
}

data class EntryFormState(
    val barcode: String = "",
    val productName: String = "",
    val expectedQuantity: String = "",
    val actualQuantity: String = "",
    val expiryDate: String = "",
    val isLoadingProduct: Boolean = false,
    val productLoaded: Boolean = false,
)

data class InventoryUiState(
    val form: EntryFormState = EntryFormState(),
    val selectedTab: RecordsTab = RecordsTab.PENDING,
    val pendingCounts: List<InventoryCount> = emptyList(),
    val submittedCounts: List<InventoryCount> = emptyList(),
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val csvContent: String? = null,
)

class InventoryViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(EntryFormState())
    private val selectedTab = MutableStateFlow(RecordsTab.PENDING)
    private val message = MutableStateFlow<String?>(null)
    private val isSubmitting = MutableStateFlow(false)
    private val csvContent = MutableStateFlow<String?>(null)

    private val pendingFlow = repository.observeCounts(CountStatus.PENDING)
    private val submittedFlow = repository.observeCounts(CountStatus.SUBMITTED)

    val uiState: StateFlow<InventoryUiState> = combine(
        combine(formState, selectedTab, pendingFlow, submittedFlow) { form, tab, pending, submitted ->
            Quad(form, tab, pending, submitted)
        },
        combine(message, isSubmitting, csvContent) { msg, submitting, csv ->
            Triple(msg, submitting, csv)
        },
    ) { primary, secondary ->
        InventoryUiState(
            form = primary.first,
            selectedTab = primary.second,
            pendingCounts = primary.third,
            submittedCounts = primary.fourth,
            message = secondary.first,
            isSubmitting = secondary.second,
            csvContent = secondary.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InventoryUiState(),
    )

    fun onBarcodeChange(value: String) {
        formState.value = formState.value.copy(barcode = value, productLoaded = false)
    }

    fun onProductNameChange(value: String) {
        formState.value = formState.value.copy(productName = value)
    }

    fun onExpectedQuantityChange(value: String) {
        formState.value = formState.value.copy(expectedQuantity = value.filter { it.isDigit() })
    }

    fun onActualQuantityChange(value: String) {
        formState.value = formState.value.copy(actualQuantity = value.filter { it.isDigit() })
    }

    fun onExpiryDateChange(value: String) {
        formState.value = formState.value.copy(expiryDate = value)
    }

    fun setBarcodeFromScan(barcode: String) {
        formState.value = formState.value.copy(barcode = barcode, productLoaded = false)
        lookupProduct(barcode)
    }

    fun lookupProduct(barcode: String = formState.value.barcode) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) {
            message.value = "Enter a barcode first"
            return
        }
        viewModelScope.launch {
            formState.value = formState.value.copy(isLoadingProduct = true)
            repository.lookupProduct(trimmed)
                .onSuccess { product -> applyProduct(product) }
                .onFailure {
                    message.value = "Product not found in catalog. Enter details manually."
                    formState.value = formState.value.copy(
                        isLoadingProduct = false,
                        productLoaded = true,
                        productName = formState.value.productName.ifBlank { "Unlisted Item" },
                        expectedQuantity = formState.value.expectedQuantity.ifBlank { "0" },
                    )
                }
        }
    }

    private fun applyProduct(product: ProductInfo) {
        formState.value = formState.value.copy(
            barcode = product.barcode,
            productName = product.name,
            expectedQuantity = product.expectedQuantity.toString(),
            isLoadingProduct = false,
            productLoaded = true,
        )
        message.value = "Loaded ${product.name}"
    }

    fun saveCount() {
        val form = formState.value
        val barcode = form.barcode.trim()
        val actual = form.actualQuantity.toIntOrNull()
        val expected = form.expectedQuantity.toIntOrNull()
        val expiry = runCatching { LocalDate.parse(form.expiryDate) }.getOrNull()

        when {
            barcode.isEmpty() -> message.value = "Barcode is required"
            form.productName.isBlank() -> message.value = "Fetch product details first"
            actual == null -> message.value = "Enter a valid actual quantity"
            expected == null -> message.value = "Expected quantity missing"
            expiry == null -> message.value = "Expiry date must be YYYY-MM-DD"
            expiry.isBefore(LocalDate.now()) -> message.value = "Product is expired — adjust expiry or discard"
            else -> viewModelScope.launch {
                when (
                    val result = repository.saveCount(
                        barcode = barcode,
                        productName = form.productName,
                        expectedQuantity = expected,
                        actualQuantity = actual,
                        expiryDate = expiry,
                    )
                ) {
                    is InventoryRepository.SaveResult.Created ->
                        message.value = "Count saved for $barcode"

                    is InventoryRepository.SaveResult.Merged ->
                        message.value = "Merged with existing pending count for $barcode"

                    is InventoryRepository.SaveResult.BlockedSubmitted ->
                        message.value = "Barcode already submitted — start a new cycle or use another SKU"
                }
                resetForm()
            }
        }
    }

    private fun resetForm() {
        formState.value = EntryFormState()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun selectTab(tab: RecordsTab) {
        selectedTab.value = tab
    }

    fun clearMessage() {
        message.value = null
    }

    fun submitAllPending() {
        viewModelScope.launch {
            isSubmitting.value = true
            repository.submitPendingCounts()
                .onSuccess { count ->
                    message.value = if (count == 0) "No pending counts to submit" else "Submitted $count count(s)"
                }
                .onFailure { error ->
                    message.value = error.message ?: "Submit failed"
                }
            isSubmitting.value = false
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val all = repository.getAllCountsOnce()
            if (all.isEmpty()) {
                message.value = "No records to export"
                return@launch
            }
            csvContent.value = CsvExporter.toCsv(all)
            message.value = "CSV ready to share"
        }
    }

    fun consumeCsvExport(): String? {
        val content = csvContent.value
        csvContent.value = null
        return content
    }

    fun visibleCounts(state: InventoryUiState): List<InventoryCount> = when (state.selectedTab) {
        RecordsTab.PENDING -> state.pendingCounts
        RecordsTab.SUBMITTED -> state.submittedCounts
        RecordsTab.ALL -> (state.pendingCounts + state.submittedCounts)
            .sortedByDescending { it.updatedAtMillis }
    }
}
