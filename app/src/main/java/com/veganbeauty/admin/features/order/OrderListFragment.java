package com.veganbeauty.admin.features.order;

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

import com.google.firebase.firestore.ListenerRegistration;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.EntityUtils;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.OrderRepository;
import com.veganbeauty.admin.databinding.OrderFragmentListBinding;
import com.veganbeauty.admin.features.home.BottomNavHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class OrderListFragment extends RootieAdminFragment {

    private OrderFragmentListBinding binding;
    private OrderRepository repository;
    private OrderAdapter adapter;

    private final List<OrderEntity> allOrdersList = new ArrayList<>();
    private List<OrderEntity> filteredOrdersList = new ArrayList<>();
    private final Set<String> selectedOrderIds = new HashSet<>();

    private String currentSearchQuery = "";
    private String currentSelectedTab = "tất cả";

    private String currentSort = "DEFAULT";
    private final Set<String> filterPaymentMethods = new HashSet<>();
    private final Set<String> filterPriceRanges = new HashSet<>();
    private final Set<String> filterRegions = new HashSet<>();
    private ListenerRegistration firestoreListener = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = OrderFragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            BottomNavHelper.setup(
                    mainActivity,
                    binding.getRoot(),
                    R.id.nav_order,
                    tabId -> BottomNavHelper.navigate(mainActivity, tabId)
            );
        }

        // Initialize Repository
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        repository = new OrderRepository(database.orderDao(), new FirebaseService());

        // Observe local database updates
        repository.getAllOrders().observe(getViewLifecycleOwner(), localOrders -> {
            if (localOrders != null && !localOrders.isEmpty()) {
                allOrdersList.clear();
                allOrdersList.addAll(localOrders);
                applyFilters();
            } else {
                loadData();
            }
        });

        // Start listening to Firestore remote changes in real-time
        firestoreListener = repository.startRealtimeSync();

        setupRecyclerView();
        setupListeners();
        selectTab("tất cả", binding.tabAll);
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(
                new ArrayList<>(),
                selectedOrderIds,
                order -> toggleOrderSelection(order.getOrderId()),
                order -> updateSingleOrderStatus(order.getOrderId(), "Đã hủy"),
                order -> {
                    String nextStatus = getNextStatus(order.getStatus());
                    updateSingleOrderStatus(order.getOrderId(), nextStatus);
                },
                order -> {
                    OrderDetailFragment detailFragment = OrderDetailFragment.newInstance(order.getOrderId());
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) {
                        mainActivity.loadFragment(detailFragment);
                    }
                }
        );

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupListeners() {
        // Notification bell click
        binding.btnNotification.setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show()
        );

        // Search Input
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tabs Click Listeners
        binding.tabAll.setOnClickListener(v -> selectTab("tất cả", binding.tabAll));
        binding.tabPending.setOnClickListener(v -> selectTab("chờ xác nhận", binding.tabPending));
        binding.tabPreparing.setOnClickListener(v -> selectTab("đang chuẩn bị", binding.tabPreparing));
        binding.tabDelivering.setOnClickListener(v -> selectTab("đang giao", binding.tabDelivering));
        binding.tabCompleted.setOnClickListener(v -> selectTab("hoàn tất", binding.tabCompleted));
        binding.tabCancelled.setOnClickListener(v -> selectTab("đã hủy", binding.tabCancelled));

        // Select All Click Listener
        binding.layoutSelectAll.setOnClickListener(v -> toggleSelectAll());

        // Bulk Actions
        binding.btnBulkCancel.setOnClickListener(v -> performBulkStatusUpdate("Đã hủy"));
        binding.btnBulkApprove.setOnClickListener(v -> performBulkApprove());

        // Lọc & Sắp xếp
        binding.btnSort.setOnClickListener(v -> {
            OrderSortBottomSheet sortSheet = OrderSortBottomSheet.newInstance(currentSort, selectedSort -> {
                currentSort = selectedSort;
                applyFilters();
            });
            sortSheet.show(getChildFragmentManager(), "OrderSortBottomSheet");
        });

        binding.btnFilter.setOnClickListener(v -> {
            OrderFilterBottomSheet filterSheet = OrderFilterBottomSheet.newInstance(
                    filterPaymentMethods, filterPriceRanges, filterRegions,
                    (payments, prices, regions) -> {
                        filterPaymentMethods.clear();
                        filterPaymentMethods.addAll(payments);
                        filterPriceRanges.clear();
                        filterPriceRanges.addAll(prices);
                        filterRegions.clear();
                        filterRegions.addAll(regions);
                        applyFilters();
                    }
            );
            filterSheet.show(getChildFragmentManager(), "OrderFilterBottomSheet");
        });
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<OrderEntity> localOrders = repository.getAllOrders().getValue();
                if (localOrders == null) {
                    localOrders = new ArrayList<>();
                }

                // Fallback: If DB is empty, parse from assets/orders.json and seed the local DB
                if (localOrders.isEmpty()) {
                    List<OrderEntity> parsedOrders = parseOrdersFromAssets();
                    if (!parsedOrders.isEmpty()) {
                        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
                        database.orderDao().insertAllSync(parsedOrders);
                        localOrders = parsedOrders;
                    }
                }

                final List<OrderEntity> finalOrders = localOrders;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    allOrdersList.clear();
                    allOrdersList.addAll(finalOrders);
                    applyFilters();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<OrderEntity> parseOrdersFromAssets() {
        List<OrderEntity> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(requireContext().getAssets().open("orders.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONArray jsonArray = root.getJSONArray("orders");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONArray itemsRaw = obj.optJSONArray("items");
                List<OrderItem> orderItems = new ArrayList<>();
                if (itemsRaw != null) {
                    for (int j = 0; j < itemsRaw.length(); j++) {
                        JSONObject itemObj = itemsRaw.getJSONObject(j);
                        OrderItem orderItem = new OrderItem();
                        orderItem.setProductId(itemObj.optString("productId", ""));
                        orderItem.setProductName(itemObj.optString("productName", ""));
                        orderItem.setProductImage(itemObj.optString("productImage", ""));
                        orderItem.setQuantity(itemObj.optInt("quantity", 0));
                        orderItem.setPrice(itemObj.optLong("price", 0L));
                        orderItems.add(orderItem);
                    }
                }

                OrderEntity entity = new OrderEntity();
                entity.setOrderId(obj.optString("id", UUID.randomUUID().toString()));
                entity.setUserId(obj.optString("userId", ""));
                entity.setOrderDate(obj.optString("orderDate", ""));
                entity.setOrderTime(obj.optString("orderTime", ""));
                entity.setStatus(obj.optString("status", ""));
                entity.setTotalAmount(obj.optLong("totalAmount", 0L));
                entity.setItems(orderItems);
                entity.setShippingName(obj.optString("shippingName", ""));
                entity.setShippingPhone(obj.optString("shippingPhone", ""));
                entity.setShippingAddress(obj.optString("shippingAddress", ""));
                entity.setShippingCost(obj.optLong("shippingCost", 0L));
                entity.setVoucherDiscount(obj.optLong("voucherDiscount", 0L));
                entity.setPaymentMethod(obj.optString("paymentMethod", ""));
                list.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void selectTab(String tab, TextView selectedView) {
        currentSelectedTab = tab;
        selectedOrderIds.clear(); // Clear selection on tab change

        // Reset all tabs styles
        List<TextView> tabs = Arrays.asList(
                binding.tabAll,
                binding.tabPending,
                binding.tabPreparing,
                binding.tabDelivering,
                binding.tabCompleted,
                binding.tabCancelled
        );

        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_search_bar);
            t.setTextColor(Color.parseColor("#3E4D44"));
            t.setBackgroundTintList(null);
            t.setTypeface(null, Typeface.NORMAL);
        }

        // Highlight selected tab
        selectedView.setBackgroundResource(R.drawable.bg_nav_pill);
        selectedView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
        selectedView.setTextColor(Color.parseColor("#FFFFFF"));
        selectedView.setTypeface(null, Typeface.BOLD);

        applyFilters();
    }

    private void applyFilters() {
        List<OrderEntity> result = new ArrayList<>(allOrdersList);

        // 1. Apply non-tab filters first (Search, Payment, Price, Region)
        if (!currentSearchQuery.isEmpty()) {
            List<OrderEntity> searchFiltered = new ArrayList<>();
            for (OrderEntity order : result) {
                if (order.getOrderId().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                    (order.getShippingName() != null && order.getShippingName().toLowerCase().contains(currentSearchQuery.toLowerCase()))) {
                    searchFiltered.add(order);
                }
            }
            result = searchFiltered;
        }

        // Payment Filtering
        if (!filterPaymentMethods.isEmpty()) {
            List<OrderEntity> paymentFiltered = new ArrayList<>();
            for (OrderEntity order : result) {
                String method = order.getPaymentMethod() != null ? order.getPaymentMethod().toLowerCase() : "";
                boolean match = false;
                if (filterPaymentMethods.contains("COD") && (method.contains("cod") || method.contains("nhận hàng"))) match = true;
                if (filterPaymentMethods.contains("ATM") && (method.contains("chuyển khoản") || method.contains("atm") || method.contains("ngân hàng"))) match = true;
                if (filterPaymentMethods.contains("MOMO") && method.contains("momo")) match = true;
                if (filterPaymentMethods.contains("VNPAY") && method.contains("vnpay")) match = true;
                if (match) paymentFiltered.add(order);
            }
            result = paymentFiltered;
        }

        // Price Range Filtering
        if (!filterPriceRanges.isEmpty()) {
            List<OrderEntity> priceFiltered = new ArrayList<>();
            for (OrderEntity order : result) {
                boolean match = false;
                long total = order.getTotalAmount();
                if (filterPriceRanges.contains("UNDER_500K") && total < 500000L) match = true;
                if (filterPriceRanges.contains("500K_TO_1500K") && total >= 500000L && total <= 1500000L) match = true;
                if (filterPriceRanges.contains("OVER_1500K") && total > 1500000L) match = true;
                if (match) priceFiltered.add(order);
            }
            result = priceFiltered;
        }

        // Region Filtering
        if (!filterRegions.isEmpty()) {
            List<OrderEntity> regionFiltered = new ArrayList<>();
            for (OrderEntity order : result) {
                String addr = order.getShippingAddress() != null ? order.getShippingAddress().toLowerCase() : "";
                boolean match = false;
                if (filterRegions.contains("HCMC") && (addr.contains("hồ chí minh") || addr.contains("hcm"))) match = true;
                if (filterRegions.contains("HN") && (addr.contains("hà nội") || addr.contains("hn"))) match = true;
                if (filterRegions.contains("OTHERS") && !addr.contains("hồ chí minh") && !addr.contains("hcm") && !addr.contains("hà nội") && !addr.contains("hn")) match = true;
                if (match) regionFiltered.add(order);
            }
            result = regionFiltered;
        }

        // Update tab counts
        updateTabCounts(result);

        // 2. Apply Tab Filtering
        if (!"tất cả".equals(currentSelectedTab)) {
            List<OrderEntity> tabFiltered = new ArrayList<>();
            for (OrderEntity order : result) {
                String status = order.getStatus();
                String statusLower = status != null ? status.trim().toLowerCase() : "";
                boolean match = false;
                switch (currentSelectedTab) {
                    case "chờ xác nhận":
                        match = statusLower.contains("chờ xử lý") || statusLower.contains("chờ xác nhận");
                        break;
                    case "đang chuẩn bị":
                        match = statusLower.contains("đang xử lý") || statusLower.contains("đang chuẩn bị");
                        break;
                    case "đang giao":
                        match = statusLower.contains("đang giao");
                        break;
                    case "hoàn tất":
                        match = statusLower.contains("hoàn tất");
                        break;
                    case "đã hủy":
                        match = statusLower.contains("đã hủy");
                        break;
                }
                if (match) tabFiltered.add(order);
            }
            result = tabFiltered;
        }

        // Sorting
        if (!"DEFAULT".equals(currentSort)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
            result.sort((o1, o2) -> {
                if (currentSort.equals("DATE_DESC") || currentSort.equals("DATE_ASC")) {
                    Date d1 = null;
                    Date d2 = null;
                    try {
                        d1 = dateFormat.parse(o1.getOrderDate() + " " + o1.getOrderTime());
                    } catch (Exception ignored) {}
                    try {
                        d2 = dateFormat.parse(o2.getOrderDate() + " " + o2.getOrderTime());
                    } catch (Exception ignored) {}

                    int dateCompare;
                    if (d1 == null && d2 == null) {
                        dateCompare = 0;
                    } else if (d1 == null) {
                        dateCompare = 1;
                    } else if (d2 == null) {
                        dateCompare = -1;
                    } else {
                        dateCompare = currentSort.equals("DATE_DESC") ? d2.compareTo(d1) : d1.compareTo(d2);
                    }
                    if (dateCompare != 0) return dateCompare;
                    return o2.getOrderId().compareTo(o1.getOrderId());
                } else if (currentSort.equals("PRICE_DESC")) {
                    return Long.compare(o2.getTotalAmount(), o1.getTotalAmount());
                } else {
                    return Long.compare(o1.getTotalAmount(), o2.getTotalAmount());
                }
            });
        }

        filteredOrdersList = result;
        updateListUI();
    }

    private void updateListUI() {
        boolean selectionAllowed = !"tất cả".equals(currentSelectedTab) && !"hoàn tất".equals(currentSelectedTab) && !"đã hủy".equals(currentSelectedTab);
        adapter.updateData(filteredOrdersList, selectedOrderIds, selectionAllowed);
        if (selectionAllowed) {
            binding.layoutSelectionRow.setVisibility(View.VISIBLE);
        } else {
            binding.layoutSelectionRow.setVisibility(View.GONE);
        }
        updateBulkActionsBarVisibility();
        updateSelectAllCheckboxUI();
    }

    private void toggleOrderSelection(String orderId) {
        if (selectedOrderIds.contains(orderId)) {
            selectedOrderIds.remove(orderId);
        } else {
            selectedOrderIds.add(orderId);
        }
        updateListUI();
    }

    private void toggleSelectAll() {
        List<String> allDisplayedIds = new ArrayList<>();
        for (OrderEntity order : filteredOrdersList) {
            allDisplayedIds.add(order.getOrderId());
        }
        boolean allDisplayedSelected = selectedOrderIds.containsAll(allDisplayedIds) && !allDisplayedIds.isEmpty();

        if (allDisplayedSelected) {
            selectedOrderIds.removeAll(allDisplayedIds);
        } else {
            selectedOrderIds.addAll(allDisplayedIds);
        }
        updateListUI();
    }

    private void updateSelectAllCheckboxUI() {
        List<String> allDisplayedIds = new ArrayList<>();
        for (OrderEntity order : filteredOrdersList) {
            allDisplayedIds.add(order.getOrderId());
        }
        boolean isAllSelected = selectedOrderIds.containsAll(allDisplayedIds) && !allDisplayedIds.isEmpty();

        binding.imgSelectAllCheckbox.setImageResource(
                isAllSelected ? R.drawable.ic_checkbox_checked
                        : R.drawable.ic_radio_primary_unchecked
        );

        binding.txtSelectedCount.setText("Đã chọn: " + selectedOrderIds.size());
    }

    private void updateBulkActionsBarVisibility() {
        int size = selectedOrderIds.size();
        if (size > 0 && !"tất cả".equals(currentSelectedTab) && !"hoàn tất".equals(currentSelectedTab) && !"đã hủy".equals(currentSelectedTab)) {
            binding.layoutBulkActions.setVisibility(View.VISIBLE);
            switch (currentSelectedTab) {
                case "chờ xác nhận":
                    binding.btnBulkCancel.setText("Hủy (" + size + ")");
                    binding.btnBulkApprove.setText("Xác nhận (" + size + ")");
                    break;
                case "đang chuẩn bị":
                    binding.btnBulkCancel.setText("Hủy (" + size + ")");
                    binding.btnBulkApprove.setText("Giao hàng (" + size + ")");
                    break;
                case "đang giao":
                    binding.btnBulkCancel.setText("Thất bại (" + size + ")");
                    binding.btnBulkApprove.setText("Hoàn tất (" + size + ")");
                    break;
            }
        } else {
            binding.layoutBulkActions.setVisibility(View.GONE);
        }
    }

    private void updateSingleOrderStatus(String orderId, String newStatus) {
        new Thread(() -> {
            try {
                boolean success = repository.updateOrderStatus(orderId, newStatus);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (success) {
                        Toast.makeText(requireContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                        // Refresh local lists
                        int index = -1;
                        for (int i = 0; i < allOrdersList.size(); i++) {
                            if (allOrdersList.get(i).getOrderId().equals(orderId)) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            OrderEntity updated = EntityUtils.copy(allOrdersList.get(index), newStatus);
                            allOrdersList.set(index, updated);
                        }
                        selectedOrderIds.remove(orderId);
                        applyFilters();
                    } else {
                        Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void performBulkStatusUpdate(String newStatus) {
        new Thread(() -> {
            try {
                List<String> idsToUpdate = new ArrayList<>(selectedOrderIds);
                int failCount = 0;

                for (String id : idsToUpdate) {
                    boolean success = repository.updateOrderStatus(id, newStatus);
                    if (!success) {
                        failCount++;
                    }
                }

                final int finalFailCount = failCount;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (finalFailCount == 0) {
                        Toast.makeText(requireContext(), "Cập nhật hàng loạt thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Cập nhật thành công. Thất bại " + finalFailCount + " đơn.", Toast.LENGTH_SHORT).show();
                    }
                    selectedOrderIds.clear();
                    loadData();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void performBulkApprove() {
        new Thread(() -> {
            try {
                List<String> idsToUpdate = new ArrayList<>(selectedOrderIds);
                int failCount = 0;

                for (String id : idsToUpdate) {
                    OrderEntity order = null;
                    for (OrderEntity o : allOrdersList) {
                        if (o.getOrderId().equals(id)) {
                            order = o;
                            break;
                        }
                    }
                    if (order != null) {
                        String nextStatus = getNextStatus(order.getStatus());
                        boolean success = repository.updateOrderStatus(id, nextStatus);
                        if (!success) {
                            failCount++;
                        }
                    }
                }

                final int finalFailCount = failCount;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (finalFailCount == 0) {
                        Toast.makeText(requireContext(), "Cập nhật hàng loạt thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Hoàn thành cập nhật. Lỗi " + finalFailCount + " đơn.", Toast.LENGTH_SHORT).show();
                    }
                    selectedOrderIds.clear();
                    loadData();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getNextStatus(String currentStatus) {
        String statusClean = currentStatus != null ? currentStatus.trim().toLowerCase() : "";
        if (statusClean.contains("chờ xử lý") || statusClean.contains("chờ xác nhận")) {
            return "Đang chuẩn bị";
        } else if (statusClean.contains("đang xử lý") || statusClean.contains("đang chuẩn bị")) {
            return "Đang giao";
        } else if (statusClean.contains("đang giao")) {
            return "Hoàn tất";
        } else {
            return currentStatus;
        }
    }

    private void updateTabCounts(List<OrderEntity> orders) {
        int countAll = orders.size();
        int countPending = 0;
        int countPreparing = 0;
        int countDelivering = 0;
        int countCompleted = 0;
        int countCancelled = 0;

        for (OrderEntity order : orders) {
            String status = order.getStatus();
            String statusLower = status != null ? status.trim().toLowerCase() : "";
            if (statusLower.contains("chờ xử lý") || statusLower.contains("chờ xác nhận")) {
                countPending++;
            } else if (statusLower.contains("đang xử lý") || statusLower.contains("đang chuẩn bị")) {
                countPreparing++;
            } else if (statusLower.contains("đang giao")) {
                countDelivering++;
            } else if (statusLower.contains("hoàn tất")) {
                countCompleted++;
            } else if (statusLower.contains("đã hủy")) {
                countCancelled++;
            }
        }

        binding.tabAll.setText("Tất cả (" + countAll + ")");
        binding.tabPending.setText("Chờ xác nhận (" + countPending + ")");
        binding.tabPreparing.setText("Đang chuẩn bị (" + countPreparing + ")");
        binding.tabDelivering.setText("Đang giao (" + countDelivering + ")");
        binding.tabCompleted.setText("Hoàn tất (" + countCompleted + ")");
        binding.tabCancelled.setText("Đã hủy (" + countCancelled + ")");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        binding = null;
    }
}
