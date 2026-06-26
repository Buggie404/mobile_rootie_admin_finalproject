package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.core.utils.KeyboardUtils;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.local.entities.EntityUtils;
import com.veganbeauty.admin.databinding.CustomerNoteBottomSheetBinding;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class CustomerNoteBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CUSTOMER_ID = "arg_customer_id";

    public interface OnNoteSavedListener {
        void onNoteSaved();
    }

    private CustomerNoteBottomSheetBinding binding;
    private String customerId;
    private CustomerEntity currentCustomer;
    private OnNoteSavedListener onNoteSavedListener;

    public static CustomerNoteBottomSheet newInstance(String customerId) {
        CustomerNoteBottomSheet fragment = new CustomerNoteBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnNoteSavedListener(OnNoteSavedListener listener) {
        this.onNoteSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = CustomerNoteBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        KeyboardUtils.setupKeyboardAutoHiding(view, getActivity());
        setupListeners();
        loadCustomerData();
    }

    private void loadCustomerData() {
        if (customerId == null) return;
        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                CustomerEntity customer = db.customerDao().getByIdSync(customerId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (customer != null) {
                            currentCustomer = customer;
                            bindCustomer(customer);
                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindCustomer(CustomerEntity customer) {
        binding.txtName.setText(customer.getName());
        binding.txtLastActive.setText("Mua lần cuối: " + customer.getLastActive());

        // Tier Badge
        binding.badgeTier.setText(customer.getTier());
        String tier = customer.getTier() != null ? customer.getTier().toLowerCase() : "";
        switch (tier) {
            case "vip":
                binding.badgeTier.setTextColor(Color.parseColor("#4F6544"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
                break;
            case "vàng":
                binding.badgeTier.setTextColor(Color.parseColor("#FFFFFF"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C69C2C")));
                break;
            case "bạc":
                binding.badgeTier.setTextColor(Color.parseColor("#FFFFFF"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                break;
            default: // Thường / Normal
                binding.badgeTier.setTextColor(Color.parseColor("#616161"));
                binding.badgeTier.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
                break;
        }

        // Avatar
        if (customer.getAvatar() != null && !customer.getAvatar().isEmpty()) {
            ImageUtils.loadImage(requireContext(), binding.imgAvatar, customer.getAvatar(), R.drawable.imv_avatar);
        } else {
            binding.imgAvatar.setImageResource(R.drawable.imv_avatar);
        }

        // Previous notes
        if (customer.getNotes() != null && !customer.getNotes().isEmpty()) {
            binding.txtPreviousNotes.setText(customer.getNotes());
        } else {
            binding.txtPreviousNotes.setText("Chưa có ghi chú nào.");
        }
    }

    private void setupListeners() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // Tags
        binding.tagSkin.setOnClickListener(v -> appendTag("#Tình_trạng_da"));
        binding.tagReaction.setOnClickListener(v -> appendTag("#Phản_ứng_SP"));
        binding.tagFollowup.setOnClickListener(v -> appendTag("#Follow_up"));
        binding.tagRecommendation.setOnClickListener(v -> appendTag("#Khuyến_nghị"));

        // Save
        binding.btnSave.setOnClickListener(v -> saveNote());
    }

    private void appendTag(String tag) {
        String currentText = binding.edtNote.getText().toString();
        String space = (!currentText.isEmpty() && !currentText.endsWith(" ")) ? " " : "";
        binding.edtNote.append(space + tag + " ");
    }

    private void saveNote() {
        String noteContent = binding.edtNote.getText().toString().trim();
        if (noteContent.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập nội dung ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCustomer == null) return;

        // Format the new note with date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        String formattedNewNote = "[" + currentDate + "] " + noteContent;

        // Append to existing notes
        String existingNotes = currentCustomer.getNotes() != null ? currentCustomer.getNotes() : "";
        String updatedNotes = !existingNotes.isEmpty() ? existingNotes + "\n\n" + formattedNewNote : formattedNewNote;

        CustomerEntity updatedCustomer = EntityUtils.copy(currentCustomer, updatedNotes);

        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                db.customerDao().insertAllSync(Collections.singletonList(updatedCustomer));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Đã lưu ghi chú", Toast.LENGTH_SHORT).show();
                        if (onNoteSavedListener != null) {
                            onNoteSavedListener.onNoteSaved();
                        }
                        dismiss();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
