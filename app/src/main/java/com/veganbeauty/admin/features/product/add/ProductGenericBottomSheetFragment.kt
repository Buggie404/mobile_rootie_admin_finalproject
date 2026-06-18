package com.veganbeauty.admin.features.product.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R

class ProductGenericBottomSheetFragment : BottomSheetDialogFragment() {

    private var title: String = ""
    private var options: List<String> = emptyList()
    private var isMultiSelect: Boolean = false
    private var currentSelected: Set<String> = emptySet()
    
    private var onSingleItemSelected: ((Int) -> Unit)? = null
    private var onMultiItemsSelected: ((Set<String>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.product_bottom_sheet_generic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Header Title
        view.findViewById<TextView>(R.id.txt_title).text = title

        // Close button click listener
        view.findViewById<ImageView>(R.id.btn_close).setOnClickListener { dismiss() }

        val container = view.findViewById<LinearLayout>(R.id.layout_options_container)
        container.removeAllViews()

        val confirmBtn = view.findViewById<Button>(R.id.btn_confirm)

        if (isMultiSelect) {
            confirmBtn.visibility = View.VISIBLE
            val tempSelection = mutableSetOf<String>().apply { addAll(currentSelected) }

            options.forEach { option ->
                val checkbox = layoutInflater.inflate(R.layout.product_item_subcategory_checkbox, container, false) as CheckBox
                checkbox.text = option
                checkbox.isChecked = tempSelection.contains(option)
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        tempSelection.add(option)
                    } else {
                        tempSelection.remove(option)
                    }
                }
                container.addView(checkbox)
            }

            confirmBtn.setOnClickListener {
                onMultiItemsSelected?.invoke(tempSelection)
                dismiss()
            }
        } else {
            confirmBtn.visibility = View.GONE

            options.forEachIndexed { index, option ->
                val itemView = layoutInflater.inflate(R.layout.item_bottom_sheet_option, container, false)
                val txtName = itemView.findViewById<TextView>(R.id.txt_option_name)
                val ivRadio = itemView.findViewById<ImageView>(R.id.iv_radio)

                txtName.text = option
                val isSelected = currentSelected.contains(option) || (currentSelected.isEmpty() && option.equals(currentSelected.firstOrNull() ?: "", ignoreCase = true))
                if (isSelected) {
                    ivRadio.setImageResource(R.drawable.ic_radio_primary_checked)
                } else {
                    ivRadio.setImageResource(R.drawable.ic_radio_primary_unchecked)
                }

                itemView.setOnClickListener {
                    onSingleItemSelected?.invoke(index)
                    dismiss()
                }
                container.addView(itemView)
            }
        }
    }

    companion object {
        fun newSingleSelectInstance(
            title: String,
            options: List<String>,
            currentSelected: String,
            onItemSelected: (Int) -> Unit
        ): ProductGenericBottomSheetFragment {
            return ProductGenericBottomSheetFragment().apply {
                this.title = title
                this.options = options
                this.isMultiSelect = false
                this.currentSelected = if (currentSelected.isNotEmpty()) setOf(currentSelected) else emptySet()
                this.onSingleItemSelected = onItemSelected
            }
        }

        fun newMultiSelectInstance(
            title: String,
            options: List<String>,
            currentSelected: Set<String>,
            onSelected: (Set<String>) -> Unit
        ): ProductGenericBottomSheetFragment {
            return ProductGenericBottomSheetFragment().apply {
                this.title = title
                this.options = options
                this.isMultiSelect = true
                this.currentSelected = currentSelected
                this.onMultiItemsSelected = onSelected
            }
        }
    }
}
