package com.veganbeauty.admin.features.customer

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.R
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.databinding.CustomerCreateCampaignBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomerCreateCampaignFragment : RootieAdminFragment() {

    private var _binding: CustomerCreateCampaignBinding? = null
    private val binding get() = _binding!!

    private val categoriesList = mutableListOf<String>()
    private val offerTypes = listOf("Giảm giá phần trăm (%)", "Giảm giá cố định (VNĐ)")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CustomerCreateCampaignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        setupListeners()
        loadCategories()
        setupOfferTypeSpinner()
        setupInputWatchers()
        updateSaveButtonState()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.btnCancel.setOnClickListener {
            navigateBack()
        }

        // Date selection inputs
        binding.edtStartDate.setOnClickListener {
            showDatePicker(binding.edtStartDate)
        }

        binding.edtEndDate.setOnClickListener {
            showDatePicker(binding.edtEndDate)
        }

        // Save campaign
        binding.btnSaveCampaign.setOnClickListener {
            saveCampaign()
        }
    }

    private fun setupInputWatchers() {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveButtonState()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.edtCampaignName.addTextChangedListener(watcher)
        binding.edtCampaignDesc.addTextChangedListener(watcher)
        binding.edtMinOrderValue.addTextChangedListener(watcher)
        binding.edtDiscountValue.addTextChangedListener(watcher)
        binding.edtStartDate.addTextChangedListener(watcher)
        binding.edtEndDate.addTextChangedListener(watcher)
    }

    private fun updateSaveButtonState() {
        val title = binding.edtCampaignName.text.toString().trim()
        val description = binding.edtCampaignDesc.text.toString().trim()
        val minOrder = binding.edtMinOrderValue.text.toString().trim()
        val discount = binding.edtDiscountValue.text.toString().trim()
        val startDate = binding.edtStartDate.text.toString().trim()
        val endDate = binding.edtEndDate.text.toString().trim()

        val allFilled = title.isNotEmpty() && 
                        description.isNotEmpty() && 
                        minOrder.isNotEmpty() && 
                        discount.isNotEmpty() && 
                        startDate.isNotEmpty() && 
                        endDate.isNotEmpty()

        binding.btnSaveCampaign.isEnabled = allFilled
        binding.btnSaveCampaign.alpha = if (allFilled) 1.0f else 0.5f
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val currentText = editText.text.toString()
        if (currentText.isNotEmpty()) {
            try {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(currentText)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Ignore parsing errors and use current calendar date
            }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(Calendar.YEAR, year)
                selectedCal.set(Calendar.MONTH, month)
                selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                editText.setText(sdf.format(selectedCal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val loadedCategories = withContext(Dispatchers.IO) {
                val list = mutableListOf<String>()
                list.add("Tất cả sản phẩm") // Default option
                try {
                    val inputStream = requireContext().assets.open("categories.json")
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.use { it.readText() }
                    val jsonObject = JSONObject(jsonString)
                    val jsonArray = jsonObject.getJSONArray("categories")
                    for (i in 0 until jsonArray.length()) {
                        val catObj = jsonArray.getJSONObject(i)
                        val name = catObj.optString("name", "")
                        if (name.isNotEmpty() && !list.contains(name)) {
                            list.add(name)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            }

            categoriesList.clear()
            categoriesList.addAll(loadedCategories)

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoriesList
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spnApplicableProducts.adapter = adapter
        }
    }

    private fun setupOfferTypeSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            offerTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnOfferType.adapter = adapter

        binding.spnOfferType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    binding.txtDiscountLabel.text = "Giá trị (%)"
                    binding.edtDiscountValue.hint = "10"
                } else {
                    binding.txtDiscountLabel.text = "Giá trị (VNĐ)"
                    binding.edtDiscountValue.hint = "50000"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun saveCampaign() {
        val title = binding.edtCampaignName.text.toString().trim()
        val description = binding.edtCampaignDesc.text.toString().trim()
        val minOrderStr = binding.edtMinOrderValue.text.toString().trim()
        val discountValStr = binding.edtDiscountValue.text.toString().trim()
        val startDateStr = binding.edtStartDate.text.toString().trim()
        val endDateStr = binding.edtEndDate.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên chương trình!", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập mô tả chương trình!", Toast.LENGTH_SHORT).show()
            return
        }

        val minOrderValue = minOrderStr.toLongOrNull() ?: 0L
        val discountValue = discountValStr.toLongOrNull()

        if (discountValue == null || discountValue <= 0) {
            Toast.makeText(requireContext(), "Vui lòng nhập giá trị khuyến mãi hợp lệ!", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn ngày kết thúc!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // 1. Get writeable file in internal storage
                    val internalFile = File(requireContext().filesDir, "vouchers.json")
                    
                    // 2. Load existing vouchers
                    val jsonArray = if (internalFile.exists()) {
                        val text = internalFile.readText()
                        JSONArray(text)
                    } else {
                        // Load from assets initially
                        val inputStream = requireContext().assets.open("vouchers.json")
                        val text = inputStream.bufferedReader().use { it.readText() }
                        JSONArray(text)
                    }

                    // 3. Generate unique ID & Code
                    var maxVal = 0
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val idStr = item.optString("id", "")
                        if (idStr.startsWith("v")) {
                            idStr.substring(1).toIntOrNull()?.let { num ->
                                if (num > maxVal) maxVal = num
                            }
                        }
                    }
                    val nextIdNum = maxVal + 1
                    val newId = "v${String.format(Locale.getDefault(), "%03d", nextIdNum)}"

                    // Voucher Type Mapping
                    val voucherType = if (binding.radTypeDiscount.isChecked) "discount" else "free ship"

                    // Offer Type Mapping
                    val offerType = if (binding.spnOfferType.selectedItemPosition == 0) "percentage" else "fixed_amount"

                    // Auto-generated Voucher Code
                    val autoCode = generateVoucherCode(title, offerType, discountValue)

                    // Format HSD: end date with 23:59:59 time
                    val hsdVal = try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = inputFormat.parse(endDateStr)
                        if (date != null) {
                            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            "${outputFormat.format(date)} 23:59:59"
                        } else {
                            "2026-12-31 23:59:59"
                        }
                    } catch (e: Exception) {
                        "2026-12-31 23:59:59"
                    }

                    // Selected applicable category name
                    val selectedCategory = binding.spnApplicableProducts.selectedItem?.toString() ?: "Tất cả sản phẩm"

                    // 4. Create new voucher JSON object
                    val newVoucher = JSONObject().apply {
                        put("id", newId)
                        put("title", title)
                        put("description", description)
                        put("code", autoCode)
                        put("status", "valid")
                        put("hsd", hsdVal)
                        put("type", voucherType)
                        put("from-gift", false)
                        put("quantity", 50) // Default quantity
                        put("minOrderValue", minOrderValue)
                        put("applicableProducts", selectedCategory)
                        put("offerType", offerType)
                        put("discountValue", discountValue)
                    }

                    // 5. Append & Save
                    jsonArray.put(newVoucher)
                    FileOutputStream(internalFile).use { fos ->
                        fos.write(jsonArray.toString(2).toByteArray())
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(requireContext(), "Tạo chương trình khuyến mãi thành công!", Toast.LENGTH_SHORT).show()
            navigateBack()
        }
    }

    private fun generateVoucherCode(campaignName: String, offerType: String, discountValue: Long): String {
        // Create a prefix from campaign name
        val words = campaignName.split(" ").filter { it.isNotEmpty() }
        val prefix = words.take(3)
            .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
            .filter { it.isLetter() }

        val cleanPrefix = if (prefix.isEmpty()) "KM" else prefix
        val suffix = if (offerType == "percentage") "${discountValue}PCT" else "${discountValue / 1000}K"
        val charPool = ('A'..'Z') + ('0'..'9')
        val randomPart = (1..3).map { charPool.random() }.joinToString("")
        
        return "$cleanPrefix$suffix$randomPart".take(12).uppercase(Locale.getDefault())
    }

    private fun navigateBack() {
        val mainActivity = activity as? MainActivity ?: return
        mainActivity.loadFragment(CustomerAdminFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
