package com.veganbeauty.admin.features.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.databinding.ProfileFragmentBinding;
import com.veganbeauty.admin.features.auth.LoginActivity;
import com.veganbeauty.admin.features.home.HomeMessageFragment;

public class ProfileFragment extends RootieAdminFragment {

    private ProfileFragmentBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ProfileFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        sessionManager = new SessionManager(requireContext());

        // Load profile info from Session
        String name = sessionManager.getFullName();
        if (name == null) name = "Chưa cập nhật";
        String username = sessionManager.getUsername();
        if (username == null) username = "unknown";
        String role = sessionManager.getRole();
        if (role == null) role = "admin";
        String store = sessionManager.getAssignedStore();
        if (store == null) store = "Tất cả chi nhánh";

        binding.tvProfileName.setText(name);
        binding.tvProfileUsername.setText("@" + username);
        binding.tvProfileStore.setText(store);

        if ("admin".equals(role)) {
            binding.tvProfileRole.setText("Quản trị viên");
            binding.tvProfileRole.setBackgroundResource(R.drawable.bg_nav_pill);
            binding.tvProfileRole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F6544")));
            binding.tvProfileRole.setTextColor(Color.WHITE);

            // Show Message Inbox for Admin
            binding.cardMessageInbox.setVisibility(View.VISIBLE);
            binding.cardMessageInbox.setOnClickListener(v -> {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.loadFragment(new HomeMessageFragment());
                }
            });
        } else {
            binding.tvProfileRole.setText("Nhân viên Spa");
            binding.tvProfileRole.setBackgroundResource(R.drawable.bg_nav_pill);
            binding.tvProfileRole.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
            binding.tvProfileRole.setTextColor(Color.parseColor("#4F6544"));

            // Hide Message Inbox for Staff
            binding.cardMessageInbox.setVisibility(View.GONE);
        }

        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
