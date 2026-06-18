package com.veganbeauty.admin.features.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.CheckBox
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R

class OrderFilterBottomSheet(
    private val initialPayments: Set<String>,
    private val initialPrices: Set<String>,
    private val initialRegions: Set<String>,
    private val onFiltersApplied: (payments: Set<String>, prices: Set<String>, regions: Set<String>) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.order_bottom_sheet_filter, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivClose).setOnClickListener { dismiss() }

        val layoutPaymentHeader = view.findViewById<LinearLayout>(R.id.layoutPaymentHeader)
        val layoutPaymentContent = view.findViewById<LinearLayout>(R.id.layoutPaymentContent)
        val ivPaymentChevron = view.findViewById<ImageView>(R.id.ivPaymentChevron)

        val layoutPriceHeader = view.findViewById<LinearLayout>(R.id.layoutPriceHeader)
        val layoutPriceContent = view.findViewById<LinearLayout>(R.id.layoutPriceContent)
        val ivPriceChevron = view.findViewById<ImageView>(R.id.ivPriceChevron)

        val layoutRegionHeader = view.findViewById<LinearLayout>(R.id.layoutRegionHeader)
        val layoutRegionContent = view.findViewById<LinearLayout>(R.id.layoutRegionContent)
        val ivRegionChevron = view.findViewById<ImageView>(R.id.ivRegionChevron)

        val cbPaymentCOD = view.findViewById<CheckBox>(R.id.cbPaymentCOD)
        val cbPaymentATM = view.findViewById<CheckBox>(R.id.cbPaymentATM)
        val cbPaymentMoMo = view.findViewById<CheckBox>(R.id.cbPaymentMoMo)
        val cbPaymentVNPay = view.findViewById<CheckBox>(R.id.cbPaymentVNPay)

        val cbPriceUnder500k = view.findViewById<CheckBox>(R.id.cbPriceUnder500k)
        val cbPrice500kTo1500k = view.findViewById<CheckBox>(R.id.cbPrice500kTo1500k)
        val cbPriceOver1500k = view.findViewById<CheckBox>(R.id.cbPriceOver1500k)

        val cbRegionHCMC = view.findViewById<CheckBox>(R.id.cbRegionHCMC)
        val cbRegionHN = view.findViewById<CheckBox>(R.id.cbRegionHN)
        val cbRegionOthers = view.findViewById<CheckBox>(R.id.cbRegionOthers)

        // Expand/Collapse click listeners
        layoutPaymentHeader.setOnClickListener {
            val isVisible = layoutPaymentContent.visibility == View.VISIBLE
            layoutPaymentContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivPaymentChevron.rotation = if (isVisible) 0f else 180f
        }

        layoutPriceHeader.setOnClickListener {
            val isVisible = layoutPriceContent.visibility == View.VISIBLE
            layoutPriceContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivPriceChevron.rotation = if (isVisible) 0f else 180f
        }

        layoutRegionHeader.setOnClickListener {
            val isVisible = layoutRegionContent.visibility == View.VISIBLE
            layoutRegionContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            ivRegionChevron.rotation = if (isVisible) 0f else 180f
        }

        // Set initial state
        cbPaymentCOD.isChecked = initialPayments.contains("COD")
        cbPaymentATM.isChecked = initialPayments.contains("ATM")
        cbPaymentMoMo.isChecked = initialPayments.contains("MOMO")
        cbPaymentVNPay.isChecked = initialPayments.contains("VNPAY")

        cbPriceUnder500k.isChecked = initialPrices.contains("UNDER_500K")
        cbPrice500kTo1500k.isChecked = initialPrices.contains("500K_TO_1500K")
        cbPriceOver1500k.isChecked = initialPrices.contains("OVER_1500K")

        cbRegionHCMC.isChecked = initialRegions.contains("HCMC")
        cbRegionHN.isChecked = initialRegions.contains("HN")
        cbRegionOthers.isChecked = initialRegions.contains("OTHERS")

        // Payment Method is expanded by default
        layoutPaymentContent.visibility = View.VISIBLE
        ivPaymentChevron.rotation = 180f

        // Auto-expand other sections that have active filters
        if (initialPrices.isNotEmpty()) {
            layoutPriceContent.visibility = View.VISIBLE
            ivPriceChevron.rotation = 180f
        }
        if (initialRegions.isNotEmpty()) {
            layoutRegionContent.visibility = View.VISIBLE
            ivRegionChevron.rotation = 180f
        }

        // Reset click listener
        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            cbPaymentCOD.isChecked = false
            cbPaymentATM.isChecked = false
            cbPaymentMoMo.isChecked = false
            cbPaymentVNPay.isChecked = false

            cbPriceUnder500k.isChecked = false
            cbPrice500kTo1500k.isChecked = false
            cbPriceOver1500k.isChecked = false

            cbRegionHCMC.isChecked = false
            cbRegionHN.isChecked = false
            cbRegionOthers.isChecked = false
        }

        // Confirm click listener
        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val selectedPayments = mutableSetOf<String>()
            if (cbPaymentCOD.isChecked) selectedPayments.add("COD")
            if (cbPaymentATM.isChecked) selectedPayments.add("ATM")
            if (cbPaymentMoMo.isChecked) selectedPayments.add("MOMO")
            if (cbPaymentVNPay.isChecked) selectedPayments.add("VNPAY")

            val selectedPrices = mutableSetOf<String>()
            if (cbPriceUnder500k.isChecked) selectedPrices.add("UNDER_500K")
            if (cbPrice500kTo1500k.isChecked) selectedPrices.add("500K_TO_1500K")
            if (cbPriceOver1500k.isChecked) selectedPrices.add("OVER_1500K")

            val selectedRegions = mutableSetOf<String>()
            if (cbRegionHCMC.isChecked) selectedRegions.add("HCMC")
            if (cbRegionHN.isChecked) selectedRegions.add("HN")
            if (cbRegionOthers.isChecked) selectedRegions.add("OTHERS")

            onFiltersApplied(selectedPayments, selectedPrices, selectedRegions)
            dismiss()
        }
    }
}
