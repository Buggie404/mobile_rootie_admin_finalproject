package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class CustomerEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String username;
    private String full_name;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String avatar;
    private String primary_image;
    private long spending;
    private String tier;
    private String lastActive;
    private String notes;
    private String role;
    private String birthday;
    private String region;
    private int joinYear;
    private int orderCount;
    private String recentPurchase;
    private long spendingYear;
    private long spendingMonth;
    private int points;

    public CustomerEntity() {
        this.id = "";
        this.username = "";
        this.full_name = "";
        this.name = "";
        this.email = "";
        this.phone = "";
        this.address = "";
        this.avatar = "";
        this.primary_image = "";
        this.spending = 0L;
        this.tier = "Thường";
        this.lastActive = "";
        this.notes = "";
        this.role = "customer";
        this.birthday = "";
        this.region = "";
        this.joinYear = 1;
        this.orderCount = 0;
        this.recentPurchase = "";
        this.spendingYear = 0L;
        this.spendingMonth = 0L;
        this.points = 0;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getPrimary_image() { return primary_image; }
    public void setPrimary_image(String primary_image) { this.primary_image = primary_image; }

    public long getSpending() { return spending; }
    public void setSpending(long spending) { this.spending = spending; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getLastActive() { return lastActive; }
    public void setLastActive(String lastActive) { this.lastActive = lastActive; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public int getJoinYear() { return joinYear; }
    public void setJoinYear(int joinYear) { this.joinYear = joinYear; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public String getRecentPurchase() { return recentPurchase; }
    public void setRecentPurchase(String recentPurchase) { this.recentPurchase = recentPurchase; }

    public long getSpendingYear() { return spendingYear; }
    public void setSpendingYear(long spendingYear) { this.spendingYear = spendingYear; }

    public long getSpendingMonth() { return spendingMonth; }
    public void setSpendingMonth(long spendingMonth) { this.spendingMonth = spendingMonth; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}
