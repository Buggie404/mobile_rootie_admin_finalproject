package com.veganbeauty.admin.features.product

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import com.veganbeauty.admin.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository
    val allProducts: LiveData<List<ProductEntity>>

    // LiveData for search query, active category, and sort method
    val searchQuery = MutableLiveData<String>("")
    val selectedCategory = MutableLiveData<String>("")
    val selectedSubcategories = MutableLiveData<Set<String>>(emptySet())
    val filterStockStatus = MutableLiveData<String>("ALL") // ALL, IN_STOCK, OUT_OF_STOCK
    val filterHiddenStatus = MutableLiveData<String>("ALL") // ALL, HIDDEN, VISIBLE
    val sortOrder = MutableLiveData<String>("DEFAULT") // DEFAULT, PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC

    private val _filteredProducts = MediatorLiveData<List<ProductEntity>>()
    val filteredProducts: LiveData<List<ProductEntity>> get() = _filteredProducts

    init {
        val database = RootieAdminDatabase.getDatabase(application)
        repository = ProductRepository(database.productDao(), FirebaseService())
        allProducts = repository.allProducts

        // Combine sources for filtered products
        _filteredProducts.addSource(allProducts) { updateFilteredProducts() }
        _filteredProducts.addSource(searchQuery) { updateFilteredProducts() }
        _filteredProducts.addSource(selectedCategory) { updateFilteredProducts() }
        _filteredProducts.addSource(selectedSubcategories) { updateFilteredProducts() }
        _filteredProducts.addSource(filterStockStatus) { updateFilteredProducts() }
        _filteredProducts.addSource(filterHiddenStatus) { updateFilteredProducts() }
        _filteredProducts.addSource(sortOrder) { updateFilteredProducts() }
    }

    fun syncFromFirebase() {
        viewModelScope.launch {
            try {
                repository.checkAndSeedProducts(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.saveProduct(product)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleProductVisibility(product: ProductEntity) {
        val updatedProduct = product.copy(isHidden = !product.isHidden)
        saveProduct(updatedProduct)
    }

    private fun updateFilteredProducts() {
        val products = allProducts.value ?: emptyList()
        val query = searchQuery.value?.trim()?.lowercase() ?: ""
        val category = selectedCategory.value ?: ""
        val subcategories = selectedSubcategories.value ?: emptySet()
        val stockStatus = filterStockStatus.value ?: "ALL"
        val hiddenStatus = filterHiddenStatus.value ?: "ALL"
        val sort = sortOrder.value ?: "DEFAULT"

        var result = products

        // 1. Search Filter
        if (query.isNotEmpty()) {
            result = result.filter {
                it.name.lowercase().contains(query) || it.sku.lowercase().contains(query)
            }
        }

        // 2. Category Filter
        if (category.isNotEmpty()) {
            result = result.filter {
                it.category.equals(category, ignoreCase = true)
            }
        }

        // 3. Subcategories Filter
        if (subcategories.isNotEmpty()) {
            result = result.filter { product ->
                val productSubs = product.subcategory.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                productSubs.any { it in subcategories }
            }
        }

        // 4. Stock Status Filter
        result = when (stockStatus) {
            "IN_STOCK" -> result.filter { it.stock > 0 }
            "OUT_OF_STOCK" -> result.filter { it.stock == 0 }
            else -> result
        }

        // 5. Hidden Status Filter
        result = when (hiddenStatus) {
            "HIDDEN" -> result.filter { it.isHidden }
            "VISIBLE" -> result.filter { !it.isHidden }
            else -> result
        }

        // 6. Sorting
        result = when (sort) {
            "PRICE_ASC" -> result.sortedBy { it.price }
            "PRICE_DESC" -> result.sortedByDescending { it.price }
            "NAME_ASC" -> result.sortedBy { it.name }
            "NAME_DESC" -> result.sortedByDescending { it.name }
            else -> result // DEFAULT
        }

        _filteredProducts.value = result
    }
}
