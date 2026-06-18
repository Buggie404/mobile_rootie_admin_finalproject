package com.veganbeauty.admin.data.repository

import androidx.lifecycle.LiveData
import com.veganbeauty.admin.data.local.dao.ProductDao
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val productDao: ProductDao,
    private val firebaseService: FirebaseService
) {

    val allProducts: LiveData<List<ProductEntity>> = productDao.getAllLiveData()

    suspend fun syncFromFirebase() = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllProducts()
        if (remoteList.isNotEmpty()) {
            productDao.insertAllSync(remoteList)
        }
    }

    suspend fun checkAndSeedProducts(context: android.content.Context) = withContext(Dispatchers.IO) {
        val remoteList = firebaseService.fetchAllProducts()
        if (remoteList.isEmpty()) {
            val localProducts = parseProductsFromAssets(context)
            if (localProducts.isNotEmpty()) {
                // Upload to Firebase
                for (product in localProducts) {
                    firebaseService.saveProduct(product)
                }
                // Save to Local DB
                productDao.insertAllSync(localProducts)
            }
        } else {
            productDao.insertAllSync(remoteList)
        }
    }

    private fun parseProductsFromAssets(context: android.content.Context): List<ProductEntity> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            val productList = mutableListOf<ProductEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val categoryIdRaw = obj.opt("categoryId")
                val categoryIdsStr = when (categoryIdRaw) {
                    is org.json.JSONArray -> {
                        val list = mutableListOf<String>()
                        for (j in 0 until categoryIdRaw.length()) {
                            list.add(categoryIdRaw.getString(j))
                        }
                        list.joinToString(",")
                    }
                    is String -> categoryIdRaw
                    else -> ""
                }

                val subcategoryRaw = obj.opt("subcategory")
                val subcategoryStr = when (subcategoryRaw) {
                    is org.json.JSONArray -> {
                        val list = mutableListOf<String>()
                        for (j in 0 until subcategoryRaw.length()) {
                            list.add(subcategoryRaw.getString(j))
                        }
                        list.joinToString(",")
                    }
                    is String -> subcategoryRaw
                    else -> ""
                }

                val albumArr = obj.optJSONArray("album")
                val albumList = mutableListOf<String>()
                if (albumArr != null) {
                    for (j in 0 until albumArr.length()) {
                        albumList.add(albumArr.getString(j))
                    }
                }

                val keyArr = obj.optJSONArray("keyIngredients")
                val keyIngredientsList = mutableListOf<com.veganbeauty.admin.data.local.entities.KeyIngredient>()
                if (keyArr != null) {
                    for (j in 0 until keyArr.length()) {
                        val keyObj = keyArr.getJSONObject(j)
                        keyIngredientsList.add(
                            com.veganbeauty.admin.data.local.entities.KeyIngredient(
                                name = keyObj.optString("name", ""),
                                description = keyObj.optString("description", "")
                            )
                        )
                    }
                }

                val detailedArr = obj.optJSONArray("detailedIngredients")
                val detailedIngredientsList = mutableListOf<String>()
                if (detailedArr != null) {
                    for (j in 0 until detailedArr.length()) {
                        detailedIngredientsList.add(detailedArr.getString(j))
                    }
                }

                val idealArr = obj.optJSONArray("idealFor")
                val idealForList = mutableListOf<String>()
                if (idealArr != null) {
                    for (j in 0 until idealArr.length()) {
                        idealForList.add(idealArr.getString(j))
                    }
                }

                val benefitsArr = obj.optJSONArray("benefits")
                val benefitsList = mutableListOf<String>()
                if (benefitsArr != null) {
                    for (j in 0 until benefitsArr.length()) {
                        benefitsList.add(benefitsArr.getString(j))
                    }
                }

                productList.add(
                    ProductEntity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Sản phẩm không tên"),
                        sku = obj.optString("sku", ""),
                        barcode = obj.optString("barcode", ""),
                        price = obj.optLong("price", 0L),
                        originalPrice = if (obj.has("originalPrice") && !obj.isNull("originalPrice")) obj.getLong("originalPrice") else null,
                        category = obj.optString("category", ""),
                        subcategory = subcategoryStr,
                        brand = obj.optString("brand", ""),
                        stock = obj.optInt("stock", 0),
                        description = obj.optString("description", ""),
                        mainImage = obj.optString("mainImage", ""),
                        suitableFor = obj.optString("suitableFor", ""),
                        origin = obj.optString("origin", ""),
                        expiryDate = obj.optString("expiryDate", ""),
                        isNew = obj.optBoolean("newProduct", false) || obj.optBoolean("isNew", false),
                        categoryIds = categoryIdsStr,
                        album = albumList,
                        mainIngredientsSummary = obj.optString("mainIngredientsSummary", ""),
                        allergyInformation = obj.optString("allergyInformation", ""),
                        keyIngredients = keyIngredientsList,
                        detailedIngredients = detailedIngredientsList,
                        storyDescription = obj.optString("storyDescription", ""),
                        storyImage = obj.optString("storyImage", ""),
                        ingredientsImage = obj.optString("ingredientsImage", ""),
                        usageMedia = obj.optString("usageMedia", ""),
                        idealFor = idealForList,
                        benefits = benefitsList,
                        usage = obj.optString("usage", ""),
                        usageAmount = obj.optString("usageAmount", ""),
                        texture = obj.optString("texture", ""),
                        scent = obj.optString("scent", ""),
                        notes = obj.optString("notes", ""),
                        rating = obj.optDouble("rating", 0.0).toFloat(),
                        sold = obj.optInt("sold", 0),
                        isHidden = obj.optBoolean("isHidden", false)
                    )
                )
            }
            productList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        productDao.insertSync(product)
        val success = firebaseService.saveProduct(product)
        success
    }

    suspend fun deleteProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        productDao.deleteSync(product)
        val success = firebaseService.deleteProduct(product.id)
        success
    }
}
