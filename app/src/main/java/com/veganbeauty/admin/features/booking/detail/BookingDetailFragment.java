package com.veganbeauty.admin.features.booking.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.base.RootieAdminFragment;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.core.utils.ImageViewerDialog;
import com.veganbeauty.admin.core.utils.ReviewImageParser;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.data.remote.FirebaseService;
import com.veganbeauty.admin.data.repository.BookingRepository;
import com.veganbeauty.admin.databinding.BookingDetailFragmentBinding;
import com.veganbeauty.admin.features.booking.list.BookingListFragment;

import java.util.List;

public class BookingDetailFragment extends RootieAdminFragment {

    private static final String ARG_BOOKING_ID = "arg_booking_id";

    private BookingDetailFragmentBinding binding;
    private String bookingId;
    private BookingRepository repository;

    public static BookingDetailFragment newInstance(String bookingId) {
        BookingDetailFragment fragment = new BookingDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    public BookingDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookingId = getArguments().getString(ARG_BOOKING_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BookingDetailFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(View view) {
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(requireContext().getApplicationContext());
        repository = new BookingRepository(database.bookingDao(), new FirebaseService(), requireContext());

        binding.btnBack.setOnClickListener(v -> navigateBack());
        loadBookingData();
    }

    private void loadBookingData() {
        String id = bookingId;
        if (id == null) {
            return;
        }

        new Thread(() -> {
            try {
                BookingEntity booking = repository.fetchBookingById(id);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    if (booking != null) {
                        bindBooking(booking);
                    } else {
                        Toast.makeText(requireContext(), "Không tìm thấy lịch đặt!", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void bindBooking(BookingEntity booking) {
        binding.txtBookingTitle.setText("Lịch " + booking.getId());
        binding.txtServiceName.setText(booking.getServiceName());

        String status = booking.getStatus() != null ? booking.getStatus().trim() : "";
        binding.txtStatusVal.setText(status);
        setupStatusStyle(status);

        String day = booking.getDayOfWeek() != null && !booking.getDayOfWeek().isEmpty()
                ? booking.getDayOfWeek() + ", " : "";
        String monthClean = booking.getMonthDisplay() != null
                ? booking.getMonthDisplay().replace("\n", " ") : "";
        binding.txtBookingDate.setText(day + "ngày " + booking.getDateDisplay() + " " + monthClean);

        String duration = booking.getDuration() != null && !booking.getDuration().isEmpty()
                ? " (" + booking.getDuration() + ")" : "";
        binding.txtBookingTime.setText(booking.getTime() + duration);
        binding.txtStoreInfo.setText(booking.getStoreName() + " - " + booking.getStoreAddress());

        String consultant = booking.getConsultantName();
        if (consultant != null && !consultant.trim().isEmpty()) {
            binding.txtConsultant.setVisibility(View.VISIBLE);
            binding.txtConsultant.setText("Chuyên viên: " + consultant.trim());
        } else {
            binding.txtConsultant.setVisibility(View.GONE);
        }

        binding.txtCustomerName.setText(booking.getUserName());
        binding.txtCustomerContact.setText(
                "SĐT: " + booking.getUserPhone() + " | Email: " + booking.getUserEmail()
        );

        if (booking.getNote() != null && !booking.getNote().trim().isEmpty()) {
            binding.txtBookingNote.setVisibility(View.VISIBLE);
            binding.txtBookingNote.setText("Ghi chú: " + booking.getNote().trim());
        } else {
            binding.txtBookingNote.setVisibility(View.GONE);
        }

        boolean isCancelled = status.equalsIgnoreCase("Đã huỷ")
                || status.equalsIgnoreCase("Đã hủy")
                || status.equalsIgnoreCase("cancelled");
        if (isCancelled && booking.getCancelReason() != null && !booking.getCancelReason().trim().isEmpty()) {
            binding.txtCancelReason.setVisibility(View.VISIBLE);
            binding.txtCancelReason.setText("Lý do hủy: " + booking.getCancelReason().trim());
        } else {
            binding.txtCancelReason.setVisibility(View.GONE);
        }

        bindCustomerFeedback(booking);
    }

    private void bindCustomerFeedback(BookingEntity booking) {
        String status = booking.getStatus() != null ? booking.getStatus().trim().toLowerCase() : "";
        boolean isCompleted = status.contains("hoàn") || status.contains("completed");
        boolean hasFeedback = booking.hasCustomerFeedback();

        if (!isCompleted) {
            binding.txtFeedbackSectionHeader.setVisibility(View.GONE);
            binding.layoutCustomerFeedback.setVisibility(View.GONE);
            return;
        }

        binding.txtFeedbackSectionHeader.setVisibility(View.VISIBLE);
        binding.layoutCustomerFeedback.setVisibility(View.VISIBLE);

        if (!hasFeedback) {
            binding.txtFeedbackStars.setVisibility(View.GONE);
            binding.txtFeedbackScore.setVisibility(View.GONE);
            binding.txtFeedbackText.setVisibility(View.GONE);
            binding.layoutFeedbackImages.setVisibility(View.GONE);
            binding.txtFeedbackDate.setVisibility(View.GONE);
            binding.txtFeedbackEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.txtFeedbackEmpty.setVisibility(View.GONE);
        binding.txtFeedbackStars.setVisibility(View.VISIBLE);
        binding.txtFeedbackScore.setVisibility(View.VISIBLE);

        int stars = Math.max(0, Math.min(5, Math.round(booking.getUserRating())));
        binding.txtFeedbackStars.setText(buildStarText(stars));
        binding.txtFeedbackScore.setText(stars + "/5");

        String reviewText = booking.getUserReview();
        if (reviewText != null && !reviewText.trim().isEmpty()) {
            binding.txtFeedbackText.setVisibility(View.VISIBLE);
            binding.txtFeedbackText.setText(reviewText.trim());
        } else {
            binding.txtFeedbackText.setVisibility(View.GONE);
        }

        bindFeedbackImages(booking.getFeedbackImageUrls());

        String reviewDate = booking.getReviewDate();
        if (reviewDate != null && !reviewDate.trim().isEmpty()) {
            binding.txtFeedbackDate.setVisibility(View.VISIBLE);
            binding.txtFeedbackDate.setText("Đánh giá ngày " + reviewDate.trim());
        } else {
            binding.txtFeedbackDate.setVisibility(View.GONE);
        }
    }

    private void bindFeedbackImages(String feedbackImageUrls) {
        binding.layoutFeedbackImages.removeAllViews();
        List<String> imageUrls = ReviewImageParser.parseLoadableUrls(feedbackImageUrls);
        if (imageUrls.isEmpty()) {
            binding.layoutFeedbackImages.setVisibility(View.GONE);
            return;
        }

        binding.layoutFeedbackImages.setVisibility(View.VISIBLE);
        int imageSize = dpToPx(120);
        int margin = dpToPx(8);

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            final int imageIndex = i;
            ImageView imageView = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageSize, imageSize);
            if (i > 0) {
                params.setMarginStart(margin);
            }
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_rounded_white_card);
            imageView.setContentDescription("Ảnh phản hồi " + (i + 1));
            ImageUtils.loadImage(requireContext(), imageView, imageUrl, R.drawable.nuoc_sen_hau_giang);
            imageView.setOnClickListener(v ->
                    ImageViewerDialog.show(requireContext(), imageUrls, imageIndex)
            );
            binding.layoutFeedbackImages.addView(imageView);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private String buildStarText(int stars) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            builder.append(i < stars ? "★" : "☆");
        }
        return builder.toString();
    }

    private void setupStatusStyle(String status) {
        String normalizedStatus = status.toLowerCase();
        int textColor;
        int bgColor;

        if (normalizedStatus.contains("chờ") || normalizedStatus.contains("pending")) {
            textColor = Color.parseColor("#FF9F1C");
            bgColor = Color.parseColor("#FFEED6");
        } else if (normalizedStatus.contains("sắp") || normalizedStatus.contains("confirmed") || normalizedStatus.contains("upcoming")) {
            textColor = Color.parseColor("#3498DB");
            bgColor = Color.parseColor("#E8F4F8");
        } else if (normalizedStatus.contains("hoàn") || normalizedStatus.contains("completed")) {
            textColor = Color.parseColor("#2ECC71");
            bgColor = Color.parseColor("#E8F8F5");
        } else if (normalizedStatus.contains("hủy") || normalizedStatus.contains("huỷ") || normalizedStatus.contains("cancelled")) {
            textColor = Color.parseColor("#E74C3C");
            bgColor = Color.parseColor("#FADBD8");
        } else {
            textColor = Color.parseColor("#95A192");
            bgColor = Color.parseColor("#F2F4EB");
        }

        binding.txtStatusVal.setTextColor(textColor);
        binding.txtStatusVal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
    }

    private void navigateBack() {
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.loadFragment(new BookingListFragment());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
