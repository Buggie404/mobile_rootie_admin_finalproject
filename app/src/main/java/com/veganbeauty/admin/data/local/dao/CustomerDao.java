package com.veganbeauty.admin.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;

import java.util.List;

@Dao
public interface CustomerDao {

    @Query("SELECT * FROM customers")
    LiveData<List<CustomerEntity>> getAllLiveData();

    @Query("SELECT * FROM customers")
    List<CustomerEntity> getAllSync();

    @Query("SELECT * FROM customers WHERE id = :id")
    @Nullable
    CustomerEntity getByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(CustomerEntity customer);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<CustomerEntity> customers);

    @Update
    void updateSync(CustomerEntity customer);

    @Delete
    void deleteSync(CustomerEntity customer);
}
