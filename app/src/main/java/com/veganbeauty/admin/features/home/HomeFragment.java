package com.veganbeauty.admin.features.home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.base.UserSession;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.data.local.entities.ProductEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.OrderRepository;
import com.veganbeauty.admin.data.repository.ProductRepository;
import com.veganbeauty.admin.databinding.HomeFragmentBinding;
import com.veganbeauty.admin.databinding.HomeItemTopSellingBinding;
import com.veganbeauty.admin.features.order.OrderDetailFragment;
import com.veganbeauty.admin.features.order.OrderListFragment;
import com.veganbeauty.admin.features.product.add.ProductAddFragment;
import com.veganbeauty.admin.features.product.list.ProductListFragment;

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

public class HomeFragment extends RootieAdminFragment {

    private HomeFragmentBinding binding;
    private RecentActivityAdapter recentActivityAdapter;
    private final NumberFormat vndFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private interface DateFilter {
        boolean filter(String date);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = HomeFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        SessionManager sessionManager = new SessionManager(requireContext());
        String name = sessionManager.getFullName();
        if (name == null) {
            name = "Xuân";
        }
        binding.tvGreeting.setText("Chào buổi sáng, " + name);

        binding.cardSpaBooking.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(new com.veganbeauty.admin.features.booking.list.BookingListFragment());
            }
        });

        binding.sparklineRevenue.setLineColor(0xFF677559);
        binding.sparklineOrders.setLineColor(0xFF677559);

        recentActivityAdapter = new RecentActivityAdapter(
                new ArrayList<>(),
                activityItem -> {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) {
                        mainActivity.loadFragment(OrderDetailFragment.newInstance(activityItem.getId()));
                    }
                }
        );
        binding.rvRecentActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentActivities.setAdapter(recentActivityAdapter);

        binding.btnSeeAll.setOnClickListener(v -> openOrdersWithTab(OrderListFragment.TAB_ALL));

        setupSparklineCardClicks();
        setupAdminStatCardClicks();

        binding.btnSeeAllTopSelling.setOnClickListener(v -> openProductList());

        // Bind header message icon
        setupHeaderMessageButton(binding.header.homeHeaderMessageBtn);

        // Role switcher (test)
        binding.header.homeHeaderAvatarContainer.setOnClickListener(v -> {
            android.content.Context context = requireContext();
            String currentRole = UserSession.getRole(context);
            String[] roles = {"admin", "staff"};
            int checkedItem = "admin".equals(currentRole) ? 0 : 1;

            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Chọn Role để Test")
                .setSingleChoiceItems(roles, checkedItem, (dialog, which) -> {
                    String selectedRole = roles[which];
                    UserSession.setRole(context, selectedRole);
                    Toast.makeText(context, "Đã chuyển sang role: " + selectedRole, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
        });

        // === DASHBOARD THEO ROLE ===
        // Data loaded in onResume()
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHomeData(new SessionManager(requireContext()));
    }

    private void setupSparklineCardClicks() {
        binding.cardSparklineRevenue.setOnClickListener(v ->
                openOrdersWithTab(OrderListFragment.TAB_COMPLETED));
        binding.cardSparklineOrders.setOnClickListener(v ->
                openOrdersWithTab(OrderListFragment.TAB_ALL));
    }

    private void loadHomeData(SessionManager sessionManager) {
        new Thread(() -> {
            try {
                RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
                OrderRepository orderRepository = new OrderRepository(db.orderDao(), new FirebaseService(), db);
                orderRepository.checkAndSeedOrders(requireContext().getApplicationContext());
                List<OrderEntity> orders = orderRepository.getAllOrdersSync();

                String role = sessionManager.getRole();
                if (role == null) {
                    role = "admin";
                }
                boolean isAdmin = role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("business");
                List<JSONObject> products = isAdmin ? null : parseProductsJson();

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    bindOverviewCards(orders);
                    bindRecentActivities(orders);
                    if (isAdmin) {
                        binding.llAdminDashboard.setVisibility(View.VISIBLE);
                        binding.llStaffDashboard.setVisibility(View.GONE);
                        bindAdminDashboard(orders);
                    } else {
                        binding.llAdminDashboard.setVisibility(View.GONE);
                        binding.llStaffDashboard.setVisibility(View.VISIBLE);
                        bindStaffDashboard(products);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindOverviewCards(List<OrderEntity> orders) {
        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        long todayRevenue = calcRevenue(orders, dateStr -> isToday(dateStr, sdf, today));
        int todayOrderCount = countOrders(orders, dateStr -> isToday(dateStr, sdf, today));

        binding.tvSparklineRevenueValue.setText(formatVnd(todayRevenue));
        binding.tvSparklineOrdersValue.setText(String.valueOf(todayOrderCount));

        binding.sparklineRevenue.setData(buildDailyRevenueSparkline(orders, sdf, today));
        binding.sparklineOrders.setData(buildDailyOrderSparkline(orders, sdf, today));

        long thisWeekRevenue = calcRevenue(orders, dateStr -> isThisWeek(dateStr, sdf, today));
        long lastWeekRevenue = calcRevenue(orders, dateStr -> isLastWeek(dateStr, sdf, today));
        updateGrowthBadge(binding.tvSparklineRevenueBadge, thisWeekRevenue, lastWeekRevenue);

        int thisWeekOrders = countOrders(orders, dateStr -> isThisWeek(dateStr, sdf, today));
        int lastWeekOrders = countOrders(orders, dateStr -> isLastWeek(dateStr, sdf, today));
        updateGrowthBadge(binding.tvSparklineOrdersBadge, thisWeekOrders, lastWeekOrders);
    }

    private void bindRecentActivities(List<OrderEntity> orders) {
        List<OrderEntity> sorted = new ArrayList<>(orders);
        sorted.sort((o1, o2) -> {
            Date d1 = parseOrderDateTime(o1);
            Date d2 = parseOrderDateTime(o2);
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });

        List<RecentActivity> activities = new ArrayList<>();
        int limit = Math.min(4, sorted.size());
        for (int i = 0; i < limit; i++) {
            OrderEntity order = sorted.get(i);
            String title = getOrderDisplayTitle(order);
            String imageUrl = null;
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                imageUrl = order.getItems().get(0).getProductImage();
            }
            activities.add(new RecentActivity(
                    title,
                    order.getId(),
                    formatTimeAgo(order),
                    formatVnd(order.getTotalAmount()),
                    formatStatusLabel(order.getStatus()),
                    null,
                    imageUrl
            ));
        }
        recentActivityAdapter.updateItems(activities);
    }

    // -------------------------------------------------------------------------
    // Dashboard dispatcher
    // -------------------------------------------------------------------------

    private void setupAdminStatCardClicks() {
        binding.cardStatNewOrders.setOnClickListener(v ->
                openOrdersWithTab(OrderListFragment.TAB_NEW_ORDERS));
        binding.cardStatPendingOrders.setOnClickListener(v ->
                openOrdersWithTab(OrderListFragment.TAB_PENDING_CONFIRM));
        binding.cardStatShippingOrders.setOnClickListener(v ->
                openOrdersWithTab(OrderListFragment.TAB_SHIPPING));
    }

    private void openProductList() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;

        mainActivity.setCurrentTabId(R.id.nav_product);
        mainActivity.loadFragment(new ProductListFragment());

        View bottomNav = mainActivity.findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            BottomNavHelper.highlightTab(bottomNav, R.id.nav_product);
        }
    }

    private void openOrdersWithTab(String tab) {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null) return;

        mainActivity.setCurrentTabId(R.id.nav_order);
        mainActivity.loadFragment(OrderListFragment.newInstance(tab));

        View bottomNav = mainActivity.findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            BottomNavHelper.highlightTab(bottomNav, R.id.nav_order);
        }
    }

    private void bindAdminDashboard(List<OrderEntity> orders) {
        int newOrders = 0;
        int pendingOrders = 0;
        int shippingOrders = 0;
        long totalAmountSum = 0;

        for (OrderEntity order : orders) {
            String status = order.getStatus();
            if ("Chờ xử lý".equals(status)) {
                newOrders++;
            }
            if ("Chờ xử lý".equals(status) || "Đang xử lý".equals(status)) {
                pendingOrders++;
            }
            if ("Đang giao".equals(status)) {
                shippingOrders++;
            }
            totalAmountSum += order.getTotalAmount();
        }

        long avgValue = orders.isEmpty() ? 0L : totalAmountSum / orders.size();

        binding.tvStatNewOrders.setText(String.valueOf(newOrders));
        binding.tvStatPendingOrders.setText(String.valueOf(pendingOrders));
        binding.tvStatShippingOrders.setText(String.valueOf(shippingOrders));
        binding.tvStatAvgOrderValue.setText(formatShort(avgValue));

        setupRevenueTabs(orders);
        loadTopSellingProducts();
    }

    private void setupRevenueTabs(List<OrderEntity> orders) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar today = Calendar.getInstance();

        DateFilter todayFilter = dateStr -> isToday(dateStr, sdf, today);
        DateFilter weekFilter = dateStr -> isThisWeek(dateStr, sdf, today);
        DateFilter monthFilter = dateStr -> isThisMonth(dateStr, sdf, today);
        DateFilter quarterFilter = dateStr -> isThisQuarter(dateStr, sdf, today);

        // Default: hôm nay
        selectTab(binding.tabRevenueToday, todayFilter, orders);

        binding.tabRevenueToday.setOnClickListener(v -> selectTab(binding.tabRevenueToday, todayFilter, orders));
        binding.tabRevenueWeek.setOnClickListener(v -> selectTab(binding.tabRevenueWeek, weekFilter, orders));
        binding.tabRevenueMonth.setOnClickListener(v -> selectTab(binding.tabRevenueMonth, monthFilter, orders));
        binding.tabRevenueQuarter.setOnClickListener(v -> selectTab(binding.tabRevenueQuarter, quarterFilter, orders));
    }

    private long calcRevenue(List<OrderEntity> orders, DateFilter filter) {
        long sum = 0;
        for (OrderEntity order : orders) {
            if (filter.filter(order.getOrderDate()) && "Hoàn tất".equals(order.getStatus())) {
                sum += order.getTotalAmount();
            }
        }
        return sum;
    }

    private float calcCancelRate(List<OrderEntity> orders, DateFilter filter) {
        int total = 0;
        int cancelled = 0;
        for (OrderEntity order : orders) {
            if (filter.filter(order.getOrderDate())) {
                total++;
                if ("Đã hủy".equals(order.getStatus()) || "Hoàn hàng".equals(order.getStatus())) {
                    cancelled++;
                }
            }
        }
        return total == 0 ? 0f : ((float) cancelled / total) * 100f;
    }

    private boolean isToday(String dateStr, SimpleDateFormat sdf, Calendar today) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                   cal.get(Calendar.YEAR) == today.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isThisWeek(String dateStr, SimpleDateFormat sdf, Calendar today) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR) &&
                   cal.get(Calendar.YEAR) == today.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isThisMonth(String dateStr, SimpleDateFormat sdf, Calendar today) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                   cal.get(Calendar.YEAR) == today.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isThisQuarter(String dateStr, SimpleDateFormat sdf, Calendar today) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return (cal.get(Calendar.MONTH) / 3) == (today.get(Calendar.MONTH) / 3) &&
                   cal.get(Calendar.YEAR) == today.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLastWeek(String dateStr, SimpleDateFormat sdf, Calendar today) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            Calendar lastWeek = (Calendar) today.clone();
            lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
            return cal.get(Calendar.WEEK_OF_YEAR) == lastWeek.get(Calendar.WEEK_OF_YEAR) &&
                   cal.get(Calendar.YEAR) == lastWeek.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private int countOrders(List<OrderEntity> orders, DateFilter filter) {
        int count = 0;
        for (OrderEntity order : orders) {
            if (filter.filter(order.getOrderDate())) {
                count++;
            }
        }
        return count;
    }

    private List<Float> buildDailyRevenueSparkline(List<OrderEntity> orders, SimpleDateFormat sdf, Calendar today) {
        List<Float> points = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            long revenue = 0;
            for (OrderEntity order : orders) {
                if ("Hoàn tất".equals(order.getStatus()) && isSameCalendarDay(order.getOrderDate(), sdf, day)) {
                    revenue += order.getTotalAmount();
                }
            }
            points.add((float) revenue);
        }
        return ensureSparklinePoints(points);
    }

    private List<Float> buildDailyOrderSparkline(List<OrderEntity> orders, SimpleDateFormat sdf, Calendar today) {
        List<Float> points = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) today.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            int count = 0;
            for (OrderEntity order : orders) {
                if (isSameCalendarDay(order.getOrderDate(), sdf, day)) {
                    count++;
                }
            }
            points.add((float) count);
        }
        return ensureSparklinePoints(points);
    }

    private List<Float> ensureSparklinePoints(List<Float> points) {
        if (points.size() >= 2) {
            return points;
        }
        List<Float> fallback = new ArrayList<>();
        fallback.add(0f);
        fallback.add(0f);
        return fallback;
    }

    private boolean isSameCalendarDay(String dateStr, SimpleDateFormat sdf, Calendar targetDay) {
        try {
            Date date = sdf.parse(dateStr);
            if (date == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.DAY_OF_YEAR) == targetDay.get(Calendar.DAY_OF_YEAR) &&
                   cal.get(Calendar.YEAR) == targetDay.get(Calendar.YEAR);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateGrowthBadge(TextView badgeView, long current, long previous) {
        if (previous <= 0 && current <= 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }
        badgeView.setVisibility(View.VISIBLE);
        if (previous <= 0) {
            badgeView.setText("+100%");
            return;
        }
        float change = ((float) (current - previous) / previous) * 100f;
        badgeView.setText(formatPercentChange(change));
    }

    private void updateGrowthBadge(TextView badgeView, int current, int previous) {
        updateGrowthBadge(badgeView, (long) current, (long) previous);
    }

    private String formatPercentChange(float change) {
        String sign = change >= 0 ? "+" : "";
        return String.format(Locale.getDefault(), "%s%.0f%%", sign, change);
    }

    private Date parseOrderDateTime(OrderEntity order) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String time = order.getOrderTime() != null ? order.getOrderTime() : "00:00";
            return format.parse(order.getOrderDate() + " " + time);
        } catch (Exception e) {
            try {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(order.getOrderDate());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String formatTimeAgo(OrderEntity order) {
        Date orderDate = parseOrderDateTime(order);
        if (orderDate == null) {
            return order.getOrderDate() != null ? order.getOrderDate() : "";
        }
        long diffMs = System.currentTimeMillis() - orderDate.getTime();
        if (diffMs < 0) diffMs = 0;
        long minutes = diffMs / (60 * 1000);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";
        long days = hours / 24;
        if (days < 7) return days + " ngày trước";
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(orderDate);
    }

    private String formatStatusLabel(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "KHÔNG RÕ";
        }
        return status.trim().toUpperCase(Locale.getDefault());
    }

    private String getOrderDisplayTitle(OrderEntity order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem firstItem = order.getItems().get(0);
            if (firstItem.getProductName() != null && !firstItem.getProductName().trim().isEmpty()) {
                return firstItem.getProductName();
            }
        }
        return "Đơn hàng " + order.getId();
    }

    private void selectTab(TextView selected, DateFilter filter, List<OrderEntity> orders) {
        List<TextView> tabs = Arrays.asList(binding.tabRevenueToday, binding.tabRevenueWeek, binding.tabRevenueMonth, binding.tabRevenueQuarter);
        for (TextView tab : tabs) {
            tab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F2F4EB")));
            tab.setTextColor(Color.parseColor("#677559"));
            tab.setTypeface(null, Typeface.NORMAL);
        }
        selected.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F6544")));
        selected.setTextColor(Color.WHITE);
        selected.setTypeface(null, Typeface.BOLD);

        long revenue = calcRevenue(orders, filter);
        float rate = calcCancelRate(orders, filter);
        binding.tvRevenueValue.setText(vndFormat.format(revenue));
        binding.tvCancelRate.setText(String.format(Locale.getDefault(), "%.1f%%", rate));
    }

    private void loadTopSellingProducts() {
        new Thread(() -> {
            try {
                RootieAdminDatabase db = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
                ProductRepository productRepository = new ProductRepository(db.productDao(), new FirebaseService());
                productRepository.checkAndSeedProducts(requireContext().getApplicationContext());

                List<ProductEntity> products = new ArrayList<>(db.productDao().getAllSync());
                products.sort((p1, p2) -> Integer.compare(p2.getSold(), p1.getSold()));
                List<ProductEntity> top5 = products.subList(0, Math.min(5, products.size()));

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.llTopSelling.removeAllViews();

                    if (top5.isEmpty()) {
                        TextView emptyView = new TextView(requireContext());
                        emptyView.setText("Chưa có dữ liệu sản phẩm");
                        emptyView.setTextColor(Color.parseColor("#95A192"));
                        emptyView.setTextSize(13f);
                        emptyView.setPadding(0, dpToPx(8), 0, dpToPx(8));
                        binding.llTopSelling.addView(emptyView);
                        return;
                    }

                    for (int i = 0; i < top5.size(); i++) {
                        binding.llTopSelling.addView(createTopSellingRow(top5.get(i), i + 1, i < top5.size() - 1));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private View createTopSellingRow(ProductEntity product, int rank, boolean showDivider) {
        HomeItemTopSellingBinding rowBinding = HomeItemTopSellingBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.llTopSelling,
                false
        );

        rowBinding.txtRank.setText("#" + rank);
        rowBinding.txtProductName.setText(product.getName());
        rowBinding.txtSoldCount.setText(vndFormat.format(product.getSold()) + " đã bán");
        rowBinding.txtPrice.setText(formatVnd(product.getPrice()));
        rowBinding.divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);

        String imageUrl = product.getMainImage();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ImageUtils.loadImage(requireContext(), rowBinding.imvProduct, imageUrl, R.color.gray_light);
        } else {
            rowBinding.imvProduct.setImageResource(R.color.gray_light);
        }

        rowBinding.rowTopSelling.setOnClickListener(v -> {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity == null) return;
            View bottomNav = mainActivity.findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            mainActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, ProductAddFragment.newInstance(product.getId()))
                    .addToBackStack(null)
                    .commit();
        });

        return rowBinding.getRoot();
    }

    // -------------------------------------------------------------------------
    // STAFF Dashboard
    // -------------------------------------------------------------------------

    private void bindStaffDashboard(List<JSONObject> products) {
        if (products == null) {
            products = new ArrayList<>();
        }
        binding.llTopViewed.removeAllViews();
        List<JSONObject> topViewed = new ArrayList<>(products);
        topViewed.sort((p1, p2) -> Long.compare(p2.optLong("sold", 0), p1.optLong("sold", 0)));
        List<JSONObject> finalTopViewed = topViewed.subList(0, Math.min(5, topViewed.size()));

        List<JSONObject> topRated = new ArrayList<>(products);
        topRated.sort((p1, p2) -> Double.compare(p2.optDouble("rating", 0.0), p1.optDouble("rating", 0.0)));
        List<JSONObject> finalTopRated = topRated.subList(0, Math.min(5, topRated.size()));

        List<JSONObject> badProducts = new ArrayList<>();
        for (JSONObject p : products) {
            if (p.optDouble("rating", 5.0) < 3.5) {
                badProducts.add(p);
            }
        }
        badProducts.sort((p1, p2) -> Double.compare(p1.optDouble("rating", 0.0), p2.optDouble("rating", 0.0)));
        List<JSONObject> finalBadProducts = badProducts.subList(0, Math.min(5, badProducts.size()));

        for (int i = 0; i < finalTopViewed.size(); i++) {
            JSONObject p = finalTopViewed.get(i);
            binding.llTopViewed.addView(
                buildRankRow(i + 1, p.optString("name"), "Đã bán: " + vndFormat.format(p.optLong("sold", 0)), "#4F6544")
            );
        }

        binding.llTopRated.removeAllViews();
        for (int i = 0; i < finalTopRated.size(); i++) {
            JSONObject p = finalTopRated.get(i);
            double rating = p.optDouble("rating", 0.0);
            binding.llTopRated.addView(
                buildRankRow(i + 1, p.optString("name"), String.format(Locale.getDefault(), "⭐ %.1f", rating), "#59AE7B")
            );
        }

        binding.llBadFeedback.removeAllViews();
        if (finalBadProducts.isEmpty()) {
            binding.llBadFeedback.addView(buildInfoText("Không có sản phẩm nào có phản hồi xấu 🎉", "#1B5E20"));
        } else {
            for (JSONObject p : finalBadProducts) {
                double rating = p.optDouble("rating", 0.0);
                binding.llBadFeedback.addView(
                    buildInfoText(String.format(Locale.getDefault(), "⚠ %s — rating %.1f", p.optString("name"), rating), "#B71C1C")
                );
            }
        }

        buildSkinChart(products);
        buildFaqSection();
    }

    private void buildSkinChart(List<JSONObject> products) {
        String[] keys = {"Da thường", "Da dầu", "Da khô", "Da hỗn hợp", "Da nhạy cảm", "Mọi loại da"};
        java.util.Map<String, Integer> skinMap = new java.util.LinkedHashMap<>();
        for (String key : keys) {
            skinMap.put(key, 0);
        }
        for (JSONObject p : products) {
            String suitable = p.optString("suitableFor", "");
            for (String key : keys) {
                if (suitable.toLowerCase().contains(key.toLowerCase())) {
                    skinMap.put(key, skinMap.get(key) + 1);
                }
            }
        }

        int total = 0;
        for (int val : skinMap.values()) {
            total += val;
        }
        total = Math.max(total, 1);

        List<String> colors = Arrays.asList("#4F6544", "#59AE7B", "#677559", "#95A192", "#E65100", "#0D47A1");

        binding.llSkinChart.removeAllViews();

        List<java.util.Map.Entry<String, Integer>> entryList = new ArrayList<>(skinMap.entrySet());
        entryList.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        for (int idx = 0; idx < entryList.size(); idx++) {
            java.util.Map.Entry<String, Integer> entry = entryList.get(idx);
            String label = entry.getKey();
            int count = entry.getValue();
            float pct = (float) count / total;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = dpToPx(10);
            row.setLayoutParams(rowParams);

            TextView labelView = new TextView(requireContext());
            labelView.setText(label + "  (" + count + " sản phẩm)");
            labelView.setTextSize(12f);
            labelView.setTextColor(Color.parseColor("#7E8A83"));
            labelView.setTypeface(Typeface.DEFAULT);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.bottomMargin = dpToPx(4);
            labelView.setLayoutParams(labelParams);

            LinearLayout track = new LinearLayout(requireContext());
            track.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(10)));
            android.graphics.drawable.GradientDrawable trackBg = new android.graphics.drawable.GradientDrawable();
            trackBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            trackBg.setCornerRadius(dpToPx(5));
            trackBg.setColor(Color.parseColor("#E5E8DA"));
            track.setBackground(trackBg);

            View fill = new View(requireContext());
            int barWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.75f * pct);
            LinearLayout.LayoutParams fillParams = new LinearLayout.LayoutParams(
                Math.max(barWidth, dpToPx(4)),
                LinearLayout.LayoutParams.MATCH_PARENT
            );
            fill.setLayoutParams(fillParams);

            android.graphics.drawable.GradientDrawable fillBg = new android.graphics.drawable.GradientDrawable();
            fillBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            fillBg.setCornerRadius(dpToPx(5));
            fillBg.setColor(Color.parseColor(colors.get(idx % colors.size())));
            fill.setBackground(fillBg);

            track.addView(fill);
            row.addView(labelView);
            row.addView(track);
            binding.llSkinChart.addView(row);
        }
    }

    private void buildFaqSection() {
        List<String> faqs = Arrays.asList(
            "Da dầu nên dùng sản phẩm nào của Rootie?",
            "Serum nghệ có phù hợp với da nhạy cảm không?",
            "Tôi bị mụn ẩn, cần routine như thế nào?",
            "Kem chống nắng bí đao dùng được cho da khô không?",
            "Chu trình chăm sóc da tối giản cho người mới bắt đầu?"
        );
        binding.llFaq.removeAllViews();
        for (String question : faqs) {
            androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(requireContext());
            card.setRadius(dpToPx(12));
            card.setCardElevation(dpToPx(1));
            card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.bottomMargin = dpToPx(10);
            card.setLayoutParams(cardParams);

            TextView tv = new TextView(requireContext());
            tv.setText("💬  " + question);
            tv.setTextSize(13f);
            tv.setTextColor(Color.parseColor("#3E4D44"));
            tv.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            tv.setLineSpacing(0f, 1.3f);

            card.addView(tv);
            binding.llFaq.addView(card);
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private List<JSONObject> parseProductsJson() {
        List<JSONObject> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(requireContext().getAssets().open("products.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray arr = new JSONObject(sb.toString()).getJSONArray("products");
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private View buildRankRow(int rank, String name, String subtitle, String accentColor) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dpToPx(12);
        row.setLayoutParams(rowParams);

        TextView rankBadge = new TextView(requireContext());
        rankBadge.setText("#" + rank);
        rankBadge.setTextSize(11f);
        rankBadge.setTypeface(null, Typeface.BOLD);
        rankBadge.setTextColor(Color.parseColor(accentColor));
        android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
        badgeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(dpToPx(8));
        badgeBg.setColor(Color.parseColor("#E5E8DA"));
        rankBadge.setBackground(badgeBg);
        rankBadge.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.setMarginEnd(dpToPx(12));
        rankBadge.setLayoutParams(badgeParams);

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(requireContext());
        nameView.setText(name.length() > 50 ? name.substring(0, 50) + "…" : name);
        nameView.setTextSize(13f);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(Color.parseColor("#3E4D44"));
        nameView.setMaxLines(2);

        TextView subView = new TextView(requireContext());
        subView.setText(subtitle);
        subView.setTextSize(12f);
        subView.setTextColor(Color.parseColor("#7E8A83"));
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dpToPx(2);
        subView.setLayoutParams(subParams);

        info.addView(nameView);
        info.addView(subView);

        row.addView(rankBadge);
        row.addView(info);
        return row;
    }

    private TextView buildInfoText(String text, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(Color.parseColor(color));
        tv.setLineSpacing(0f, 1.4f);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvParams.bottomMargin = dpToPx(8);
        tv.setLayoutParams(tvParams);
        return tv;
    }

    private String formatVnd(long value) {
        return vndFormat.format(value) + "đ";
    }

    private String formatShort(long value) {
        if (value >= 1_000_000_000) {
            return String.format(Locale.getDefault(), "%.1ftỷ", value / 1_000_000_000.0);
        } else if (value >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.0ftr", value / 1_000_000.0);
        } else {
            return vndFormat.format(value);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
