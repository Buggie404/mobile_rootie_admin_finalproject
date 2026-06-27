package com.veganbeauty.admin.features.customer

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.admin.R
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.CustomerEntity
import com.veganbeauty.admin.databinding.CustomerNoteBottomSheetBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.veganbeauty.admin.core.utils.KeyboardUtils

class CustomerNoteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: CustomerNoteBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var customerId: String? = null
    private var currentCustomer: CustomerEntity? = null
    
    var onNoteSaved: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            customerId = it.getString(ARG_CUSTOMER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CustomerNoteBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtils.setupKeyboardAutoHiding(view, activity)
        setupListeners()
        loadCustomerData()
    }

    private fun loadCustomerData() {
        val id = customerId ?: return
        lifecycleScope.launch {
            val db = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
            currentCustomer = withContext(Dispatchers.IO) {
                db.customerDao().getByIdSync(id)
            }
            currentCustomer?.let {
                bindCustomer(it)
            } ?: run {
                Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun bindCustomer(customer: CustomerEntity) {
        with(binding) {
            txtName.text = customer.name
            txtLastActive.text = "Mua lần cuối: ${customer.lastActive}"

            // Tier Badge
            badgeTier.text = customer.tier
            when (customer.tier.lowercase()) {
                "vip" -> {
                    badgeTier.setTextColor(Color.parseColor("#4F6544"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E8DA"))
                }
                "vàng" -> {
                    badgeTier.setTextColor(Color.parseColor("#FFFFFF"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C69C2C"))
                }
                "bạc" -> {
                    badgeTier.setTextColor(Color.parseColor("#FFFFFF"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                }
                else -> { // Thường / Normal
                    badgeTier.setTextColor(Color.parseColor("#616161"))
                    badgeTier.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5F5F5"))
                }
            }

            // Avatar
            if (customer.avatar.isNotEmpty()) {
                imgAvatar.load(customer.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.imv_avatar)
                    error(R.drawable.imv_avatar)
                }
            } else {
                imgAvatar.setImageResource(R.drawable.imv_avatar)
            }

            // Previous notes
            if (customer.notes.isNotEmpty()) {
                txtPreviousNotes.text = customer.notes
            } else {
                txtPreviousNotes.text = "Chưa có ghi chú nào."
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Tags
        binding.tagSkin.setOnClickListener { appendTag("#Tình_trạng_da") }
        binding.tagReaction.setOnClickListener { appendTag("#Phản_ứng_SP") }
        binding.tagFollowup.setOnClickListener { appendTag("#Follow_up") }
        binding.tagRecommendation.setOnClickListener { appendTag("#Khuyến_nghị") }

        // Save
        binding.btnSave.setOnClickListener {
            saveNote()
        }
    }

    private fun appendTag(tag: String) {
        val currentText = binding.edtNote.text.toString()
        val space = if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " " else ""
        binding.edtNote.append("$space$tag ")
    }

    private fun saveNote() {
        val noteContent = binding.edtNote.text.toString().trim()
        if (noteContent.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập nội dung ghi chú", Toast.LENGTH_SHORT).show()
            return
        }

        val customer = currentCustomer ?: return
        
        // Format the new note with date
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val formattedNewNote = "[$currentDate] $noteContent"

        // Append to existing notes
        val updatedNotes = if (customer.notes.isNotEmpty()) {
            "${customer.notes}\n\n$formattedNewNote"
        } else {
            formattedNewNote
        }

        val updatedCustomer = customer.copy(notes = updatedNotes)

        lifecycleScope.launch {
            val db = RootieAdminDatabase.getDatabase(requireContext().applicationContext)
            withContext(Dispatchers.IO) {
                // We assume there's an updateSync method or insert with replace strategy
                // Let's use standard insert which usually replaces in Room if @Insert(onConflict = OnConflictStrategy.REPLACE)
                // But CustomerDao might have an update method.
                try {
                    // Using insertAllSync since it probably uses REPLACE
                    db.customerDao().insertAllSync(listOf(updatedCustomer))
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
            Toast.makeText(requireContext(), "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
            onNoteSaved?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CUSTOMER_ID = "arg_customer_id"

        fun newInstance(customerId: String): CustomerNoteBottomSheet {
            val fragment = CustomerNoteBottomSheet()
            val args = Bundle()
            args.putString(ARG_CUSTOMER_ID, customerId)
            fragment.arguments = args
            return fragment
        }
    }
}
