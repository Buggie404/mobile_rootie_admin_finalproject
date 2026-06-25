package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookings")
public class BookingEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String userName;
    private String userPhone;
    private String userEmail;
    private String serviceName;
    private String dateDisplay;
    private String monthDisplay;
    private String dayOfWeek;
    private String time;
    private String duration;
    private String storeName;
    private String storeAddress;
    private String storePhone;
    private String storeImage;
    private String storeID;
    private String note;
    private String status;
    private String createdAt;
    private String consultantName;
    private String cancelReason;

    public BookingEntity() {
        this.id = "";
        this.userId = "";
        this.userName = "";
        this.userPhone = "";
        this.userEmail = "";
        this.serviceName = "";
        this.dateDisplay = "";
        this.monthDisplay = "";
        this.dayOfWeek = "";
        this.time = "";
        this.duration = "";
        this.storeName = "";
        this.storeAddress = "";
        this.storePhone = "";
        this.storeImage = "";
        this.storeID = "";
        this.note = "";
        this.status = "";
        this.createdAt = "";
        this.consultantName = "";
        this.cancelReason = "";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDateDisplay() { return dateDisplay; }
    public void setDateDisplay(String dateDisplay) { this.dateDisplay = dateDisplay; }

    public String getMonthDisplay() { return monthDisplay; }
    public void setMonthDisplay(String monthDisplay) { this.monthDisplay = monthDisplay; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }

    public String getStorePhone() { return storePhone; }
    public void setStorePhone(String storePhone) { this.storePhone = storePhone; }

    public String getStoreImage() { return storeImage; }
    public void setStoreImage(String storeImage) { this.storeImage = storeImage; }

    public String getStoreID() { return storeID; }
    public void setStoreID(String storeID) { this.storeID = storeID; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getConsultantName() { return consultantName; }
    public void setConsultantName(String consultantName) { this.consultantName = consultantName; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
