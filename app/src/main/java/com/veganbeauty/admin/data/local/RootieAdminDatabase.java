package com.veganbeauty.admin.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.veganbeauty.admin.data.local.dao.AdminDao;
import com.veganbeauty.admin.data.local.dao.BookingDao;
import com.veganbeauty.admin.data.local.dao.ChatMessageDao;
import com.veganbeauty.admin.data.local.dao.CustomerDao;
import com.veganbeauty.admin.data.local.dao.OrderDao;
import com.veganbeauty.admin.data.local.dao.ProductDao;
import com.veganbeauty.admin.data.local.entities.AdminEntity;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.local.entities.ChatMessageEntity;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.ProductEntity;

@Database(
    entities = {
        ProductEntity.class,
        OrderEntity.class,
        CustomerEntity.class,
        BookingEntity.class,
        ChatMessageEntity.class,
        AdminEntity.class
    },
    version = 12
)
@TypeConverters({OrderConverters.class, ProductConverters.class})
public abstract class RootieAdminDatabase extends RoomDatabase {

    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract CustomerDao customerDao();
    public abstract BookingDao bookingDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract AdminDao adminDao();

    private static volatile RootieAdminDatabase INSTANCE;

    public static RootieAdminDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RootieAdminDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        RootieAdminDatabase.class,
                        "rootie-admin-db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
