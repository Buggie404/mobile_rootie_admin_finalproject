package com.veganbeauty.admin.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.dao.OrderDao;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderRepository {
    private final OrderDao orderDao;
    private final FirebaseService firebaseService;
    private final RootieAdminDatabase database;

    public OrderRepository(OrderDao orderDao, FirebaseService firebaseService, RootieAdminDatabase database) {
        this.orderDao = orderDao;
        this.firebaseService = firebaseService;
        this.database = database;
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        return orderDao.getAllLiveData();
    }

    public void syncFromFirebase() {
        List<OrderEntity> remoteList = firebaseService.fetchAllOrders();
        if (!remoteList.isEmpty()) {
            orderDao.insertAllSync(remoteList);
        }
    }

    public void checkAndSeedOrders(Context context) {
        if (orderDao.getAllSync().isEmpty()) {
            List<OrderEntity> localOrders = parseOrdersFromAssets(context);
            if (!localOrders.isEmpty()) {
                orderDao.insertAllSync(localOrders);
            }
        }
        syncFromFirebase();
    }

    public List<OrderEntity> getAllOrdersSync() {
        return orderDao.getAllSync();
    }

    private List<OrderEntity> parseOrdersFromAssets(Context context) {
        List<OrderEntity> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("orders.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONArray jsonArray = root.getJSONArray("orders");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONArray itemsRaw = obj.optJSONArray("items");
                List<OrderItem> orderItems = new ArrayList<>();
                if (itemsRaw != null) {
                    for (int j = 0; j < itemsRaw.length(); j++) {
                        JSONObject itemObj = itemsRaw.getJSONObject(j);
                        OrderItem orderItem = new OrderItem();
                        orderItem.setProductId(itemObj.optString("productId", ""));
                        orderItem.setProductName(itemObj.optString("productName", ""));
                        orderItem.setProductImage(itemObj.optString("productImage", ""));
                        orderItem.setQuantity(itemObj.optInt("quantity", 0));
                        orderItem.setPrice(itemObj.optLong("price", 0L));
                        orderItems.add(orderItem);
                    }
                }

                OrderEntity entity = new OrderEntity();
                entity.setOrderId(obj.optString("id", UUID.randomUUID().toString()));
                entity.setUserId(obj.optString("userId", ""));
                entity.setOrderDate(obj.optString("orderDate", ""));
                entity.setOrderTime(obj.optString("orderTime", ""));
                entity.setStatus(obj.optString("status", ""));
                entity.setTotalAmount(obj.optLong("totalAmount", 0L));
                entity.setItems(orderItems);
                entity.setShippingName(obj.optString("shippingName", ""));
                entity.setShippingPhone(obj.optString("shippingPhone", ""));
                entity.setShippingAddress(obj.optString("shippingAddress", ""));
                entity.setShippingCost(obj.optLong("shippingCost", 0L));
                entity.setVoucherDiscount(obj.optLong("voucherDiscount", 0L));
                entity.setPaymentMethod(obj.optString("paymentMethod", ""));
                list.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateOrderStatus(String orderId, String status) {
        boolean success = firebaseService.updateOrderStatus(orderId, status);
        OrderEntity localOrder = orderDao.getByIdSync(orderId);
        if (localOrder != null) {
            localOrder.setStatus(status);
            orderDao.insertSync(localOrder);
            return true;
        } else {
            return success;
        }
    }

    public com.google.firebase.firestore.ListenerRegistration startRealtimeSync() {
        return firebaseService.listenToOrders(orders -> {
            if (orders != null && !orders.isEmpty()) {
                android.util.Log.d("OrderRepository", "startRealtimeSync: Received " + orders.size() + " orders");
                new Thread(() -> {
                    // Update local orders and sync product stock from Firestore
                    for (OrderEntity order : orders) {
                        orderDao.insertSync(order);
                        android.util.Log.d("OrderRepository", "startRealtimeSync: Inserted order " + order.getOrderId());
                        // Fetch the updated products from Firestore to sync local DB stock
                        if (order.getItems() != null) {
                            for (OrderItem item : order.getItems()) {
                                String productId = item.getProductId();
                                if (productId != null && !productId.isEmpty()) {
                                    try {
                                        ProductEntity remoteProduct = firebaseService.fetchProductById(productId);
                                        if (remoteProduct != null) {
                                            database.productDao().insertSync(remoteProduct);
                                            android.util.Log.d("OrderRepository", "startRealtimeSync: Synced product stock from Firestore for " + productId + " to " + remoteProduct.getStock());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }).start();
            } else {
                android.util.Log.d("OrderRepository", "startRealtimeSync: No orders received");
            }
        });
    }
}
