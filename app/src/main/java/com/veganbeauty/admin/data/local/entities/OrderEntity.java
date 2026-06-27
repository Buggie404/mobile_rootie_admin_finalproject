package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "orders")
public class OrderEntity {
    @PrimaryKey
    @NonNull
    private String orderId;
    private String userId;
    private String orderDate;
    private String orderTime;
    private String status;
    private long totalAmount;
    private List<OrderItem> items;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private long shippingCost;
    private long voucherDiscount;
    private String paymentMethod;
    @Nullable
    private String expectedDeliveryTime;
    private boolean hasReview;
    private int reviewStars;
    @Nullable
    private String reviewText;
    @Nullable
    private String reviewImage;
    private boolean isAnonymous;
    private boolean recommendToFriends;
    private String storeName;
    private String storeID;

    public OrderEntity() {
        this.orderId = "";
        this.userId = "";
        this.orderDate = "";
        this.orderTime = "";
        this.status = "";
        this.totalAmount = 0L;
        this.items = new ArrayList<>();
        this.shippingName = "";
        this.shippingPhone = "";
        this.shippingAddress = "";
        this.shippingCost = 0L;
        this.voucherDiscount = 0L;
        this.paymentMethod = "";
        this.expectedDeliveryTime = null;
        this.hasReview = false;
        this.reviewStars = 0;
        this.reviewText = null;
        this.reviewImage = null;
        this.isAnonymous = false;
        this.recommendToFriends = false;
        this.storeName = "";
        this.storeID = "";
    }

    @NonNull
    public String getOrderId() { return orderId; }
    public void setOrderId(@NonNull String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getOrderTime() { return orderTime; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public long getShippingCost() { return shippingCost; }
    public void setShippingCost(long shippingCost) { this.shippingCost = shippingCost; }

    public long getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(long voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @Nullable
    public String getExpectedDeliveryTime() { return expectedDeliveryTime; }
    public void setExpectedDeliveryTime(@Nullable String expectedDeliveryTime) { this.expectedDeliveryTime = expectedDeliveryTime; }

    public boolean isHasReview() { return hasReview; }
    public void setHasReview(boolean hasReview) { this.hasReview = hasReview; }

    public int getReviewStars() { return reviewStars; }
    public void setReviewStars(int reviewStars) { this.reviewStars = reviewStars; }

    @Nullable
    public String getReviewText() { return reviewText; }
    public void setReviewText(@Nullable String reviewText) { this.reviewText = reviewText; }

    @Nullable
    public String getReviewImage() { return reviewImage; }
    public void setReviewImage(@Nullable String reviewImage) { this.reviewImage = reviewImage; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public boolean isRecommendToFriends() { return recommendToFriends; }
    public void setRecommendToFriends(boolean recommendToFriends) { this.recommendToFriends = recommendToFriends; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreID() { return storeID; }
    public void setStoreID(String storeID) { this.storeID = storeID; }
}
