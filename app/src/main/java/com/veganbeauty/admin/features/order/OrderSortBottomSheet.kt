package com.veganbeauty.admin.features.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R

class OrderSortBottomSheet(
    private val currentSort: String,
    private val onSortSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.order_bottom_sheet_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }

        val ivRadioDefault = view.findViewById<ImageView>(R.id.ivRadioDefault)
        val ivRadioDateDesc = view.findViewById<ImageView>(R.id.ivRadioDateDesc)
        val ivRadioDateAsc = view.findViewById<ImageView>(R.id.ivRadioDateAsc)
        val ivRadioPriceDesc = view.findViewById<ImageView>(R.id.ivRadioPriceDesc)
        val ivRadioPriceAsc = view.findViewById<ImageView>(R.id.ivRadioPriceAsc)

        val radios = mapOf(
            "DEFAULT" to ivRadioDefault,
            "DATE_DESC" to ivRadioDateDesc,
            "DATE_ASC" to ivRadioDateAsc,
            "PRICE_DESC" to ivRadioPriceDesc,
            "PRICE_ASC" to ivRadioPriceAsc
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
            R.id.layoutSortDateDesc to "DATE_DESC",
            R.id.layoutSortDateAsc to "DATE_ASC",
            R.id.layoutSortPriceDesc to "PRICE_DESC",
            R.id.layoutSortPriceAsc to "PRICE_ASC"
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
