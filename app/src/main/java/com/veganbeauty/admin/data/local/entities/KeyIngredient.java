package com.veganbeauty.admin.data.local.entities;

public class KeyIngredient {
    private String name;
    private String description;

    public KeyIngredient() {
        this.name = "";
        this.description = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
