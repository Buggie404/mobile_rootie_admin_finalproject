package com.veganbeauty.admin.features.order;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final OrderRepository repository;
    private final LiveData<List<OrderEntity>> allOrders;
    private final MutableLiveData<String> activeTabStatus = new MutableLiveData<>("ALL"); // ALL, PENDING, PROCESSING, SHIPPING, COMPLETED, CANCELLED
    private final MediatorLiveData<List<OrderEntity>> filteredOrders = new MediatorLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(application);
        repository = new OrderRepository(database.orderDao(), new FirebaseService());
        allOrders = repository.getAllOrders();

        filteredOrders.addSource(allOrders, orders -> updateFilteredOrders());
        filteredOrders.addSource(activeTabStatus, status -> updateFilteredOrders());
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        return allOrders;
    }

    public MutableLiveData<String> getActiveTabStatus() {
        return activeTabStatus;
    }

    public LiveData<List<OrderEntity>> getFilteredOrders() {
        return filteredOrders;
    }

    public void syncFromFirebase() {
        new Thread(() -> {
            try {
                repository.syncFromFirebase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateOrderStatus(String orderId, String status) {
        new Thread(() -> {
            try {
                repository.updateOrderStatus(orderId, status);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateFilteredOrders() {
        List<OrderEntity> orders = allOrders.getValue();
        if (orders == null) {
            orders = new ArrayList<>();
        }
        String statusFilter = activeTabStatus.getValue();
        if (statusFilter == null) {
            statusFilter = "ALL";
        }

        List<OrderEntity> result = new ArrayList<>();
        for (OrderEntity order : orders) {
            String status = order.getStatus();
            if (status == null) status = "";

            switch (statusFilter) {
                case "PENDING":
                    if ("Chờ xử lý".equals(status)) {
                        result.add(order);
                    }
                    break;
                case "PROCESSING":
                    if ("Đang xử lý".equals(status)) {
                        result.add(order);
                    }
                    break;
                case "SHIPPING":
                    if ("Đang giao".equals(status)) {
                        result.add(order);
                    }
                    break;
                case "COMPLETED":
                    if ("Hoàn tất".equals(status)) {
                        result.add(order);
                    }
                    break;
                case "CANCELLED":
                    if ("Đã hủy".equals(status)) {
                        result.add(order);
                    }
                    break;
                default:
                    result.add(order);
                    break;
            }
        }

        filteredOrders.setValue(result);
    }
}
