package com.veganbeauty.admin.features.product.add

import androidx.appcompat.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.entities.KeyIngredient
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.databinding.ProductFragmentAddBinding
import com.veganbeauty.admin.features.product.ProductViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProductAddFragment : RootieAdminFragment() {

    private var _binding: ProductFragmentAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductViewModel
    private val selectedImages = mutableListOf<String>()
    private val selectedSubcategoriesSet = mutableSetOf<String>()
    
    private var ingredientsImageUrl: String = ""
    private var usageMediaUrl: String = ""

    private var editingProductId: String? = null
    private var isEditMode: Boolean = false
    private var isDataLoaded: Boolean = false

    // Image Picker & Camera Launchers
    private val pickAlbumImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImages.add(it.toString())
            renderImages()
        }
    }

    private val captureAlbumImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(it)
            selectedImages.add(uri.toString())
            renderImages()
        }
    }

    private val pickIngredientsImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            ingredientsImageUrl = it.toString()
            binding.imvIngredientsImage.visibility = View.VISIBLE
            binding.imvIngredientsImage.load(it)
        }
    }

    private val captureIngredientsImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(it)
            ingredientsImageUrl = uri.toString()
            binding.imvIngredientsImage.visibility = View.VISIBLE
            binding.imvIngredientsImage.load(uri)
        }
    }

    private val pickUsageMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            usageMediaUrl = it.toString()
            binding.imvUsageMedia.visibility = View.VISIBLE
            binding.imvUsageMedia.load(it)
        }
    }

    private val captureUsageMediaLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(it)
            usageMediaUrl = uri.toString()
            binding.imvUsageMedia.visibility = View.VISIBLE
            binding.imvUsageMedia.load(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProductFragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        viewModel = ViewModelProvider(requireActivity())[ProductViewModel::class.java]

        // Read arguments for edit mode
        editingProductId = arguments?.getString(ARG_PRODUCT_ID)
        isEditMode = !editingProductId.isNullOrEmpty()

        if (isEditMode) {
            binding.txtTitle.text = "Chỉnh sửa sản phẩm"
        }

        // Setup Back Button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup Spinners and Multi-choice Subcategory Dropdown
        setupCategoriesSpinner()

        // Setup Product Images Click (Album + Camera)
        binding.btnAddImage.setOnClickListener {
            showImageSourceDialog(
                onGallerySelected = { pickAlbumImageLauncher.launch("image/*") },
                onCameraSelected = { captureAlbumImageLauncher.launch(null) }
            )
        }

        // Setup Ingredients Image Click
        binding.btnUploadIngredientsImage.setOnClickListener {
            showImageSourceDialog(
                onGallerySelected = { pickIngredientsImageLauncher.launch("image/*") },
                onCameraSelected = { captureIngredientsImageLauncher.launch(null) }
            )
        }

        // Setup Usage Media Click
        binding.btnUploadUsageMedia.setOnClickListener {
            showImageSourceDialog(
                onGallerySelected = { pickUsageMediaLauncher.launch("image/*") },
                onCameraSelected = { captureUsageMediaLauncher.launch(null) }
            )
        }

        // Setup Dynamic Key Ingredients Row Add
        binding.btnAddKeyIngredient.setOnClickListener {
            addKeyIngredientRow()
        }

        // Setup Save Button
        binding.btnSave.setOnClickListener {
            saveProduct()
        }

        // Render initial empty images list
        renderImages()
    }

    private var selectedCategoryName: String = ""

    override fun observeViewModel() {
        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            if (!products.isNullOrEmpty()) {
                setupCategoriesSpinner(products)
                if (isEditMode && !isDataLoaded) {
                    val product = products.find { it.id == editingProductId }
                    if (product != null) {
                        isDataLoaded = true
                        populateProductData(product, products)
                    }
                }
            }
        }
    }

    private fun setupSubcategories(category: String, allProducts: List<ProductEntity>) {
        val subs = allProducts
            .filter { it.category.equals(category, ignoreCase = true) }
            .map { it.subcategory }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .distinct()
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (subs.isEmpty()) {
            when (category) {
                "Chăm sóc da" -> subs.addAll(listOf("Chăm Sóc Da Mặt", "Chăm Sóc Cơ Thể", "Chăm Sóc Mái Tóc", "Chăm Sóc Môi", "Chống Nắng"))
                "Combo & Bộ Sản Phẩm" -> subs.addAll(listOf("Chăm Sóc Da Mặt", "Chăm Sóc Mái Tóc"))
                else -> subs.addAll(listOf("Chăm sóc toàn thân", "Chăm sóc đặc biệt"))
            }
        }

        binding.layoutCategoryLevel1.setOnClickListener {
            val multiSelectSheet = ProductGenericBottomSheetFragment.newMultiSelectInstance(
                "Chọn phân loại con",
                subs,
                selectedSubcategoriesSet
            ) { selected ->
                selectedSubcategoriesSet.clear()
                selectedSubcategoriesSet.addAll(selected)
                if (selectedSubcategoriesSet.isEmpty()) {
                    binding.txtCategoryLevel1.text = "Chọn phân loại con"
                    binding.txtCategoryLevel1.setTextColor(resources.getColor(R.color.tertiary, null))
                } else {
                    binding.txtCategoryLevel1.text = selectedSubcategoriesSet.joinToString(", ")
                    binding.txtCategoryLevel1.setTextColor(resources.getColor(R.color.primary, null))
                }
            }
            multiSelectSheet.show(childFragmentManager, "CategoryLevel1BottomSheet")
        }
    }

    private fun populateProductData(product: ProductEntity, allProducts: List<ProductEntity>) {
        binding.txtTitle.text = "Chỉnh sửa sản phẩm"
        
        binding.edtProductName.setText(product.name)
        binding.edtSku.setText(product.sku)
        binding.edtStock.setText(product.stock.toString())
        binding.edtPrice.setText(product.price.toString())
        binding.edtOriginalPrice.setText(product.originalPrice?.toString() ?: "")
        
        // Category Level 0
        selectedCategoryName = product.category
        binding.txtCategoryLevel0.text = product.category
        binding.txtCategoryLevel0.setTextColor(resources.getColor(R.color.primary, null))
        
        // Subcategory Level 1
        selectedSubcategoriesSet.clear()
        val subsList = product.subcategory.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        selectedSubcategoriesSet.addAll(subsList)
        if (selectedSubcategoriesSet.isNotEmpty()) {
            binding.txtCategoryLevel1.text = selectedSubcategoriesSet.joinToString(", ")
            binding.txtCategoryLevel1.setTextColor(resources.getColor(R.color.primary, null))
        } else {
            binding.txtCategoryLevel1.text = "Chọn phân loại con"
            binding.txtCategoryLevel1.setTextColor(resources.getColor(R.color.tertiary, null))
        }
        
        setupSubcategories(product.category, allProducts)

        // Images
        selectedImages.clear()
        selectedImages.addAll(product.album)
        renderImages()

        // Ingredients Image
        ingredientsImageUrl = product.ingredientsImage
        if (ingredientsImageUrl.isNotEmpty()) {
            binding.imvIngredientsImage.visibility = View.VISIBLE
            binding.imvIngredientsImage.load(ingredientsImageUrl)
        }

        // Usage Media
        usageMediaUrl = product.usageMedia
        if (usageMediaUrl.isNotEmpty()) {
            binding.imvUsageMedia.visibility = View.VISIBLE
            binding.imvUsageMedia.load(usageMediaUrl)
        }

        // Key Ingredients
        binding.layoutKeyIngredientsContainer.removeAllViews()
        product.keyIngredients.forEach { ingredient ->
            addKeyIngredientRow(ingredient.name, ingredient.description)
        }

        binding.edtDescription.setText(product.description)
        binding.edtSuitableFor.setText(product.suitableFor)
        binding.edtMainIngredients.setText(product.mainIngredientsSummary)
        binding.edtAllergyInfo.setText(product.allergyInformation)
        binding.edtStoryDescription.setText(product.storyDescription)
        binding.edtBenefits.setText(product.benefits.joinToString("\n"))
        binding.edtDetailedIngredients.setText(product.detailedIngredients.joinToString(", "))
        binding.edtTexture.setText(product.texture)
        binding.edtScent.setText(product.scent)
        binding.edtNotes.setText(product.notes)
        binding.edtOrigin.setText(product.origin)
        binding.edtUsageAmount.setText(product.usageAmount)
        binding.edtUsage.setText(product.usage)
    }

    private fun setupCategoriesSpinner(allProducts: List<ProductEntity> = viewModel.allProducts.value ?: emptyList()) {
        val categories = allProducts.map { it.category }
            .distinct()
            .filter { it.isNotEmpty() && !it.equals("Chăm sóc tóc", ignoreCase = true) }
            .toMutableList()
        
        // Add defaults if empty
        if (categories.isEmpty()) {
            categories.addAll(listOf("Chăm sóc da", "Combo & Bộ Sản Phẩm"))
        }

        // Set click listener for Category Level 0 Bottom Sheet
        binding.layoutCategoryLevel0.setOnClickListener {
            val bottomSheet = ProductGenericBottomSheetFragment.newSingleSelectInstance(
                "Chọn danh mục",
                categories,
                selectedCategoryName
            ) { index ->
                val selectedCat = categories[index]
                selectedCategoryName = selectedCat
                binding.txtCategoryLevel0.text = selectedCat
                binding.txtCategoryLevel0.setTextColor(resources.getColor(R.color.primary, null))

                // Reset subcategory selection
                selectedSubcategoriesSet.clear()
                binding.txtCategoryLevel1.text = "Chọn phân loại con"
                binding.txtCategoryLevel1.setTextColor(resources.getColor(R.color.tertiary, null))

                setupSubcategories(selectedCat, allProducts)
            }
            bottomSheet.show(childFragmentManager, "CategoryLevel0BottomSheet")
        }
    }

    private fun showImageSourceDialog(
        onGallerySelected: () -> Unit,
        onCameraSelected: () -> Unit
    ) {
        val options = listOf("Chọn từ thư viện ảnh (Album)", "Chụp ảnh mới (Máy ảnh)")
        val bottomSheet = ProductGenericBottomSheetFragment.newSingleSelectInstance(
            "Chọn nguồn hình ảnh",
            options,
            ""
        ) { index ->
            when (index) {
                0 -> onGallerySelected()
                1 -> onCameraSelected()
            }
        }
        bottomSheet.show(childFragmentManager, "ImageSourceBottomSheet")
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }

    private fun addKeyIngredientRow(name: String = "", desc: String = "") {
        val rowView = layoutInflater.inflate(R.layout.item_key_ingredient_row, binding.layoutKeyIngredientsContainer, false)
        val edtName = rowView.findViewById<EditText>(R.id.edt_ingredient_name)
        val edtDesc = rowView.findViewById<EditText>(R.id.edt_ingredient_desc)
        val btnDelete = rowView.findViewById<View>(R.id.btn_delete_ingredient)

        edtName.setText(name)
        edtDesc.setText(desc)

        btnDelete.setOnClickListener {
            binding.layoutKeyIngredientsContainer.removeView(rowView)
        }

        binding.layoutKeyIngredientsContainer.addView(rowView)
    }

    private fun renderImages() {
        binding.layoutImagesContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        selectedImages.forEachIndexed { index, imageSource ->
            val itemView = inflater.inflate(R.layout.item_add_product_image, binding.layoutImagesContainer, false)
            val imvPhoto = itemView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imv_photo)
            val btnDelete = itemView.findViewById<ImageView>(R.id.btn_delete)

            imvPhoto.load(imageSource) {
                placeholder(R.color.gray_light)
                error(R.color.gray_light)
            }

            btnDelete.setOnClickListener {
                selectedImages.removeAt(index)
                renderImages()
            }

            binding.layoutImagesContainer.addView(itemView)
        }

        // Limit maximum 5 images
        if (selectedImages.size >= 5) {
            binding.btnAddImage.visibility = View.GONE
        } else {
            binding.btnAddImage.visibility = View.VISIBLE
        }
    }

    private fun saveProduct() {
        val name = binding.edtProductName.text.toString().trim()
        val sku = binding.edtSku.text.toString().trim()
        val stockStr = binding.edtStock.text.toString().trim()
        val priceStr = binding.edtPrice.text.toString().trim()
        val originalPriceStr = binding.edtOriginalPrice.text.toString().trim()

        if (name.isEmpty()) {
            binding.edtProductName.error = "Tên sản phẩm bắt buộc"
            return
        }
        if (sku.isEmpty()) {
            binding.edtSku.error = "SKU bắt buộc"
            return
        }
        if (stockStr.isEmpty()) {
            binding.edtStock.error = "Số lượng bắt buộc"
            return
        }
        if (priceStr.isEmpty()) {
            binding.edtPrice.error = "Giá bán bắt buộc"
            return
        }

        val stock = stockStr.toIntOrNull() ?: 0
        val price = priceStr.toLongOrNull() ?: 0L
        val originalPrice = originalPriceStr.toLongOrNull()

        val category = selectedCategoryName
        val subcategory = selectedSubcategoriesSet.joinToString(",")

        if (category.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn danh mục chính", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.edtDescription.text.toString().trim()
        val suitableFor = binding.edtSuitableFor.text.toString().trim()
        val mainIngredients = binding.edtMainIngredients.text.toString().trim()
        val allergyInfo = binding.edtAllergyInfo.text.toString().trim()
        val storyDescription = binding.edtStoryDescription.text.toString().trim()

        // Extract Dynamic Key Ingredients List
        val keyIngredients = mutableListOf<KeyIngredient>()
        for (i in 0 until binding.layoutKeyIngredientsContainer.childCount) {
            val row = binding.layoutKeyIngredientsContainer.getChildAt(i)
            val rowName = row.findViewById<EditText>(R.id.edt_ingredient_name).text.toString().trim()
            val rowDesc = row.findViewById<EditText>(R.id.edt_ingredient_desc).text.toString().trim()
            if (rowName.isNotEmpty() && rowDesc.isNotEmpty()) {
                keyIngredients.add(KeyIngredient(name = rowName, description = rowDesc))
            }
        }

        // Extract Benefits list (split by newline)
        val benefitsRaw = binding.edtBenefits.text.toString().trim()
        val benefits = if (benefitsRaw.isNotEmpty()) {
            benefitsRaw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // Extract Detailed Ingredients list (split by newline or comma)
        val detailedIngredientsRaw = binding.edtDetailedIngredients.text.toString().trim()
        val detailedIngredients = if (detailedIngredientsRaw.isNotEmpty()) {
            detailedIngredientsRaw.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        val texture = binding.edtTexture.text.toString().trim()
        val scent = binding.edtScent.text.toString().trim()
        val notes = binding.edtNotes.text.toString().trim()
        val origin = binding.edtOrigin.text.toString().trim()
        val usageAmount = binding.edtUsageAmount.text.toString().trim()
        val usage = binding.edtUsage.text.toString().trim()

        val mainImage = selectedImages.firstOrNull() ?: ""

        val productId = if (isEditMode) editingProductId ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()

        val product = ProductEntity(
            id = productId,
            name = name,
            sku = sku,
            price = price,
            originalPrice = originalPrice,
            category = category,
            subcategory = subcategory,
            stock = stock,
            description = description,
            mainImage = mainImage,
            album = selectedImages,
            suitableFor = suitableFor,
            mainIngredientsSummary = mainIngredients,
            allergyInformation = allergyInfo,
            storyDescription = storyDescription,
            storyImage = "",
            ingredientsImage = ingredientsImageUrl,
            keyIngredients = keyIngredients,
            benefits = benefits,
            texture = texture,
            scent = scent,
            notes = notes,
            origin = origin,
            usageAmount = usageAmount,
            usage = usage,
            usageMedia = usageMediaUrl,
            detailedIngredients = detailedIngredients
        )

        // Save using ViewModel
        viewModel.saveProduct(product)
        val successMsg = if (isEditMode) "Cập nhật sản phẩm thành công" else "Thêm sản phẩm thành công"
        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()

        // Return to product list
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PRODUCT_ID = "arg_product_id"
    }
}
