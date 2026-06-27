package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "admins")
public class AdminEntity {
    @PrimaryKey
    @NonNull
    private String username;
    private String password;
    private String fullName;
    private String role;
    private String storeID;
    private String storeName;
    private String storeAddress;

    public AdminEntity() {
        this.username = "";
        this.password = "123456";
        this.fullName = "";
        this.role = "";
        this.storeID = "";
        this.storeName = "";
        this.storeAddress = "";
    }

    @NonNull
    public String getUsername() { return username; }
    public void setUsername(@NonNull String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStoreID() { return storeID; }
    public void setStoreID(String storeID) { this.storeID = storeID; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }
}
