package com.veganbeauty.admin.features.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.data.local.entities.BookingEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.data.repository.BookingRepository
import kotlinx.coroutines.launch

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookingRepository
    private val sessionManager: SessionManager

    val allBookings: LiveData<List<BookingEntity>>
    val activeTabStatus = MutableLiveData<String>("PENDING") // PENDING, UPCOMING, COMPLETED, CANCELLED

    private val _filteredBookings = MediatorLiveData<List<BookingEntity>>()
    val filteredBookings: LiveData<List<BookingEntity>> get() = _filteredBookings

    init {
        val database = RootieAdminDatabase.getDatabase(application)
        repository = BookingRepository(database.bookingDao(), FirebaseService(), application)
        sessionManager = SessionManager(application)
        allBookings = repository.allBookings

        _filteredBookings.addSource(allBookings) { updateFilteredBookings() }
        _filteredBookings.addSource(activeTabStatus) { updateFilteredBookings() }
    }

    fun syncFromFirebase() {
        viewModelScope.launch {
            try {
                repository.syncFromFirebase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateBookingStatus(bookingId: String, status: String, cancelReason: String = "") {
        viewModelScope.launch {
            try {
                repository.updateBookingStatus(bookingId, status, cancelReason)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFilteredBookings() {
        val bookings = allBookings.value ?: emptyList()
        val statusFilter = activeTabStatus.value ?: "PENDING"
        val userRole = sessionManager.getRole() ?: "admin"
        val userStoreId = sessionManager.getStoreID() ?: ""
        val isStaff = userRole.equals("staff", ignoreCase = true) || userRole.equals("nhân viên", ignoreCase = true)

        // 1. Branch/Store filtering based on role and storeID
        var result = if (isStaff && userStoreId.isNotEmpty()) {
            bookings.filter {
                it.storeID == userStoreId ||
                (it.storeID.isEmpty() && (
                    (userStoreId == "CH001" && it.storeName.contains("Cơ sở 1", ignoreCase = true)) ||
                    (userStoreId == "CH005" && it.storeName.contains("Cơ sở 5", ignoreCase = true))
                ))
            }
        } else {
            bookings
        }

        // 2. Status filtering based on tab
        result = when (statusFilter) {
            "PENDING" -> result.filter { 
                it.status.equals("Chờ xác nhận", ignoreCase = true) || 
                it.status.equals("pending", ignoreCase = true) 
            }
            "UPCOMING" -> result.filter { 
                it.status.equals("Sắp diễn ra", ignoreCase = true) || 
                it.status.equals("confirmed", ignoreCase = true) || 
                it.status.equals("upcoming", ignoreCase = true) 
            }
            "COMPLETED" -> result.filter { 
                it.status.equals("Đã hoàn thành", ignoreCase = true) || 
                it.status.equals("completed", ignoreCase = true) 
            }
            "CANCELLED" -> result.filter { 
                it.status.equals("Đã huỷ", ignoreCase = true) || 
                it.status.equals("Đã hủy", ignoreCase = true) || 
                it.status.equals("cancelled", ignoreCase = true) 
            }
            else -> result
        }

        _filteredBookings.value = result
    }
}
