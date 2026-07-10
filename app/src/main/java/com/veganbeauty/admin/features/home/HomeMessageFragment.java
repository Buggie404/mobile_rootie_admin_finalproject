package com.veganbeauty.admin.features.home;

import android.graphics.Color;
import android.graphics.Typeface;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.ChatMessageLocalStore;
import com.veganbeauty.admin.data.remote.AdminCommunityMessageSeeder;
import com.veganbeauty.admin.data.remote.ChatMessage;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.databinding.FragmentHomeMessageBinding;
import com.veganbeauty.admin.databinding.ItemChatThreadBinding;
import com.veganbeauty.admin.databinding.ItemStoryUserBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeMessageFragment extends RootieAdminFragment {

    private FragmentHomeMessageBinding binding;

    private final List<ChatMessage> allMessages = new ArrayList<>();
    private final List<ChatThread> chatThreads = new ArrayList<>();
    private final List<ChatThread> filteredThreads = new ArrayList<>();

    private StoriesAdapter storiesAdapter;
    private ChatsAdapter chatsAdapter;

    private String currentFilter = "all"; // all, unread, pending
    private String searchQuery = "";
    private boolean localMessagesLoaded = false;

    private final FirebaseService firebaseService = new FirebaseService();
    private final MainActivity.ChatMessagesListener chatMessagesListener = this::applyRemoteMessages;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        // Back Button click
        binding.btnBack.setOnClickListener(v -> {
            MainActivity mainAct = (MainActivity) getActivity();
            if (mainAct != null) {
                BottomNavHelper.navigate(mainAct, R.id.nav_home);
            }
        });

        // Notification Button click
        binding.btnNotification.setOnClickListener(v -> Toast.makeText(getContext(), "Mở thông báo", Toast.LENGTH_SHORT).show());

        // Setup Dropdown Title Menu
        binding.layoutTitleDropdown.setOnClickListener(v -> showFilterMenu());

        // Setup Search
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim() : "";
                applyFiltersAndSearch();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupRecyclerViews();
        updateStoryUsers();
        loadAndSyncMessages();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && localMessagesLoaded) {
            refreshFromLocalStore();
        }
    }

    private void refreshFromLocalStore() {
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            List<ChatMessage> localList = ChatMessageLocalStore.load(appContext);
            List<ChatMessage> merged = ChatMessageLocalStore.merge(allMessages, localList);
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                allMessages.clear();
                allMessages.addAll(merged);
                updateChatThreads();
            });
        }).start();
    }

    private void setupRecyclerViews() {
        // 1. Stories Horizontal List
        storiesAdapter = new StoriesAdapter(new ArrayList<>(), story -> openChatDetail(story.userId, story.name, story.avatar));
        binding.rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storiesAdapter);

        // 2. Chat Threads Vertical List
        chatsAdapter = new ChatsAdapter(filteredThreads, thread -> openChatDetail(thread.userId, thread.username, thread.avatar));
        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChats.setAdapter(chatsAdapter);
    }

    private void loadAndSyncMessages() {
        if (getContext() == null) {
            return;
        }
        localMessagesLoaded = false;
        Context appContext = requireContext().getApplicationContext();

        List<ChatMessage> assetMessages = AdminCommunityMessageSeeder.loadMessagesFromAssets(appContext);
        List<ChatMessage> localList = ChatMessageLocalStore.load(appContext);
        List<ChatMessage> initial = ChatMessageLocalStore.merge(localList, assetMessages);
        if (!initial.isEmpty()) {
            allMessages.clear();
            allMessages.addAll(initial);
            localMessagesLoaded = true;
            ChatMessageLocalStore.saveAll(appContext, initial);
            updateChatThreads();

            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.addChatMessagesListener(chatMessagesListener);
            }
        }

        new Thread(() -> syncMessagesWithFirebase(appContext)).start();
    }

    private void syncMessagesWithFirebase(Context appContext) {
        try {
            AdminCommunityMessageSeeder.restoreRootieChatsFromAssets(appContext, firebaseService);
            List<ChatMessage> remoteList = firebaseService.fetchChatMessages();

            MainActivity mainAct = (MainActivity) getActivity();
            if ((remoteList == null || remoteList.isEmpty()) && mainAct != null) {
                remoteList = new ArrayList<>(mainAct.getCachedChatMessages());
            }
            if (remoteList == null) {
                remoteList = new ArrayList<>();
            }

            List<ChatMessage> localList = ChatMessageLocalStore.load(appContext);
            List<ChatMessage> assetMessages = AdminCommunityMessageSeeder.loadMessagesFromAssets(appContext);
            List<ChatMessage> merged = ChatMessageLocalStore.merge(localList, assetMessages);
            merged = ChatMessageLocalStore.merge(merged, remoteList);

            if (!merged.isEmpty()) {
                ChatMessageLocalStore.saveAll(appContext, merged);
            }

            List<ChatMessage> finalList = new ArrayList<>(merged);
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                allMessages.clear();
                allMessages.addAll(finalList);
                localMessagesLoaded = true;
                updateChatThreads();

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.addChatMessagesListener(chatMessagesListener);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                if (binding == null || localMessagesLoaded) {
                    return;
                }
                List<ChatMessage> fallback = AdminCommunityMessageSeeder.loadMessagesFromAssets(appContext);
                if (!fallback.isEmpty()) {
                    allMessages.clear();
                    allMessages.addAll(fallback);
                    localMessagesLoaded = true;
                    updateChatThreads();
                }
            });
        }
    }

    private void applyRemoteMessages(List<ChatMessage> remoteMessages) {
        if (binding == null || !localMessagesLoaded || remoteMessages == null) {
            return;
        }
        List<ChatMessage> merged = ChatMessageLocalStore.merge(allMessages, remoteMessages);
        allMessages.clear();
        allMessages.addAll(merged);
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> ChatMessageLocalStore.saveAll(appContext, merged)).start();
        updateChatThreads();
    }

    private void updateChatThreads() {
        chatThreads.clear();

        // Group messages by partner ID
        Map<String, List<ChatMessage>> grouped = new HashMap<>();
        for (ChatMessage msg : allMessages) {
            String partnerId = "rootie_vn".equals(msg.getSenderId()) ? msg.getReceiverId() : msg.getSenderId();
            if (partnerId == null || partnerId.trim().isEmpty() || "rootie_vn".equals(partnerId)) {
                continue;
            }
            List<ChatMessage> list = grouped.computeIfAbsent(partnerId, k -> new ArrayList<>());
            list.add(msg);
        }

        for (Map.Entry<String, List<ChatMessage>> entry : grouped.entrySet()) {
            String partnerId = entry.getKey();
            List<ChatMessage> messages = entry.getValue();

            ChatMessage lastMsg = null;
            for (ChatMessage msg : messages) {
                if (lastMsg == null || msg.getTimestamp() > lastMsg.getTimestamp()) {
                    lastMsg = msg;
                }
            }

            if (lastMsg == null) continue;

            String partnerName = "rootie_vn".equals(lastMsg.getSenderId()) ? lastMsg.getReceiverName() : lastMsg.getSenderName();
            String partnerAvatar = "rootie_vn".equals(lastMsg.getSenderId()) ? lastMsg.getReceiverAvatar() : lastMsg.getSenderAvatar();

            int unreadCount = 0;
            for (ChatMessage msg : messages) {
                if (partnerId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId()) && !msg.isRead()) {
                    unreadCount++;
                }
            }

            boolean isActive = partnerId.equals("48228004") || partnerId.equals("quynh_nhu_user");
            boolean lastFromCustomer = !"rootie_vn".equals(lastMsg.getSenderId());
            String snippet = lastFromCustomer
                ? lastMsg.getContent()
                : "Bạn: " + lastMsg.getContent();

            chatThreads.add(new ChatThread(
                partnerId,
                partnerName,
                partnerAvatar,
                snippet,
                formatRelativeTime(lastMsg.getTimestamp()),
                unreadCount,
                isActive,
                lastMsg.getTimestamp(),
                lastFromCustomer
            ));
        }

        // Sort by timestamp desc
        Collections.sort(chatThreads, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

        applyFiltersAndSearch();
    }

    private void applyFiltersAndSearch() {
        filteredThreads.clear();
        List<ChatThread> result = new ArrayList<>(chatThreads);

        // 1. Category Filter
        if ("unread".equals(currentFilter)) {
            List<ChatThread> temp = new ArrayList<>();
            for (ChatThread ct : result) {
                if (ct.unreadCount > 0) {
                    temp.add(ct);
                }
            }
            result = temp;
        } else if ("pending".equals(currentFilter)) {
            List<ChatThread> temp = new ArrayList<>();
            for (ChatThread ct : result) {
                if (ct.lastMessageFromCustomer) {
                    temp.add(ct);
                }
            }
            result = temp;
        }

        // 2. Search Query
        if (!searchQuery.isEmpty()) {
            List<ChatThread> temp = new ArrayList<>();
            for (ChatThread ct : result) {
                boolean nameMatches = ct.username != null && ct.username.toLowerCase().contains(searchQuery.toLowerCase());
                boolean msgMatches = ct.lastMessage != null && ct.lastMessage.toLowerCase().contains(searchQuery.toLowerCase());
                if (nameMatches || msgMatches) {
                    temp.add(ct);
                }
            }
            result = temp;
        }

        filteredThreads.addAll(result);
        if (chatsAdapter != null) {
            chatsAdapter.notifyDataSetChanged();
        }
        updateStoryUsers();
        updateNotificationBadge();

        // 3. Show/Hide Empty State
        if (filteredThreads.isEmpty()) {
            binding.rvChats.setVisibility(View.GONE);
            binding.lblMessages.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvChats.setVisibility(View.VISIBLE);
            binding.lblMessages.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void showFilterMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), binding.layoutTitleDropdown, Gravity.CENTER);
        popup.getMenu().add(0, 1, 0, "Tất cả");
        popup.getMenu().add(0, 2, 0, "Chưa đọc");
        popup.getMenu().add(0, 3, 0, "Tin nhắn đang chờ");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    currentFilter = "all";
                    binding.txtTitle.setText("Tin nhắn");
                    break;
                case 2:
                    currentFilter = "unread";
                    binding.txtTitle.setText("Chưa đọc");
                    break;
                case 3:
                    currentFilter = "pending";
                    binding.txtTitle.setText("Tin nhắn đang chờ");
                    break;
            }
            applyFiltersAndSearch();
            return true;
        });
        popup.show();
    }

    private void openChatDetail(String userId, String username, String avatar) {
        ChatDetailFragment chatDetail = ChatDetailFragment.newInstance(userId, username, avatar);
        MainActivity mainAct = (MainActivity) getActivity();
        if (mainAct != null) {
            mainAct.loadFragmentHidingNav(chatDetail);
        }
    }

    private void updateStoryUsers() {
        if (storiesAdapter == null || binding == null) {
            return;
        }
        List<StoryUser> stories = new ArrayList<>();
        int limit = Math.min(filteredThreads.size(), 12);
        for (int i = 0; i < limit; i++) {
            ChatThread thread = filteredThreads.get(i);
            stories.add(new StoryUser(thread.userId, thread.username, thread.avatar, thread.isActive, false));
        }
        storiesAdapter.updateStories(stories);
        binding.rvStories.setVisibility(stories.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateNotificationBadge() {
        if (binding == null) {
            return;
        }
        int totalUnread = 0;
        for (ChatThread thread : chatThreads) {
            totalUnread += thread.unreadCount;
        }
        if (totalUnread > 0) {
            binding.notificationBadge.setVisibility(View.VISIBLE);
            binding.notificationBadge.setText(totalUnread > 99 ? "99+" : String.valueOf(totalUnread));
        } else {
            binding.notificationBadge.setVisibility(View.GONE);
        }
    }

    private String formatRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút";
        } else if (hours < 24) {
            return hours + " giờ";
        } else if (days < 7) {
            return days + " ngày";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    @Override
    public void onDestroyView() {
        MainActivity mainAct = (MainActivity) getActivity();
        if (mainAct != null) {
            mainAct.removeChatMessagesListener(chatMessagesListener);
        }
        super.onDestroyView();
        binding = null;
    }

    // Models
    public static class ChatThread {
        final String userId;
        final String username;
        final String avatar;
        final String lastMessage;
        final String lastMessageTime;
        final int unreadCount;
        final boolean isActive;
        final long timestamp;
        final boolean lastMessageFromCustomer;

        public ChatThread(String userId, String username, String avatar, String lastMessage, String lastMessageTime, int unreadCount, boolean isActive, long timestamp, boolean lastMessageFromCustomer) {
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
            this.isActive = isActive;
            this.timestamp = timestamp;
            this.lastMessageFromCustomer = lastMessageFromCustomer;
        }
    }

    public static class StoryUser {
        final String userId;
        final String name;
        final String avatar;
        final boolean isActive;
        final boolean isMe;

        public StoryUser(String userId, String name, String avatar, boolean isActive, boolean isMe) {
            this.userId = userId;
            this.name = name;
            this.avatar = avatar;
            this.isActive = isActive;
            this.isMe = isMe;
        }
    }

    // Recycler view adapters
    private static class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.ViewHolder> {
        interface OnStoryClickListener {
            void onStoryClick(StoryUser story);
        }

        private final List<StoryUser> list;
        private final OnStoryClickListener listener;

        StoriesAdapter(List<StoryUser> list, OnStoryClickListener listener) {
            this.list = new ArrayList<>(list);
            this.listener = listener;
        }

        void updateStories(List<StoryUser> stories) {
            list.clear();
            if (stories != null) {
                list.addAll(stories);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemStoryUserBinding binding = ItemStoryUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoryUser item = list.get(position);
            ItemStoryUserBinding binding = holder.binding;
            binding.txtUsername.setText(item.name);

            if (item.avatar != null && !item.avatar.isEmpty()) {
                ImageUtils.loadImage(binding.getRoot().getContext(), binding.imgAvatar, item.avatar, R.drawable.imv_avatar);
            } else {
                binding.imgAvatar.setImageResource(R.drawable.imv_avatar);
            }

            if (item.isMe) {
                binding.layoutPlus.setVisibility(View.VISIBLE);
                binding.viewActiveDot.setVisibility(View.GONE);
            } else {
                binding.layoutPlus.setVisibility(View.GONE);
                binding.viewActiveDot.setVisibility(item.isActive ? View.VISIBLE : View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (!item.isMe && listener != null) {
                    listener.onStoryClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemStoryUserBinding binding;

            ViewHolder(ItemStoryUserBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private static class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {
        interface OnChatClickListener {
            void onChatClick(ChatThread thread);
        }

        private final List<ChatThread> list;
        private final OnChatClickListener listener;

        ChatsAdapter(List<ChatThread> list, OnChatClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemChatThreadBinding binding = ItemChatThreadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatThread item = list.get(position);
            ItemChatThreadBinding binding = holder.binding;
            binding.txtName.setText(item.username);

            boolean isUnread = item.unreadCount > 0;
            holder.itemView.setAlpha(isUnread ? 1f : 0.58f);

            if (isUnread) {
                setTextStyleBold(binding.txtName);
                setTextStyleBold(binding.txtMessageSnippet);
                binding.txtMessageSnippet.setTextColor(Color.parseColor("#3E4D44"));
                binding.txtTime.setTextColor(Color.parseColor("#4F6544"));
                binding.txtUnreadCount.setVisibility(View.VISIBLE);
                binding.txtUnreadCount.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
            } else {
                setTextStyleNormal(binding.txtName);
                setTextStyleNormal(binding.txtMessageSnippet);
                binding.txtMessageSnippet.setTextColor(Color.parseColor("#7E8A83"));
                binding.txtTime.setTextColor(Color.parseColor("#97A49C"));
                binding.txtUnreadCount.setVisibility(View.GONE);
            }

            binding.txtMessageSnippet.setText(item.lastMessage);
            binding.txtTime.setText(item.lastMessageTime);
            binding.viewActiveDot.setVisibility(item.isActive ? View.VISIBLE : View.GONE);

            if (item.avatar != null && !item.avatar.isEmpty()) {
                ImageUtils.loadImage(binding.getRoot().getContext(), binding.imgAvatar, item.avatar, R.drawable.imv_avatar);
            } else {
                binding.imgAvatar.setImageResource(R.drawable.imv_avatar);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        private void setTextStyleBold(TextView textView) {
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        }

        private void setTextStyleNormal(TextView textView) {
            textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.NORMAL));
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemChatThreadBinding binding;

            ViewHolder(ItemChatThreadBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
