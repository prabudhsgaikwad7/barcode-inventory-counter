package com.inventory.barcodecounter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inventory.barcodecounter.data.local.entity.InventoryCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryCountDao {
    @Query("SELECT * FROM inventory_counts ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<InventoryCountEntity>>

    @Query("SELECT * FROM inventory_counts WHERE status = :status ORDER BY updatedAtMillis DESC")
    fun observeByStatus(status: String): Flow<List<InventoryCountEntity>>

    @Query("SELECT * FROM inventory_counts WHERE barcode = :barcode AND status = 'PENDING' LIMIT 1")
    suspend fun findPendingByBarcode(barcode: String): InventoryCountEntity?

    @Query("SELECT * FROM inventory_counts WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): InventoryCountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: InventoryCountEntity): Long

    @Update
    suspend fun update(entity: InventoryCountEntity)

    @Query("UPDATE inventory_counts SET status = 'SUBMITTED', updatedAtMillis = :updatedAt WHERE status = 'PENDING'")
    suspend fun markAllPendingAsSubmitted(updatedAt: Long)

    @Query("SELECT * FROM inventory_counts")
    suspend fun getAllOnce(): List<InventoryCountEntity>
}
