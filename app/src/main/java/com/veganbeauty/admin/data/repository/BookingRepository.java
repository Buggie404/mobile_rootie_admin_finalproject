package com.veganbeauty.admin.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.veganbeauty.admin.data.local.dao.BookingDao;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {
    private final BookingDao bookingDao;
    private final FirebaseService firebaseService;
    private final Context context;

    public BookingRepository(BookingDao bookingDao, FirebaseService firebaseService, Context context) {
        this.bookingDao = bookingDao;
        this.firebaseService = firebaseService;
        this.context = context;
    }

    public BookingRepository(BookingDao bookingDao, FirebaseService firebaseService) {
        this(bookingDao, firebaseService, null);
    }

    public LiveData<List<BookingEntity>> getAllBookings() {
        return bookingDao.getAllLiveData();
    }

    public void syncFromFirebase() {
        List<BookingEntity> remoteList = firebaseService.fetchAllBookings();
        if (!remoteList.isEmpty()) {
            bookingDao.insertAllSync(remoteList);
        } else {
            int localCount = bookingDao.getAllSync().size();
            if (localCount == 0 && context != null) {
                seedFromAssets(context);
            }
        }
    }

    private void seedFromAssets(Context context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("skin_bookings.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            List<BookingEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String rawStoreName = obj.optString("storeName", "");
                String rawStoreAddress = obj.optString("storeAddress", "");
                String storeID = "";
                if (rawStoreName.toLowerCase().contains("cơ sở 1") || rawStoreAddress.toLowerCase().contains("minh khai")) {
                    storeID = "CH001";
                } else if (rawStoreName.toLowerCase().contains("cơ sở 5") || rawStoreAddress.toLowerCase().contains("hoàng văn thụ")) {
                    storeID = "CH005";
                }

                BookingEntity be = new BookingEntity();
                be.setId(obj.optString("id"));
                be.setUserId(obj.optString("userId", ""));
                be.setUserName(obj.optString("userName", ""));
                be.setUserPhone(obj.optString("userPhone", ""));
                be.setUserEmail(obj.optString("userEmail", ""));
                be.setServiceName(obj.optString("serviceName", ""));
                be.setDateDisplay(obj.optString("dateDisplay", ""));
                be.setMonthDisplay(obj.optString("monthDisplay", ""));
                be.setDayOfWeek(obj.optString("dayOfWeek", ""));
                be.setTime(obj.optString("time", ""));
                be.setDuration(obj.optString("duration", ""));
                be.setStoreName(rawStoreName);
                be.setStoreAddress(rawStoreAddress);
                be.setStorePhone(obj.optString("storePhone", ""));
                be.setStoreImage(obj.optString("storeImage", ""));
                be.setStoreID(storeID);
                be.setNote(obj.optString("note", ""));
                be.setStatus(obj.optString("status", ""));
                be.setCreatedAt(obj.optString("createdAt", ""));
                be.setConsultantName(obj.optString("consultantName", ""));
                be.setCancelReason(obj.optString("cancelReason", ""));
                list.add(be);
            }
            if (!list.isEmpty()) {
                bookingDao.insertAllSync(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean updateBookingStatus(String bookingId, String status, String cancelReason) {
        BookingEntity localBooking = bookingDao.getByIdSync(bookingId);
        boolean success;
        if (localBooking != null) {
            localBooking.setStatus(status);
            localBooking.setCancelReason(cancelReason);
            success = firebaseService.uploadBooking(localBooking);
            if (success) {
                bookingDao.insertSync(localBooking);
            }
        } else {
            success = firebaseService.updateBookingStatus(bookingId, status, cancelReason);
        }

        if (!success && localBooking != null) {
            // Cập nhật local dù Firebase offline/thất bại
            localBooking.setStatus(status);
            localBooking.setCancelReason(cancelReason);
            bookingDao.insertSync(localBooking);
        }
        return true;
    }

    public boolean updateBookingStatus(String bookingId, String status) {
        return updateBookingStatus(bookingId, status, "");
    }

}
