package com.veganbeauty.admin.features.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.data.repository.OrderRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository


    val allOrders: LiveData<List<OrderEntity>>
    val activeTabStatus = MutableLiveData<String>("ALL") // ALL, PENDING, PROCESSING, SHIPPING, COMPLETED, CANCELLED

    private val _filteredOrders = MediatorLiveData<List<OrderEntity>>()
    val filteredOrders: LiveData<List<OrderEntity>> get() = _filteredOrders

    init {
        val database = RootieAdminDatabase.getDatabase(application)
        repository = OrderRepository(database.orderDao(), FirebaseService())

        allOrders = repository.allOrders

        _filteredOrders.addSource(allOrders) { updateFilteredOrders() }
        _filteredOrders.addSource(activeTabStatus) { updateFilteredOrders() }
    }

    fun syncFromFirebase() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.syncFromFirebase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateOrderStatus(orderId, status)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFilteredOrders() {
        // Order là đơn hàng ONLINE - tất cả nhân viên đều thấy toàn bộ đơn hàng
        // Không lọc theo chi nhánh
        val orders = allOrders.value ?: emptyList()
        val statusFilter = activeTabStatus.value ?: "ALL"

        val result = when (statusFilter) {
            "PENDING"    -> orders.filter { it.status == "Chờ xử lý" }
            "PROCESSING" -> orders.filter { it.status == "Đang xử lý" }
            "SHIPPING"   -> orders.filter { it.status == "Đang giao" }
            "COMPLETED"  -> orders.filter { it.status == "Hoàn tất" }
            "CANCELLED"  -> orders.filter { it.status == "Đã hủy" }
            else         -> orders
        }

        _filteredOrders.value = result
    }
}
