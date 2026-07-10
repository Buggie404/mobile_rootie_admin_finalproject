package com.veganbeauty.admin.features.home;



import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import android.text.Editable;

import android.text.TextWatcher;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.admin.MainActivity;

import com.veganbeauty.admin.R;

import com.veganbeauty.admin.core.base.RootieAdminFragment;

import com.veganbeauty.admin.core.utils.ImageUtils;

import com.veganbeauty.admin.data.local.ChatMessageLocalStore;

import com.veganbeauty.admin.data.remote.ChatMessage;

import com.veganbeauty.admin.data.remote.FirebaseService;

import com.veganbeauty.admin.databinding.FragmentChatDetailBinding;

import com.veganbeauty.admin.databinding.ItemChatDateHeaderBinding;

import com.veganbeauty.admin.databinding.ItemChatMessageIncomingBinding;

import com.veganbeauty.admin.databinding.ItemChatMessageOutgoingBinding;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Calendar;

import java.util.Collections;

import java.util.Date;

import java.util.List;

import java.util.Locale;

import java.util.UUID;



public class ChatDetailFragment extends RootieAdminFragment {



    private static final String ARG_USER_ID = "arg_user_id";

    private static final String ARG_USERNAME = "arg_username";

    private static final String ARG_AVATAR = "arg_avatar";



    private FragmentChatDetailBinding binding;

    private final FirebaseService firebaseService = new FirebaseService();



    private final List<ChatMessage> allMessages = new ArrayList<>();

    private final List<ChatMessage> conversationMessages = new ArrayList<>();

    private final List<ChatDisplayItem> displayItems = new ArrayList<>();



    private ChatMessagesAdapter chatAdapter;



    private String userId = "";

    private String username = "";

    private String avatar = "";

    private boolean readMarked = false;

    private boolean localMessagesLoaded = false;



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

        binding.txtHeaderTitle.setText(username);

        binding.txtHeaderSubtitle.setText("Khách hàng Rootie");

        if (!avatar.isEmpty()) {

            ImageUtils.loadImage(requireContext(), binding.imgHeaderAvatar, avatar, R.drawable.imv_avatar);

        } else {

            binding.imgHeaderAvatar.setImageResource(R.drawable.imv_avatar);

        }



        binding.btnBack.setOnClickListener(v -> {

            MainActivity mainActivity = (MainActivity) getActivity();

            if (mainActivity != null) {

                if (getParentFragmentManager().getBackStackEntryCount() > 0) {

                    getParentFragmentManager().popBackStack();

                    mainActivity.ensureBottomNavVisible();

                } else {

                    mainActivity.loadFragment(new HomeMessageFragment());

                }

            }

        });



        binding.btnSend.setOnClickListener(v -> sendMessage());



        binding.edtInputMessage.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == EditorInfo.IME_ACTION_SEND) {

                sendMessage();

                return true;

            }

            return false;

        });



        binding.edtInputMessage.addTextChangedListener(new TextWatcher() {

            @Override

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}



            @Override

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                updateSendButtonState(s != null && s.toString().trim().length() > 0);

            }



            @Override

            public void afterTextChanged(Editable s) {}

        });

        updateSendButtonState(false);

        setupKeyboardInsets();
        setupRecyclerView();
        loadConversation();
    }

    private void setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomInset = Math.max(imeInsets.bottom, systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            if (imeInsets.bottom > 0) {
                binding.rvChatHistory.postDelayed(this::scrollToLatest, 120);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());

        binding.edtInputMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.rvChatHistory.postDelayed(this::scrollToLatest, 200);
            }
        });
    }

    private void updateSendButtonState(boolean enabled) {

        binding.btnSend.setAlpha(enabled ? 1f : 0.45f);

        binding.btnSend.setEnabled(enabled);

    }



    private void registerSharedChatListener() {

        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {

            activity.addChatMessagesListener(sharedChatListener);

        }

    }



    private void setupRecyclerView() {

        chatAdapter = new ChatMessagesAdapter(displayItems, avatar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        layoutManager.setStackFromEnd(true);

        binding.rvChatHistory.setLayoutManager(layoutManager);

        binding.rvChatHistory.setAdapter(chatAdapter);

    }



    private void loadConversation() {

        if (getActivity() == null) return;

        localMessagesLoaded = false;

        new Thread(() -> {

            try {

                Context appContext = requireContext().getApplicationContext();

                List<ChatMessage> localList = ChatMessageLocalStore.load(appContext);



                if (getActivity() != null) {

                    getActivity().runOnUiThread(() -> {

                        allMessages.clear();

                        allMessages.addAll(localList);

                        localMessagesLoaded = true;



                        boolean updatedAny = false;

                        for (ChatMessage msg : allMessages) {

                            if (userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId()) && !msg.isRead()) {

                                msg.setRead(true);

                                updatedAny = true;

                            }

                        }



                        if (updatedAny) {

                            Context ctx = requireContext().getApplicationContext();

                            new Thread(() -> {

                                ChatMessageLocalStore.saveMerged(ctx, allMessages);

                                markConversationReadOnce();

                            }).start();

                            notifyReadStateUpdated();

                        }



                        rebuildConversation();

                        registerSharedChatListener();

                    });

                }

            } catch (Exception e) {

                e.printStackTrace();

            }

        }).start();

    }



    private void rebuildConversation() {

        conversationMessages.clear();

        for (ChatMessage msg : allMessages) {

            boolean incoming = userId.equals(msg.getSenderId()) && "rootie_vn".equals(msg.getReceiverId());

            boolean outgoing = "rootie_vn".equals(msg.getSenderId()) && userId.equals(msg.getReceiverId());

            if (incoming || outgoing) {

                conversationMessages.add(msg);

            }

        }

        Collections.sort(conversationMessages, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        buildDisplayItems();

        chatAdapter.notifyDataSetChanged();

        scrollToLatest();

    }



    private void buildDisplayItems() {

        displayItems.clear();

        String lastDateKey = null;

        ChatMessage previousMessage = null;



        for (ChatMessage msg : conversationMessages) {

            String dateKey = getDateKey(msg.getTimestamp());

            if (!dateKey.equals(lastDateKey)) {

                displayItems.add(ChatDisplayItem.date(formatDateHeader(msg.getTimestamp())));

                lastDateKey = dateKey;

                previousMessage = null;

            }



            boolean outgoing = "rootie_vn".equals(msg.getSenderId());

            if (outgoing) {

                displayItems.add(ChatDisplayItem.outgoing(msg));

            } else {

                boolean showAvatar = previousMessage == null || "rootie_vn".equals(previousMessage.getSenderId());

                displayItems.add(ChatDisplayItem.incoming(msg, showAvatar));

            }

            previousMessage = msg;

        }

    }



    private void scrollToLatest() {

        if (!displayItems.isEmpty()) {

            binding.rvChatHistory.scrollToPosition(displayItems.size() - 1);

        }

    }



    private String getDateKey(long timestamp) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        return sdf.format(new Date(timestamp));

    }



    private String formatDateHeader(long timestamp) {

        Calendar messageCal = Calendar.getInstance();

        messageCal.setTimeInMillis(timestamp);



        Calendar today = Calendar.getInstance();

        Calendar yesterday = Calendar.getInstance();

        yesterday.add(Calendar.DAY_OF_YEAR, -1);



        if (isSameDay(messageCal, today)) {

            return "Hôm nay";

        }

        if (isSameDay(messageCal, yesterday)) {

            return "Hôm qua";

        }



        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));

        String formatted = sdf.format(new Date(timestamp));

        if (formatted.length() > 0) {

            formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);

        }

        return formatted;

    }



    private boolean isSameDay(Calendar first, Calendar second) {

        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)

            && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);

    }



    private String formatMessageTime(long timestamp) {

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return sdf.format(new Date(timestamp));

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

        newMessage.setRead(true);



        binding.edtInputMessage.getText().clear();

        updateSendButtonState(false);



        allMessages.add(newMessage);

        conversationMessages.add(newMessage);

        buildDisplayItems();

        chatAdapter.notifyDataSetChanged();

        scrollToLatest();



        new Thread(() -> {

            try {

                Context appContext = requireContext().getApplicationContext();

                ChatMessageLocalStore.saveMerged(appContext, Collections.singletonList(newMessage));

                boolean success = firebaseService.saveChatMessage(newMessage);

                if (!success && getActivity() != null) {

                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi kết nối Firebase, đã lưu ngoại tuyến", Toast.LENGTH_SHORT).show());

                }

            } catch (Exception e) {

                e.printStackTrace();

            }

        }).start();

    }



    private void onSharedChatMessagesUpdated(List<ChatMessage> messages) {

        if (binding == null || userId.isEmpty() || !localMessagesLoaded) {

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



        List<ChatMessage> merged = ChatMessageLocalStore.merge(allMessages, remoteList);

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

        Context appContext = requireContext().getApplicationContext();

        new Thread(() -> {

            ChatMessageLocalStore.saveMerged(appContext, merged);

            if (finalHasUnread) {

                markConversationReadOnce();

            }

        }).start();



        if (finalHasUnread) {

            notifyReadStateUpdated();

        }



        rebuildConversation();

    }



    private void markConversationReadOnce() {

        if (readMarked) {

            return;

        }

        readMarked = true;

        firebaseService.markConversationAsRead(userId);

    }



    private void notifyReadStateUpdated() {

        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {

            activity.broadcastChatMessageUpdate(new ArrayList<>(allMessages));

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

        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {

            activity.removeChatMessagesListener(sharedChatListener);

        }

        super.onDestroyView();

        binding = null;

    }



    private static class ChatDisplayItem {

        static final int TYPE_DATE = 0;

        static final int TYPE_INCOMING = 1;

        static final int TYPE_OUTGOING = 2;



        final int type;

        final String dateLabel;

        final ChatMessage message;

        final boolean showAvatar;



        private ChatDisplayItem(int type, String dateLabel, ChatMessage message, boolean showAvatar) {

            this.type = type;

            this.dateLabel = dateLabel;

            this.message = message;

            this.showAvatar = showAvatar;

        }



        static ChatDisplayItem date(String label) {

            return new ChatDisplayItem(TYPE_DATE, label, null, false);

        }



        static ChatDisplayItem incoming(ChatMessage message, boolean showAvatar) {

            return new ChatDisplayItem(TYPE_INCOMING, null, message, showAvatar);

        }



        static ChatDisplayItem outgoing(ChatMessage message) {

            return new ChatDisplayItem(TYPE_OUTGOING, null, message, false);

        }

    }



    private class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<ChatDisplayItem> list;

        private final String partnerAvatar;



        ChatMessagesAdapter(List<ChatDisplayItem> list, String partnerAvatar) {

            this.list = list;

            this.partnerAvatar = partnerAvatar;

        }



        @Override

        public int getItemViewType(int position) {

            return list.get(position).type;

        }



        @NonNull

        @Override

        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            if (viewType == ChatDisplayItem.TYPE_DATE) {

                return new DateViewHolder(ItemChatDateHeaderBinding.inflate(inflater, parent, false));

            }

            if (viewType == ChatDisplayItem.TYPE_OUTGOING) {

                return new OutgoingViewHolder(ItemChatMessageOutgoingBinding.inflate(inflater, parent, false));

            }

            return new IncomingViewHolder(ItemChatMessageIncomingBinding.inflate(inflater, parent, false));

        }



        @Override

        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            ChatDisplayItem item = list.get(position);

            if (holder instanceof DateViewHolder) {

                ((DateViewHolder) holder).binding.txtDateLabel.setText(item.dateLabel);

            } else if (holder instanceof OutgoingViewHolder) {

                ItemChatMessageOutgoingBinding binding = ((OutgoingViewHolder) holder).binding;

                binding.txtOutgoingMessage.setText(item.message.getContent());

                binding.txtOutgoingTime.setText(formatMessageTime(item.message.getTimestamp()));

            } else if (holder instanceof IncomingViewHolder) {

                ItemChatMessageIncomingBinding binding = ((IncomingViewHolder) holder).binding;

                binding.txtIncomingMessage.setText(item.message.getContent());
                binding.txtIncomingTime.setText(formatMessageTime(item.message.getTimestamp()));

                binding.layoutIncomingBubble.setAlpha(1f);
                binding.txtIncomingMessage.setAlpha(1f);
                binding.txtIncomingTime.setAlpha(1f);

                if (item.message.isRead()) {
                    binding.layoutIncomingBubble.setBackgroundResource(R.drawable.bg_incoming_bubble_read);
                    binding.txtIncomingMessage.setTextColor(Color.parseColor("#3E4D44"));
                    binding.txtIncomingMessage.setTypeface(Typeface.create(binding.txtIncomingMessage.getTypeface(), Typeface.NORMAL));
                    binding.txtIncomingTime.setTextColor(Color.parseColor("#8A958C"));
                } else {
                    binding.layoutIncomingBubble.setBackgroundResource(R.drawable.bg_incoming_bubble);
                    binding.txtIncomingMessage.setTextColor(Color.parseColor("#2F3D34"));
                    binding.txtIncomingMessage.setTypeface(Typeface.create(binding.txtIncomingMessage.getTypeface(), Typeface.BOLD));
                    binding.txtIncomingTime.setTextColor(Color.parseColor("#6B776E"));
                }

                if (item.showAvatar) {

                    binding.cardIncomingAvatar.setVisibility(View.VISIBLE);

                    if (partnerAvatar != null && !partnerAvatar.isEmpty()) {

                        ImageUtils.loadImage(binding.getRoot().getContext(), binding.imgIncomingAvatar, partnerAvatar, R.drawable.imv_avatar);

                    } else {

                        binding.imgIncomingAvatar.setImageResource(R.drawable.imv_avatar);

                    }

                } else {

                    binding.cardIncomingAvatar.setVisibility(View.INVISIBLE);

                }

            }

        }



        @Override

        public int getItemCount() {

            return list != null ? list.size() : 0;

        }



        class DateViewHolder extends RecyclerView.ViewHolder {

            final ItemChatDateHeaderBinding binding;



            DateViewHolder(ItemChatDateHeaderBinding binding) {

                super(binding.getRoot());

                this.binding = binding;

            }

        }



        class IncomingViewHolder extends RecyclerView.ViewHolder {

            final ItemChatMessageIncomingBinding binding;



            IncomingViewHolder(ItemChatMessageIncomingBinding binding) {

                super(binding.getRoot());

                this.binding = binding;

            }

        }



        class OutgoingViewHolder extends RecyclerView.ViewHolder {

            final ItemChatMessageOutgoingBinding binding;



            OutgoingViewHolder(ItemChatMessageOutgoingBinding binding) {

                super(binding.getRoot());

                this.binding = binding;

            }

        }

    }

}


