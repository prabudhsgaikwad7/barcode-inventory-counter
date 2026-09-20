package com.inventory.barcodecounter.ui

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.inventory.barcodecounter.ui.components.CountListItem
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryHomeScreen(
    viewModel: InventoryViewModel,
    onOpenScanner: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.csvContent) {
        val csv = viewModel.consumeCsvExport() ?: return@LaunchedEffect
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "inventory_counts.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export inventory CSV"))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.onExpiryDateChange(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Barcode Inventory Counter") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.form.barcode,
                onValueChange = viewModel::onBarcodeChange,
                label = { Text("Barcode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.lookupProduct() }) {
                    Text("Fetch product")
                }
                OutlinedButton(onClick = onOpenScanner) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Text("Scan")
                }
            }

            if (state.form.isLoadingProduct) {
                CircularProgressIndicator()
            }

            if (state.form.productLoaded) {
                OutlinedTextField(
                    value = state.form.productName,
                    onValueChange = viewModel::onProductNameChange,
                    label = { Text("Product name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.form.expectedQuantity,
                    onValueChange = viewModel::onExpectedQuantityChange,
                    label = { Text("Expected quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.form.actualQuantity,
                    onValueChange = viewModel::onActualQuantityChange,
                    label = { Text("Actual quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.form.expiryDate,
                    onValueChange = viewModel::onExpiryDateChange,
                    label = { Text("Expiry date (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select expiry date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Expired products are blocked on save") },
                )

                // Quick Date Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val today = LocalDate.now()
                    AssistChip(
                        onClick = { viewModel.onExpiryDateChange(today.plusDays(30).toString()) },
                        label = { Text("+30 Days") },
                    )
                    AssistChip(
                        onClick = { viewModel.onExpiryDateChange(today.plusDays(90).toString()) },
                        label = { Text("+90 Days") },
                    )
                    AssistChip(
                        onClick = { viewModel.onExpiryDateChange(today.plusYears(1).toString()) },
                        label = { Text("+1 Year") },
                    )
                    AssistChip(
                        onClick = { viewModel.onExpiryDateChange(today.minusDays(1).toString()) },
                        label = { Text("Expired (-1d)") },
                    )
                }

                Button(onClick = viewModel::saveCount, modifier = Modifier.fillMaxWidth()) {
                    Text("Save count locally")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = viewModel::submitAllPending,
                    enabled = !state.isSubmitting && state.pendingCounts.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator()
                    } else {
                        Text("Submit pending (${state.pendingCounts.size})")
                    }
                }
                OutlinedButton(onClick = viewModel::exportCsv) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.selectedTab == RecordsTab.PENDING,
                    onClick = { viewModel.selectTab(RecordsTab.PENDING) },
                    label = { Text("Pending (${state.pendingCounts.size})") },
                )
                FilterChip(
                    selected = state.selectedTab == RecordsTab.SUBMITTED,
                    onClick = { viewModel.selectTab(RecordsTab.SUBMITTED) },
                    label = { Text("Submitted (${state.submittedCounts.size})") },
                )
                FilterChip(
                    selected = state.selectedTab == RecordsTab.ALL,
                    onClick = { viewModel.selectTab(RecordsTab.ALL) },
                    label = { Text("All") },
                )
            }

            Text(
                text = "Catalog samples: 1234567890123, 9876543210987, 5556667778889",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val visible = viewModel.visibleCounts(state)
            if (visible.isEmpty()) {
                Text("No records in this tab", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible, key = { it.id }) { count ->
                        CountListItem(count)
                    }
                }
            }
        }
    }
}
