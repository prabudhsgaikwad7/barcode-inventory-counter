package com.inventory.barcodecounter.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inventory.barcodecounter.ui.scan.BarcodeScannerScreen

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
}

@Composable
fun InventoryNavHost(viewModel: InventoryViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            InventoryHomeScreen(
                viewModel = viewModel,
                onOpenScanner = { navController.navigate(Routes.SCAN) },
            )
        }
        composable(Routes.SCAN) {
            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    viewModel.setBarcodeFromScan(barcode)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
