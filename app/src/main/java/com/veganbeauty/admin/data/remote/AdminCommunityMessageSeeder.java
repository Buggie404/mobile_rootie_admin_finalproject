package com.veganbeauty.admin.data.remote;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Restores {@code chat_rootie_vn_*} conversations from bundled assets to Firestore.
 */
public final class AdminCommunityMessageSeeder {

    private static final String TAG = "AdminCommunityMsgSeed";
    private static final String ASSET_FILE = "community_message.json";

    private AdminCommunityMessageSeeder() {
    }

    public static List<ChatMessage> loadMessagesFromAssets(Context context) {
        List<ChatMessage> messages = new ArrayList<>();
        if (context == null) {
            return messages;
        }
        try {
            String json = readAsset(context, ASSET_FILE);
            if (json == null || json.trim().isEmpty()) {
                return messages;
            }
            JSONArray conversations = new JSONArray(json);
            for (int i = 0; i < conversations.length(); i++) {
                messages.addAll(parseConversationMessages(conversations.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadMessagesFromAssets failed", e);
        }
        return messages;
    }

    public static int restoreRootieChatsFromAssets(Context context, FirebaseService firebaseService) {
        if (context == null || firebaseService == null) {
            return 0;
        }
        int restored = 0;
        try {
            String json = readAsset(context, ASSET_FILE);
            if (json == null || json.trim().isEmpty()) {
                return 0;
            }
            JSONArray conversations = new JSONArray(json);
            for (int i = 0; i < conversations.length(); i++) {
                JSONObject conversation = conversations.getJSONObject(i);
                if (!hasRootieMember(conversation)) {
                    continue;
                }
                String docId = conversation.optString("id", "");
                if (docId.isEmpty()) {
                    continue;
                }
                Map<String, Object> payload = jsonObjectToMap(conversation);
                if (firebaseService.upsertCommunityMessageDocument(docId, payload)) {
                    restored++;
                }
            }
            if (restored > 0) {
                Log.i(TAG, "Restored " + restored + " rootie_vn conversations from assets");
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreRootieChatsFromAssets failed", e);
        }
        return restored;
    }

    private static boolean hasRootieMember(JSONObject conversation) {
        JSONArray members = conversation.optJSONArray("members");
        if (members == null) {
            return false;
        }
        for (int i = 0; i < members.length(); i++) {
            if ("rootie_vn".equals(members.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static List<ChatMessage> parseConversationMessages(JSONObject conversation) throws Exception {
        List<ChatMessage> list = new ArrayList<>();
        if (!hasRootieMember(conversation)) {
            return list;
        }

        String userId = "";
        JSONArray members = conversation.getJSONArray("members");
        for (int i = 0; i < members.length(); i++) {
            String member = members.optString(i);
            if (!"rootie_vn".equals(member)) {
                userId = member;
                break;
            }
        }
        if (userId.isEmpty()) {
            return list;
        }

        String username = "User";
        String avatar = "";
        JSONObject memberInfo = conversation.optJSONObject("member_info");
        if (memberInfo != null) {
            JSONObject partnerInfo = memberInfo.optJSONObject(userId);
            if (partnerInfo != null) {
                username = partnerInfo.optString("name", username);
                avatar = partnerInfo.optString("avatar", "");
            }
        }

        JSONArray unreadBy = conversation.optJSONArray("unread_by");
        boolean unreadForAdmin = false;
        if (unreadBy != null) {
            for (int i = 0; i < unreadBy.length(); i++) {
                if ("rootie_vn".equals(unreadBy.optString(i))) {
                    unreadForAdmin = true;
                    break;
                }
            }
        }

        JSONArray messagesRaw = conversation.optJSONArray("messages");
        if (messagesRaw == null) {
            return list;
        }

        for (int i = 0; i < messagesRaw.length(); i++) {
            JSONObject msgMap = messagesRaw.getJSONObject(i);
            String senderId = msgMap.optString("sender_id", "");
            String text = msgMap.optString("text", "");
            String sentAt = msgMap.optString("sent_at", "");
            boolean isAgent = "rootie_vn".equals(senderId);
            long timestamp = parseIsoString(sentAt);
            String msgId = msgMap.optString("id", "msg_" + userId + "_" + timestamp);

            ChatMessage msg = new ChatMessage();
            msg.setId(msgId);
            msg.setSenderId(isAgent ? "rootie_vn" : userId);
            msg.setSenderName(isAgent ? "Rootie VietNam" : username);
            msg.setSenderAvatar(isAgent
                    ? "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
                    : avatar);
            msg.setReceiverId(isAgent ? userId : "rootie_vn");
            msg.setReceiverName(isAgent ? username : "Rootie VietNam");
            msg.setReceiverAvatar(isAgent
                    ? avatar
                    : "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png");
            msg.setContent(text);
            msg.setTimestamp(timestamp);
            boolean read = isAgent || !unreadForAdmin || !userId.equals(senderId);
            msg.setRead(read);
            list.add(msg);
        }
        return list;
    }

    private static long parseIsoString(String isoStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(isoStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private static String readAsset(Context context, String fileName) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject object) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> jsonArrayToList(JSONArray array) throws Exception {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            }
            list.add(value);
        }
        return list;
    }
}
