package com.veganbeauty.admin.data.repository;

import androidx.lifecycle.LiveData;
import com.veganbeauty.admin.data.local.dao.OrderDao;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;

import java.util.List;

public class OrderRepository {
    private final OrderDao orderDao;
    private final FirebaseService firebaseService;

    public OrderRepository(OrderDao orderDao, FirebaseService firebaseService) {
        this.orderDao = orderDao;
        this.firebaseService = firebaseService;
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
}
