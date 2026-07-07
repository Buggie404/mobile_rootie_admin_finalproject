package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "orders")
public class OrderEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String orderDate;
    private String orderTime;
    private String status;
    private long totalAmount;
    private long subTotal;
    private List<OrderItem> items;
    private boolean isGuest;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private long shippingCost;
    private long voucherDiscount;
    private String paymentMethod;
    @Nullable
    private String expectedDeliveryTime;
    @Nullable
    private String deliveryDate;
    private boolean isAffiliate;
    @Nullable
    @Embedded(prefix = "aff_")
    private AffiliateInfo affiliate;
    private boolean hasReview;
    private int reviewStars;
    @Nullable
    private String reviewText;
    @Nullable
    private String reviewImage;
    private boolean isAnonymous;
    private boolean recommendToFriends;
    @Nullable
    private String billingName;
    @Nullable
    private String billingPhone;
    @Nullable
    private String billingEmail;
    @Nullable
    private String orderNote;
    /** Admin-only fields (not in user Room, kept for store routing). */
    private String storeName;
    private String storeID;

    @Ignore
    private long createdAt;

    public OrderEntity() {
        this.id = "";
        this.userId = "";
        this.orderDate = "";
        this.orderTime = "";
        this.status = "";
        this.totalAmount = 0L;
        this.subTotal = 0L;
        this.items = new ArrayList<>();
        this.isGuest = false;
        this.shippingName = "";
        this.shippingPhone = "";
        this.shippingAddress = "";
        this.shippingCost = 0L;
        this.voucherDiscount = 0L;
        this.paymentMethod = "";
        this.expectedDeliveryTime = null;
        this.deliveryDate = null;
        this.isAffiliate = false;
        this.affiliate = null;
        this.hasReview = false;
        this.reviewStars = 0;
        this.reviewText = null;
        this.reviewImage = null;
        this.isAnonymous = false;
        this.recommendToFriends = false;
        this.billingName = null;
        this.billingPhone = null;
        this.billingEmail = null;
        this.orderNote = null;
        this.storeName = "";
        this.storeID = "";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

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

    public long getSubTotal() { return subTotal; }
    public void setSubTotal(long subTotal) { this.subTotal = subTotal; }

    public List<OrderItem> getItems() { return items != null ? items : new ArrayList<>(); }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }

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

    @Nullable
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(@Nullable String deliveryDate) { this.deliveryDate = deliveryDate; }

    public boolean isAffiliate() { return isAffiliate; }
    public void setAffiliate(boolean affiliate) { isAffiliate = affiliate; }

    @Nullable
    public AffiliateInfo getAffiliate() { return affiliate; }
    public void setAffiliate(@Nullable AffiliateInfo affiliate) { this.affiliate = affiliate; }

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

    @Nullable
    public String getBillingName() { return billingName; }
    public void setBillingName(@Nullable String billingName) { this.billingName = billingName; }

    @Nullable
    public String getBillingPhone() { return billingPhone; }
    public void setBillingPhone(@Nullable String billingPhone) { this.billingPhone = billingPhone; }

    @Nullable
    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(@Nullable String billingEmail) { this.billingEmail = billingEmail; }

    @Nullable
    public String getOrderNote() { return orderNote; }
    public void setOrderNote(@Nullable String orderNote) { this.orderNote = orderNote; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreID() { return storeID; }
    public void setStoreID(String storeID) { this.storeID = storeID; }

    public static class AffiliateInfo {
        private String affiliate_id;
        private String referrerUserId;
        private long commissionAmount;
        private String commissionStatus;

        public String getAffiliate_id() { return affiliate_id; }
        public void setAffiliate_id(String affiliate_id) { this.affiliate_id = affiliate_id; }
        public String getReferrerUserId() { return referrerUserId; }
        public void setReferrerUserId(String referrerUserId) { this.referrerUserId = referrerUserId; }
        public long getCommissionAmount() { return commissionAmount; }
        public void setCommissionAmount(long commissionAmount) { this.commissionAmount = commissionAmount; }
        public String getCommissionStatus() { return commissionStatus; }
        public void setCommissionStatus(String commissionStatus) { this.commissionStatus = commissionStatus; }
    }
}
