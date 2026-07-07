package com.veganbeauty.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.veganbeauty.admin.core.base.RootieAdminActivity;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.data.remote.ChatMessage;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.databinding.ActivityMainBinding;
import com.veganbeauty.admin.features.auth.LoginActivity;
import com.veganbeauty.admin.features.home.BottomNavHelper;
import com.veganbeauty.admin.features.home.HomeFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends RootieAdminActivity {

    public interface ChatMessagesListener {
        void onChatMessagesUpdated(List<ChatMessage> messages);
    }

    private ActivityMainBinding binding;
    private ListenerRegistration chatListenerRegistration = null;
    private final FirebaseService firebaseService = new FirebaseService();
    private final List<ChatMessagesListener> chatMessagesListeners = new ArrayList<>();
    private List<ChatMessage> cachedChatMessages = new ArrayList<>();
    private int lastUnreadCount = 0;
    private int currentTabId = R.id.nav_home;

    public int getLastUnreadCount() {
        return lastUnreadCount;
    }

    public int getCurrentTabId() {
        return currentTabId;
    }

    public void setCurrentTabId(int currentTabId) {
        this.currentTabId = currentTabId;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            super.onCreate(savedInstanceState);
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                ensureBottomNavVisible();
            }
        });

        startChatUnreadListener();
        requestNotificationPermission();
        logFCMToken();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101
                );
            }
        }
    }

    private void logFCMToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                Log.d("FCM", "==================================================");
                Log.d("FCM", "Current FCM Token: " + token);
                Log.d("FCM", "==================================================");
                saveTokenToFirestore(token);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTokenToFirestore(String token) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("updated_at", FieldValue.serverTimestamp());

            db.collection("admin_metadata").document("fcm")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("FCM", "FCM Token saved to Firestore successfully"))
                .addOnFailureListener(e -> Log.e("FCM", "FCM Token failed to save to Firestore", e));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setupUI() {
        SessionManager sessionManager = new SessionManager(this);
        String role = sessionManager.getRole();
        if (role == null) {
            role = "admin";
        }

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            View navProduct = bottomNav.findViewById(R.id.nav_product);
            View navCustomer = bottomNav.findViewById(R.id.nav_customer);
            if ("staff".equals(role) || "nhân viên".equals(role)) {
                if (navProduct != null) navProduct.setVisibility(View.GONE);
                if (navCustomer != null) navCustomer.setVisibility(View.GONE);
            } else {
                if (navProduct != null) navProduct.setVisibility(View.VISIBLE);
                if (navCustomer != null) navCustomer.setVisibility(View.VISIBLE);
            }

            BottomNavHelper.setup(
                this,
                bottomNav,
                currentTabId,
                tabId -> {
                    currentTabId = tabId;
                    BottomNavHelper.navigate(this, tabId);
                }
            );
            BottomNavHelper.highlightTab(bottomNav, currentTabId);
        }
    }

    public void loadFragment(Fragment fragment) {
        ensureBottomNavVisible();
        getSupportFragmentManager().beginTransaction()
            .replace(binding.mainContainer.getId(), fragment)
            .commit();
        bringBottomNavToFront();
    }

    public void loadFragmentHidingNav(Fragment fragment) {
        hideBottomNav();
        getSupportFragmentManager().beginTransaction()
            .replace(binding.mainContainer.getId(), fragment)
            .addToBackStack(null)
            .commit();
    }

    public void ensureBottomNavVisible() {
        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bringBottomNavToFront();
        }
    }

    public void hideBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
    }

    private void bringBottomNavToFront() {
        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.bringToFront();
            bottomNav.requestLayout();
        }
    }

    public void addChatMessagesListener(ChatMessagesListener listener) {
        if (listener == null || chatMessagesListeners.contains(listener)) {
            return;
        }
        chatMessagesListeners.add(listener);
        if (!cachedChatMessages.isEmpty()) {
            listener.onChatMessagesUpdated(new ArrayList<>(cachedChatMessages));
        }
    }

    public void removeChatMessagesListener(ChatMessagesListener listener) {
        chatMessagesListeners.remove(listener);
    }

    public List<ChatMessage> getCachedChatMessages() {
        return new ArrayList<>(cachedChatMessages);
    }

    private void notifyChatMessagesListeners(List<ChatMessage> messages) {
        List<ChatMessage> copy = new ArrayList<>(messages);
        runOnUiThread(() -> {
            for (ChatMessagesListener listener : new ArrayList<>(chatMessagesListeners)) {
                listener.onChatMessagesUpdated(copy);
            }
        });
    }

    private void startChatUnreadListener() {
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) return;

        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }

        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            chatListenerRegistration = firestore.collection("community_message")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    if (snapshot != null) {
                        List<ChatMessage> messages = firebaseService.parseChatMessagesFromSnapshot(snapshot);
                        cachedChatMessages = messages;
                        int unreadConvCount = firebaseService.countUnreadConversations(snapshot);
                        lastUnreadCount = unreadConvCount;
                        updateGlobalMessageBadges(unreadConvCount);
                        notifyChatMessagesListeners(messages);
                    }
                });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateGlobalMessageBadges(int count) {
        TextView homeBadge = findViewById(R.id.home_header_message_badge);
        TextView otherBadge = findViewById(R.id.message_badge);

        updateBadgeTextView(homeBadge, count);
        updateBadgeTextView(otherBadge, count);
    }

    private void updateBadgeTextView(TextView badgeView, int count) {
        if (badgeView == null) return;
        if (count > 0) {
            badgeView.setText(String.valueOf(count));
            badgeView.setVisibility(View.VISIBLE);
        } else {
            badgeView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }
    }
}
