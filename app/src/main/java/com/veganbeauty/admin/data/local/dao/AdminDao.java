package com.veganbeauty.admin.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.veganbeauty.admin.data.local.entities.AdminEntity;

import java.util.List;

@Dao
public interface AdminDao {

    @Query("SELECT * FROM admins")
    LiveData<List<AdminEntity>> getAllLiveData();

    @Query("SELECT * FROM admins")
    List<AdminEntity> getAllSync();

    @Query("SELECT * FROM admins WHERE username = :username")
    @Nullable
    AdminEntity getByUsernameSync(String username);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(AdminEntity admin);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<AdminEntity> admins);

    @Query("DELETE FROM admins")
    void deleteAllSync();
}
