package com.veganbeauty.admin.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.veganbeauty.admin.data.local.entities.AdminEntity

@Dao
interface AdminDao {

    @Query("SELECT * FROM admins")
    fun getAllLiveData(): LiveData<List<AdminEntity>>

    @Query("SELECT * FROM admins")
    fun getAllSync(): List<AdminEntity>

    @Query("SELECT * FROM admins WHERE username = :username")
    fun getByUsernameSync(username: String): AdminEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(admin: AdminEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(admins: List<AdminEntity>)

    @Query("DELETE FROM admins")
    fun deleteAllSync()
}
