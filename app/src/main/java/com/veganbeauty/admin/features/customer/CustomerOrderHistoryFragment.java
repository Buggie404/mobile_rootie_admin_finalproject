package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.databinding.CustomerOrderHistoryBinding;
import com.veganbeauty.admin.databinding.ItemOrderHistoryBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomerOrderHistoryFragment extends RootieAdminFragment {

    private static final String ARG_CUSTOMER_ID = "arg_customer_id";
    private static final String ARG_FROM_STAFF = "arg_from_staff";

    private CustomerOrderHistoryBinding binding;
    private String customerId;
    private boolean fromStaff = false;

    private final List<OrderEntity> allOrdersList = new ArrayList<>();
    private List<OrderEntity> filteredOrdersList = new ArrayList<>();

    private OrderHistoryAdapter adapter;

    private String currentFilter = "all"; // all, completed, delivering
    private int showLimit = 5;
    private CustomerEntity currentCustomer;

    public static CustomerOrderHistoryFragment newInstance(String customerId, boolean fromStaff) {
        CustomerOrderHistoryFragment fragment = new CustomerOrderHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        args.putBoolean(ARG_FROM_STAFF, fromStaff);
        fragment.setArguments(args);
        return fragment;
    }

    public static CustomerOrderHistoryFragment newInstance(String customerId) {
        return newInstance(customerId, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
            fromStaff = getArguments().getBoolean(ARG_FROM_STAFF, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = CustomerOrderHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        setupRecyclerView();
        setupListeners();
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(new ArrayList<>());
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());

        binding.tabAll.setOnClickListener(v -> selectTab("all"));
        binding.tabCompleted.setOnClickListener(v -> selectTab("completed"));
        binding.tabDelivering.setOnClickListener(v -> selectTab("delivering"));

        binding.btnLoadMore.setOnClickListener(v -> {
            // Expand to show all orders
            showLimit = filteredOrdersList.size();
            updateListUI();
        });
    }

    private void selectTab(String filter) {
        currentFilter = filter;
        showLimit = 5; // Reset page limit when switching tabs

        // Reset tabs style
        List<TextView> tabs = Arrays.asList(binding.tabAll, binding.tabCompleted, binding.tabDelivering);
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_search_bar);
            t.setTextColor(Color.parseColor("#3E4D44"));
            t.setBackgroundTintList(null);
        }

        // Apply active styling to selected tab matching mockup
        TextView selectedView = binding.tabAll;
        if ("completed".equals(filter)) {
            selectedView = binding.tabCompleted;
        } else if ("delivering".equals(filter)) {
            selectedView = binding.tabDelivering;
        }

        selectedView.setBackgroundResource(R.drawable.bg_nav_pill);
        selectedView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
        selectedView.setTextColor(Color.parseColor("#4F6544"));

        applyFilters();
    }

    private void loadData() {
        if (customerId == null) return;
        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                // 1. Fetch customer
                currentCustomer = db.customerDao().getByIdSync(customerId);

                // 2. Fetch orders, populate if empty
                List<OrderEntity> orders = db.orderDao().getAllSync();
                if (orders.isEmpty()) {
                    List<OrderEntity> parsedOrders = parseOrdersFromAssets();
                    if (!parsedOrders.isEmpty()) {
                        db.orderDao().insertAllSync(parsedOrders);
                        orders = parsedOrders;
                    }
                }

                final List<OrderEntity> finalOrders = orders;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentCustomer != null) {
                            bindCustomerHeader(currentCustomer);
                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show();
                            navigateBack();
                            return;
                        }

                        // 3. Filter orders belonging to this user
                        List<OrderEntity> userOrders = new ArrayList<>();
                        for (OrderEntity oe : finalOrders) {
                            if (customerId.equals(oe.getUserId())) {
                                userOrders.add(oe);
                            }
                        }
                        allOrdersList.clear();
                        allOrdersList.addAll(userOrders);

                        selectTab("all");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindCustomerHeader(CustomerEntity customer) {
        binding.txtCustomerName.setText(customer.getName());
        binding.txtCustomerId.setText("KH-" + customer.getId().toUpperCase());
        binding.txtTotalOrders.setText(String.valueOf(customer.getOrderCount()));
        binding.txtTotalSpending.setText(formatSpending(customer.getSpending()));
    }

    private void applyFilters() {
        List<OrderEntity> temp = new ArrayList<>();
        if ("completed".equals(currentFilter)) {
            for (OrderEntity oe : allOrdersList) {
                String s = oe.getStatus() != null ? oe.getStatus().toLowerCase() : "";
                if (s.equals("hoàn thành") || s.equals("hoàn tất")) {
                    temp.add(oe);
                }
            }
        } else if ("delivering".equals(currentFilter)) {
            for (OrderEntity oe : allOrdersList) {
                String s = oe.getStatus() != null ? oe.getStatus().toLowerCase() : "";
                if (s.equals("đang giao")) {
                    temp.add(oe);
                }
            }
        } else {
            temp.addAll(allOrdersList);
        }

        filteredOrdersList = temp;
        updateListUI();
    }

    private void updateListUI() {
        int totalCount = filteredOrdersList.size();
        List<OrderEntity> itemsToShow = new ArrayList<>();
        for (int i = 0; i < Math.min(showLimit, totalCount); i++) {
            itemsToShow.add(filteredOrdersList.get(i));
        }
        adapter.updateData(itemsToShow);

        if (totalCount > showLimit) {
            int remaining = totalCount - showLimit;
            binding.btnLoadMore.setVisibility(View.VISIBLE);
            binding.txtLoadMore.setText("Xem thêm (còn " + remaining + " đơn hàng)");
        } else {
            binding.btnLoadMore.setVisibility(View.GONE);
        }
    }

    private List<OrderEntity> parseOrdersFromAssets() {
        List<OrderEntity> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("orders.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("orders");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONArray itemsArray = obj.getJSONArray("items");
                List<OrderItem> orderItems = new ArrayList<>();
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    OrderItem item = new OrderItem();
                    item.setProductId(itemObj.getString("productId"));
                    item.setProductName(itemObj.getString("productName"));
                    item.setProductImage(itemObj.getString("productImage"));
                    item.setQuantity(itemObj.getInt("quantity"));
                    item.setPrice(itemObj.getLong("price"));
                    orderItems.add(item);
                }

                OrderEntity oe = new OrderEntity();
                oe.setId(obj.getString("id"));
                oe.setUserId(obj.getString("userId"));
                oe.setOrderDate(obj.getString("orderDate"));
                oe.setOrderTime(obj.getString("orderTime"));
                oe.setStatus(obj.getString("status"));
                oe.setTotalAmount(obj.getLong("totalAmount"));
                oe.setItems(orderItems);
                oe.setShippingName(obj.optString("shippingName", ""));
                oe.setShippingPhone(obj.optString("shippingPhone", ""));
                oe.setShippingAddress(obj.optString("shippingAddress", ""));
                oe.setShippingCost(obj.optLong("shippingCost", 0L));
                oe.setVoucherDiscount(obj.optLong("voucherDiscount", 0L));
                oe.setPaymentMethod(obj.optString("paymentMethod", ""));
                if (obj.has("expectedDeliveryTime") && !obj.isNull("expectedDeliveryTime")) {
                    oe.setExpectedDeliveryTime(obj.getString("expectedDeliveryTime"));
                }
                list.add(oe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String formatSpending(long spending) {
        double million = spending / 1000000.0;
        String formatted = String.format(Locale.US, "%.1f", million);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2) + "M";
        } else {
            return formatted + "M";
        }
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;
        if (customerId != null) {
            mainActivity.loadFragment(CustomerDetailFragment.newInstance(customerId, fromStaff));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // RecyclerView Adapter
    private static class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {
        private List<OrderEntity> list;

        OrderHistoryAdapter(List<OrderEntity> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemOrderHistoryBinding binding = ItemOrderHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrderEntity order = list.get(position);
            ItemOrderHistoryBinding binding = holder.binding;

            binding.txtOrderId.setText("#" + order.getId());
            binding.txtOrderDatetime.setText(order.getOrderTime() + ", " + order.getOrderDate());

            int totalQty = 0;
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    totalQty += item.getQuantity();
                }
            }
            binding.txtProductsCount.setText(String.format(Locale.US, "%02d Sản phẩm", totalQty));

            // Format price
            NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
            binding.txtTotalAmount.setText(formatter.format(order.getTotalAmount()) + "đ");

            // Status styling
            String status = order.getStatus() != null ? order.getStatus() : "";
            binding.badgeStatus.setText(status.toUpperCase());
            switch (status.toLowerCase()) {
                case "hoàn thành":
                case "hoàn tất":
                    binding.badgeStatus.setTextColor(Color.parseColor("#4F6544"));
                    binding.badgeStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E8DA")));
                    break;
                case "đang giao":
                    binding.badgeStatus.setTextColor(Color.parseColor("#FFFFFF"));
                    binding.badgeStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
                    break;
                case "huỷ":
                case "đã huỷ":
                    binding.badgeStatus.setTextColor(Color.parseColor("#D32F2F"));
                    binding.badgeStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                    break;
                default: // Đang xử lý / Chờ xử lý
                    binding.badgeStatus.setTextColor(Color.parseColor("#C69C2C"));
                    binding.badgeStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFDE7")));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return list != null ? list.size() : 0;
        }

        void updateData(List<OrderEntity> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemOrderHistoryBinding binding;

            ViewHolder(ItemOrderHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
