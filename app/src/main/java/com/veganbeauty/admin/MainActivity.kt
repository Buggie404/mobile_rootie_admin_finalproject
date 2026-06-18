package com.veganbeauty.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.veganbeauty.admin.core.base.RootieAdminActivity
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.ActivityMainBinding
import com.veganbeauty.admin.features.auth.LoginActivity
import com.veganbeauty.admin.features.home.BottomNavHelper
import com.veganbeauty.admin.features.home.HomeFragment

class MainActivity : RootieAdminActivity() {

    private lateinit var binding: ActivityMainBinding
    private var chatListenerRegistration: ListenerRegistration? = null
    var lastUnreadCount: Int = 0

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
        
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        startChatUnreadListener()
        requestNotificationPermission()
        logFCMToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun logFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("FCM", "==================================================")
                Log.d("FCM", "Current FCM Token: $token")
                Log.d("FCM", "==================================================")
                saveTokenToFirestore(token)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTokenToFirestore(token: String) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "token" to token,
                "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.collection("admin_metadata").document("fcm")
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FCM", "FCM Token saved to Firestore successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "FCM Token failed to save to Firestore", e)
                }
        } catch (e: Exception) {
            e.printStackTrace()
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
                if (tabId != currentTabId) {
                    currentTabId = tabId
                    BottomNavHelper.navigate(this, tabId)
                }
            }
            BottomNavHelper.highlightTab(bottomNav, currentTabId)
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.mainContainer.id, fragment)
            .commit()
    }

    private fun startChatUnreadListener() {
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) return

        chatListenerRegistration?.remove()

        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            chatListenerRegistration = firestore.collection("community_message")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        e.printStackTrace()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        var unreadConvCount = 0
                        for (doc in snapshot.documents) {
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val members = doc.get("members") as? List<String> ?: emptyList()
                                if (!members.contains("rootie_vn")) continue

                                @Suppress("UNCHECKED_CAST")
                                val messagesRaw = doc.get("messages") as? List<Map<String, Any>> ?: emptyList()
                                val hasUnread = messagesRaw.any { msgMap ->
                                    val senderId = msgMap["sender_id"]?.toString() ?: ""
                                    senderId != "rootie_vn" && msgMap["seen_at"] == null
                                }
                                if (hasUnread) {
                                    unreadConvCount++
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }

                        lastUnreadCount = unreadConvCount
                        updateGlobalMessageBadges(unreadConvCount)
                    }
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun updateGlobalMessageBadges(count: Int) {
        val homeBadge = findViewById<TextView>(R.id.home_header_message_badge)
        val otherBadge = findViewById<TextView>(R.id.message_badge)

        updateBadgeTextView(homeBadge, count)
        updateBadgeTextView(otherBadge, count)
    }

    private fun updateBadgeTextView(badgeView: TextView?, count: Int) {
        if (badgeView == null) return
        if (count > 0) {
            badgeView.text = count.toString()
            badgeView.visibility = View.VISIBLE
        } else {
            badgeView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListenerRegistration?.remove()
    }
}
