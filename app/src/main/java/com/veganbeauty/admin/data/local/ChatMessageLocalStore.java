package com.veganbeauty.admin.data.local;

import android.content.Context;

import com.veganbeauty.admin.data.remote.ChatMessage;

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

public final class ChatMessageLocalStore {

    private static final String FILE_NAME = "community_message.json";

    private ChatMessageLocalStore() {
    }

    public static List<ChatMessage> load(Context context) {
        List<ChatMessage> list = new ArrayList<>();
        try {
            File localFile = new File(context.getFilesDir(), FILE_NAME);
            if (!localFile.exists()) {
                return list;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String jsonString = sb.toString();
            if (jsonString.trim().isEmpty()) {
                return list;
            }
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (!obj.has("senderId") && !obj.has("content")) {
                    continue;
                }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void saveMerged(Context context, List<ChatMessage> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        List<ChatMessage> merged = merge(load(context), updates);
        write(context, merged);
    }

    public static void saveAll(Context context, List<ChatMessage> messages) {
        write(context, messages != null ? messages : Collections.emptyList());
    }

    public static List<ChatMessage> merge(List<ChatMessage> local, List<ChatMessage> remote) {
        Map<String, ChatMessage> map = new HashMap<>();
        if (local != null) {
            for (ChatMessage msg : local) {
                if (msg != null && msg.getId() != null && !msg.getId().isEmpty()) {
                    map.put(msg.getId(), msg);
                }
            }
        }
        if (remote != null) {
            for (ChatMessage msg : remote) {
                if (msg != null && msg.getId() != null && !msg.getId().isEmpty()) {
                    ChatMessage existing = map.get(msg.getId());
                    if (existing != null && existing.isRead() && !msg.isRead()) {
                        msg.setRead(true);
                    }
                    map.put(msg.getId(), msg);
                }
            }
        }
        List<ChatMessage> merged = new ArrayList<>(map.values());
        Collections.sort(merged, (o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
        return merged;
    }

    private static void write(Context context, List<ChatMessage> messages) {
        try {
            File localFile = new File(context.getFilesDir(), FILE_NAME);
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
}
