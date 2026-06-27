package com.veganbeauty.admin.features.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R
import com.veganbeauty.admin.features.product.ProductViewModel

class ProductFilterBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewModel: ProductViewModel

    // Temporary state to hold choices before Apply
    private var tempSelectedCategory: String = ""
    private val tempSelectedSubcategories = mutableSetOf<String>()
    private var tempStockStatus: String = "ALL"
    private var tempHiddenStatus: String = "ALL"

    // Keep track of all checkboxes/radio buttons to manage single category selection
    private val categoryCheckboxes = mutableMapOf<String, CheckBox>() // Category -> "Tất cả" Checkbox
    private val subcategoryCheckboxes = mutableMapOf<String, List<CheckBox>>() // Category -> list of Subcategory Checkboxes

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireParentFragment())[ProductViewModel::class.java]
        return inflater.inflate(R.layout.product_bottom_sheet_filter, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current filters
        tempSelectedCategory = viewModel.selectedCategory.value ?: ""
        tempSelectedSubcategories.addAll(viewModel.selectedSubcategories.value ?: emptySet())
        tempStockStatus = viewModel.filterStockStatus.value ?: "ALL"
        tempHiddenStatus = viewModel.filterHiddenStatus.value ?: "ALL"

        // Close button
        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }

        // Setup Stock Status RadioGroup
        val rgStockStatus = view.findViewById<RadioGroup>(R.id.rgStockStatus)
        when (tempStockStatus) {
            "IN_STOCK" -> rgStockStatus.check(R.id.rbStockIn)
            "OUT_OF_STOCK" -> rgStockStatus.check(R.id.rbStockOut)
            else -> rgStockStatus.check(R.id.rbStockAll)
        }
        rgStockStatus.setOnCheckedChangeListener { _, checkedId ->
            tempStockStatus = when (checkedId) {
                R.id.rbStockIn -> "IN_STOCK"
                R.id.rbStockOut -> "OUT_OF_STOCK"
                else -> "ALL"
            }
        }

        // Setup Visibility Status RadioGroup
        val rgVisibilityStatus = view.findViewById<RadioGroup>(R.id.rgVisibilityStatus)
        when (tempHiddenStatus) {
            "HIDDEN" -> rgVisibilityStatus.check(R.id.rbVisibilityHidden)
            "VISIBLE" -> rgVisibilityStatus.check(R.id.rbVisibilityVisible)
            else -> rgVisibilityStatus.check(R.id.rbVisibilityAll)
        }
        rgVisibilityStatus.setOnCheckedChangeListener { _, checkedId ->
            tempHiddenStatus = when (checkedId) {
                R.id.rbVisibilityHidden -> "HIDDEN"
                R.id.rbVisibilityVisible -> "VISIBLE"
                else -> "ALL"
            }
        }

        // Setup Categories Spinner
        // Dynamic categories & subcategories setup
        setupCategoriesContainer(view)

        // Reset Button
        view.findViewById<View>(R.id.btnReset).setOnClickListener {
            tempSelectedCategory = ""
            tempSelectedSubcategories.clear()
            tempStockStatus = "ALL"
            tempHiddenStatus = "ALL"

            rgStockStatus.check(R.id.rbStockAll)
            rgVisibilityStatus.check(R.id.rbVisibilityAll)

            // Clear checkboxes UI
            categoryCheckboxes.values.forEach { it.isChecked = false }
            subcategoryCheckboxes.values.flatten().forEach { it.isChecked = false }
        }

        // Apply Button
        view.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            viewModel.selectedCategory.value = tempSelectedCategory
            viewModel.selectedSubcategories.value = tempSelectedSubcategories.toSet()
            viewModel.filterStockStatus.value = tempStockStatus
            viewModel.filterHiddenStatus.value = tempHiddenStatus
            dismiss()
        }
    }

    private fun setupCategoriesContainer(rootView: View) {
        val container = rootView.findViewById<LinearLayout>(R.id.layoutCategoriesContainer)
        container.removeAllViews()

        val products = viewModel.allProducts.value ?: emptyList()
        val categories = products.map { it.category }.distinct().filter { it.isNotEmpty() }

        val inflater = LayoutInflater.from(context)

        for (category in categories) {
            val categoryView = inflater.inflate(R.layout.product_item_filter_category_header, container, false)
            val tvCategoryName = categoryView.findViewById<TextView>(R.id.tvCategoryName)
            val ivChevron = categoryView.findViewById<ImageView>(R.id.ivChevron)
            val layoutCategoryHeader = categoryView.findViewById<LinearLayout>(R.id.layoutCategoryHeader)
            val layoutSubcategoriesContainer = categoryView.findViewById<LinearLayout>(R.id.layoutSubcategoriesContainer)

            tvCategoryName.text = category

            // Expand/Collapse click listener
            layoutCategoryHeader.setOnClickListener {
                val isVisible = layoutSubcategoriesContainer.visibility == View.VISIBLE
                layoutSubcategoriesContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
                ivChevron.rotation = if (isVisible) 0f else 180f
            }

            // Get subcategories of this category
            val subcategories = products
                .filter { it.category.equals(category, ignoreCase = true) }
                .map { it.subcategory }
                .flatMap { it.split(",") }
                .map { it.trim() }
                .distinct()
                .filter { it.isNotEmpty() }

            val subCheckboxesList = mutableListOf<CheckBox>()

            // 1. Add "Tất cả [Category]" checkbox
            val allCategoryCb = inflater.inflate(R.layout.product_item_subcategory_checkbox, layoutSubcategoriesContainer, false) as CheckBox
            allCategoryCb.text = "Tất cả $category"
            allCategoryCb.isChecked = (tempSelectedCategory == category && tempSelectedSubcategories.isEmpty())
            categoryCheckboxes[category] = allCategoryCb

            allCategoryCb.setOnClickListener {
                val isChecked = allCategoryCb.isChecked
                if (isChecked) {
                    // Select this category, clear others
                    tempSelectedCategory = category
                    tempSelectedSubcategories.clear()
                    updateCategorySelectionUI(category, null)
                } else {
                    if (tempSelectedCategory == category) {
                        tempSelectedCategory = ""
                    }
                }
            }
            layoutSubcategoriesContainer.addView(allCategoryCb)

            // 2. Add each Subcategory checkbox
            for (sub in subcategories) {
                val subCb = inflater.inflate(R.layout.product_item_subcategory_checkbox, layoutSubcategoriesContainer, false) as CheckBox
                subCb.text = sub
                subCb.isChecked = tempSelectedSubcategories.contains(sub)
                subCheckboxesList.add(subCb)

                subCb.setOnClickListener {
                    val isChecked = subCb.isChecked
                    if (isChecked) {
                        // Switch category if needed
                        if (tempSelectedCategory != category) {
                            tempSelectedCategory = category
                            tempSelectedSubcategories.clear()
                        }
                        tempSelectedSubcategories.add(sub)
                        updateCategorySelectionUI(category, subCb)
                    } else {
                        tempSelectedSubcategories.remove(sub)
                        if (tempSelectedSubcategories.isEmpty() && tempSelectedCategory == category) {
                            // If no subcategories checked, keep category selected
                            categoryCheckboxes[category]?.isChecked = true
                        }
                    }
                }
                layoutSubcategoriesContainer.addView(subCb)
            }

            subcategoryCheckboxes[category] = subCheckboxesList
            container.addView(categoryView)

            // If this category is currently selected, expand it by default
            if (tempSelectedCategory == category) {
                layoutSubcategoriesContainer.visibility = View.VISIBLE
                ivChevron.rotation = 180f
            }
        }
    }

    private fun updateCategorySelectionUI(selectedCategory: String, activeSubCb: CheckBox?) {
        // Uncheck all other categories and their subcategories
        categoryCheckboxes.forEach { (cat, cb) ->
            if (cat != selectedCategory) {
                cb.isChecked = false
            } else {
                cb.isChecked = (activeSubCb == null)
            }
        }

        subcategoryCheckboxes.forEach { (cat, cbs) ->
            if (cat != selectedCategory) {
                cbs.forEach { it.isChecked = false }
            }
        }
    }
}
