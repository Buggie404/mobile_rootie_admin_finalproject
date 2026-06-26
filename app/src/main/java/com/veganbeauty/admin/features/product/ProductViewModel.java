package com.veganbeauty.admin.features.product;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.EntityUtils;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final LiveData<List<ProductEntity>> allProducts;

    // LiveData for search query, active category, and sort method
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("");
    private final MutableLiveData<Set<String>> selectedSubcategories = new MutableLiveData<>(Collections.emptySet());
    private final MutableLiveData<String> filterStockStatus = new MutableLiveData<>("ALL"); // ALL, IN_STOCK, OUT_OF_STOCK
    private final MutableLiveData<String> filterHiddenStatus = new MutableLiveData<>("ALL"); // ALL, HIDDEN, VISIBLE
    private final MutableLiveData<String> sortOrder = new MutableLiveData<>("DEFAULT"); // DEFAULT, PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC

    private final MediatorLiveData<List<ProductEntity>> filteredProducts = new MediatorLiveData<>();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(application);
        repository = new ProductRepository(database.productDao(), new FirebaseService());
        allProducts = repository.getAllProducts();

        // Combine sources for filtered products
        filteredProducts.addSource(allProducts, products -> updateFilteredProducts());
        filteredProducts.addSource(searchQuery, query -> updateFilteredProducts());
        filteredProducts.addSource(selectedCategory, category -> updateFilteredProducts());
        filteredProducts.addSource(selectedSubcategories, subs -> updateFilteredProducts());
        filteredProducts.addSource(filterStockStatus, stock -> updateFilteredProducts());
        filteredProducts.addSource(filterHiddenStatus, hidden -> updateFilteredProducts());
        filteredProducts.addSource(sortOrder, sort -> updateFilteredProducts());
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return allProducts;
    }

    public MutableLiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public MutableLiveData<String> getSelectedCategory() {
        return selectedCategory;
    }

    public MutableLiveData<Set<String>> getSelectedSubcategories() {
        return selectedSubcategories;
    }

    public MutableLiveData<String> getFilterStockStatus() {
        return filterStockStatus;
    }

    public MutableLiveData<String> getFilterHiddenStatus() {
        return filterHiddenStatus;
    }

    public MutableLiveData<String> getSortOrder() {
        return sortOrder;
    }

    public LiveData<List<ProductEntity>> getFilteredProducts() {
        return filteredProducts;
    }

    public void syncFromFirebase() {
        new Thread(() -> {
            try {
                repository.checkAndSeedProducts(getApplication());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void deleteProduct(ProductEntity product) {
        new Thread(() -> {
            try {
                repository.deleteProduct(product);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void saveProduct(ProductEntity product) {
        new Thread(() -> {
            try {
                repository.saveProduct(product);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void toggleProductVisibility(ProductEntity product) {
        ProductEntity updatedProduct = EntityUtils.copy(product, !product.isHidden());
        saveProduct(updatedProduct);
    }

    private void updateFilteredProducts() {
        List<ProductEntity> products = allProducts.getValue();
        if (products == null) {
            products = new ArrayList<>();
        }
        String query = searchQuery.getValue();
        query = query != null ? query.trim().toLowerCase() : "";
        String category = selectedCategory.getValue();
        if (category == null) category = "";
        Set<String> subcategories = selectedSubcategories.getValue();
        if (subcategories == null) subcategories = new HashSet<>();
        String stockStatus = filterStockStatus.getValue();
        if (stockStatus == null) stockStatus = "ALL";
        String hiddenStatus = filterHiddenStatus.getValue();
        if (hiddenStatus == null) hiddenStatus = "ALL";
        String sort = sortOrder.getValue();
        if (sort == null) sort = "DEFAULT";

        List<ProductEntity> result = new ArrayList<>();

        for (ProductEntity product : products) {
            // 1. Search Filter
            if (!query.isEmpty()) {
                String name = product.getName() != null ? product.getName().toLowerCase() : "";
                String sku = product.getSku() != null ? product.getSku().toLowerCase() : "";
                if (!name.contains(query) && !sku.contains(query)) {
                    continue;
                }
            }

            // 2. Category Filter
            if (!category.isEmpty()) {
                String cat = product.getCategory();
                if (cat == null || !cat.equalsIgnoreCase(category)) {
                    continue;
                }
            }

            // 3. Subcategories Filter
            if (!subcategories.isEmpty()) {
                String sub = product.getSubcategory();
                if (sub == null) {
                    continue;
                }
                String[] parts = sub.split(",");
                boolean subMatch = false;
                for (String part : parts) {
                    if (subcategories.contains(part.trim())) {
                        subMatch = true;
                        break;
                    }
                }
                if (!subMatch) {
                    continue;
                }
            }

            // 4. Stock Status Filter
            if ("IN_STOCK".equals(stockStatus)) {
                if (product.getStock() <= 0) {
                    continue;
                }
            } else if ("OUT_OF_STOCK".equals(stockStatus)) {
                if (product.getStock() != 0) {
                    continue;
                }
            }

            // 5. Hidden Status Filter
            if ("HIDDEN".equals(hiddenStatus)) {
                if (!product.isHidden()) {
                    continue;
                }
            } else if ("VISIBLE".equals(hiddenStatus)) {
                if (product.isHidden()) {
                    continue;
                }
            }

            result.add(product);
        }

        // 6. Sorting
        final String finalSort = sort;
        result.sort((p1, p2) -> {
            switch (finalSort) {
                case "PRICE_ASC":
                    return Long.compare(p1.getPrice(), p2.getPrice());
                case "PRICE_DESC":
                    return Long.compare(p2.getPrice(), p1.getPrice());
                case "NAME_ASC":
                    String n1 = p1.getName() != null ? p1.getName() : "";
                    String n2 = p2.getName() != null ? p2.getName() : "";
                    return n1.compareTo(n2);
                case "NAME_DESC":
                    String nd1 = p1.getName() != null ? p1.getName() : "";
                    String nd2 = p2.getName() != null ? p2.getName() : "";
                    return nd2.compareTo(nd1);
                default:
                    return 0;
            }
        });

        filteredProducts.setValue(result);
    }
}
