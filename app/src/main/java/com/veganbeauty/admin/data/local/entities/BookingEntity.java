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
    private float userRating;
    private String userReview;
    private String reviewDate;
    /** Pipe-separated image URLs from Firestore feedbackImageUrls */
    private String feedbackImageUrls;

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
        this.userRating = 0f;
        this.userReview = "";
        this.reviewDate = "";
        this.feedbackImageUrls = "";
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

    public float getUserRating() { return userRating; }
    public void setUserRating(float userRating) { this.userRating = userRating; }

    public String getUserReview() { return userReview; }
    public void setUserReview(String userReview) { this.userReview = userReview; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public String getFeedbackImageUrls() { return feedbackImageUrls; }
    public void setFeedbackImageUrls(String feedbackImageUrls) { this.feedbackImageUrls = feedbackImageUrls; }

    public boolean hasCustomerFeedback() {
        return userRating > 0f
                || (userReview != null && !userReview.trim().isEmpty())
                || (feedbackImageUrls != null && !feedbackImageUrls.trim().isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingEntity that = (BookingEntity) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(userId, that.userId) &&
                java.util.Objects.equals(userName, that.userName) &&
                java.util.Objects.equals(userPhone, that.userPhone) &&
                java.util.Objects.equals(userEmail, that.userEmail) &&
                java.util.Objects.equals(serviceName, that.serviceName) &&
                java.util.Objects.equals(dateDisplay, that.dateDisplay) &&
                java.util.Objects.equals(monthDisplay, that.monthDisplay) &&
                java.util.Objects.equals(dayOfWeek, that.dayOfWeek) &&
                java.util.Objects.equals(time, that.time) &&
                java.util.Objects.equals(duration, that.duration) &&
                java.util.Objects.equals(storeName, that.storeName) &&
                java.util.Objects.equals(storeAddress, that.storeAddress) &&
                java.util.Objects.equals(storePhone, that.storePhone) &&
                java.util.Objects.equals(storeImage, that.storeImage) &&
                java.util.Objects.equals(storeID, that.storeID) &&
                java.util.Objects.equals(note, that.note) &&
                java.util.Objects.equals(status, that.status) &&
                java.util.Objects.equals(createdAt, that.createdAt) &&
                java.util.Objects.equals(consultantName, that.consultantName) &&
                java.util.Objects.equals(cancelReason, that.cancelReason) &&
                Float.compare(that.userRating, userRating) == 0 &&
                java.util.Objects.equals(userReview, that.userReview) &&
                java.util.Objects.equals(reviewDate, that.reviewDate) &&
                java.util.Objects.equals(feedbackImageUrls, that.feedbackImageUrls);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, userId, userName, userPhone, userEmail, serviceName, dateDisplay, monthDisplay, dayOfWeek, time, duration, storeName, storeAddress, storePhone, storeImage, storeID, note, status, createdAt, consultantName, cancelReason, userRating, userReview, reviewDate, feedbackImageUrls);
    }
}
