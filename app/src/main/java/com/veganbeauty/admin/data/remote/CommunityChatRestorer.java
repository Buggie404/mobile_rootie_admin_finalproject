package com.veganbeauty.admin.data.remote;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Restores missing {@code chat_rootie_vn_{userId}} conversations on Firestore.
 * Uses merge-only writes so existing chats are never deleted.
 */
public final class CommunityChatRestorer {

    private static final String TAG = "CommunityChatRestorer";

    private static final Set<String> SKIP_USER_IDS = new HashSet<>(Arrays.asList(
            "rootie_vn",
            "72400102",
            "86237409",
            "75675216",
            "39751498"
    ));

    private static final String[] CUSTOMER_OPENERS = {
            "Shop ơi, mình muốn hỏi về serum vitamin C ạ?",
            "Dạ shop tư vấn giúp em kem chống nắm vegan được không ạ?",
            "Đơn hàng của mình khi nào giao vậy shop?",
            "Serum nghệ Hưng Yên dùng sáng hay tối vậy ạ?",
            "Da mình bị mụn ẩn, shop gợi ý routine giúp em với ạ?",
            "Shop còn voucher cho đơn spa không ạ?",
            "Mình muốn đặt lịch soi da, shop hỗ trợ giúp em nhé!"
    };

    private static final String[] ADMIN_REPLIES = {
            "Chào bạn! Rootie rất vui được hỗ trợ bạn hôm nay ạ.",
            "Dạ được ạ, bạn cho shop biết thêm loại da hiện tại nhé!",
            "Shop đã nhận tin nhắn và sẽ phản hồi chi tiết cho bạn ngay ạ.",
            "Cảm ơn bạn đã tin tưởng Rootie, shop tư vấn thêm cho bạn nhé!"
    };

    private CommunityChatRestorer() {
    }

    public static int restoreMissingRootieChats(Context context, FirebaseService firebaseService) {
        if (context == null || firebaseService == null) {
            return 0;
        }
        int created = 0;
        try {
            Set<String> existingCustomerIds = new HashSet<>(firebaseService.listRootieChatCustomerIds());
            List<CustomerSeed> customers = loadCustomersFromAssets(context);
            for (CustomerSeed customer : customers) {
                if (existingCustomerIds.contains(customer.userId)) {
                    continue;
                }
                if (firebaseService.ensureRootieChatIfMissing(
                        customer.userId,
                        customer.name,
                        customer.avatar,
                        pickOpener(customer.userId),
                        pickReply(customer.userId)
                )) {
                    created++;
                }
            }
            if (created > 0) {
                Log.i(TAG, "Created " + created + " missing rootie_vn chats");
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreMissingRootieChats failed", e);
        }
        return created;
    }

    private static String pickOpener(String userId) {
        int index = Math.abs(userId.hashCode()) % CUSTOMER_OPENERS.length;
        return CUSTOMER_OPENERS[index];
    }

    private static String pickReply(String userId) {
        int index = Math.abs((userId + "_reply").hashCode()) % ADMIN_REPLIES.length;
        return ADMIN_REPLIES[index];
    }

    private static List<CustomerSeed> loadCustomersFromAssets(Context context) throws Exception {
        List<CustomerSeed> customers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("users.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray users = new JSONArray(sb.toString().replace("\uFEFF", ""));
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                String userId = user.optString("user_id", "").trim();
                if (userId.isEmpty() || SKIP_USER_IDS.contains(userId)) {
                    continue;
                }
                if (user.optBoolean("brand", false)) {
                    continue;
                }
                if ("rootie_vn".equalsIgnoreCase(user.optString("username", ""))) {
                    continue;
                }
                String name = user.optString("full_name", user.optString("username", "Khách hàng"));
                String avatar = user.optString("avatar", user.optString("primary_image", ""));
                customers.add(new CustomerSeed(userId, name, avatar));
            }
        }
        return customers;
    }

    private static final class CustomerSeed {
        final String userId;
        final String name;
        final String avatar;

        CustomerSeed(String userId, String name, String avatar) {
            this.userId = userId;
            this.name = name;
            this.avatar = avatar;
        }
    }
}
