package com.veganbeauty.admin.features.product.list

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.admin.R
import com.veganbeauty.admin.data.local.entities.ProductEntity
import com.veganbeauty.admin.databinding.ProductItemBinding
import java.text.DecimalFormat

class ProductAdapter(
    private val onItemClick: (ProductEntity) -> Unit,
    private val onDeleteClick: (ProductEntity) -> Unit,
    private val onEditClick: (ProductEntity) -> Unit,
    private val onViewClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, ProductAdapter.ViewHolder>(ProductDiffCallback()) {

    class ViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        val context = holder.binding.root.context
        val formatter = DecimalFormat("#,###")

        with(holder.binding) {
            txtProductName.text = product.name
            txtPrice.text = "${formatter.format(product.price)}đ"

            // Strike-through original price for visuals
            val originalPrice = if (product.originalPrice != null && product.originalPrice > 0) {
                product.originalPrice
            } else {
                (product.price * 1.35).toLong()
            }
            txtOriginalPrice.text = "${formatter.format(originalPrice)}đ"
            txtOriginalPrice.paintFlags = txtOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Set up tags dynamically
            layoutTags.removeAllViews()
            val inflater = LayoutInflater.from(context)

            if (product.category.isNotEmpty()) {
                val tagTextView = inflater.inflate(R.layout.item_tag_pill, layoutTags, false) as android.widget.TextView
                tagTextView.text = product.category
                layoutTags.addView(tagTextView)
            }

            if (product.subcategory.isNotEmpty()) {
                val subcategories = product.subcategory.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (sub in subcategories) {
                    val tagTextView = inflater.inflate(R.layout.item_tag_pill, layoutTags, false) as android.widget.TextView
                    tagTextView.text = sub
                    
                    if (layoutTags.childCount > 0) {
                        val lp = tagTextView.layoutParams as android.widget.LinearLayout.LayoutParams
                        lp.marginStart = (4 * context.resources.displayMetrics.density).toInt()
                        tagTextView.layoutParams = lp
                    }
                    layoutTags.addView(tagTextView)
                }
            } else {
                val tagTextView = inflater.inflate(R.layout.item_tag_pill, layoutTags, false) as android.widget.TextView
                tagTextView.text = "Thuần chay"
                if (layoutTags.childCount > 0) {
                    val lp = tagTextView.layoutParams as android.widget.LinearLayout.LayoutParams
                    lp.marginStart = (4 * context.resources.displayMetrics.density).toInt()
                    tagTextView.layoutParams = lp
                }
                layoutTags.addView(tagTextView)
            }

            // Stock badge logic
            if (product.stock <= 5) {
                badgeStock.setBackgroundResource(R.drawable.bg_stock_low)
                imvStockIcon.setImageResource(R.drawable.ic_warning_triangle)
                txtStockQuantity.text = "Kho: ${product.stock}"
            } else {
                badgeStock.setBackgroundResource(R.drawable.bg_stock_normal)
                imvStockIcon.setImageResource(R.drawable.ic_warehouse)
                txtStockQuantity.text = "Kho: ${product.stock}"
            }

            // Load Image safely
            if (product.mainImage.isNotEmpty()) {
                val resourceId = context.resources.getIdentifier(
                    product.mainImage,
                    "drawable",
                    context.packageName
                )
                if (resourceId != 0) {
                    imvProduct.load(resourceId) {
                        crossfade(true)
                        placeholder(R.color.gray_light)
                        error(R.color.gray_light)
                    }
                } else {
                    imvProduct.load(product.mainImage) {
                        crossfade(true)
                        placeholder(R.color.gray_light)
                        error(R.color.gray_light)
                    }
                }
            } else {
                imvProduct.setImageResource(R.color.gray_light)
            }

            // Eye icon hidden/visible state
            if (product.isHidden) {
                btnView.setImageResource(R.drawable.ic_eye_off)
                btnView.alpha = 0.35f
                btnView.setColorFilter(android.graphics.Color.parseColor("#7E8A83"))
            } else {
                btnView.setImageResource(R.drawable.ic_eye)
                btnView.alpha = 1.0f
                btnView.setColorFilter(android.graphics.Color.parseColor("#677559"))
            }

            // Click listeners
            btnDelete.setOnClickListener { onDeleteClick(product) }
            btnEdit.setOnClickListener { onEditClick(product) }
            btnView.setOnClickListener { onViewClick(product) }
            root.setOnClickListener { onItemClick(product) }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
