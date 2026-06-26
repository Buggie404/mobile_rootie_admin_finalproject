package com.veganbeauty.admin.features.home;

public class RecentActivity {
    private final String title;
    private final String orderId;
    private final String timeAgo;
    private final String price;
    private final String status;
    private final Integer imageRes;
    private final String imageUrl;

    public RecentActivity(String title, String orderId, String timeAgo, String price, String status, Integer imageRes, String imageUrl) {
        this.title = title;
        this.orderId = orderId;
        this.timeAgo = timeAgo;
        this.price = price;
        this.status = status;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
    }

    public RecentActivity(String title, String orderId, String timeAgo, String price, String status) {
        this(title, orderId, timeAgo, price, status, null, null);
    }

    public String getTitle() {
        return title;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public String getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public Integer getImageRes() {
        return imageRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
