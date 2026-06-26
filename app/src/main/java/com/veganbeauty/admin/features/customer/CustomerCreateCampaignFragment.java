package com.veganbeauty.admin.features.customer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.databinding.CustomerCreateCampaignBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CustomerCreateCampaignFragment extends RootieAdminFragment {

    private CustomerCreateCampaignBinding binding;
    private final List<String> categoriesList = new ArrayList<>();
    private final List<String> offerTypes = Arrays.asList("Giảm giá phần trăm (%)", "Giảm giá cố định (VNĐ)");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = CustomerCreateCampaignBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        setupListeners();
        loadCategories();
        setupOfferTypeSpinner();
        setupInputWatchers();
        updateSaveButtonState();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnCancel.setOnClickListener(v -> navigateBack());

        binding.edtStartDate.setOnClickListener(v -> showDatePicker(binding.edtStartDate));
        binding.edtEndDate.setOnClickListener(v -> showDatePicker(binding.edtEndDate));

        binding.btnSaveCampaign.setOnClickListener(v -> saveCampaign());
    }

    private void setupInputWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.edtCampaignName.addTextChangedListener(watcher);
        binding.edtCampaignDesc.addTextChangedListener(watcher);
        binding.edtMinOrderValue.addTextChangedListener(watcher);
        binding.edtDiscountValue.addTextChangedListener(watcher);
        binding.edtStartDate.addTextChangedListener(watcher);
        binding.edtEndDate.addTextChangedListener(watcher);
    }

    private void updateSaveButtonState() {
        String title = binding.edtCampaignName.getText().toString().trim();
        String description = binding.edtCampaignDesc.getText().toString().trim();
        String minOrder = binding.edtMinOrderValue.getText().toString().trim();
        String discount = binding.edtDiscountValue.getText().toString().trim();
        String startDate = binding.edtStartDate.getText().toString().trim();
        String endDate = binding.edtEndDate.getText().toString().trim();

        boolean allFilled = !title.isEmpty() &&
                            !description.isEmpty() &&
                            !minOrder.isEmpty() &&
                            !discount.isEmpty() &&
                            !startDate.isEmpty() &&
                            !endDate.isEmpty();

        binding.btnSaveCampaign.setEnabled(allFilled);
        binding.btnSaveCampaign.setAlpha(allFilled ? 1.0f : 0.5f);
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        String currentText = editText.getText().toString();
        if (!currentText.isEmpty()) {
            try {
                Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(currentText);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(Calendar.YEAR, year);
                selectedCal.set(Calendar.MONTH, month);
                selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                editText.setText(sdf.format(selectedCal.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadCategories() {
        if (getActivity() == null) return;
        new Thread(() -> {
            List<String> loadedCategories = new ArrayList<>();
            loadedCategories.add("Tất cả sản phẩm"); // Default option
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("categories.json")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JSONObject jsonObject = new JSONObject(sb.toString());
                JSONArray jsonArray = jsonObject.getJSONArray("categories");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject catObj = jsonArray.getJSONObject(i);
                    String name = catObj.optString("name", "");
                    if (!name.isEmpty() && !loadedCategories.contains(name)) {
                        loadedCategories.add(name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoriesList.clear();
                    categoriesList.addAll(loadedCategories);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categoriesList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spnApplicableProducts.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void setupOfferTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            offerTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spnOfferType.setAdapter(adapter);

        binding.spnOfferType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    binding.txtDiscountLabel.setText("Giá trị (%)");
                    binding.edtDiscountValue.setHint("10");
                } else {
                    binding.txtDiscountLabel.setText("Giá trị (VNĐ)");
                    binding.edtDiscountValue.setHint("50000");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void saveCampaign() {
        String title = binding.edtCampaignName.getText().toString().trim();
        String description = binding.edtCampaignDesc.getText().toString().trim();
        String minOrderStr = binding.edtMinOrderValue.getText().toString().trim();
        String discountValStr = binding.edtDiscountValue.getText().toString().trim();
        String startDateStr = binding.edtStartDate.getText().toString().trim();
        String endDateStr = binding.edtEndDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên chương trình!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập mô tả chương trình!", Toast.LENGTH_SHORT).show();
            return;
        }

        long minOrderValue = 0L;
        try {
            minOrderValue = Long.parseLong(minOrderStr);
        } catch (NumberFormatException ignored) {}

        Long discountValue = null;
        try {
            discountValue = Long.parseLong(discountValStr);
        } catch (NumberFormatException ignored) {}

        if (discountValue == null || discountValue <= 0) {
            Toast.makeText(requireContext(), "Vui lòng nhập giá trị khuyến mãi hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn ngày kết thúc!", Toast.LENGTH_SHORT).show();
            return;
        }

        final long finalMinOrderValue = minOrderValue;
        final long finalDiscountValue = discountValue;

        if (getActivity() == null) return;
        new Thread(() -> {
            try {
                // 1. Get writeable file in internal storage
                File internalFile = new File(requireContext().getFilesDir(), "vouchers.json");

                // 2. Load existing vouchers
                JSONArray jsonArray;
                if (internalFile.exists()) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(internalFile)))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    jsonArray = new JSONArray(sb.toString());
                } else {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("vouchers.json")))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    jsonArray = new JSONArray(sb.toString());
                }

                // 3. Generate unique ID & Code
                int maxVal = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String idStr = item.optString("id", "");
                    if (idStr.startsWith("v")) {
                        try {
                            int num = Integer.parseInt(idStr.substring(1));
                            if (num > maxVal) maxVal = num;
                        } catch (NumberFormatException ignored) {}
                    }
                }
                int nextIdNum = maxVal + 1;
                String newId = "v" + String.format(Locale.getDefault(), "%03d", nextIdNum);

                // Voucher Type Mapping
                String voucherType = binding.radTypeDiscount.isChecked() ? "discount" : "free ship";

                // Offer Type Mapping
                String offerType = binding.spnOfferType.getSelectedItemPosition() == 0 ? "percentage" : "fixed_amount";

                // Auto-generated Voucher Code
                String autoCode = generateVoucherCode(title, offerType, finalDiscountValue);

                // Format HSD: end date with 23:59:59 time
                String hsdVal;
                try {
                    Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(endDateStr);
                    if (date != null) {
                        hsdVal = new SimpleDateFormat("yyyy-MM-year", Locale.getDefault()).format(date)
                            .replace("year", new SimpleDateFormat("yyyy", Locale.getDefault()).format(date)) + " 23:59:59";
                        // Note: To avoid year format complications in simpledateformat, let's just do:
                        hsdVal = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date) + " 23:59:59";
                    } else {
                        hsdVal = "2026-12-31 23:59:59";
                    }
                } catch (Exception e) {
                    hsdVal = "2026-12-31 23:59:59";
                }

                // Selected applicable category name
                String selectedCategory = binding.spnApplicableProducts.getSelectedItem() != null ? binding.spnApplicableProducts.getSelectedItem().toString() : "Tất cả sản phẩm";

                // 4. Create new voucher JSON object
                JSONObject newVoucher = new JSONObject();
                newVoucher.put("id", newId);
                newVoucher.put("title", title);
                newVoucher.put("description", description);
                newVoucher.put("code", autoCode);
                newVoucher.put("status", "valid");
                newVoucher.put("hsd", hsdVal);
                newVoucher.put("type", voucherType);
                newVoucher.put("from-gift", false);
                newVoucher.put("quantity", 50); // Default quantity
                newVoucher.put("minOrderValue", finalMinOrderValue);
                newVoucher.put("applicableProducts", selectedCategory);
                newVoucher.put("offerType", offerType);
                newVoucher.put("discountValue", finalDiscountValue);

                // 5. Append & Save
                jsonArray.put(newVoucher);
                try (FileOutputStream fos = new FileOutputStream(internalFile)) {
                    fos.write(jsonArray.toString(2).getBytes());
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Tạo chương trình khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String generateVoucherCode(String campaignName, String offerType, long discountValue) {
        String[] words = campaignName.split(" ");
        StringBuilder prefixBuilder = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (!w.trim().isEmpty() && count < 3) {
                char firstChar = w.trim().charAt(0);
                if (Character.isLetter(firstChar)) {
                    prefixBuilder.append(Character.toUpperCase(firstChar));
                    count++;
                }
            }
        }
        String prefix = prefixBuilder.toString();
        String cleanPrefix = prefix.isEmpty() ? "KM" : prefix;
        String suffix = offerType.equals("percentage") ? discountValue + "PCT" : (discountValue / 1000) + "K";

        String charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            randomPart.append(charPool.charAt(random.nextInt(charPool.length())));
        }

        String combined = cleanPrefix + suffix + randomPart.toString();
        if (combined.length() > 12) {
            combined = combined.substring(0, 12);
        }
        return combined.toUpperCase(Locale.getDefault());
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.loadFragment(new CustomerAdminFragment());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
