package com.veganbeauty.admin.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String sku;
    private String barcode;
    private long price;
    @Nullable
    private Long originalPrice;
    private String category;
    private String subcategory;
    private String brand;
    private int stock;
    private String description;
    private String mainImage;
    private String suitableFor;
    private String origin;
    private String expiryDate;
    private boolean isNew;
    private String categoryIds;
    private List<String> album;
    private String mainIngredientsSummary;
    private String allergyInformation;
    private List<KeyIngredient> keyIngredients;
    private List<String> detailedIngredients;
    private String storyDescription;
    private String storyImage;
    private String ingredientsImage;
    private String usageMedia;
    private List<String> idealFor;
    private List<String> benefits;
    private String usage;
    private String usageAmount;
    private String texture;
    private String scent;
    private String notes;
    private float rating;
    private int sold;
    private boolean isHidden;

    public ProductEntity() {
        this.id = "";
        this.name = "";
        this.sku = "";
        this.barcode = "";
        this.price = 0L;
        this.originalPrice = null;
        this.category = "";
        this.subcategory = "";
        this.brand = "";
        this.stock = 0;
        this.description = "";
        this.mainImage = "";
        this.suitableFor = "";
        this.origin = "";
        this.expiryDate = "";
        this.isNew = false;
        this.categoryIds = "";
        this.album = new ArrayList<>();
        this.mainIngredientsSummary = "";
        this.allergyInformation = "";
        this.keyIngredients = new ArrayList<>();
        this.detailedIngredients = new ArrayList<>();
        this.storyDescription = "";
        this.storyImage = "";
        this.ingredientsImage = "";
        this.usageMedia = "";
        this.idealFor = new ArrayList<>();
        this.benefits = new ArrayList<>();
        this.usage = "";
        this.usageAmount = "";
        this.texture = "";
        this.scent = "";
        this.notes = "";
        this.rating = 0f;
        this.sold = 0;
        this.isHidden = false;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    @Nullable
    public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(@Nullable Long originalPrice) { this.originalPrice = originalPrice; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(String suitableFor) { this.suitableFor = suitableFor; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public String getCategoryIds() { return categoryIds; }
    public void setCategoryIds(String categoryIds) { this.categoryIds = categoryIds; }

    public List<String> getAlbum() { return album; }
    public void setAlbum(List<String> album) { this.album = album; }

    public String getMainIngredientsSummary() { return mainIngredientsSummary; }
    public void setMainIngredientsSummary(String mainIngredientsSummary) { this.mainIngredientsSummary = mainIngredientsSummary; }

    public String getAllergyInformation() { return allergyInformation; }
    public void setAllergyInformation(String allergyInformation) { this.allergyInformation = allergyInformation; }

    public List<KeyIngredient> getKeyIngredients() { return keyIngredients; }
    public void setKeyIngredients(List<KeyIngredient> keyIngredients) { this.keyIngredients = keyIngredients; }

    public List<String> getDetailedIngredients() { return detailedIngredients; }
    public void setDetailedIngredients(List<String> detailedIngredients) { this.detailedIngredients = detailedIngredients; }

    public String getStoryDescription() { return storyDescription; }
    public void setStoryDescription(String storyDescription) { this.storyDescription = storyDescription; }

    public String getStoryImage() { return storyImage; }
    public void setStoryImage(String storyImage) { this.storyImage = storyImage; }

    public String getIngredientsImage() { return ingredientsImage; }
    public void setIngredientsImage(String ingredientsImage) { this.ingredientsImage = ingredientsImage; }

    public String getUsageMedia() { return usageMedia; }
    public void setUsageMedia(String usageMedia) { this.usageMedia = usageMedia; }

    public List<String> getIdealFor() { return idealFor; }
    public void setIdealFor(List<String> idealFor) { this.idealFor = idealFor; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }

    public String getUsageAmount() { return usageAmount; }
    public void setUsageAmount(String usageAmount) { this.usageAmount = usageAmount; }

    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }

    public String getScent() { return scent; }
    public void setScent(String scent) { this.scent = scent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean isHidden) { this.isHidden = isHidden; }
}
