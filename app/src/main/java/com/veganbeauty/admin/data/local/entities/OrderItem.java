package com.veganbeauty.admin.data.local.entities;

public class OrderItem {
    private String productId;
    private String productName;
    private String productImage;
    private int quantity;
    private long price;

    public OrderItem() {
        this.productId = "";
        this.productName = "";
        this.productImage = "";
        this.quantity = 0;
        this.price = 0L;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
}
