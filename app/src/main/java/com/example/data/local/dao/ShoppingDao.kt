package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE conversationId = :conversationId ORDER BY isChecked ASC, createdAt DESC")
    fun getShoppingItemsForConversation(conversationId: Long): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun setItemChecked(id: Long, isChecked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItemById(id: Long)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()
}
