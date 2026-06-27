package com.veganbeauty.admin.data.remote;

import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.local.entities.KeyIngredient;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.data.local.entities.ProductEntity;

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

public class FirebaseService {

    private final FirebaseFirestore db;

    public FirebaseService() {
        FirebaseFirestore firestore = null;
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseService", "Failed to initialize FirebaseFirestore", e);
        }
        this.db = firestore;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    private Long toNullableLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private float toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        return 0f;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List) {
            List<?> rawList = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj != null) {
                    result.add(obj.toString());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private List<KeyIngredient> toKeyIngredientsList(Object value) {
        List<KeyIngredient> result = new ArrayList<>();
        if (value instanceof List) {
            List<?> rawList = (List<?>) value;
            for (Object item : rawList) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Object nameObj = map.get("name");
                    Object descObj = map.get("description");
                    KeyIngredient keyIng = new KeyIngredient();
                    keyIng.setName(nameObj != null ? nameObj.toString() : "");
                    keyIng.setDescription(descObj != null ? descObj.toString() : "");
                    result.add(keyIng);
                }
            }
        }
        return result;
    }

    private String joinListOrToString(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(item.toString());
                }
            }
            return sb.toString();
        }
        return value != null ? value.toString() : "";
    }

    public List<ProductEntity> fetchAllProducts() {
        if (db == null) {
            return Collections.emptyList();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("products").get());
            List<ProductEntity> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    List<String> albumList = toStringList(doc.get("album"));
                    List<KeyIngredient> keyIngredientsList = toKeyIngredientsList(doc.get("keyIngredients"));
                    List<String> detailedList = toStringList(doc.get("detailedIngredients"));
                    List<String> idealList = toStringList(doc.get("idealFor"));
                    List<String> benefitsList = toStringList(doc.get("benefits"));

                    String categoryIdsStr = joinListOrToString(doc.get("categoryId"));
                    String subcategoryStr = joinListOrToString(doc.get("subcategory"));

                    long price = toLong(doc.get("price"));
                    Long originalPrice = toNullableLong(doc.get("originalPrice"));
                    int stock = toInt(doc.get("stock"));
                    int sold = toInt(doc.get("sold"));

                    Object isNewRaw = doc.get("isNew");
                    if (isNewRaw == null) {
                        isNewRaw = doc.get("newProduct");
                    }
                    boolean isNew = toBoolean(isNewRaw);
                    boolean isHidden = toBoolean(doc.get("isHidden"));
                    float rating = toFloat(doc.get("rating"));

                    ProductEntity product = new ProductEntity();
                    product.setId(doc.getId());
                    product.setName(doc.getString("name") != null ? doc.getString("name") : "");
                    product.setSku(doc.getString("sku") != null ? doc.getString("sku") : "");
                    product.setBarcode(doc.getString("barcode") != null ? doc.getString("barcode") : "");
                    product.setPrice(price);
                    product.setOriginalPrice(originalPrice);
                    product.setCategory(doc.getString("category") != null ? doc.getString("category") : "");
                    product.setSubcategory(subcategoryStr);
                    product.setBrand(doc.getString("brand") != null ? doc.getString("brand") : "");
                    product.setStock(stock);
                    product.setDescription(doc.getString("description") != null ? doc.getString("description") : "");
                    product.setMainImage(doc.getString("mainImage") != null ? doc.getString("mainImage") : "");
                    product.setSuitableFor(doc.getString("suitableFor") != null ? doc.getString("suitableFor") : "");
                    product.setOrigin(doc.getString("origin") != null ? doc.getString("origin") : "");
                    product.setExpiryDate(doc.getString("expiryDate") != null ? doc.getString("expiryDate") : "");
                    product.setNew(isNew);
                    product.setCategoryIds(categoryIdsStr);
                    product.setAlbum(albumList);
                    product.setMainIngredientsSummary(doc.getString("mainIngredientsSummary") != null ? doc.getString("mainIngredientsSummary") : "");
                    product.setAllergyInformation(doc.getString("allergyInformation") != null ? doc.getString("allergyInformation") : "");
                    product.setKeyIngredients(keyIngredientsList);
                    product.setDetailedIngredients(detailedList);
                    product.setStoryDescription(doc.getString("storyDescription") != null ? doc.getString("storyDescription") : "");
                    product.setStoryImage(doc.getString("storyImage") != null ? doc.getString("storyImage") : "");
                    product.setIngredientsImage(doc.getString("ingredientsImage") != null ? doc.getString("ingredientsImage") : "");
                    product.setUsageMedia(doc.getString("usageMedia") != null ? doc.getString("usageMedia") : "");
                    product.setIdealFor(idealList);
                    product.setBenefits(benefitsList);
                    product.setUsage(doc.getString("usage") != null ? doc.getString("usage") : "");
                    product.setUsageAmount(doc.getString("usageAmount") != null ? doc.getString("usageAmount") : "");
                    product.setTexture(doc.getString("texture") != null ? doc.getString("texture") : "");
                    product.setScent(doc.getString("scent") != null ? doc.getString("scent") : "");
                    product.setNotes(doc.getString("notes") != null ? doc.getString("notes") : "");
                    product.setRating(rating);
                    product.setSold(sold);
                    product.setHidden(isHidden);
                    list.add(product);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean saveProduct(ProductEntity product) {
        if (db == null) {
            return false;
        }
        try {
            List<Map<String, String>> keyIngredientsMap = new ArrayList<>();
            for (KeyIngredient ki : product.getKeyIngredients()) {
                Map<String, String> m = new HashMap<>();
                m.put("name", ki.getName());
                m.put("description", ki.getDescription());
                keyIngredientsMap.add(m);
            }

            List<String> catIds = new ArrayList<>();
            for (String s : product.getCategoryIds().split(",")) {
                if (!s.trim().isEmpty()) {
                    catIds.add(s.trim());
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", product.getName());
            data.put("sku", product.getSku());
            data.put("barcode", product.getBarcode());
            data.put("price", product.getPrice());
            data.put("originalPrice", product.getOriginalPrice());
            data.put("category", product.getCategory());
            data.put("subcategory", product.getSubcategory());
            data.put("brand", product.getBrand());
            data.put("stock", product.getStock());
            data.put("description", product.getDescription());
            data.put("mainImage", product.getMainImage());
            data.put("suitableFor", product.getSuitableFor());
            data.put("origin", product.getOrigin());
            data.put("expiryDate", product.getExpiryDate());
            data.put("isNew", product.isNew());
            data.put("categoryId", catIds);
            data.put("album", product.getAlbum());
            data.put("mainIngredientsSummary", product.getMainIngredientsSummary());
            data.put("allergyInformation", product.getAllergyInformation());
            data.put("keyIngredients", keyIngredientsMap);
            data.put("detailedIngredients", product.getDetailedIngredients());
            data.put("storyDescription", product.getStoryDescription());
            data.put("storyImage", product.getStoryImage());
            data.put("ingredientsImage", product.getIngredientsImage());
            data.put("usageMedia", product.getUsageMedia());
            data.put("idealFor", product.getIdealFor());
            data.put("benefits", product.getBenefits());
            data.put("usage", product.getUsage());
            data.put("usageAmount", product.getUsageAmount());
            data.put("texture", product.getTexture());
            data.put("scent", product.getScent());
            data.put("notes", product.getNotes());
            data.put("rating", product.getRating());
            data.put("sold", product.getSold());
            data.put("isHidden", product.isHidden());

            Tasks.await(db.collection("products").document(product.getId()).set(data));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteProduct(String productId) {
        if (db == null) {
            return false;
        }
        try {
            Tasks.await(db.collection("products").document(productId).delete());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface OrderListener {
        void onOrdersUpdated(List<OrderEntity> orders);
    }

    public com.google.firebase.firestore.ListenerRegistration listenToOrders(final OrderListener listener) {
        if (db == null) {
            return null;
        }
        return db.collection("orders").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }
            if (snapshot != null) {
                List<OrderEntity> list = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    try {
                        OrderEntity oe = parseOrderFromDoc(doc);
                        list.add(oe);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                listener.onOrdersUpdated(list);
            }
        });
    }

    public OrderEntity parseOrderFromDoc(DocumentSnapshot doc) {
        List<OrderItem> orderItems = new ArrayList<>();
        Object itemsRawObj = doc.get("items");
        if (itemsRawObj instanceof List) {
            List<?> itemsRaw = (List<?>) itemsRawObj;
            for (Object item : itemsRaw) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Object prodId = map.get("productId");
                    Object prodName = map.get("productName");
                    Object prodImg = map.get("productImage");
                    Object qty = map.get("quantity");
                    Object prc = map.get("price");

                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(prodId != null ? prodId.toString() : "");
                    orderItem.setProductName(prodName != null ? prodName.toString() : "");
                    orderItem.setProductImage(prodImg != null ? prodImg.toString() : "");
                    orderItem.setQuantity(qty != null ? toInt(qty) : 0);
                    orderItem.setPrice(prc != null ? toLong(prc) : 0L);
                    orderItems.add(orderItem);
                }
            }
        }

        String orderId = doc.getId();
        String storeName = doc.getString("storeName");
        if (storeName == null) {
            int lastDigit = 0;
            try {
                lastDigit = Integer.parseInt(orderId.substring(orderId.length() - 1));
            } catch (Exception ignored) {
            }
            if (lastDigit % 2 == 1) {
                storeName = "Cửa hàng mỹ phẩm Rootie - Cơ sở 1";
            } else {
                storeName = "Cửa hàng mỹ phẩm Rootie - Cơ sở 5";
            }
        }

        String storeID = doc.getString("storeID");
        if (storeID == null) {
            storeID = doc.getString("storeId");
        }
        if (storeID == null) {
            if (storeName.toLowerCase().contains("cơ sở 1")) {
                storeID = "CH001";
            } else {
                storeID = "CH005";
            }
        }

        OrderEntity oe = new OrderEntity();
        oe.setOrderId(orderId);
        oe.setUserId(doc.getString("userId") != null ? doc.getString("userId") : "");
        oe.setOrderDate(doc.getString("orderDate") != null ? doc.getString("orderDate") : "");
        oe.setOrderTime(doc.getString("orderTime") != null ? doc.getString("orderTime") : "");
        oe.setStatus(doc.getString("status") != null ? doc.getString("status") : "");
        oe.setTotalAmount(toLong(doc.get("totalAmount")));
        oe.setItems(orderItems);
        oe.setShippingName(doc.getString("shippingName") != null ? doc.getString("shippingName") : "");
        oe.setShippingPhone(doc.getString("shippingPhone") != null ? doc.getString("shippingPhone") : "");
        oe.setShippingAddress(doc.getString("shippingAddress") != null ? doc.getString("shippingAddress") : "");
        oe.setShippingCost(toLong(doc.get("shippingCost")));
        oe.setVoucherDiscount(toLong(doc.get("voucherDiscount")));
        oe.setPaymentMethod(doc.getString("paymentMethod") != null ? doc.getString("paymentMethod") : "");
        oe.setExpectedDeliveryTime(doc.getString("expectedDeliveryTime"));
        oe.setHasReview(toBoolean(doc.get("hasReview")));
        oe.setReviewStars(toInt(doc.get("reviewStars")));
        oe.setReviewText(doc.getString("reviewText"));
        oe.setReviewImage(doc.getString("reviewImage"));
        oe.setAnonymous(toBoolean(doc.get("isAnonymous")));
        oe.setRecommendToFriends(toBoolean(doc.get("recommendToFriends")));
        oe.setStoreName(storeName);
        oe.setStoreID(storeID);
        return oe;
    }

    public List<OrderEntity> fetchAllOrders() {
        if (db == null) {
            return Collections.emptyList();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("orders").get());
            List<OrderEntity> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    OrderEntity oe = parseOrderFromDoc(doc);
                    list.add(oe);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean updateOrderStatus(String orderId, String status) {
        if (db == null) {
            return false;
        }
        try {
            Tasks.await(db.collection("orders").document(orderId).update("status", status));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<BookingEntity> fetchAllBookings() {
        if (db == null) {
            return Collections.emptyList();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("bookings").get());
            List<BookingEntity> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    String rawStoreName = doc.getString("storeName") != null ? doc.getString("storeName") : "";
                    String rawStoreAddress = doc.getString("storeAddress") != null ? doc.getString("storeAddress") : "";
                    String storeID = doc.getString("storeID");
                    if (storeID == null) {
                        storeID = doc.getString("storeId");
                    }
                    if (storeID == null) {
                        if (rawStoreName.toLowerCase().contains("cơ sở 1") || rawStoreAddress.toLowerCase().contains("minh khai")) {
                            storeID = "CH001";
                        } else if (rawStoreName.toLowerCase().contains("cơ sở 5") || rawStoreAddress.toLowerCase().contains("hoàng văn thụ")) {
                            storeID = "CH005";
                        } else {
                            storeID = "";
                        }
                    }

                    BookingEntity be = new BookingEntity();
                    be.setId(doc.getId());
                    be.setUserId(doc.getString("userId") != null ? doc.getString("userId") : "");
                    be.setUserName(doc.getString("userName") != null ? doc.getString("userName") : "");
                    be.setUserPhone(doc.getString("userPhone") != null ? doc.getString("userPhone") : "");
                    be.setUserEmail(doc.getString("userEmail") != null ? doc.getString("userEmail") : "");
                    be.setServiceName(doc.getString("serviceName") != null ? doc.getString("serviceName") : "");
                    be.setDateDisplay(doc.getString("dateDisplay") != null ? doc.getString("dateDisplay") : "");
                    be.setMonthDisplay(doc.getString("monthDisplay") != null ? doc.getString("monthDisplay") : "");
                    be.setDayOfWeek(doc.getString("dayOfWeek") != null ? doc.getString("dayOfWeek") : "");
                    be.setTime(doc.getString("time") != null ? doc.getString("time") : "");
                    be.setDuration(doc.getString("duration") != null ? doc.getString("duration") : "");
                    be.setStoreName(rawStoreName);
                    be.setStoreAddress(rawStoreAddress);
                    be.setStorePhone(doc.getString("storePhone") != null ? doc.getString("storePhone") : "");
                    be.setStoreImage(doc.getString("storeImage") != null ? doc.getString("storeImage") : "");
                    be.setStoreID(storeID);
                    be.setNote(doc.getString("note") != null ? doc.getString("note") : "");
                    be.setStatus(doc.getString("status") != null ? doc.getString("status") : "");
                    be.setCreatedAt(doc.getString("createdAt") != null ? doc.getString("createdAt") : "");
                    be.setConsultantName(doc.getString("consultantName") != null ? doc.getString("consultantName") : "");
                    be.setCancelReason(doc.getString("cancelReason") != null ? doc.getString("cancelReason") : "");
                    list.add(be);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean updateBookingStatus(String bookingId, String status, String cancelReason) {
        if (db == null) {
            return false;
        }
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("cancelReason", cancelReason);
            Tasks.await(db.collection("bookings").document(bookingId).update(updates));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadBooking(BookingEntity booking) {
        if (db == null) {
            return false;
        }
        try {
            Map<String, Object> bookingMap = new HashMap<>();
            bookingMap.put("id", booking.getId());
            bookingMap.put("userId", booking.getUserId());
            bookingMap.put("userName", booking.getUserName());
            bookingMap.put("userPhone", booking.getUserPhone());
            bookingMap.put("userEmail", booking.getUserEmail());
            bookingMap.put("serviceName", booking.getServiceName());
            bookingMap.put("dateDisplay", booking.getDateDisplay());
            bookingMap.put("monthDisplay", booking.getMonthDisplay());
            bookingMap.put("dayOfWeek", booking.getDayOfWeek());
            bookingMap.put("time", booking.getTime());
            bookingMap.put("duration", booking.getDuration());
            bookingMap.put("storeName", booking.getStoreName());
            bookingMap.put("storeAddress", booking.getStoreAddress());
            bookingMap.put("storePhone", booking.getStorePhone());
            bookingMap.put("storeImage", booking.getStoreImage());
            bookingMap.put("status", booking.getStatus());
            bookingMap.put("note", booking.getNote());
            bookingMap.put("createdAt", booking.getCreatedAt());
            bookingMap.put("consultantName", booking.getConsultantName());
            bookingMap.put("cancelReason", booking.getCancelReason());
            bookingMap.put("storeID", booking.getStoreID());
            bookingMap.put("storeId", booking.getStoreID());

            Tasks.await(db.collection("bookings").document(booking.getId()).set(bookingMap));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<CustomerEntity> fetchAllCustomers() {
        if (db == null) {
            return Collections.emptyList();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("users").get());
            List<CustomerEntity> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    CustomerEntity customer = new CustomerEntity();
                    customer.setId(doc.getId());
                    customer.setName(doc.getString("username") != null ? doc.getString("username") : "");
                    customer.setEmail(doc.getString("email") != null ? doc.getString("email") : "");
                    customer.setPhone(doc.getString("phone") != null ? doc.getString("phone") : "");
                    customer.setAddress(doc.getString("address") != null ? doc.getString("address") : "");
                    customer.setAvatar(doc.getString("avatar") != null ? doc.getString("avatar") : "");
                    customer.setSpending(toLong(doc.get("spending")));
                    customer.setTier(doc.getString("tier") != null ? doc.getString("tier") : "Thường");
                    customer.setLastActive(doc.getString("last_active") != null ? doc.getString("last_active") : "");
                    customer.setNotes(doc.getString("notes") != null ? doc.getString("notes") : "");
                    customer.setRole(doc.getString("role") != null ? doc.getString("role") : "customer");
                    customer.setBirthday(doc.getString("birthday") != null ? doc.getString("birthday") : "");
                    customer.setRegion(doc.getString("region") != null ? doc.getString("region") : "");
                    customer.setJoinYear(doc.get("join_year") != null ? toInt(doc.get("join_year")) : 1);
                    customer.setOrderCount(doc.get("order_count") != null ? toInt(doc.get("order_count")) : 0);
                    customer.setRecentPurchase(doc.getString("recent_purchase") != null ? doc.getString("recent_purchase") : "");
                    customer.setSpendingYear(toLong(doc.get("spending_year")));
                    customer.setSpendingMonth(toLong(doc.get("spending_month")));
                    customer.setPoints(doc.get("points") != null ? toInt(doc.get("points")) : 0);
                    list.add(customer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private long parseIsoString(String isoStr) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(isoStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    private String getCurrentTimeString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    public List<ChatMessage> fetchChatMessages() {
        if (db == null) {
            return Collections.emptyList();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("community_message").get());
            List<ChatMessage> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                try {
                    Object membersRaw = doc.get("members");
                    List<String> members = toStringList(membersRaw);
                    if (!members.contains("rootie_vn")) {
                        continue;
                    }

                    String userId = "";
                    for (String m : members) {
                        if (!m.equals("rootie_vn")) {
                            userId = m;
                            break;
                        }
                    }
                    if (userId.isEmpty()) {
                        continue;
                    }

                    Object memberInfoRaw = doc.get("member_info");
                    String username = "User";
                    String avatar = "";
                    if (memberInfoRaw instanceof Map) {
                        Map<?, ?> memberInfo = (Map<?, ?>) memberInfoRaw;
                        Object partnerInfoObj = memberInfo.get(userId);
                        if (partnerInfoObj instanceof Map) {
                            Map<?, ?> partnerInfo = (Map<?, ?>) partnerInfoObj;
                            Object nameObj = partnerInfo.get("name");
                            Object avatarObj = partnerInfo.get("avatar");
                            if (nameObj != null) username = nameObj.toString();
                            if (avatarObj != null) avatar = avatarObj.toString();
                        }
                    }

                    Object messagesRawObj = doc.get("messages");
                    if (messagesRawObj instanceof List) {
                        List<?> messagesRaw = (List<?>) messagesRawObj;
                        for (Object msgObj : messagesRaw) {
                            if (msgObj instanceof Map) {
                                Map<?, ?> msgMap = (Map<?, ?>) msgObj;
                                String senderId = msgMap.get("sender_id") != null ? msgMap.get("sender_id").toString() : "";
                                String text = msgMap.get("text") != null ? msgMap.get("text").toString() : "";
                                String sentAt = msgMap.get("sent_at") != null ? msgMap.get("sent_at").toString() : "";

                                boolean isAgent = senderId.equals("rootie_vn");
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
                                list.add(msg);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean saveChatMessage(ChatMessage message) {
        if (db == null) {
            Log.e("FirebaseService", "db is null!");
            return false;
        }

        String customerId = message.getSenderId().equals("rootie_vn") ? message.getReceiverId() : message.getSenderId();
        if (customerId == null || customerId.trim().isEmpty()) {
            Log.e("FirebaseService", "customerId is blank! senderId=" + message.getSenderId() + ", receiverId=" + message.getReceiverId());
            return false;
        }

        String customerName = message.getSenderId().equals("rootie_vn") ? message.getReceiverName() : message.getSenderName();
        String customerAvatar = message.getSenderId().equals("rootie_vn") ? message.getReceiverAvatar() : message.getSenderAvatar();

        String convId = "chat_rootie_vn_" + customerId;
        String timeStr = getCurrentTimeString();
        String msgId = (message.getId() != null && !message.getId().trim().isEmpty()) ? message.getId() : ("m_" + UUID.randomUUID().toString().substring(0, 8));

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("id", msgId);
        msgMap.put("sender_id", message.getSenderId());
        msgMap.put("text", message.getContent());
        msgMap.put("sent_at", timeStr);
        msgMap.put("delivered_at", timeStr);
        msgMap.put("seen_at", message.getSenderId().equals("rootie_vn") ? null : timeStr);

        Log.d("FirebaseService", "Saving message: convId=" + convId + ", msgId=" + msgId + ", sender=" + message.getSenderId() + ", text=" + message.getContent());
        com.google.firebase.firestore.DocumentReference docRef = db.collection("community_message").document(convId);

        Map<String, Object> rootieInfo = new HashMap<>();
        rootieInfo.put("name", "Rootie VietNam");
        rootieInfo.put("avatar", "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png");

        Map<String, Object> custInfo = new HashMap<>();
        custInfo.put("name", customerName);
        custInfo.put("avatar", customerAvatar);

        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("rootie_vn", rootieInfo);
        memberInfo.put(customerId, custInfo);

        List<String> members = new ArrayList<>();
        members.add(customerId);
        members.add("rootie_vn");

        Map<String, Object> data = new HashMap<>();
        data.put("id", convId);
        data.put("chat_type", "private");
        data.put("members", members);
        data.put("member_info", memberInfo);
        data.put("last_message", message.getContent());
        data.put("last_message_at", timeStr);
        data.put("updated_at", timeStr);
        data.put("unread_by", FieldValue.arrayUnion(customerId));
        data.put("messages", FieldValue.arrayUnion(msgMap));

        try {
            Tasks.await(docRef.set(data, SetOptions.merge()));
            Log.d("FirebaseService", "Successfully saved message: convId=" + convId);
            return true;
        } catch (Exception e) {
            Log.e("FirebaseService", "saveChatMessage failed: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean markConversationAsRead(String customerId) {
        if (db == null) {
            return false;
        }
        String convId = "chat_rootie_vn_" + customerId;
        com.google.firebase.firestore.DocumentReference docRef = db.collection("community_message").document(convId);

        try {
            DocumentSnapshot snapshot = Tasks.await(docRef.get());
            if (snapshot.exists()) {
                String timeStr = getCurrentTimeString();
                Object unreadByRaw = snapshot.get("unread_by");
                List<String> unreadBy = toStringList(unreadByRaw);
                List<String> updatedUnread = new ArrayList<>();
                for (String s : unreadBy) {
                    if (!s.equals("rootie_vn")) {
                        updatedUnread.add(s);
                    }
                }

                Object messagesRawObj = snapshot.get("messages");
                List<Map<String, Object>> updatedMessages = new ArrayList<>();
                if (messagesRawObj instanceof List) {
                    List<?> messagesRaw = (List<?>) messagesRawObj;
                    for (Object msgObj : messagesRaw) {
                        if (msgObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> msgMap = (Map<String, Object>) msgObj;
                            String senderId = msgMap.get("sender_id") != null ? msgMap.get("sender_id").toString() : "";
                            if (!senderId.equals("rootie_vn") && msgMap.get("seen_at") == null) {
                                Map<String, Object> newMap = new HashMap<>(msgMap);
                                newMap.put("seen_at", timeStr);
                                updatedMessages.add(newMap);
                            } else {
                                updatedMessages.add(msgMap);
                            }
                        }
                    }
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("unread_by", updatedUnread);
                updates.put("messages", updatedMessages);

                Tasks.await(docRef.update(updates));
                return true;
            } else {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
