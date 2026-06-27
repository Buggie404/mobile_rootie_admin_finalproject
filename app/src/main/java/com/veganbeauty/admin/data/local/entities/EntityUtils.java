package com.veganbeauty.admin.data.local.entities;

public class EntityUtils {

    public static CustomerEntity copy(CustomerEntity original, String notes) {
        if (original == null) return null;
        CustomerEntity entity = new CustomerEntity();
        entity.setId(original.getId());
        entity.setName(original.getName());
        entity.setEmail(original.getEmail());
        entity.setPhone(original.getPhone());
        entity.setAddress(original.getAddress());
        entity.setAvatar(original.getAvatar());
        entity.setSpending(original.getSpending());
        entity.setTier(original.getTier());
        entity.setLastActive(original.getLastActive());
        entity.setNotes(notes);
        entity.setRole(original.getRole());
        entity.setBirthday(original.getBirthday());
        entity.setRegion(original.getRegion());
        entity.setJoinYear(original.getJoinYear());
        entity.setOrderCount(original.getOrderCount());
        entity.setRecentPurchase(original.getRecentPurchase());
        entity.setSpendingYear(original.getSpendingYear());
        entity.setSpendingMonth(original.getSpendingMonth());
        entity.setPoints(original.getPoints());
        return entity;
    }

    public static ProductEntity copy(ProductEntity original, boolean isHidden) {
        if (original == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(original.getId());
        entity.setName(original.getName());
        entity.setSku(original.getSku());
        entity.setBarcode(original.getBarcode());
        entity.setPrice(original.getPrice());
        entity.setOriginalPrice(original.getOriginalPrice());
        entity.setCategory(original.getCategory());
        entity.setSubcategory(original.getSubcategory());
        entity.setBrand(original.getBrand());
        entity.setStock(original.getStock());
        entity.setDescription(original.getDescription());
        entity.setMainImage(original.getMainImage());
        entity.setSuitableFor(original.getSuitableFor());
        entity.setOrigin(original.getOrigin());
        entity.setExpiryDate(original.getExpiryDate());
        entity.setNew(original.isNew());
        entity.setCategoryIds(original.getCategoryIds());
        entity.setAlbum(original.getAlbum());
        entity.setMainIngredientsSummary(original.getMainIngredientsSummary());
        entity.setAllergyInformation(original.getAllergyInformation());
        entity.setKeyIngredients(original.getKeyIngredients());
        entity.setDetailedIngredients(original.getDetailedIngredients());
        entity.setStoryDescription(original.getStoryDescription());
        entity.setStoryImage(original.getStoryImage());
        entity.setIngredientsImage(original.getIngredientsImage());
        entity.setUsageMedia(original.getUsageMedia());
        entity.setIdealFor(original.getIdealFor());
        entity.setBenefits(original.getBenefits());
        entity.setUsage(original.getUsage());
        entity.setUsageAmount(original.getUsageAmount());
        entity.setTexture(original.getTexture());
        entity.setScent(original.getScent());
        entity.setNotes(original.getNotes());
        entity.setRating(original.getRating());
        entity.setSold(original.getSold());
        entity.setHidden(isHidden);
        return entity;
    }

    public static OrderEntity copy(OrderEntity original, String status) {
        if (original == null) return null;
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(original.getOrderId());
        entity.setUserId(original.getUserId());
        entity.setOrderDate(original.getOrderDate());
        entity.setOrderTime(original.getOrderTime());
        entity.setStatus(status);
        entity.setTotalAmount(original.getTotalAmount());
        entity.setItems(original.getItems());
        entity.setShippingName(original.getShippingName());
        entity.setShippingPhone(original.getShippingPhone());
        entity.setShippingAddress(original.getShippingAddress());
        entity.setShippingCost(original.getShippingCost());
        entity.setVoucherDiscount(original.getVoucherDiscount());
        entity.setPaymentMethod(original.getPaymentMethod());
        entity.setExpectedDeliveryTime(original.getExpectedDeliveryTime());
        entity.setHasReview(original.isHasReview());
        entity.setReviewStars(original.getReviewStars());
        entity.setReviewText(original.getReviewText());
        entity.setReviewImage(original.getReviewImage());
        entity.setAnonymous(original.isAnonymous());
        entity.setRecommendToFriends(original.isRecommendToFriends());
        entity.setStoreName(original.getStoreName());
        entity.setStoreID(original.getStoreID());
        return entity;
    }
}
