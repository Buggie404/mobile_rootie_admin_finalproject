package com.veganbeauty.admin.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.veganbeauty.admin.data.local.entities.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Query("SELECT * FROM products")
    LiveData<List<ProductEntity>> getAllLiveData();

    @Query("SELECT * FROM products")
    List<ProductEntity> getAllSync();

    @Query("SELECT * FROM products WHERE id = :id")
    @Nullable
    ProductEntity getByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<ProductEntity> products);

    @Update
    void updateSync(ProductEntity product);

    @Delete
    void deleteSync(ProductEntity product);
}
