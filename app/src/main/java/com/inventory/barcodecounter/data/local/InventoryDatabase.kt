package com.inventory.barcodecounter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.inventory.barcodecounter.data.local.dao.InventoryCountDao
import com.inventory.barcodecounter.data.local.entity.InventoryCountEntity

@Database(
    entities = [InventoryCountEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryCountDao(): InventoryCountDao

    companion object {
        fun create(context: Context): InventoryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "inventory_counts.db",
            ).build()
    }
}
