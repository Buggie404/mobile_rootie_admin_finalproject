package com.veganbeauty.admin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.local.entities.BookingEntity
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

    // ... (rest of products code stays the same) ...


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
                        val albumRaw = doc.get("album") as? List<*>
                        val albumList = albumRaw?.mapNotNull { it?.toString() } ?: emptyList()

                        val keyIngredientsRaw = doc.get("keyIngredients") as? List<*>
                        val keyIngredientsList = mutableListOf<KeyIngredient>()
                        if (keyIngredientsRaw != null) {
                            for (item in keyIngredientsRaw) {
                                if (item is Map<*, *>) {
                                    keyIngredientsList.add(
                                        KeyIngredient(
                                            name = item["name"]?.toString() ?: "",
                                            description = item["description"]?.toString() ?: ""
                                        )
                                    )
                                }
                            }
                        }

                        val detailedRaw = doc.get("detailedIngredients") as? List<*>
                        val detailedList = detailedRaw?.mapNotNull { it?.toString() } ?: emptyList()

                        val idealRaw = doc.get("idealFor") as? List<*>
                        val idealList = idealRaw?.mapNotNull { it?.toString() } ?: emptyList()

                        val benefitsRaw = doc.get("benefits") as? List<*>
                        val benefitsList = benefitsRaw?.mapNotNull { it?.toString() } ?: emptyList()

                        val categoryIdRaw = doc.get("categoryId")
                        val categoryIdsStr = when (categoryIdRaw) {
                            is List<*> -> categoryIdRaw.mapNotNull { it?.toString() }.joinToString(",")
                            else -> categoryIdRaw?.toString() ?: ""
                        }

                        val subcategoryRaw = doc.get("subcategory")
                        val subcategoryStr = when (subcategoryRaw) {
                            is List<*> -> subcategoryRaw.mapNotNull { it?.toString() }.joinToString(",")
                            else -> subcategoryRaw?.toString() ?: ""
                        }

                        val priceRaw = doc.get("price")
                        val price = when (priceRaw) {
                            is Number -> priceRaw.toLong()
                            is String -> priceRaw.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        val originalPriceRaw = doc.get("originalPrice")
                        val originalPrice = when (originalPriceRaw) {
                            is Number -> originalPriceRaw.toLong()
                            is String -> originalPriceRaw.toLongOrNull()
                            else -> null
                        }

                        val stockRaw = doc.get("stock")
                        val stock = when (stockRaw) {
                            is Number -> stockRaw.toInt()
                            is String -> stockRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val soldRaw = doc.get("sold")
                        val sold = when (soldRaw) {
                            is Number -> soldRaw.toInt()
                            is String -> soldRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        val isNewRaw = doc.get("isNew") ?: doc.get("newProduct")
                        val isNew = when (isNewRaw) {
                            is Boolean -> isNewRaw
                            is String -> isNewRaw.toBoolean()
                            else -> false
                        }

                        val isHiddenRaw = doc.get("isHidden")
                        val isHidden = when (isHiddenRaw) {
                            is Boolean -> isHiddenRaw
                            is String -> isHiddenRaw.toBoolean()
                            else -> false
                        }

                        val ratingRaw = doc.get("rating")
                        val rating = when (ratingRaw) {
                            is Number -> ratingRaw.toFloat()
                            is String -> ratingRaw.toFloatOrNull() ?: 0f
                            else -> 0f
                        }

                        ProductEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            sku = doc.getString("sku") ?: "",
                            barcode = doc.getString("barcode") ?: "",
                            price = price,
                            originalPrice = originalPrice,
                            category = doc.getString("category") ?: "",
                            subcategory = subcategoryStr,
                            brand = doc.getString("brand") ?: "",
                            stock = stock,
                            description = doc.getString("description") ?: "",
                            mainImage = doc.getString("mainImage") ?: "",
                            suitableFor = doc.getString("suitableFor") ?: "",
                            origin = doc.getString("origin") ?: "",
                            expiryDate = doc.getString("expiryDate") ?: "",
                            isNew = isNew,
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
                            rating = rating,
                            sold = sold,
                            isHidden = isHidden
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

                    val orderId = doc.id
                    val storeName = doc.getString("storeName") ?: if (orderId.takeLast(1).toIntOrNull()?.let { it % 2 == 1 } == true) {
                        "Cửa hàng mỹ phẩm Rootie - Cơ sở 1"
                    } else {
                        "Cửa hàng mỹ phẩm Rootie - Cơ sở 5"
                    }
                    val storeID = doc.getString("storeID") ?: doc.getString("storeId") ?: if (storeName.contains("Cơ sở 1", ignoreCase = true)) {
                        "CH001"
                    } else {
                        "CH005"
                    }

                    OrderEntity(
                        orderId = orderId,
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
                        paymentMethod = doc.getString("paymentMethod") ?: "",
                        storeName = storeName,
                        storeID = storeID
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

    suspend fun fetchAllBookings(): List<BookingEntity> = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        firestore.collection("bookings").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val rawStoreName = doc.getString("storeName") ?: ""
                        val rawStoreAddress = doc.getString("storeAddress") ?: ""
                        val storeID = doc.getString("storeID") ?: doc.getString("storeId") ?: when {
                            rawStoreName.contains("Cơ sở 1", ignoreCase = true) || rawStoreAddress.contains("Minh Khai", ignoreCase = true) -> "CH001"
                            rawStoreName.contains("Cơ sở 5", ignoreCase = true) || rawStoreAddress.contains("Hoàng Văn Thụ", ignoreCase = true) -> "CH005"
                            else -> ""
                        }

                        BookingEntity(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userPhone = doc.getString("userPhone") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            serviceName = doc.getString("serviceName") ?: "",
                            dateDisplay = doc.getString("dateDisplay") ?: "",
                            monthDisplay = doc.getString("monthDisplay") ?: "",
                            dayOfWeek = doc.getString("dayOfWeek") ?: "",
                            time = doc.getString("time") ?: "",
                            duration = doc.getString("duration") ?: "",
                            storeName = rawStoreName,
                            storeAddress = rawStoreAddress,
                            storePhone = doc.getString("storePhone") ?: "",
                            storeImage = doc.getString("storeImage") ?: "",
                            storeID = storeID,
                            note = doc.getString("note") ?: "",
                            status = doc.getString("status") ?: "",
                            createdAt = doc.getString("createdAt") ?: "",
                            consultantName = doc.getString("consultantName") ?: "",
                            cancelReason = doc.getString("cancelReason") ?: ""
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

    suspend fun updateBookingStatus(bookingId: String, status: String, cancelReason: String = ""): Boolean = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "cancelReason" to cancelReason
        )
        firestore.collection("bookings").document(bookingId).update(updates)
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

    suspend fun fetchChatMessages(): List<ChatMessage> = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        firestore.collection("chats").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderAvatar = doc.getString("senderAvatar") ?: "",
                            receiverId = doc.getString("receiverId") ?: "",
                            receiverName = doc.getString("receiverName") ?: "",
                            receiverAvatar = doc.getString("receiverAvatar") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
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

    suspend fun saveChatMessage(message: ChatMessage): Boolean = suspendCancellableCoroutine { continuation ->
        val firestore = db
        if (firestore == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val data = hashMapOf(
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "senderAvatar" to message.senderAvatar,
            "receiverId" to message.receiverId,
            "receiverName" to message.receiverName,
            "receiverAvatar" to message.receiverAvatar,
            "content" to message.content,
            "timestamp" to message.timestamp,
            "isRead" to message.isRead
        )
        firestore.collection("chats").document(message.id).set(data)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }
}
