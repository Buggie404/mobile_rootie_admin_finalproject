package com.veganbeauty.admin.data.local.dao;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.veganbeauty.admin.data.local.entities.BookingEntity;

import java.util.List;

@Dao
public interface BookingDao {

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    LiveData<List<BookingEntity>> getAllLiveData();

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    List<BookingEntity> getAllSync();

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    @Nullable
    BookingEntity getByIdSync(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllSync(List<BookingEntity> bookings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSync(BookingEntity booking);

    @Query("DELETE FROM bookings")
    void clearAllSync();
}
