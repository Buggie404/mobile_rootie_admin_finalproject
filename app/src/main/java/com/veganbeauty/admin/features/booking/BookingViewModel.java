package com.veganbeauty.admin.features.booking;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.BookingRepository;
import java.util.ArrayList;
import java.util.List;

public class BookingViewModel extends AndroidViewModel {

    private final BookingRepository repository;
    private final SessionManager sessionManager;

    private final LiveData<List<BookingEntity>> allBookings;
    private final MutableLiveData<String> activeTabStatus = new MutableLiveData<>("PENDING"); // PENDING, UPCOMING, COMPLETED, CANCELLED
    private final MediatorLiveData<List<BookingEntity>> filteredBookings = new MediatorLiveData<>();

    public BookingViewModel(@NonNull Application application) {
        super(application);
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(application);
        repository = new BookingRepository(database.bookingDao(), new FirebaseService(), application);
        sessionManager = new SessionManager(application);
        allBookings = repository.getAllBookings();

        filteredBookings.addSource(allBookings, bookings -> updateFilteredBookings());
        filteredBookings.addSource(activeTabStatus, status -> updateFilteredBookings());
    }

    public LiveData<List<BookingEntity>> getAllBookings() {
        return allBookings;
    }

    public MutableLiveData<String> getActiveTabStatus() {
        return activeTabStatus;
    }

    public LiveData<List<BookingEntity>> getFilteredBookings() {
        return filteredBookings;
    }

    public void syncFromFirebase() {
        new Thread(() -> {
            try {
                repository.syncFromFirebase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateBookingStatus(String bookingId, String status, String cancelReason) {
        new Thread(() -> {
            try {
                repository.updateBookingStatus(bookingId, status, cancelReason);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateBookingStatus(String bookingId, String status) {
        updateBookingStatus(bookingId, status, "");
    }

    private void updateFilteredBookings() {
        List<BookingEntity> bookings = allBookings.getValue();
        if (bookings == null) {
            bookings = new ArrayList<>();
        }
        String statusFilter = activeTabStatus.getValue();
        if (statusFilter == null) {
            statusFilter = "PENDING";
        }
        String userRole = sessionManager.getRole();
        if (userRole == null) {
            userRole = "admin";
        }
        String userStoreId = sessionManager.getStoreID();
        if (userStoreId == null) {
            userStoreId = "";
        }
        boolean isStaff = "staff".equalsIgnoreCase(userRole) || "nhân viên".equalsIgnoreCase(userRole);

        // 1. Branch/Store filtering based on role and storeID
        List<BookingEntity> branchFiltered = new ArrayList<>();
        if (isStaff && !userStoreId.isEmpty()) {
            for (BookingEntity be : bookings) {
                boolean storeIdMatches = userStoreId.equals(be.getStoreID());
                boolean storeNameMatches = be.getStoreID() != null && be.getStoreID().isEmpty() &&
                    (("CH001".equals(userStoreId) && be.getStoreName() != null && be.getStoreName().toLowerCase().contains("cơ sở 1")) ||
                     ("CH005".equals(userStoreId) && be.getStoreName() != null && be.getStoreName().toLowerCase().contains("cơ sở 5")));
                if (storeIdMatches || storeNameMatches) {
                    branchFiltered.add(be);
                }
            }
        } else {
            branchFiltered.addAll(bookings);
        }

        // 2. Status filtering based on tab
        List<BookingEntity> statusFiltered = new ArrayList<>();
        for (BookingEntity be : branchFiltered) {
            String status = be.getStatus();
            if (status == null) status = "";
            switch (statusFilter) {
                case "PENDING":
                    if ("Chờ xác nhận".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
                        statusFiltered.add(be);
                    }
                    break;
                case "UPCOMING":
                    if ("Sắp diễn ra".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status) || "upcoming".equalsIgnoreCase(status)) {
                        statusFiltered.add(be);
                    }
                    break;
                case "COMPLETED":
                    if ("Đã hoàn thành".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                        statusFiltered.add(be);
                    }
                    break;
                case "CANCELLED":
                    if ("Đã huỷ".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                        statusFiltered.add(be);
                    }
                    break;
                default:
                    statusFiltered.add(be);
                    break;
            }
        }

        filteredBookings.setValue(statusFiltered);
    }
}
