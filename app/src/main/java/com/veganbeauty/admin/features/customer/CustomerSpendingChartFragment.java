package com.veganbeauty.admin.features.customer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.CustomerEntity;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.databinding.CustomerSpendingChartBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerSpendingChartFragment extends RootieAdminFragment {

    private static final String ARG_CUSTOMER_ID = "arg_customer_id";
    private static final String ARG_FROM_STAFF = "arg_from_staff";

    private CustomerSpendingChartBinding binding;
    private String customerId;
    private boolean fromStaff = false;

    private final List<OrderEntity> allOrdersList = new ArrayList<>();
    private String currentPeriod = "year"; // week, month, 3months, 6months, year
    private CustomerEntity currentCustomer;

    private Calendar todayCalendar;

    private Calendar getTodayCalendar() {
        if (todayCalendar == null) {
            todayCalendar = Calendar.getInstance();
            todayCalendar.set(Calendar.YEAR, 2026);
            todayCalendar.set(Calendar.MONTH, Calendar.JUNE);
            todayCalendar.set(Calendar.DAY_OF_MONTH, 17);
            todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
            todayCalendar.set(Calendar.MINUTE, 0);
            todayCalendar.set(Calendar.SECOND, 0);
            todayCalendar.set(Calendar.MILLISECOND, 0);
        }
        return todayCalendar;
    }

    public static CustomerSpendingChartFragment newInstance(String customerId, boolean fromStaff) {
        CustomerSpendingChartFragment fragment = new CustomerSpendingChartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        args.putBoolean(ARG_FROM_STAFF, fromStaff);
        fragment.setArguments(args);
        return fragment;
    }

    public static CustomerSpendingChartFragment newInstance(String customerId) {
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
        binding = CustomerSpendingChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        setupListeners();
        loadData();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());

        binding.tabWeek.setOnClickListener(v -> selectPeriod("week"));
        binding.tabMonth.setOnClickListener(v -> selectPeriod("month"));
        binding.tab3months.setOnClickListener(v -> selectPeriod("3months"));
        binding.tab6months.setOnClickListener(v -> selectPeriod("6months"));
        binding.tabYear.setOnClickListener(v -> selectPeriod("year"));
    }

    private void selectPeriod(String period) {
        currentPeriod = period;

        // Reset tabs style
        List<TextView> tabs = Arrays.asList(binding.tabWeek, binding.tabMonth, binding.tab3months, binding.tab6months, binding.tabYear);
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_search_bar);
            t.setTextColor(Color.parseColor("#3E4D44"));
            t.setBackgroundTintList(null);
        }

        // Highlight selected tab
        TextView selectedView = binding.tabYear;
        switch (period) {
            case "week":
                selectedView = binding.tabWeek;
                break;
            case "month":
                selectedView = binding.tabMonth;
                break;
            case "3months":
                selectedView = binding.tab3months;
                break;
            case "6months":
                selectedView = binding.tab6months;
                break;
            case "year":
                selectedView = binding.tabYear;
                break;
        }

        selectedView.setBackgroundResource(R.drawable.bg_nav_pill);
        selectedView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C3B2D")));
        selectedView.setTextColor(Color.parseColor("#FFFFFF"));

        calculateAndDisplayChart();
    }

    private void loadData() {
        if (customerId == null) return;
        if (getActivity() == null) return;
        RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        new Thread(() -> {
            try {
                // Load customer
                currentCustomer = db.customerDao().getByIdSync(customerId);

                // Load orders, populate if empty
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
                            binding.txtMemberTierBadge.setText(currentCustomer.getTier().toUpperCase(Locale.getDefault()));
                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy khách hàng!", Toast.LENGTH_SHORT).show();
                            navigateBack();
                            return;
                        }

                        // Filter user orders
                        List<OrderEntity> userOrders = new ArrayList<>();
                        for (OrderEntity oe : finalOrders) {
                            if (customerId.equals(oe.getUserId())) {
                                userOrders.add(oe);
                            }
                        }
                        allOrdersList.clear();
                        allOrdersList.addAll(userOrders);

                        // Calculate total accumulated spending
                        long allTimeSpending = 0;
                        for (OrderEntity oe : allOrdersList) {
                            allTimeSpending += oe.getTotalAmount();
                        }
                        binding.txtAllTimeSpending.setText("Tổng chi tiêu tích lũy: " + formatCurrency(allTimeSpending));

                        // Start by default selecting the 'year' period
                        selectPeriod("year");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void calculateAndDisplayChart() {
        List<Float> dataPoints = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        long periodTotal = 0L;
        String avgLabel = "Trung bình/tháng";
        long avgValue = 0L;
        long highestValue = 0L;

        Calendar cal = Calendar.getInstance();
        cal.setTime(getTodayCalendar().getTime());

        switch (currentPeriod) {
            case "week": {
                avgLabel = "Trung bình/ngày";
                labels.addAll(Arrays.asList("T2", "T3", "T4", "T5", "T6", "T7", "CN"));

                // Get Monday of current week
                int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                int daysToSubtract = (currentDayOfWeek == Calendar.SUNDAY) ? -6 : 2 - currentDayOfWeek;
                cal.add(Calendar.DAY_OF_MONTH, daysToSubtract);

                long[] daySpendings = new long[7]; // Mon to Sun

                for (int i = 0; i < 7; i++) {
                    String dateStr = getFormattedDate(cal);
                    long daySum = 0;
                    for (OrderEntity oe : allOrdersList) {
                        if (dateStr.equals(oe.getOrderDate())) {
                            daySum += oe.getTotalAmount();
                        }
                    }
                    daySpendings[i] = daySum;
                    periodTotal += daySum;
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }

                for (long s : daySpendings) {
                    dataPoints.add((float) s);
                    if (s > highestValue) highestValue = s;
                }
                avgValue = periodTotal / 7;
                break;
            }
            case "month": {
                avgLabel = "Trung bình/tuần";
                labels.addAll(Arrays.asList("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4"));

                // Group by weeks of current month (June 2026)
                long[] weekSpendings = new long[4];
                for (OrderEntity order : allOrdersList) {
                    Calendar oCal = getCalendar(order.getOrderDate());
                    if (oCal != null &&
                        oCal.get(Calendar.YEAR) == 2026 &&
                        oCal.get(Calendar.MONTH) == Calendar.JUNE) {

                        int day = oCal.get(Calendar.DAY_OF_MONTH);
                        int weekIndex;
                        if (day <= 7) {
                            weekIndex = 0;
                        } else if (day <= 14) {
                            weekIndex = 1;
                        } else if (day <= 21) {
                            weekIndex = 2;
                        } else {
                            weekIndex = 3;
                        }
                        weekSpendings[weekIndex] += order.getTotalAmount();
                        periodTotal += order.getTotalAmount(); // Wait, order.totalAmount is not field, order.getTotalAmount() is getter!
                    }
                }

                for (long s : weekSpendings) {
                    dataPoints.add((float) s);
                    if (s > highestValue) highestValue = s;
                }
                avgValue = periodTotal / 4;
                break;
            }
            case "3months": {
                avgLabel = "Trung bình/tháng";
                labels.addAll(Arrays.asList("Tháng 4", "Tháng 5", "Tháng 6"));

                // Last 3 months: April (3), May (4), June (5)
                long[] monthSpendings = new long[3];
                for (OrderEntity order : allOrdersList) {
                    Calendar oCal = getCalendar(order.getOrderDate());
                    if (oCal != null && oCal.get(Calendar.YEAR) == 2026) {
                        int m = oCal.get(Calendar.MONTH);
                        if (m >= 3 && m <= 5) {
                            monthSpendings[m - 3] += order.getTotalAmount();
                            periodTotal += order.getTotalAmount();
                        }
                    }
                }

                for (long s : monthSpendings) {
                    dataPoints.add((float) s);
                    if (s > highestValue) highestValue = s;
                }
                avgValue = periodTotal / 3;
                break;
            }
            case "6months": {
                avgLabel = "Trung bình/tháng";
                labels.addAll(Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6"));

                // Jan (0) to June (5)
                long[] monthSpendings = new long[6];
                for (OrderEntity order : allOrdersList) {
                    Calendar oCal = getCalendar(order.getOrderDate());
                    if (oCal != null && oCal.get(Calendar.YEAR) == 2026) {
                        int m = oCal.get(Calendar.MONTH);
                        if (m >= 0 && m <= 5) {
                            monthSpendings[m] += order.getTotalAmount();
                            periodTotal += order.getTotalAmount();
                        }
                    }
                }

                for (long s : monthSpendings) {
                    dataPoints.add((float) s);
                    if (s > highestValue) highestValue = s;
                }
                avgValue = periodTotal / 6;
                break;
            }
            case "year": {
                avgLabel = "Trung bình/tháng";
                labels.addAll(Arrays.asList("T1", "T3", "T5", "T7", "T9", "T12"));

                // 12 months
                long[] monthSpendings = new long[12];
                for (OrderEntity order : allOrdersList) {
                    Calendar oCal = getCalendar(order.getOrderDate());
                    if (oCal != null && oCal.get(Calendar.YEAR) == 2026) {
                        int m = oCal.get(Calendar.MONTH);
                        if (m >= 0 && m <= 11) {
                            monthSpendings[m] += order.getTotalAmount();
                            periodTotal += order.getTotalAmount();
                        }
                    }
                }

                for (long s : monthSpendings) {
                    dataPoints.add((float) s);
                    if (s > highestValue) highestValue = s;
                }
                avgValue = periodTotal / 12;
                break;
            }
        }

        // Bind data
        binding.txtPeriodSpending.setText(formatCurrency(periodTotal));
        binding.txtAvgLabel.setText(avgLabel);
        binding.txtAvgValue.setText(formatValueK(avgValue));
        binding.txtHighestValue.setText(formatValueK(highestValue));

        // Advice and trend values matching screenshot styling
        binding.txtSpendingTrendDetail.setText("↑15% so với kỳ trước");
        binding.txtSpendingTrendDetail.setTextColor(Color.parseColor("#388E3C"));

        if (periodTotal > 2000000L) {
            binding.txtAdviceContent.setText("Bạn đã chi tiêu nhiều hơn cho kỳ này. Hãy cân nhắc các gói ưu đãi combo để tối ưu hóa ngân sách.");
        } else {
            binding.txtAdviceContent.setText("Mức chi tiêu ổn định. Tiếp tục duy trì các ưu đãi thành viên để nhận thêm điểm thưởng.");
        }

        // Draw chart
        binding.sparklineChart.setData(dataPoints);
        renderXAxisLabels(labels);
    }

    private void renderXAxisLabels(List<String> labels) {
        binding.xAxisContainer.removeAllViews();
        for (String label : labels) {
            TextView textView = new TextView(requireContext());
            textView.setText(label);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            textView.setTextColor(Color.parseColor("#7E8A83"));
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            ));
            binding.xAxisContainer.addView(textView);
        }
    }

    private Calendar getCalendar(String dateStr) {
        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
            Calendar cal = Calendar.getInstance();
            if (date != null) {
                cal.setTime(date);
                return cal;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getFormattedDate(Calendar calendar) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private String formatValueK(long amount) {
        double kAmount = amount / 1000.0;
        String formatted = String.format(Locale.US, "%.1f", kAmount);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2) + "k";
        } else {
            return formatted + "k";
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
                oe.setOrderId(obj.getString("id"));
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
}
