package com.veganbeauty.admin.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.google.firebase.firestore.ListenerRegistration;
import com.veganbeauty.admin.data.local.dao.ProductDao;
import com.veganbeauty.admin.data.local.entities.KeyIngredient;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProductRepository {
    private final ProductDao productDao;
    private final FirebaseService firebaseService;

    public ProductRepository(ProductDao productDao, FirebaseService firebaseService) {
        this.productDao = productDao;
        this.firebaseService = firebaseService;
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return productDao.getAllLiveData();
    }

    public void syncFromFirebase() {
        List<ProductEntity> remoteList = firebaseService.fetchAllProducts();
        if (!remoteList.isEmpty()) {
            productDao.insertAllSync(remoteList);
        }
    }

    public void checkAndSeedProducts(Context context) {
        if (productDao.getAllSync().isEmpty()) {
            List<ProductEntity> localProducts = parseProductsFromAssets(context);
            if (!localProducts.isEmpty()) {
                productDao.insertAllSync(localProducts);
            }
            syncFromFirebase();
        }
    }

    private List<ProductEntity> parseProductsFromAssets(Context context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("products.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray jsonArray = root.getJSONArray("products");
            List<ProductEntity> productList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Object categoryIdRaw = obj.opt("categoryId");
                String categoryIdsStr = "";
                if (categoryIdRaw instanceof JSONArray) {
                    JSONArray arr = (JSONArray) categoryIdRaw;
                    List<String> list = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        list.add(arr.getString(j));
                    }
                    categoryIdsStr = joinList(list);
                } else if (categoryIdRaw instanceof String) {
                    categoryIdsStr = (String) categoryIdRaw;
                }

                Object subcategoryRaw = obj.opt("subcategory");
                String subcategoryStr = "";
                if (subcategoryRaw instanceof JSONArray) {
                    JSONArray arr = (JSONArray) subcategoryRaw;
                    List<String> list = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        list.add(arr.getString(j));
                    }
                    subcategoryStr = joinList(list);
                } else if (subcategoryRaw instanceof String) {
                    subcategoryStr = (String) subcategoryRaw;
                }

                JSONArray albumArr = obj.optJSONArray("album");
                List<String> albumList = new ArrayList<>();
                if (albumArr != null) {
                    for (int j = 0; j < albumArr.length(); j++) {
                        albumList.add(albumArr.getString(j));
                    }
                }

                JSONArray keyArr = obj.optJSONArray("keyIngredients");
                List<KeyIngredient> keyIngredientsList = new ArrayList<>();
                if (keyArr != null) {
                    for (int j = 0; j < keyArr.length(); j++) {
                        JSONObject keyObj = keyArr.getJSONObject(j);
                        KeyIngredient ki = new KeyIngredient();
                        ki.setName(keyObj.optString("name", ""));
                        ki.setDescription(keyObj.optString("description", ""));
                        keyIngredientsList.add(ki);
                    }
                }

                JSONArray detailedArr = obj.optJSONArray("detailedIngredients");
                List<String> detailedIngredientsList = new ArrayList<>();
                if (detailedArr != null) {
                    for (int j = 0; j < detailedArr.length(); j++) {
                        detailedIngredientsList.add(detailedArr.getString(j));
                    }
                }

                JSONArray idealArr = obj.optJSONArray("idealFor");
                List<String> idealForList = new ArrayList<>();
                if (idealArr != null) {
                    for (int j = 0; j < idealArr.length(); j++) {
                        idealForList.add(idealArr.getString(j));
                    }
                }

                JSONArray benefitsArr = obj.optJSONArray("benefits");
                List<String> benefitsList = new ArrayList<>();
                if (benefitsArr != null) {
                    for (int j = 0; j < benefitsArr.length(); j++) {
                        benefitsList.add(benefitsArr.getString(j));
                    }
                }

                Long originalPrice = null;
                if (obj.has("originalPrice") && !obj.isNull("originalPrice")) {
                    originalPrice = obj.getLong("originalPrice");
                }

                ProductEntity pe = new ProductEntity();
                pe.setId(obj.optString("id", UUID.randomUUID().toString()));
                pe.setName(obj.optString("name", "Sản phẩm không tên"));
                pe.setSku(obj.optString("sku", ""));
                pe.setBarcode(obj.optString("barcode", ""));
                pe.setPrice(obj.optLong("price", 0L));
                pe.setOriginalPrice(originalPrice);
                pe.setCategory(obj.optString("category", ""));
                pe.setSubcategory(subcategoryStr);
                pe.setBrand(obj.optString("brand", ""));
                pe.setStock(obj.optInt("stock", 0));
                pe.setDescription(obj.optString("description", ""));
                pe.setMainImage(obj.optString("mainImage", ""));
                pe.setSuitableFor(obj.optString("suitableFor", ""));
                pe.setOrigin(obj.optString("origin", ""));
                pe.setExpiryDate(obj.optString("expiryDate", ""));
                pe.setNew(obj.optBoolean("newProduct", false) || obj.optBoolean("isNew", false));
                pe.setCategoryIds(categoryIdsStr);
                pe.setAlbum(albumList);
                pe.setMainIngredientsSummary(obj.optString("mainIngredientsSummary", ""));
                pe.setAllergyInformation(obj.optString("allergyInformation", ""));
                pe.setKeyIngredients(keyIngredientsList);
                pe.setDetailedIngredients(detailedIngredientsList);
                pe.setStoryDescription(obj.optString("storyDescription", ""));
                pe.setStoryImage(obj.optString("storyImage", ""));
                pe.setIngredientsImage(obj.optString("ingredientsImage", ""));
                pe.setUsageMedia(obj.optString("usageMedia", ""));
                pe.setIdealFor(idealForList);
                pe.setBenefits(benefitsList);
                pe.setUsage(obj.optString("usage", ""));
                pe.setUsageAmount(obj.optString("usageAmount", ""));
                pe.setTexture(obj.optString("texture", ""));
                pe.setScent(obj.optString("scent", ""));
                pe.setNotes(obj.optString("notes", ""));
                pe.setRating((float) obj.optDouble("rating", 0.0));
                pe.setSold(obj.optInt("sold", 0));
                pe.setHidden(obj.optBoolean("isHidden", false));
                productList.add(pe);
            }
            return productList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String joinList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public boolean saveProduct(ProductEntity product) {
        productDao.insertSync(product);
        return firebaseService.saveProduct(product);
    }

    public boolean deleteProduct(ProductEntity product) {
        productDao.deleteSync(product);
        android.util.Log.d("ProductRepository", "deleteProduct: Deleted from local DB: " + product.getId());
        boolean success = firebaseService.deleteProduct(product.getId());
        android.util.Log.d("ProductRepository", "deleteProduct: Delete from Firebase: " + success);
        return success;
    }

    public boolean updateProductStock(String productId, int newStock) {
        productDao.updateStockSync(productId, newStock);
        android.util.Log.d("ProductRepository", "updateProductStock: Updated local stock for " + productId + " to " + newStock);
        boolean success = firebaseService.updateProductStock(productId, newStock);
        android.util.Log.d("ProductRepository", "updateProductStock: Updated Firebase stock: " + success);
        return success;
    }

    public com.google.firebase.firestore.ListenerRegistration startRealtimeSync() {
        return firebaseService.listenToProducts(products -> {
            if (products != null && !products.isEmpty()) {
                android.util.Log.d("ProductRepository", "startRealtimeSync: Received " + products.size() + " products from Firebase");
                new Thread(() -> {
                    productDao.insertAllSync(products);
                    android.util.Log.d("ProductRepository", "startRealtimeSync: Inserted " + products.size() + " products to local DB");
                }).start();
            } else {
                android.util.Log.d("ProductRepository", "startRealtimeSync: No products received");
            }
        });
    }
}
