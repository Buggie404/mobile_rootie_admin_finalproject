package com.veganbeauty.admin.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.BookingDao
import com.veganbeauty.admin.data.local.entities.BookingEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class BookingRepository(
    private val bookingDao: BookingDao,
    private val firebaseService: FirebaseService,
    private val context: Context? = null
) {

    val allBookings: LiveData<List<BookingEntity>> = bookingDao.getAllLiveData()

    private var bookingsListener: com.google.firebase.firestore.ListenerRegistration? = null
    @Volatile private var isInitial = true

    fun startRealtimeSync(onNewBooking: ((BookingEntity) -> Unit)? = null) {
        if (bookingsListener != null) return
        isInitial = true
        
        val db = try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return

        bookingsListener = db.collection("bookings")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val currentIsInitial = isInitial
                    isInitial = false
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val addedBookings = mutableListOf<BookingEntity>()
                        val modifiedBookings = mutableListOf<BookingEntity>()
                        val removedBookingIds = mutableListOf<String>()

                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val rawStoreName = doc.getString("storeName") ?: ""
                            val rawStoreAddress = doc.getString("storeAddress") ?: ""
                            val storeID = doc.getString("storeID") ?: doc.getString("storeId") ?: when {
                                rawStoreName.contains("Cơ sở 1", ignoreCase = true) || rawStoreAddress.contains("Minh Khai", ignoreCase = true) -> "CH001"
                                rawStoreName.contains("Cơ sở 5", ignoreCase = true) || rawStoreAddress.contains("Hoàng Văn Thụ", ignoreCase = true) -> "CH005"
                                else -> ""
                            }

                            val booking = BookingEntity(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                userPhone = doc.getString("userPhone") ?: "",
                                userEmail = doc.getString("userEmail") ?: "",
                                serviceName = doc.getString("serviceName") ?: "",
                                dateDisplay = doc.getString("dateDisplay") ?: "",
                                monthDisplay = doc.getString("monthDisplay") ?: "",
                                dayOfWeek = doc.getString("dayOfWeek") ?: "",
                                time = doc.getString("time") ?: "",
                                duration = doc.getString("duration") ?: "",
                                storeName = rawStoreName,
                                storeAddress = rawStoreAddress,
                                storePhone = doc.getString("storePhone") ?: "",
                                storeImage = doc.getString("storeImage") ?: "",
                                storeID = storeID,
                                note = doc.getString("note") ?: "",
                                status = doc.getString("status") ?: "",
                                createdAt = doc.getString("createdAt") ?: "",
                                consultantName = doc.getString("consultantName") ?: "",
                                cancelReason = doc.getString("cancelReason") ?: ""
                            )

                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                    addedBookings.add(booking)
                                    if (!currentIsInitial) {
                                        val isPending = booking.status.equals("Chờ xác nhận", ignoreCase = true) || 
                                                        booking.status.equals("pending", ignoreCase = true)
                                        val isTargetBranch = booking.storeID == "CH001" || booking.storeID == "CH005" ||
                                                booking.storeName.contains("Cơ sở 1", ignoreCase = true) || 
                                                booking.storeName.contains("Cơ sở 5", ignoreCase = true)
                                        
                                        if (isPending && isTargetBranch) {
                                            withContext(Dispatchers.Main) {
                                                onNewBooking?.invoke(booking)
                                            }
                                        }
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    modifiedBookings.add(booking)
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    removedBookingIds.add(booking.id)
                                }
                            }
                        }

                        if (addedBookings.isNotEmpty()) {
                            bookingDao.insertAllSync(addedBookings)
                        }
                        if (modifiedBookings.isNotEmpty()) {
                            bookingDao.insertAllSync(modifiedBookings)
                        }
                        for (id in removedBookingIds) {
                            bookingDao.deleteByIdSync(id)
                        }
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        bookingsListener?.remove()
        bookingsListener = null
    }

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
        val localBooking = bookingDao.getByIdSync(bookingId)
        val success = if (localBooking != null) {
            val updatedBooking = localBooking.copy(status = status, cancelReason = cancelReason)
            val uploadSuccess = firebaseService.uploadBooking(updatedBooking)
            if (uploadSuccess) {
                bookingDao.insertSync(updatedBooking)
                true
            } else {
                false
            }
        } else {
            firebaseService.updateBookingStatus(bookingId, status, cancelReason)
        }

        if (!success && localBooking != null) {
            // Cập nhật local dù Firebase offline/thất bại
            val updatedBooking = localBooking.copy(status = status, cancelReason = cancelReason)
            bookingDao.insertSync(updatedBooking)
        }
        true
    }
}
