package com.veganbeauty.admin.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.BookingDao
import com.veganbeauty.admin.data.local.entities.BookingEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class BookingRepository(
    private val bookingDao: BookingDao,
    private val firebaseService: FirebaseService,
    private val context: Context? = null
) {

    val allBookings: LiveData<List<BookingEntity>> = bookingDao.getAllLiveData()

    suspend fun syncFromFirebase() = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllBookings()
        if (remoteList.isNotEmpty()) {
            bookingDao.insertAllSync(remoteList)
        } else {
            // Firebase trống hoặc offline → seed từ file assets nếu Room cũng chưa có dữ liệu
            val localCount = bookingDao.getAllSync().size
            if (localCount == 0 && context != null) {
                seedFromAssets(context)
            }
        }
    }

    private fun seedFromAssets(context: Context) {
        try {
            val jsonString = context.assets.open("skin_bookings.json")
                .bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<BookingEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rawStoreName = obj.optString("storeName", "")
                val rawStoreAddress = obj.optString("storeAddress", "")
                val storeID = when {
                    rawStoreName.contains("Cơ sở 1", ignoreCase = true) ||
                    rawStoreAddress.contains("Minh Khai", ignoreCase = true) -> "CH001"
                    rawStoreName.contains("Cơ sở 5", ignoreCase = true) ||
                    rawStoreAddress.contains("Hoàng Văn Thụ", ignoreCase = true) -> "CH005"
                    else -> ""
                }
                list.add(
                    BookingEntity(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userPhone = obj.optString("userPhone", ""),
                        userEmail = obj.optString("userEmail", ""),
                        serviceName = obj.optString("serviceName", ""),
                        dateDisplay = obj.optString("dateDisplay", ""),
                        monthDisplay = obj.optString("monthDisplay", ""),
                        dayOfWeek = obj.optString("dayOfWeek", ""),
                        time = obj.optString("time", ""),
                        duration = obj.optString("duration", ""),
                        storeName = rawStoreName,
                        storeAddress = rawStoreAddress,
                        storePhone = obj.optString("storePhone", ""),
                        storeImage = obj.optString("storeImage", ""),
                        storeID = storeID,
                        note = obj.optString("note", ""),
                        status = obj.optString("status", ""),
                        createdAt = obj.optString("createdAt", ""),
                        consultantName = obj.optString("consultantName", ""),
                        cancelReason = obj.optString("cancelReason", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                bookingDao.insertAllSync(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateBookingStatus(
        bookingId: String,
        status: String,
        cancelReason: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val success = firebaseService.updateBookingStatus(bookingId, status, cancelReason)
        if (success) {
            val localBooking = bookingDao.getByIdSync(bookingId)
            if (localBooking != null) {
                val updatedBooking = localBooking.copy(status = status, cancelReason = cancelReason)
                bookingDao.insertSync(updatedBooking)
            }
        } else {
            // Cập nhật local dù Firebase offline
            val localBooking = bookingDao.getByIdSync(bookingId)
            if (localBooking != null) {
                val updatedBooking = localBooking.copy(status = status, cancelReason = cancelReason)
                bookingDao.insertSync(updatedBooking)
            }
        }
        true
    }
}
