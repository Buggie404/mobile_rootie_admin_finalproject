package com.veganbeauty.admin.data.repository

import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.OrderDao
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderRepository(
    private val orderDao: OrderDao,
    private val firebaseService: FirebaseService
) {

    val allOrders: LiveData<List<OrderEntity>> = orderDao.getAllLiveData()

    suspend fun syncFromFirebase() = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllOrders()
        if (remoteList.isNotEmpty()) {
            orderDao.insertAllSync(remoteList)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        val success = firebaseService.updateOrderStatus(orderId, status)
        if (success) {
            val localOrder = orderDao.getByIdSync(orderId)
            if (localOrder != null) {
                val updatedOrder = localOrder.copy(status = status)
                orderDao.insertSync(updatedOrder)
            }
        }
        success
    }
}
