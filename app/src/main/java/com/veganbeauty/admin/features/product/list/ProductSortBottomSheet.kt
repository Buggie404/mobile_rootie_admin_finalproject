package com.veganbeauty.admin.features.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R

class ProductSortBottomSheet(
    private val currentSort: String,
    private val onSortSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.product_bottom_sheet_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }

        val ivRadioDefault = view.findViewById<ImageView>(R.id.ivRadioDefault)
        val ivRadioPriceAsc = view.findViewById<ImageView>(R.id.ivRadioPriceAsc)
        val ivRadioPriceDesc = view.findViewById<ImageView>(R.id.ivRadioPriceDesc)
        val ivRadioNameAsc = view.findViewById<ImageView>(R.id.ivRadioNameAsc)
        val ivRadioNameDesc = view.findViewById<ImageView>(R.id.ivRadioNameDesc)

        val radios = mapOf(
            "DEFAULT" to ivRadioDefault,
            "PRICE_ASC" to ivRadioPriceAsc,
            "PRICE_DESC" to ivRadioPriceDesc,
            "NAME_ASC" to ivRadioNameAsc,
            "NAME_DESC" to ivRadioNameDesc
        )

        fun updateUI(selected: String) {
            radios.forEach { (key, imageView) ->
                if (key == selected) {
                    imageView.setImageResource(R.drawable.ic_radio_primary_checked)
                } else {
                    imageView.setImageResource(R.drawable.ic_radio_primary_unchecked)
                }
            }
        }

        updateUI(currentSort)

        val layouts = mapOf(
            R.id.layoutSortDefault to "DEFAULT",
            R.id.layoutSortPriceAsc to "PRICE_ASC",
            R.id.layoutSortPriceDesc to "PRICE_DESC",
            R.id.layoutSortNameAsc to "NAME_ASC",
            R.id.layoutSortNameDesc to "NAME_DESC"
        )

        layouts.forEach { (layoutId, sortType) ->
            view.findViewById<LinearLayout>(layoutId).setOnClickListener {
                updateUI(sortType)
                onSortSelected(sortType)
                dismiss()
            }
        }
    }
}
