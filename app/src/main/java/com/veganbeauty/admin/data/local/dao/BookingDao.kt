package com.veganbeauty.admin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veganbeauty.admin.data.local.entities.BookingEntity

@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun getAllLiveData(): LiveData<List<BookingEntity>>

    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun getAllSync(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    fun getByIdSync(id: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(bookings: List<BookingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE id = :id")
    fun deleteByIdSync(id: String)

    @Query("DELETE FROM bookings")
    fun clearAllSync()
}
