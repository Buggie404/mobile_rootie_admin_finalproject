package com.veganbeauty.admin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.local.entities.OrderItem
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.data.local.entities.KeyIngredient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseService {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllProducts(): List<ProductEntity> = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        firestore.collection("products").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val albumList = doc.get("album") as? List<String> ?: emptyList()

                        @Suppress("UNCHECKED_CAST")
                        val keyIngredientsRaw = doc.get("keyIngredients") as? List<Map<String, Any>> ?: emptyList()
                        val keyIngredientsList = keyIngredientsRaw.map { map ->
                            KeyIngredient(
                                name = map["name"] as? String ?: "",
                                description = map["description"] as? String ?: ""
                            )
                        }

                        @Suppress("UNCHECKED_CAST")
                        val detailedList = doc.get("detailedIngredients") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val idealList = doc.get("idealFor") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val benefitsList = doc.get("benefits") as? List<String> ?: emptyList()

                        val categoryIdRaw = doc.get("categoryId")
                        val categoryIdsStr = when (categoryIdRaw) {
                            is List<*> -> categoryIdRaw.filterIsInstance<String>().joinToString(",")
                            is String -> categoryIdRaw
                            else -> ""
                        }

                        val subcategoryRaw = doc.get("subcategory")
                        val subcategoryStr = when (subcategoryRaw) {
                            is List<*> -> subcategoryRaw.filterIsInstance<String>().joinToString(",")
                            is String -> subcategoryRaw
                            else -> ""
                        }

                        ProductEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            sku = doc.getString("sku") ?: "",
                            barcode = doc.getString("barcode") ?: "",
                            price = doc.getLong("price") ?: 0L,
                            originalPrice = doc.getLong("originalPrice"),
                            category = doc.getString("category") ?: "",
                            subcategory = subcategoryStr,
                            brand = doc.getString("brand") ?: "",
                            stock = doc.getLong("stock")?.toInt() ?: 0,
                            description = doc.getString("description") ?: "",
                            mainImage = doc.getString("mainImage") ?: "",
                            suitableFor = doc.getString("suitableFor") ?: "",
                            origin = doc.getString("origin") ?: "",
                            expiryDate = doc.getString("expiryDate") ?: "",
                            isNew = doc.getBoolean("isNew") ?: false,
                            categoryIds = categoryIdsStr,
                            album = albumList,
                            mainIngredientsSummary = doc.getString("mainIngredientsSummary") ?: "",
                            allergyInformation = doc.getString("allergyInformation") ?: "",
                            keyIngredients = keyIngredientsList,
                            detailedIngredients = detailedList,
                            storyDescription = doc.getString("storyDescription") ?: "",
                            storyImage = doc.getString("storyImage") ?: "",
                            ingredientsImage = doc.getString("ingredientsImage") ?: "",
                            usageMedia = doc.getString("usageMedia") ?: "",
                            idealFor = idealList,
                            benefits = benefitsList,
                            usage = doc.getString("usage") ?: "",
                            usageAmount = doc.getString("usageAmount") ?: "",
                            texture = doc.getString("texture") ?: "",
                            scent = doc.getString("scent") ?: "",
                            notes = doc.getString("notes") ?: "",
                            rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                            sold = doc.getLong("sold")?.toInt() ?: 0,
                            isHidden = doc.getBoolean("isHidden") ?: false
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                continuation.resume(list)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }

    suspend fun saveProduct(product: ProductEntity): Boolean = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val keyIngredientsMap = product.keyIngredients.map {
            mapOf("name" to it.name, "description" to it.description)
        }
        val data = hashMapOf(
            "name" to product.name,
            "sku" to product.sku,
            "barcode" to product.barcode,
            "price" to product.price,
            "originalPrice" to product.originalPrice,
            "category" to product.category,
            "subcategory" to product.subcategory,
            "brand" to product.brand,
            "stock" to product.stock,
            "description" to product.description,
            "mainImage" to product.mainImage,
            "suitableFor" to product.suitableFor,
            "origin" to product.origin,
            "expiryDate" to product.expiryDate,
            "isNew" to product.isNew,
            "categoryId" to product.categoryIds.split(",").filter { it.isNotBlank() },
            "album" to product.album,
            "mainIngredientsSummary" to product.mainIngredientsSummary,
            "allergyInformation" to product.allergyInformation,
            "keyIngredients" to keyIngredientsMap,
            "detailedIngredients" to product.detailedIngredients,
            "storyDescription" to product.storyDescription,
            "storyImage" to product.storyImage,
            "ingredientsImage" to product.ingredientsImage,
            "usageMedia" to product.usageMedia,
            "idealFor" to product.idealFor,
            "benefits" to product.benefits,
            "usage" to product.usage,
            "usageAmount" to product.usageAmount,
            "texture" to product.texture,
            "scent" to product.scent,
            "notes" to product.notes,
            "rating" to product.rating,
            "sold" to product.sold,
            "isHidden" to product.isHidden
        )
        firestore.collection("products").document(product.id).set(data)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    suspend fun deleteProduct(productId: String): Boolean = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        firestore.collection("products").document(productId).delete()
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    suspend fun fetchAllOrders(): List<OrderEntity> = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        firestore.collection("orders").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val orderItems = itemsRaw.map { map ->
                        OrderItem(
                            productId = map["productId"] as? String ?: "",
                            productName = map["productName"] as? String ?: "",
                            productImage = map["productImage"] as? String ?: "",
                            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
                            price = (map["price"] as? Number)?.toLong() ?: 0L
                        )
                    }

                    OrderEntity(
                        orderId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        orderDate = doc.getString("orderDate") ?: "",
                        orderTime = doc.getString("orderTime") ?: "",
                        status = doc.getString("status") ?: "",
                        totalAmount = doc.getLong("totalAmount") ?: 0L,
                        items = orderItems,
                        shippingName = doc.getString("shippingName") ?: "",
                        shippingPhone = doc.getString("shippingPhone") ?: "",
                        shippingAddress = doc.getString("shippingAddress") ?: "",
                        shippingCost = doc.getLong("shippingCost") ?: 0L,
                        voucherDiscount = doc.getLong("voucherDiscount") ?: 0L,
                        paymentMethod = doc.getString("paymentMethod") ?: ""
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Boolean = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        firestore.collection("orders").document(orderId).update("status", status)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    suspend fun fetchAllCustomers(): List<CustomerEntity> = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        firestore.collection("users").get() // Customers correspond to 'users' collection
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    CustomerEntity(
                        id = doc.id,
                        name = doc.getString("username") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        address = doc.getString("address") ?: "",
                        avatar = doc.getString("avatar") ?: "",
                        spending = doc.getLong("spending") ?: 0L,
                        tier = doc.getString("tier") ?: "Thường",
                        lastActive = doc.getString("last_active") ?: "",
                        notes = doc.getString("notes") ?: "",
                        role = doc.getString("role") ?: "customer",
                        birthday = doc.getString("birthday") ?: "",
                        region = doc.getString("region") ?: "",
                        joinYear = doc.getLong("join_year")?.toInt() ?: 1,
                        orderCount = doc.getLong("order_count")?.toInt() ?: 0,
                        recentPurchase = doc.getString("recent_purchase") ?: "",
                        spendingYear = doc.getLong("spending_year") ?: 0L,
                        spendingMonth = doc.getLong("spending_month") ?: 0L,
                        points = doc.getLong("points")?.toInt() ?: 0
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }
}
