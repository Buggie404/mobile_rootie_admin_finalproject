package com.veganbeauty.admin.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.veganbeauty.admin.data.local.entities.OrderEntity;

import java.util.List;

@Dao
public interface OrderDao {

    @Query("SELECT * FROM orders")
    LiveData<List<OrderEntity>> getAllLiveData();

    @Query("SELECT * FROM orders")
    List<OrderEntity> getAllSync();

    @Query("SELECT * FROM orders WHERE id = :orderId")
    @Nullable
    OrderEntity getByIdSync(String orderId);

    @Query("SELECT * FROM orders WHERE userId = :userId")
    List<OrderEntity> getOrdersForUserSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(OrderEntity order);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<OrderEntity> orders);

    @Update
    void updateSync(OrderEntity order);

    @Delete
    void deleteSync(OrderEntity order);
}
