package com.veganbeauty.admin.data.local.entities

fun CustomerEntity.copy(notes: String): CustomerEntity {
    val entity = CustomerEntity()
    entity.id = this.id
    entity.name = this.name
    entity.email = this.email
    entity.phone = this.phone
    entity.address = this.address
    entity.avatar = this.avatar
    entity.spending = this.spending
    entity.tier = this.tier
    entity.lastActive = this.lastActive
    entity.notes = notes
    entity.role = this.role
    entity.birthday = this.birthday
    entity.region = this.region
    entity.joinYear = this.joinYear
    entity.orderCount = this.orderCount
    entity.recentPurchase = this.recentPurchase
    entity.spendingYear = this.spendingYear
    entity.spendingMonth = this.spendingMonth
    entity.points = this.points
    return entity
}

fun ProductEntity.copy(isHidden: Boolean): ProductEntity {
    val entity = ProductEntity()
    entity.id = this.id
    entity.name = this.name
    entity.sku = this.sku
    entity.barcode = this.barcode
    entity.price = this.price
    entity.originalPrice = this.originalPrice
    entity.category = this.category
    entity.subcategory = this.subcategory
    entity.brand = this.brand
    entity.stock = this.stock
    entity.description = this.description
    entity.mainImage = this.mainImage
    entity.suitableFor = this.suitableFor
    entity.origin = this.origin
    entity.expiryDate = this.expiryDate
    entity.isNew = this.isNew
    entity.categoryIds = this.categoryIds
    entity.album = this.album
    entity.mainIngredientsSummary = this.mainIngredientsSummary
    entity.allergyInformation = this.allergyInformation
    entity.keyIngredients = this.keyIngredients
    entity.detailedIngredients = this.detailedIngredients
    entity.storyDescription = this.storyDescription
    entity.storyImage = this.storyImage
    entity.ingredientsImage = this.ingredientsImage
    entity.usageMedia = this.usageMedia
    entity.idealFor = this.idealFor
    entity.benefits = this.benefits
    entity.usage = this.usage
    entity.usageAmount = this.usageAmount
    entity.texture = this.texture
    entity.scent = this.scent
    entity.notes = this.notes
    entity.rating = this.rating
    entity.sold = this.sold
    entity.isHidden = isHidden
    return entity
}

fun OrderEntity.copy(status: String): OrderEntity {
    val entity = OrderEntity()
    entity.orderId = this.orderId
    entity.userId = this.userId
    entity.orderDate = this.orderDate
    entity.orderTime = this.orderTime
    entity.status = status
    entity.totalAmount = this.totalAmount
    entity.items = this.items
    entity.shippingName = this.shippingName
    entity.shippingPhone = this.shippingPhone
    entity.shippingAddress = this.shippingAddress
    entity.shippingCost = this.shippingCost
    entity.voucherDiscount = this.voucherDiscount
    entity.paymentMethod = this.paymentMethod
    entity.expectedDeliveryTime = this.expectedDeliveryTime
    entity.isHasReview = this.isHasReview
    entity.reviewStars = this.reviewStars
    entity.reviewText = this.reviewText
    entity.reviewImage = this.reviewImage
    entity.isAnonymous = this.isAnonymous
    entity.isRecommendToFriends = this.isRecommendToFriends
    entity.storeName = this.storeName
    entity.storeID = this.storeID
    return entity
}

// Pseudo-constructors for named arguments support on Java classes in Kotlin

fun ProductEntity(
    id: String,
    name: String = "",
    sku: String = "",
    barcode: String = "",
    price: Long = 0L,
    originalPrice: Long? = null,
    category: String = "",
    subcategory: String = "",
    brand: String = "",
    stock: Int = 0,
    description: String = "",
    mainImage: String = "",
    suitableFor: String = "",
    origin: String = "",
    expiryDate: String = "",
    isNew: Boolean = false,
    categoryIds: String = "",
    album: List<String> = emptyList(),
    mainIngredientsSummary: String = "",
    allergyInformation: String = "",
    keyIngredients: List<KeyIngredient> = emptyList(),
    detailedIngredients: List<String> = emptyList(),
    storyDescription: String = "",
    storyImage: String = "",
    ingredientsImage: String = "",
    usageMedia: String = "",
    idealFor: List<String> = emptyList(),
    benefits: List<String> = emptyList(),
    usage: String = "",
    usageAmount: String = "",
    texture: String = "",
    scent: String = "",
    notes: String = "",
    rating: Float = 0f,
    sold: Int = 0,
    isHidden: Boolean = false
): ProductEntity {
    val entity = ProductEntity()
    entity.id = id
    entity.name = name
    entity.sku = sku
    entity.barcode = barcode
    entity.price = price
    entity.originalPrice = originalPrice
    entity.category = category
    entity.subcategory = subcategory
    entity.brand = brand
    entity.stock = stock
    entity.description = description
    entity.mainImage = mainImage
    entity.suitableFor = suitableFor
    entity.origin = origin
    entity.expiryDate = expiryDate
    entity.isNew = isNew
    entity.categoryIds = categoryIds
    entity.album = album
    entity.mainIngredientsSummary = mainIngredientsSummary
    entity.allergyInformation = allergyInformation
    entity.keyIngredients = keyIngredients
    entity.detailedIngredients = detailedIngredients
    entity.storyDescription = storyDescription
    entity.storyImage = storyImage
    entity.ingredientsImage = ingredientsImage
    entity.usageMedia = usageMedia
    entity.idealFor = idealFor
    entity.benefits = benefits
    entity.usage = usage
    entity.usageAmount = usageAmount
    entity.texture = texture
    entity.scent = scent
    entity.notes = notes
    entity.rating = rating
    entity.sold = sold
    entity.isHidden = isHidden
    return entity
}

fun OrderEntity(
    orderId: String,
    userId: String = "",
    orderDate: String = "",
    orderTime: String = "",
    status: String = "",
    totalAmount: Long = 0L,
    items: List<OrderItem> = emptyList(),
    shippingName: String = "",
    shippingPhone: String = "",
    shippingAddress: String = "",
    shippingCost: Long = 0L,
    voucherDiscount: Long = 0L,
    paymentMethod: String = "",
    expectedDeliveryTime: String? = null,
    hasReview: Boolean = false,
    reviewStars: Int = 0,
    reviewText: String? = null,
    reviewImage: String? = null,
    isAnonymous: Boolean = false,
    recommendToFriends: Boolean = false,
    storeName: String = "",
    storeID: String = ""
): OrderEntity {
    val entity = OrderEntity()
    entity.orderId = orderId
    entity.userId = userId
    entity.orderDate = orderDate
    entity.orderTime = orderTime
    entity.status = status
    entity.totalAmount = totalAmount
    entity.items = items
    entity.shippingName = shippingName
    entity.shippingPhone = shippingPhone
    entity.shippingAddress = shippingAddress
    entity.shippingCost = shippingCost
    entity.voucherDiscount = voucherDiscount
    entity.paymentMethod = paymentMethod
    entity.expectedDeliveryTime = expectedDeliveryTime
    entity.isHasReview = hasReview
    entity.reviewStars = reviewStars
    entity.reviewText = reviewText
    entity.reviewImage = reviewImage
    entity.isAnonymous = isAnonymous
    entity.isRecommendToFriends = recommendToFriends
    entity.storeName = storeName
    entity.storeID = storeID
    return entity
}

fun CustomerEntity(
    id: String,
    name: String = "",
    email: String = "",
    phone: String = "",
    address: String = "",
    avatar: String = "",
    spending: Long = 0L,
    tier: String = "Thường",
    lastActive: String = "",
    notes: String = "",
    role: String = "customer",
    birthday: String = "",
    region: String = "",
    joinYear: Int = 1,
    orderCount: Int = 0,
    recentPurchase: String = "",
    spendingYear: Long = 0L,
    spendingMonth: Long = 0L,
    points: Int = 0
): CustomerEntity {
    val entity = CustomerEntity()
    entity.id = id
    entity.name = name
    entity.email = email
    entity.phone = phone
    entity.address = address
    entity.avatar = avatar
    entity.spending = spending
    entity.tier = tier
    entity.lastActive = lastActive
    entity.notes = notes
    entity.role = role
    entity.birthday = birthday
    entity.region = region
    entity.joinYear = joinYear
    entity.orderCount = orderCount
    entity.recentPurchase = recentPurchase
    entity.spendingYear = spendingYear
    entity.spendingMonth = spendingMonth
    entity.points = points
    return entity
}

fun AdminEntity(
    username: String,
    password: String = "123456",
    fullName: String = "",
    role: String = "",
    storeID: String = "",
    storeName: String = "",
    storeAddress: String = ""
): AdminEntity {
    val entity = AdminEntity()
    entity.username = username
    entity.password = password
    entity.fullName = fullName
    entity.role = role
    entity.storeID = storeID
    entity.storeName = storeName
    entity.storeAddress = storeAddress
    return entity
}

fun OrderItem(
    productId: String,
    productName: String = "",
    productImage: String = "",
    quantity: Int = 0,
    price: Long = 0L
): OrderItem {
    val entity = OrderItem()
    entity.productId = productId
    entity.productName = productName
    entity.productImage = productImage
    entity.quantity = quantity
    entity.price = price
    return entity
}

fun KeyIngredient(
    name: String,
    description: String = ""
): KeyIngredient {
    val entity = KeyIngredient()
    entity.name = name
    entity.description = description
    return entity
}
