package com.veganbeauty.admin.features.order;

import android.graphics.Color;
import android.graphics.Paint;
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
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.OrderEntity;
import com.veganbeauty.admin.data.local.entities.OrderItem;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.OrderRepository;
import com.veganbeauty.admin.databinding.OrderDetailFragmentBinding;
import com.veganbeauty.admin.databinding.ItemOrderDetailProductBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderDetailFragment extends RootieAdminFragment {

    private static final String ARG_ORDER_ID = "arg_order_id";

    private OrderDetailFragmentBinding binding;
    private String orderId;
    private OrderRepository repository;

    public static OrderDetailFragment newInstance(String orderId) {
        OrderDetailFragment fragment = new OrderDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    public OrderDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString(ARG_ORDER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = OrderDetailFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        repository = new OrderRepository(database.orderDao(), new FirebaseService(), database);

        setupListeners();
        loadOrderData();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnNotification.setOnClickListener(v -> Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show());
    }

    private void loadOrderData() {
        String id = orderId;
        if (id == null) return;

        new Thread(() -> {
            try {
                RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
                OrderEntity order = database.orderDao().getByIdSync(id);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (order != null) {
                        bindOrder(order);
                    } else {
                        Toast.makeText(requireContext(), "Không tìm thấy đơn hàng!", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindOrder(OrderEntity order) {
        binding.txtOrderCodeTitle.setText("Đơn hàng " + order.getOrderId());

        // Status and styling
        String statusClean = order.getStatus() != null ? order.getStatus().trim() : "";
        binding.txtStatusVal.setText(statusClean);
        setupStatusStyle(statusClean);

        // Shipping Info
        String shippingName = order.getShippingName();
        binding.txtShippingName.setText(shippingName != null && !shippingName.trim().isEmpty() ? shippingName : "Khách hàng Rootie");
        String shippingPhone = order.getShippingPhone();
        binding.txtShippingPhone.setText(shippingPhone != null && !shippingPhone.trim().isEmpty() ? shippingPhone : "Chưa cập nhật");
        String shippingAddress = order.getShippingAddress();
        binding.txtShippingAddress.setText(shippingAddress != null && !shippingAddress.trim().isEmpty() ? shippingAddress : "Chưa cập nhật");

        // Populate Products
        binding.layoutProductsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        long calculatedSubtotal = 0L;

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                ItemOrderDetailProductBinding itemBinding = ItemOrderDetailProductBinding.inflate(inflater, binding.layoutProductsContainer, false);
                itemBinding.txtProductName.setText(item.getProductName());
                itemBinding.txtProductPrice.setText(formatCurrency(item.getPrice()));
                itemBinding.txtProductQuantity.setText("x" + item.getQuantity());

                if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                    ImageUtils.loadImage(requireContext(), itemBinding.imgProduct, item.getProductImage(), R.drawable.nuoc_sen_hau_giang);
                } else {
                    itemBinding.imgProduct.setImageResource(R.drawable.nuoc_sen_hau_giang);
                }

                // Add original price crossed out if there's any discount.
                boolean isDiscounted = order.getVoucherDiscount() > 0L;
                if (!isDiscounted) {
                    long itemsTotal = 0L;
                    for (OrderItem oi : order.getItems()) {
                        itemsTotal += oi.getPrice() * oi.getQuantity();
                    }
                    isDiscounted = order.getTotalAmount() < (itemsTotal + order.getShippingCost());
                }

                long simulatedOriginalPrice = isDiscounted ? (long) (item.getPrice() * 1.25) : 0L;

                if (simulatedOriginalPrice > 0L) {
                    itemBinding.txtOriginalPrice.setVisibility(View.VISIBLE);
                    itemBinding.txtOriginalPrice.setText(formatCurrency(simulatedOriginalPrice));
                    itemBinding.txtOriginalPrice.setPaintFlags(itemBinding.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    itemBinding.txtOriginalPrice.setVisibility(View.GONE);
                }

                binding.layoutProductsContainer.addView(itemBinding.getRoot());
                calculatedSubtotal += item.getPrice() * item.getQuantity();
            }
        }

        // Calculations
        binding.txtSubtotal.setText(formatCurrency(calculatedSubtotal));

        // Voucher Discount
        binding.txtVoucherDiscount.setText(order.getVoucherDiscount() > 0L ? "-" + formatCurrency(order.getVoucherDiscount()) : "0đ");

        // Shipping Cost
        if (order.getShippingCost() == 0L) {
            binding.txtShippingCost.setText("Miễn phí");
        } else {
            binding.txtShippingCost.setText(formatCurrency(order.getShippingCost()));
        }

        // Direct discount (calculated based on difference)
        long rawDiff = (calculatedSubtotal + order.getShippingCost() - order.getVoucherDiscount()) - order.getTotalAmount();
        long directDiscount = Math.max(rawDiff, 0L);
        binding.txtDirectDiscount.setText(directDiscount > 0L ? "-" + formatCurrency(directDiscount) : "0đ");

        // Total
        binding.txtTotalVal.setText(formatCurrency(order.getTotalAmount()));

        // Payment method subtext
        String paymentMethod = order.getPaymentMethod();
        String methodLower = paymentMethod != null ? paymentMethod.toLowerCase() : "";
        if (methodLower.contains("cod") || methodLower.contains("khi nhận hàng")) {
            binding.txtPaymentMethod.setText("(Thanh toán khi nhận hàng COD)");
        } else if (methodLower.contains("momo")) {
            binding.txtPaymentMethod.setText("(Thanh toán qua ví MoMo)");
        } else if (methodLower.contains("ngân hàng") || methodLower.contains("chuyển khoản")) {
            binding.txtPaymentMethod.setText("(Thanh toán Chuyển khoản ngân hàng)");
        } else {
            binding.txtPaymentMethod.setText("(" + (paymentMethod != null ? paymentMethod : "") + ")");
        }

        // Setup Actions
        setupActionButtons(order);
    }

    private void setupStatusStyle(String status) {
        String normalizedStatus = status.toLowerCase();
        int textColor;

        if (normalizedStatus.contains("chờ") || normalizedStatus.contains("xử lý")) {
            textColor = Color.parseColor("#4F6544");
        } else if (normalizedStatus.contains("chuẩn bị")) {
            textColor = Color.parseColor("#B0882E");
        } else if (normalizedStatus.contains("giao")) {
            textColor = Color.parseColor("#2B74B3");
        } else if (normalizedStatus.contains("hoàn") || normalizedStatus.contains("tất") || normalizedStatus.contains("thành")) {
            textColor = Color.parseColor("#1B8756");
        } else if (normalizedStatus.contains("hủy")) {
            textColor = Color.parseColor("#C92F2F");
        } else {
            textColor = Color.parseColor("#3E4D44");
        }
        binding.txtStatusVal.setTextColor(textColor);
    }

    private void setupActionButtons(OrderEntity order) {
        String status = order.getStatus();
        String normalizedStatus = status != null ? status.trim().toLowerCase() : "";
        View actionsLayout = binding.layoutActionsBar;
        android.widget.Button btnCancel = binding.btnCancel;
        android.widget.Button btnApprove = binding.btnApprove;

        if (normalizedStatus.contains("chờ xử lý") || normalizedStatus.contains("chờ xác nhận")) {
            actionsLayout.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("Hủy");
            btnApprove.setVisibility(View.VISIBLE);
            btnApprove.setText("Xác nhận");

            btnCancel.setOnClickListener(v -> updateStatus(order.getOrderId(), "Đã hủy"));
            btnApprove.setOnClickListener(v -> updateStatus(order.getOrderId(), "Đang chuẩn bị"));
        } else if (normalizedStatus.contains("đang xử lý") || normalizedStatus.contains("đang chuẩn bị")) {
            actionsLayout.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("Hủy");
            btnApprove.setVisibility(View.VISIBLE);
            btnApprove.setText("Giao hàng");

            btnCancel.setOnClickListener(v -> updateStatus(order.getOrderId(), "Đã hủy"));
            btnApprove.setOnClickListener(v -> updateStatus(order.getOrderId(), "Đang giao"));
        } else if (normalizedStatus.contains("đang giao")) {
            actionsLayout.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("Thất bại");
            btnApprove.setVisibility(View.VISIBLE);
            btnApprove.setText("Hoàn tất");

            btnCancel.setOnClickListener(v -> updateStatus(order.getOrderId(), "Đã hủy"));
            btnApprove.setOnClickListener(v -> updateStatus(order.getOrderId(), "Hoàn tất"));
        } else {
            // Completed or Cancelled -> no actions
            actionsLayout.setVisibility(View.GONE);
        }
    }

    private void updateStatus(String orderId, String newStatus) {
        new Thread(() -> {
            try {
                boolean success = repository.updateOrderStatus(orderId, newStatus);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    if (success) {
                        Toast.makeText(requireContext(), "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                        loadOrderData(); // Reload
                    } else {
                        Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.loadFragment(new OrderListFragment());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
