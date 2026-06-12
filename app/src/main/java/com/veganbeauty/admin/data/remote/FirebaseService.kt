package com.veganbeauty.admin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.data.local.entities.OrderEntity
import com.veganbeauty.admin.data.local.entities.OrderItem
import com.veganbeauty.admin.data.local.entities.ProductEntity
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
                    ProductEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        sku = doc.getString("sku") ?: "",
                        price = doc.getLong("price") ?: 0L,
                        category = doc.getString("category") ?: "",
                        brand = doc.getString("brand") ?: "",
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        description = doc.getString("description") ?: "",
                        mainImage = doc.getString("mainImage") ?: ""
                    )
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
        val data = hashMapOf(
            "name" to product.name,
            "sku" to product.sku,
            "price" to product.price,
            "category" to product.category,
            "brand" to product.brand,
            "stock" to product.stock,
            "description" to product.description,
            "mainImage" to product.mainImage
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
                        address = doc.getString("address") ?: ""
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }
}
