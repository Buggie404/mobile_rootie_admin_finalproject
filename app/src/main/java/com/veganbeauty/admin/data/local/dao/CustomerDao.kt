package com.veganbeauty.admin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.veganbeauty.admin.data.local.entities.CustomerEntity

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers")
    fun getAllLiveData(): LiveData<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    fun getAllSync(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getByIdSync(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(customers: List<CustomerEntity>)

    @Update
    fun updateSync(customer: CustomerEntity)

    @Delete
    fun deleteSync(customer: CustomerEntity)
}
