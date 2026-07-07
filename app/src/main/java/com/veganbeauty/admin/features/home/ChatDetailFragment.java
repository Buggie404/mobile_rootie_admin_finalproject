package com.veganbeauty.admin.features.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.remote.ChatMessage;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.databinding.FragmentChatDetailBinding;
import com.veganbeauty.admin.databinding.ItemChatMessageBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatDetailFragment extends RootieAdminFragment {

    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_USERNAME = "arg_username";
    private static final String ARG_AVATAR = "arg_avatar";

    private FragmentChatDetailBinding binding;
    private final FirebaseService firebaseService = new FirebaseService();

    private final List<ChatMessage> allMessages = new ArrayList<>();
    private final List<ChatMessage> filteredMessages = new ArrayList<>();

    private ChatMessagesAdapter chatAdapter;

    private String userId = "";
    private String username = "";
    private String avatar = "";
    private boolean readMarked = false;

    private final MainActivity.ChatMessagesListener sharedChatListener = this::onSharedChatMessagesUpdated;

    public static ChatDetailFragment newInstance(String userId, String username, String avatar) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_AVATAR, avatar);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID, "");
            username = getArguments().getString(ARG_USERNAME, "");
            avatar = getArguments().getString(ARG_AVATAR, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        // Bind Header Info
        binding.txtHeaderTitle.setText(username);
        if (!avatar.isEmpty()) {
            ImageUtils.loadImage(requireContext(), binding.imgHeaderAvatar, avatar, R.drawable.imv_avatar);
        } else {
            binding.imgHeaderAvatar.setImageResource(R.drawable.imv_avatar);
        }

        // Back button
        binding.btnBack.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(new HomeMessageFragment());
            }
        });

        // Send button
        binding.btnSend.setOnClickListener(v -> sendMessage());

        // IME Editor Action Send
        binding.edtInputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        setupRecyclerView();
        loadConversation();
        registerSharedChatListener();
    }

    private void registerSharedChatListener() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.addChatMessagesListener(sharedChatListener);
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatMessagesAdapter(filteredMessages, avatar);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Start showing from bottom of list
        binding.rvChatHistory.setLayoutManager(layoutManager);
        binding.rvChatHistory.setAdapter(chatAdapter);
    }

    private void loadConversation() {
        if (getActivity() == null) return;
        new Thread(() -> {
            try {
                // Load local messages
                List<ChatMessage> localList = loadLocalMessages();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allMessages.clear();
                        allMessages.addAll(localList);

                        // Mark incoming messages as read
                        boolean updatedAny = false;
                        for (ChatMessage msg : allMessages) {
                            if (userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId()) && !msg.isRead()) {
                                msg.setRead(true);
                                updatedAny = true;
                            }
                        }

                        if (updatedAny) {
                            new Thread(() -> {
                                saveLocalMessages(allMessages);
                                markConversationReadOnce();
                            }).start();
                        }

                        filterAndBindMessages();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void filterAndBindMessages() {
        filteredMessages.clear();
        // Filter messages between admin and this specific user
        for (ChatMessage msg : allMessages) {
            boolean incoming = userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId());
            boolean outgoing = "rootie_vn".equals(msg.getSenderId()) && userId.equals(msg.getReceiverId());
            if (incoming || outgoing) {
                filteredMessages.add(msg);
            }
        }
        chatAdapter.notifyDataSetChanged();
        if (!filteredMessages.isEmpty()) {
            binding.rvChatHistory.scrollToPosition(filteredMessages.size() - 1);
        }
    }

    private void sendMessage() {
        String text = binding.edtInputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String rawId = UUID.randomUUID().toString();
        String shortId = rawId.length() >= 8 ? rawId.substring(0, 8) : rawId;

        ChatMessage newMessage = new ChatMessage();
        newMessage.setId("m_" + shortId);
        newMessage.setSenderId("rootie_vn");
        newMessage.setSenderName("Rootie VietNam");
        newMessage.setSenderAvatar("https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png");
        newMessage.setReceiverId(userId);
        newMessage.setReceiverName(username);
        newMessage.setReceiverAvatar(avatar);
        newMessage.setContent(text);
        newMessage.setTimestamp(System.currentTimeMillis());
        newMessage.setRead(true); // Sender automatically reads their own message

        binding.edtInputMessage.getText().clear();

        // Append to lists
        allMessages.add(newMessage);
        filteredMessages.add(newMessage);
        chatAdapter.notifyItemInserted(filteredMessages.size() - 1);
        binding.rvChatHistory.scrollToPosition(filteredMessages.size() - 1);

        new Thread(() -> {
            try {
                // Save locally
                saveLocalMessages(allMessages);
                // Save to Firebase
                boolean success = firebaseService.saveChatMessage(newMessage);
                if (!success && getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi kết nối Firebase, đã lưu ngoại tuyến", Toast.LENGTH_SHORT).show());
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
                        msg.setId(obj.optString("id"));
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

    private void onSharedChatMessagesUpdated(List<ChatMessage> messages) {
        if (binding == null || userId.isEmpty()) {
            return;
        }

        List<ChatMessage> remoteList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            boolean incoming = userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId());
            boolean outgoing = "rootie_vn".equals(msg.getSenderId()) && userId.equals(msg.getReceiverId());
            if (incoming || outgoing) {
                remoteList.add(msg);
            }
        }

        List<ChatMessage> merged = mergeMessages(allMessages, remoteList);
        if (!isDifferent(allMessages, merged)) {
            return;
        }

        allMessages.clear();
        allMessages.addAll(merged);

        boolean hasUnreadIncoming = false;
        for (ChatMessage msg : allMessages) {
            if (userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId()) && !msg.isRead()) {
                msg.setRead(true);
                hasUnreadIncoming = true;
            }
        }

        final boolean finalHasUnread = hasUnreadIncoming;
        new Thread(() -> {
            saveLocalMessages(allMessages);
            if (finalHasUnread) {
                markConversationReadOnce();
            }
        }).start();

        filterAndBindMessages();
    }

    private void markConversationReadOnce() {
        if (readMarked) {
            return;
        }
        readMarked = true;
        firebaseService.markConversationAsRead(userId);
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
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.removeChatMessagesListener(sharedChatListener);
        }
        super.onDestroyView();
        binding = null;
    }

    // RecyclerView Adapter
    private static class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder> {
        private static final int VIEW_TYPE_INCOMING = 1;
        private static final int VIEW_TYPE_OUTGOING = 2;

        private final List<ChatMessage> list;
        private final String partnerAvatar;

        ChatMessagesAdapter(List<ChatMessage> list, String partnerAvatar) {
            this.list = list;
            this.partnerAvatar = partnerAvatar;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage item = list.get(position);
            return "rootie_vn".equals(item.getSenderId()) ? VIEW_TYPE_OUTGOING : VIEW_TYPE_INCOMING;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage item = list.get(position);
            int viewType = getItemViewType(position);
            ItemChatMessageBinding binding = holder.binding;

            if (viewType == VIEW_TYPE_OUTGOING) {
                binding.layoutOutgoing.setVisibility(View.VISIBLE);
                binding.layoutIncoming.setVisibility(View.GONE);
                binding.txtOutgoingMessage.setText(item.getContent());
            } else {
                binding.layoutOutgoing.setVisibility(View.GONE);
                binding.layoutIncoming.setVisibility(View.VISIBLE);
                binding.txtIncomingMessage.setText(item.getContent());

                if (partnerAvatar != null && !partnerAvatar.isEmpty()) {
                    ImageUtils.loadImage(binding.getRoot().getContext(), binding.imgIncomingAvatar, partnerAvatar, R.drawable.imv_avatar);
                } else {
                    binding.imgIncomingAvatar.setImageResource(R.drawable.imv_avatar);
                }
            }
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemChatMessageBinding binding;

            ViewHolder(ItemChatMessageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
