package com.veganbeauty.admin.data.repository

import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.ProductDao
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val productDao: ProductDao,
    private val firebaseService: FirebaseService
) {

    val allProducts: LiveData<List<ProductEntity>> = productDao.getAllLiveData()

    suspend fun syncFromFirebase() = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllProducts()
        if (remoteList.isNotEmpty()) {
            productDao.insertAllSync(remoteList)
        }
    }

    suspend fun saveProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        productDao.insertSync(product)
        val success = firebaseService.saveProduct(product)
        success
    }

    suspend fun deleteProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        productDao.deleteSync(product)
        val success = firebaseService.deleteProduct(product.id)
        success
    }
}
