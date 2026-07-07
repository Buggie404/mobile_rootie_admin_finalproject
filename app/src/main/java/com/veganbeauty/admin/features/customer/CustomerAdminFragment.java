package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.databinding.CustomerAdminMainFragmentBinding;
import com.veganbeauty.admin.features.home.BottomNavHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CustomerAdminFragment extends RootieAdminFragment {

    private CustomerAdminMainFragmentBinding binding;
    private CustomerAdapter adapter;
    private final List<CustomerEntity> allCustomersList = new ArrayList<>();
    private List<CustomerEntity> filteredCustomersList = new ArrayList<>();

    // Filter states
    private String currentSearchQuery = "";
    private String currentSelectedTab = "tất cả";

    // Pagination states
    private int currentPage = 1;
    private final int itemsPerPage = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = CustomerAdminMainFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            View bottomNav = mainActivity.findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                BottomNavHelper.highlightTab(bottomNav, R.id.nav_customer);
            }
        }

        setupRecyclerView();
        setupListeners();
        loadData();

        // Bind message button in header
        setupHeaderMessageButton(binding.btnMessage);
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(
            new ArrayList<>(),
            customer -> {
                CustomerDetailFragment detailFragment = CustomerDetailFragment.newInstance(customer.getId(), false);
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.loadFragment(detailFragment);
                }
            },
            customer -> {
                CustomerNoteBottomSheet bottomSheet = CustomerNoteBottomSheet.newInstance(customer.getId());
                bottomSheet.setOnNoteSavedListener(() -> loadData()); // Refresh list to show updated notes count if any
                bottomSheet.show(getParentFragmentManager(), "CustomerNoteBottomSheet");
            }
        );
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomers.setAdapter(adapter);
    }

    private void setupListeners() {
        // Logo (Back to Home shortcut)
        binding.imgLogo.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                BottomNavHelper.navigate(mainActivity, R.id.nav_home);
            }
        });

        // Notification Button
        binding.btnNotification.setOnClickListener(v -> Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show());

        // Create Campaign Button
        binding.btnCreateCampaign.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(new CustomerCreateCampaignFragment());
            }
        });

        // Search Input
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                currentPage = 1;
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tabs Selection
        binding.tabAll.setOnClickListener(v -> selectTab("tất cả", binding.tabAll));
        binding.tabVip.setOnClickListener(v -> selectTab("vip", binding.tabVip));
        binding.tabGold.setOnClickListener(v -> selectTab("vàng", binding.tabGold));
        binding.tabSilver.setOnClickListener(v -> selectTab("bạc", binding.tabSilver));
        binding.tabNormal.setOnClickListener(v -> selectTab("thường", binding.tabNormal));

        // Pagination buttons
        binding.btnPagePrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePaginationUI();
            }
        });

        binding.btnPageNext.setOnClickListener(v -> {
            int totalPages = (int) Math.ceil((double) filteredCustomersList.size() / itemsPerPage);
            if (currentPage < totalPages) {
                currentPage++;
                updatePaginationUI();
            }
        });
    }

    private void selectTab(String tab, TextView selectedView) {
        currentSelectedTab = tab;
        currentPage = 1;

        // Reset all tabs styles
        List<TextView> tabs = Arrays.asList(binding.tabAll, binding.tabVip, binding.tabGold, binding.tabSilver, binding.tabNormal);
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_search_bar);
            t.setTextColor(Color.parseColor("#3E4D44"));
            t.setBackgroundTintList(null);
        }

        // Highlight selected tab
        selectedView.setBackgroundResource(R.drawable.bg_nav_pill);
        selectedView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
        selectedView.setTextColor(Color.parseColor("#FFFFFF"));

        applyFilters();
    }

    private void loadData() {
        if (getActivity() == null) return;
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                // 1. Fetch from local DB
                List<CustomerEntity> localCustomers = database.customerDao().getAllSync();

                // 2. If local DB is empty, parse from assets/users.json
                if (localCustomers.isEmpty()) {
                    List<CustomerEntity> parsedCustomers = parseCustomersFromAssets();
                    if (!parsedCustomers.isEmpty()) {
                        database.customerDao().insertAllSync(parsedCustomers);
                        localCustomers = parsedCustomers;
                    }
                }

                // 3. Filter out admin and staff roles
                List<CustomerEntity> activeCustomers = new ArrayList<>();
                for (CustomerEntity customer : localCustomers) {
                    String r = customer.getRole() != null ? customer.getRole().toLowerCase() : "";
                    if (!r.equals("admin") && !r.equals("employee") && !r.equals("staff")) {
                        activeCustomers.add(customer);
                    }
                }

                final List<CustomerEntity> finalCustomers = activeCustomers;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allCustomersList.clear();
                        allCustomersList.addAll(finalCustomers);
                        applyFilters();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<CustomerEntity> parseCustomersFromAssets() {
        List<CustomerEntity> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("users.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                CustomerEntity entity = new CustomerEntity();
                entity.setId(obj.optString("user_id", UUID.randomUUID().toString()));
                entity.setUsername(obj.optString("username", ""));
                entity.setFull_name(obj.optString("full_name", ""));
                entity.setName(obj.has("full_name") && !obj.getString("full_name").isEmpty() ? obj.getString("full_name") : obj.optString("username", ""));
                entity.setEmail(obj.optString("email", ""));
                entity.setPhone(obj.optString("phone", ""));
                entity.setAddress(obj.optString("bio", ""));
                entity.setAvatar(obj.optString("avatar", ""));
                entity.setPrimary_image(obj.optString("primary_image", ""));
                entity.setSpending(obj.optLong("spending", 0L));
                entity.setTier(obj.optString("tier", "Thường"));
                entity.setLastActive(obj.optString("last_active", ""));
                entity.setNotes(obj.optString("notes", ""));
                entity.setRole(obj.optString("role", "customer"));
                entity.setBirthday(obj.optString("birthday", ""));
                entity.setRegion(obj.optString("region", ""));
                entity.setJoinYear(obj.optInt("join_year", 1));
                entity.setOrderCount(obj.optInt("order_count", 0));
                entity.setRecentPurchase(obj.optString("recent_purchase", ""));
                entity.setSpendingYear(obj.optLong("spending_year", 0L));
                entity.setSpendingMonth(obj.optLong("spending_month", 0L));
                entity.setPoints(obj.optInt("points", 0));
                list.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void applyFilters() {
        List<CustomerEntity> result = new ArrayList<>(allCustomersList);

        // Apply Tab Filter
        if (!"tất cả".equals(currentSelectedTab)) {
            List<CustomerEntity> tabFiltered = new ArrayList<>();
            for (CustomerEntity customer : result) {
                if (customer.getTier() != null && customer.getTier().equalsIgnoreCase(currentSelectedTab)) {
                    tabFiltered.add(customer);
                }
            }
            result = tabFiltered;
        }

        // Apply Search Query
        if (!currentSearchQuery.isEmpty()) {
            List<CustomerEntity> searchFiltered = new ArrayList<>();
            for (CustomerEntity customer : result) {
                boolean nameMatches = customer.getName() != null && customer.getName().toLowerCase().contains(currentSearchQuery.toLowerCase());
                boolean phoneMatches = customer.getPhone() != null && customer.getPhone().contains(currentSearchQuery);
                if (nameMatches || phoneMatches) {
                    searchFiltered.add(customer);
                }
            }
            result = searchFiltered;
        }

        filteredCustomersList = result;
        updatePaginationUI();
    }

    private void updatePaginationUI() {
        int totalItems = filteredCustomersList.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (currentPage < 1) {
            currentPage = 1;
        }

        // Slice items for current page
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        List<CustomerEntity> pageItems;
        if (totalItems > 0 && startIndex < totalItems) {
            pageItems = filteredCustomersList.subList(startIndex, endIndex);
        } else {
            pageItems = new ArrayList<>();
        }

        adapter.updateData(pageItems);

        // Update footer text
        int displayedCount = pageItems.size();
        binding.txtCountFooter.setText("Hiển thị " + displayedCount + " / " + totalItems + " khách hàng");

        // Update pagination buttons container visibility
        if (totalPages <= 1) {
            binding.paginationContainer.setVisibility(View.GONE);
        } else {
            binding.paginationContainer.setVisibility(View.VISIBLE);
            setupPageNumbers(totalPages);
        }
    }

    private void setupPageNumbers(int totalPages) {
        // Enable/Disable prev button
        binding.btnPagePrev.setEnabled(currentPage > 1);
        binding.btnPagePrev.setAlpha(currentPage > 1 ? 1.0f : 0.4f);

        // Enable/Disable next button
        binding.btnPageNext.setEnabled(currentPage < totalPages);
        binding.btnPageNext.setAlpha(currentPage < totalPages ? 1.0f : 0.4f);

        // We have 3 page text views: btn_page_1, btn_page_2, btn_page_3
        List<TextView> pageViews = Arrays.asList(binding.btnPage1, binding.btnPage2, binding.btnPage3);

        // Reset all page styles
        for (TextView view : pageViews) {
            view.setVisibility(View.GONE);
            view.setBackgroundResource(R.drawable.bg_search_bar);
            view.setTextColor(Color.parseColor("#3E4D44"));
            view.setBackgroundTintList(null);
        }

        // Determine which page numbers to show
        int startPage;
        if (totalPages <= 3) {
            startPage = 1;
        } else if (currentPage == 1) {
            startPage = 1;
        } else if (currentPage == totalPages) {
            startPage = totalPages - 2;
        } else {
            startPage = currentPage - 1;
        }

        for (int i = 0; i < Math.min(3, totalPages); i++) {
            final int pageNum = startPage + i;
            TextView view = pageViews.get(i);
            view.setText(String.valueOf(pageNum));
            view.setVisibility(View.VISIBLE);

            if (pageNum == currentPage) {
                view.setBackgroundResource(R.drawable.bg_nav_pill);
                view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
                view.setTextColor(Color.parseColor("#FFFFFF"));
            }

            view.setOnClickListener(v -> {
                currentPage = pageNum;
                updatePaginationUI();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
