package com.veganbeauty.admin.features.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.veganbeauty.admin.core.base.RootieAdminFragment
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.ProfileFragmentBinding
import com.veganbeauty.admin.features.auth.LoginActivity

class ProfileFragment : RootieAdminFragment() {

    private var _binding: ProfileFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        sessionManager = SessionManager(requireContext())

        // Load profile info from Session
        val name = sessionManager.getFullName() ?: "Chưa cập nhật"
        val username = sessionManager.getUsername() ?: "unknown"
        val role = sessionManager.getRole() ?: "admin"
        val store = sessionManager.getAssignedStore() ?: "Tất cả chi nhánh"

        binding.tvProfileName.text = name
        binding.tvProfileUsername.text = "@$username"
        binding.tvProfileStore.text = store

        if (role == "admin") {
            binding.tvProfileRole.text = "Quản trị viên"
            binding.tvProfileRole.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill)
            binding.tvProfileRole.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4F6544"))
            binding.tvProfileRole.setTextColor(android.graphics.Color.WHITE)
            
            // Show Message Inbox for Admin
            binding.cardMessageInbox.visibility = View.VISIBLE
            binding.cardMessageInbox.setOnClickListener {
                (requireActivity() as com.veganbeauty.admin.MainActivity).loadFragment(
                    com.veganbeauty.admin.features.home.HomeMessageFragment()
                )
            }
        } else {
            binding.tvProfileRole.text = "Nhân viên Spa"
            binding.tvProfileRole.setBackgroundResource(com.veganbeauty.admin.R.drawable.bg_nav_pill)
            binding.tvProfileRole.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5E8DA"))
            binding.tvProfileRole.setTextColor(android.graphics.Color.parseColor("#4F6544"))
            
            // Hide Message Inbox for Staff
            binding.cardMessageInbox.visibility = View.GONE
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
