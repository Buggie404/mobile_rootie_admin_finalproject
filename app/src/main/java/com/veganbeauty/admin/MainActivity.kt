package com.veganbeauty.admin

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.veganbeauty.admin.core.base.RootieAdminActivity
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.ActivityMainBinding
import com.veganbeauty.admin.features.auth.LoginActivity
import com.veganbeauty.admin.features.home.BottomNavHelper
import com.veganbeauty.admin.features.home.HomeFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import com.veganbeauty.admin.features.booking.BookingViewModel
import com.veganbeauty.admin.data.local.entities.BookingEntity
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import androidx.appcompat.app.AlertDialog
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build

class MainActivity : RootieAdminActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bookingViewModel: BookingViewModel
    
    val pendingBookingsCount = MutableLiveData<Int>(0)
    val pendingBookingCount = pendingBookingsCount // Alias for compatibility
    val pendingOrdersCount = MutableLiveData<Int>(0)
    val unreadChatsCount = MutableLiveData<Int>(0)
    val totalNotificationCount = MediatorLiveData<Int>().apply {
        addSource(pendingBookingsCount) { value = (pendingBookingsCount.value ?: 0) + (pendingOrdersCount.value ?: 0) + (unreadChatsCount.value ?: 0) }
        addSource(pendingOrdersCount) { value = (pendingBookingsCount.value ?: 0) + (pendingOrdersCount.value ?: 0) + (unreadChatsCount.value ?: 0) }
        addSource(unreadChatsCount) { value = (pendingBookingsCount.value ?: 0) + (pendingOrdersCount.value ?: 0) + (unreadChatsCount.value ?: 0) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            super.onCreate(savedInstanceState)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        
        bookingViewModel = ViewModelProvider(this)[BookingViewModel::class.java]
        
        // Start real-time Firestore sync
        bookingViewModel.startRealtimeSync { newBooking ->
            showNewBookingDialog(newBooking)
        }
        
        // Observe bookings database changes to update badge count
        bookingViewModel.allBookings.observe(this) { bookings ->
            updatePendingBookingCount(bookings)
        }
        
        val db = RootieAdminDatabase.getDatabase(this)
        
        // Observe orders database changes to update pending orders count
        db.orderDao().getAllLiveData().observe(this) { orders ->
            val count = orders.filter { it.status == "Chờ xử lý" }.size
            pendingOrdersCount.postValue(count)
        }
        
        // Observe chat messages to update unread chats count
        db.chatMessageDao().getAllMessages().observe(this) { messages ->
            val count = messages.filter { !it.isRead && it.sender == "customer" }.map { it.threadId }.distinct().size
            unreadChatsCount.postValue(count)
        }

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }
    var currentTabId: Int = R.id.nav_home

    override fun setupUI() {
        val sessionManager = SessionManager(this)
        val role = sessionManager.getRole() ?: "admin"

        val bottomNav = findViewById<View>(R.id.bottom_nav)
        if (bottomNav != null) {
            if (role == "staff" || role == "nhân viên") {
                bottomNav.findViewById<View>(R.id.nav_product)?.visibility = View.GONE
                bottomNav.findViewById<View>(R.id.nav_customer)?.visibility = View.GONE
            } else {
                bottomNav.findViewById<View>(R.id.nav_product)?.visibility = View.VISIBLE
                bottomNav.findViewById<View>(R.id.nav_customer)?.visibility = View.VISIBLE
            }

            BottomNavHelper.setup(
                activity = this,
                root = bottomNav
            ) { tabId ->
                // Luôn cho phép navigate về Home, dù đang ở Home hay không
                BottomNavHelper.navigate(this, tabId)
            }
            BottomNavHelper.highlightTab(bottomNav, currentTabId)
        }
    }

    /**
     * Được gọi bởi các fragment con khi tự điều hướng (không qua bottom nav)
     * để đồng bộ trạng thái highlight của bottom nav.
     */
    fun syncTab(tabId: Int) {
        currentTabId = tabId
        val bottomNav = findViewById<View>(R.id.bottom_nav) ?: return
        BottomNavHelper.highlightTab(bottomNav, tabId)
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.mainContainer.id, fragment)
            .commit()
    }

    private fun updatePendingBookingCount(bookings: List<BookingEntity>) {
        val sessionManager = SessionManager(this)
        val role = sessionManager.getRole() ?: "admin"
        val storeID = sessionManager.getStoreID() ?: ""
        val isStaff = role.equals("staff", ignoreCase = true) || role.equals("nhân viên", ignoreCase = true)
        
        val count = bookings.filter { booking ->
            val isPending = booking.status.equals("Chờ xác nhận", ignoreCase = true) || 
                            booking.status.equals("pending", ignoreCase = true)
            
            val isTargetBranch = if (isStaff && storeID.isNotEmpty()) {
                booking.storeID == storeID || (booking.storeID.isEmpty() && (
                    (storeID == "CH001" && booking.storeName.contains("Cơ sở 1", ignoreCase = true)) ||
                    (storeID == "CH005" && booking.storeName.contains("Cơ sở 5", ignoreCase = true))
                ))
            } else {
                // Admin matches both CH001 and CH005
                booking.storeID == "CH001" || booking.storeID == "CH005" ||
                booking.storeName.contains("Cơ sở 1", ignoreCase = true) || 
                booking.storeName.contains("Cơ sở 5", ignoreCase = true)
            }
            isPending && isTargetBranch
        }.size
        
        pendingBookingCount.postValue(count)
    }

    private fun showNewBookingDialog(booking: BookingEntity) {
        // Play notification sound
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate device
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        if (::bookingViewModel.isInitialized) {
            bookingViewModel.stopRealtimeSync()
        }
        super.onDestroy()
    }
}
