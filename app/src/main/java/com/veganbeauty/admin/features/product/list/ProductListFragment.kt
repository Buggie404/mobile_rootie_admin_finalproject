package com.veganbeauty.admin.features.product.list

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.databinding.ProductFragmentListBinding
import com.veganbeauty.admin.features.home.BottomNavHelper
import com.veganbeauty.admin.features.product.ProductViewModel
import com.veganbeauty.admin.features.product.add.ProductAddFragment


class ProductListFragment : RootieAdminFragment() {

    private var _binding: ProductFragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductViewModel
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProductFragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        viewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        // Setup RecyclerView with Grid Layout
        adapter = ProductAdapter(
            onItemClick = { product ->
                showProductDetails(product)
            },
            onDeleteClick = { product ->
                confirmDelete(product)
            },
            onEditClick = { product ->
                (activity as? MainActivity)?.let { mainAct ->
                    mainAct.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
                    val editFragment = ProductAddFragment().apply {
                        arguments = Bundle().apply {
                            putString(ProductAddFragment.ARG_PRODUCT_ID, product.id)
                        }
                    }
                    mainAct.supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, editFragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onViewClick = { product ->
                viewModel.toggleProductVisibility(product)
                val statusMessage = if (product.isHidden) "Đã hiện sản phẩm" else "Đã ẩn sản phẩm"
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvProducts.layoutManager = GridLayoutManager(context, 2)
        binding.rvProducts.adapter = adapter

        // Search text listener
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchQuery.value = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sort button click listener
        binding.btnSort.setOnClickListener {
            val sortSheet = ProductSortBottomSheet(viewModel.sortOrder.value ?: "DEFAULT") { selectedSort ->
                viewModel.sortOrder.value = selectedSort
            }
            sortSheet.show(childFragmentManager, "SortBottomSheet")
        }

        // Filter button click listener
        binding.btnFilter.setOnClickListener {
            val filterSheet = ProductFilterBottomSheet()
            filterSheet.show(childFragmentManager, "FilterBottomSheet")
        }

        // Add Product button click listener
        binding.btnAddProduct.setOnClickListener {
            (activity as? MainActivity)?.let { mainAct ->
                mainAct.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE
                mainAct.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, ProductAddFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }


        // Sync and observe data
        viewModel.syncFromFirebase()

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage)
    }

    override fun observeViewModel() {
        viewModel.filteredProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
            binding.txtTotalProducts.text = "Tổng cộng ${products.size} sản phẩm"
        }
    }

    private fun confirmDelete(product: ProductEntity) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_delete, null)
        val tvMessage = view.findViewById<android.widget.TextView>(R.id.tvMessage)
        tvMessage.text = "Bạn chắc chắn muốn xoá sản phẩm ${product.name}?"

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            viewModel.deleteProduct(product)
            Toast.makeText(context, "Đã xóa ${product.name}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showProductDetails(product: ProductEntity) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_product_detail, null)
        val tvName = view.findViewById<android.widget.TextView>(R.id.tvProductName)
        val tvPrice = view.findViewById<android.widget.TextView>(R.id.tvProductPrice)
        val tvStock = view.findViewById<android.widget.TextView>(R.id.tvProductStock)
        val tvSku = view.findViewById<android.widget.TextView>(R.id.tvProductSku)

        tvName.text = product.name
        val formatter = java.text.DecimalFormat("#,###")
        tvPrice.text = "Giá bán: ${formatter.format(product.price)}đ"
        tvStock.text = "Số lượng kho: ${product.stock}"
        tvSku.text = "Mã sản phẩm (SKU): ${product.sku}"

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
