package com.veganbeauty.admin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.admin.data.local.dao.CustomerDao
import com.veganbeauty.admin.data.local.dao.OrderDao
import com.veganbeauty.admin.data.local.dao.ProductDao
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.local.entities.ProductEntity

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        CustomerEntity::class
    ],
    version = 6
)
@TypeConverters(OrderConverters::class, ProductConverters::class)
abstract class RootieAdminDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: RootieAdminDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): RootieAdminDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RootieAdminDatabase::class.java,
                    "rootie-admin-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
