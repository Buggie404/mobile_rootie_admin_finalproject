package com.veganbeauty.admin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.veganbeauty.admin.data.local.entities.OrderEntity

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders")
    fun getAllLiveData(): LiveData<List<OrderEntity>>

    @Query("SELECT * FROM orders")
    fun getAllSync(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    fun getByIdSync(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE userId = :userId")
    fun getOrdersForUserSync(userId: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(orders: List<OrderEntity>)

    @Update
    fun updateSync(order: OrderEntity)

    @Delete
    fun deleteSync(order: OrderEntity)
}
