package com.inventory.barcodecounter

import android.app.Application
import com.inventory.barcodecounter.data.local.InventoryDatabase
import com.inventory.barcodecounter.data.remote.NetworkModule
import com.inventory.barcodecounter.data.repository.InventoryRepository

class InventoryApplication : Application() {
    lateinit var repository: InventoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = InventoryDatabase.create(this)
        repository = InventoryRepository(
            dao = db.inventoryCountDao(),
            api = NetworkModule.createApiService(),
        )
    }
}
