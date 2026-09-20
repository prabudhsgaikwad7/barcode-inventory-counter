package com.inventory.barcodecounter

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventory.barcodecounter.ui.InventoryNavHost
import com.inventory.barcodecounter.ui.InventoryViewModel
import com.inventory.barcodecounter.ui.InventoryViewModelFactory
import com.inventory.barcodecounter.ui.theme.BarcodeInventoryTheme

class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* scanner handles denial */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as InventoryApplication
        val factory = InventoryViewModelFactory(app.repository)

        setContent {
            BarcodeInventoryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: InventoryViewModel = viewModel(factory = factory)
                    InventoryNavHost(viewModel = viewModel)
                }
            }
        }

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
