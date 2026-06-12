package com.veganbeauty.admin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.veganbeauty.admin.data.local.entities.ProductEntity

@Dao
interface ProductDao {

    @Query("SELECT * FROM products")
    fun getAllLiveData(): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM products")
    fun getAllSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getByIdSync(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(products: List<ProductEntity>)

    @Update
    fun updateSync(product: ProductEntity)

    @Delete
    fun deleteSync(product: ProductEntity)
}
