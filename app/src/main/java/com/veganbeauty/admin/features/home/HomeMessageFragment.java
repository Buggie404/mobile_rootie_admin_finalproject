package com.veganbeauty.admin.features.home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import java.util.Arrays;
import android.graphics.Typeface;
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
import com.veganbeauty.admin.data.remote.ChatMessage;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.databinding.FragmentHomeMessageBinding;
import com.veganbeauty.admin.databinding.ItemChatThreadBinding;
import com.veganbeauty.admin.databinding.ItemStoryUserBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class HomeMessageFragment extends RootieAdminFragment {

    private FragmentHomeMessageBinding binding;
    private final FirebaseService firebaseService = new FirebaseService();
    private com.google.firebase.firestore.ListenerRegistration firestoreListener = null;

    private final List<ChatMessage> allMessages = new ArrayList<>();
    private final List<ChatThread> chatThreads = new ArrayList<>();
    private final List<ChatThread> filteredThreads = new ArrayList<>();

    private StoriesAdapter storiesAdapter;
    private ChatsAdapter chatsAdapter;

    private String currentFilter = "all"; // all, unread, pending
    private String searchQuery = "";

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
        loadAndSyncMessages();
        startFirestoreListener();
    }

    private void setupRecyclerViews() {
        // 1. Stories Horizontal List
        storiesAdapter = new StoriesAdapter(getMockStories(), story -> openChatDetail(story.userId, story.name, story.avatar));
        binding.rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storiesAdapter);

        // 2. Chat Threads Vertical List
        chatsAdapter = new ChatsAdapter(filteredThreads, thread -> openChatDetail(thread.userId, thread.username, thread.avatar));
        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChats.setAdapter(chatsAdapter);
    }

    private void loadAndSyncMessages() {
        if (getActivity() == null) return;
        new Thread(() -> {
            try {
                // 1. Load local messages
                List<ChatMessage> localList = loadLocalMessages();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allMessages.clear();
                        allMessages.addAll(localList);
                        updateChatThreads();
                    });
                }

                // 2. Load Firestore remote messages and merge
                List<ChatMessage> remoteList = firebaseService.fetchChatMessages();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<ChatMessage> merged = mergeMessages(allMessages, remoteList);
                        allMessages.clear();
                        allMessages.addAll(merged);

                        new Thread(() -> saveLocalMessages(allMessages)).start();

                        updateChatThreads();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<ChatMessage> loadLocalMessages() {
        List<ChatMessage> list = new ArrayList<>();
        try {
            File localFile = new File(requireContext().getFilesDir(), "community_message.json");
            if (localFile.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String jsonString = sb.toString();
                if (!jsonString.trim().isEmpty()) {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        ChatMessage msg = new ChatMessage();
                        msg.setId(obj.optString("id", UUID.randomUUID().toString()));
                        msg.setSenderId(obj.optString("senderId"));
                        msg.setSenderName(obj.optString("senderName"));
                        msg.setSenderAvatar(obj.optString("senderAvatar"));
                        msg.setReceiverId(obj.optString("receiverId"));
                        msg.setReceiverName(obj.optString("receiverName"));
                        msg.setReceiverAvatar(obj.optString("receiverAvatar"));
                        msg.setContent(obj.optString("content"));
                        msg.setTimestamp(obj.optLong("timestamp"));
                        msg.setRead(obj.optBoolean("isRead", false));
                        list.add(msg);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveLocalMessages(List<ChatMessage> messages) {
        try {
            File localFile = new File(requireContext().getFilesDir(), "community_message.json");
            JSONArray jsonArray = new JSONArray();
            for (ChatMessage msg : messages) {
                JSONObject obj = new JSONObject();
                obj.put("id", msg.getId());
                obj.put("senderId", msg.getSenderId());
                obj.put("senderName", msg.getSenderName());
                obj.put("senderAvatar", msg.getSenderAvatar());
                obj.put("receiverId", msg.getReceiverId());
                obj.put("receiverName", msg.getReceiverName());
                obj.put("receiverAvatar", msg.getReceiverAvatar());
                obj.put("content", msg.getContent());
                obj.put("timestamp", msg.getTimestamp());
                obj.put("isRead", msg.isRead());
                jsonArray.put(obj);
            }
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                fos.write(jsonArray.toString(2).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ChatMessage> mergeMessages(List<ChatMessage> local, List<ChatMessage> remote) {
        Map<String, ChatMessage> map = new HashMap<>();
        for (ChatMessage msg : local) {
            map.put(msg.getId(), msg);
        }
        for (ChatMessage msg : remote) {
            map.put(msg.getId(), msg);
        }
        List<ChatMessage> merged = new ArrayList<>(map.values());
        Collections.sort(merged, (o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
        return merged;
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
                if ("rootie_vn".equals(msg.getReceiverId()) && !msg.isRead()) {
                    unreadCount++;
                }
            }

            boolean isActive = partnerId.equals("48228004") || partnerId.equals("quynh_nhu_user");

            chatThreads.add(new ChatThread(
                partnerId,
                partnerName,
                partnerAvatar,
                lastMsg.getContent(),
                formatRelativeTime(lastMsg.getTimestamp()),
                unreadCount,
                isActive,
                lastMsg.getTimestamp()
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
            result = new ArrayList<>();
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
                    binding.txtTitle.setText("Message");
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
            mainAct.loadFragment(chatDetail);
        }
    }

    private List<StoryUser> getMockStories() {
        return Arrays.asList(
            new StoryUser("rootie_vn", "Tin của bạn", "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png", false, true),
            new StoryUser("48228004", "nguyen_bao", "https://i.pinimg.com/736x/ab/32/b1/ab32b13edefed48f94d93ee4b6f12f6b.jpg", true, false),
            new StoryUser("68751659", "khanh_xun", "https://i1-c.pinimg.com/736x/4d/fe/b7/4dfeb7f781432e75e270d3bf70f494e4.jpg", true, false),
            new StoryUser("87962440", "bin_khanh", "https://i1-c.pinimg.com/736x/9e/12/94/9e1294132dbb8f12c70f31058b98bdb1.jpg", true, false),
            new StoryUser("85097162", "mei_anh", "https://i.pinimg.com/736x/87/1c/91/871c91ffb39c0fc6a44c77f0a905a396.jpg", true, false)
        );
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

    private void startFirestoreListener() {
        try {
            com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            firestoreListener = firestore.collection("community_message")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<ChatMessage> remoteMessages = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<String> members = (List<String>) doc.get("members");
                                if (members == null || !members.contains("rootie_vn")) continue;

                                String userId = null;
                                for (String m : members) {
                                    if (!"rootie_vn".equals(m)) {
                                        userId = m;
                                        break;
                                    }
                                }
                                if (userId == null) continue;

                                @SuppressWarnings("unchecked")
                                Map<String, Map<String, Object>> memberInfo = (Map<String, Map<String, Object>>) doc.get("member_info");
                                Map<String, Object> partnerInfo = memberInfo != null ? memberInfo.get(userId) : null;
                                String username = partnerInfo != null && partnerInfo.get("name") != null ? partnerInfo.get("name").toString() : "User";
                                String avatar = partnerInfo != null && partnerInfo.get("avatar") != null ? partnerInfo.get("avatar").toString() : "";

                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> messagesRaw = (List<Map<String, Object>>) doc.get("messages");
                                if (messagesRaw == null) continue;

                                for (Map<String, Object> msgMap : messagesRaw) {
                                    String senderId = msgMap.get("sender_id") != null ? msgMap.get("sender_id").toString() : "";
                                    String text = msgMap.get("text") != null ? msgMap.get("text").toString() : "";
                                    String sentAt = msgMap.get("sent_at") != null ? msgMap.get("sent_at").toString() : "";

                                    boolean isAgent = "rootie_vn".equals(senderId);
                                    long timestamp = parseIsoString(sentAt);
                                    String msgId = msgMap.get("id") != null ? msgMap.get("id").toString() : "msg_" + userId + "_" + timestamp;

                                    ChatMessage msg = new ChatMessage();
                                    msg.setId(msgId);
                                    msg.setSenderId(isAgent ? "rootie_vn" : userId);
                                    msg.setSenderName(isAgent ? "Rootie VietNam" : username);
                                    msg.setSenderAvatar(isAgent ? "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png" : avatar);
                                    msg.setReceiverId(isAgent ? userId : "rootie_vn");
                                    msg.setReceiverName(isAgent ? username : "Rootie VietNam");
                                    msg.setReceiverAvatar(isAgent ? avatar : "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png");
                                    msg.setContent(text);
                                    msg.setTimestamp(timestamp);
                                    msg.setRead(isAgent || (msgMap.get("seen_at") != null));
                                    remoteMessages.add(msg);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        List<ChatMessage> merged = mergeMessages(allMessages, remoteMessages);
                        if (isDifferent(allMessages, merged)) {
                            allMessages.clear();
                            allMessages.addAll(merged);

                            new Thread(() -> saveLocalMessages(allMessages)).start();

                            updateChatThreads();
                        }
                    }
                });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private long parseIsoString(String isoStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = format.parse(isoStr);
            return d != null ? d.getTime() : System.currentTimeMillis();
        } catch (Exception ex) {
            return System.currentTimeMillis();
        }
    }

    private boolean isDifferent(List<ChatMessage> local, List<ChatMessage> remote) {
        if (local.size() != remote.size()) return true;
        for (int i = 0; i < local.size(); i++) {
            ChatMessage l = local.get(i);
            ChatMessage r = remote.get(i);
            if (!l.getSenderId().equals(r.getSenderId()) ||
                !l.getContent().equals(r.getContent()) ||
                l.getTimestamp() != r.getTimestamp()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
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

        public ChatThread(String userId, String username, String avatar, String lastMessage, String lastMessageTime, int unreadCount, boolean isActive, long timestamp) {
            this.userId = userId;
            this.username = username;
            this.avatar = avatar;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
            this.isActive = isActive;
            this.timestamp = timestamp;
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
            this.list = list;
            this.listener = listener;
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
            if (isUnread) {
                setTextStyleBold(binding.txtName);
                setTextStyleBold(binding.txtMessageSnippet);
                binding.txtMessageSnippet.setText(item.unreadCount + "+ tin nhắn mới");
                binding.viewUnreadDot.setVisibility(View.VISIBLE);
            } else {
                setTextStyleNormal(binding.txtName);
                setTextStyleNormal(binding.txtMessageSnippet);
                binding.txtMessageSnippet.setText(item.lastMessage);
                binding.viewUnreadDot.setVisibility(View.GONE);
            }

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
