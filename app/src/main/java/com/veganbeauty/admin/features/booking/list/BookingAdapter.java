package com.veganbeauty.admin.features.booking.list;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.veganbeauty.admin.R;
import com.veganbeauty.admin.core.utils.ImageUtils;
import com.veganbeauty.admin.core.utils.ImageViewerDialog;
import com.veganbeauty.admin.core.utils.ReviewImageParser;
import com.veganbeauty.admin.data.local.entities.BookingEntity;
import com.veganbeauty.admin.databinding.BookingItemBinding;

import java.util.List;

public class BookingAdapter extends ListAdapter<BookingEntity, BookingAdapter.BookingViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(BookingEntity booking, String status);
    }

    public interface OnCancelClickListener {
        void onCancelClick(BookingEntity booking);
    }

    public interface OnItemClickListener {
        void onItemClick(BookingEntity booking);
    }

    private final boolean isAdmin;
    private final OnActionClickListener onActionClickListener;
    private final OnCancelClickListener onCancelClickListener;
    private final OnItemClickListener onItemClickListener;

    public BookingAdapter(
        boolean isAdmin,
        OnActionClickListener onActionClickListener,
        OnCancelClickListener onCancelClickListener
    ) {
        this(isAdmin, onActionClickListener, onCancelClickListener, null);
    }

    public BookingAdapter(
        boolean isAdmin,
        OnActionClickListener onActionClickListener,
        OnCancelClickListener onCancelClickListener,
        OnItemClickListener onItemClickListener
    ) {
        super(new BookingDiffCallback());
        this.isAdmin = isAdmin;
        this.onActionClickListener = onActionClickListener;
        this.onCancelClickListener = onCancelClickListener;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BookingItemBinding binding = BookingItemBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new BookingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private final BookingItemBinding binding;

        BookingViewHolder(BookingItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BookingEntity booking) {
            binding.tvBookingService.setText(booking.getServiceName());
            binding.tvBookingTime.setText(booking.getTime());

            // Date format: e.g. "Thứ 2, ngày 20 Tháng 5"
            String day = (booking.getDayOfWeek() != null && !booking.getDayOfWeek().isEmpty()) ? booking.getDayOfWeek() + ", " : "";
            String monthClean = booking.getMonthDisplay() != null ? booking.getMonthDisplay().replace("\n", " ") : "";
            binding.tvBookingDate.setText(day + "ngày " + booking.getDateDisplay() + " " + monthClean);
            binding.tvBookingStore.setText(booking.getStoreName() + " - " + booking.getStoreAddress());

            binding.tvCustomerName.setText(booking.getUserName());
            binding.tvCustomerPhoneEmail.setText("SĐT: " + booking.getUserPhone() + " | Email: " + booking.getUserEmail());

            if (booking.getNote() != null && !booking.getNote().isEmpty()) {
                binding.tvBookingNote.setVisibility(View.VISIBLE);
                binding.tvBookingNote.setText("Ghi chú: " + booking.getNote());
            } else {
                binding.tvBookingNote.setVisibility(View.GONE);
            }

            // Cancel Reason setup
            boolean isCancelled = booking.getStatus() != null && (
                booking.getStatus().equalsIgnoreCase("Đã huỷ") ||
                booking.getStatus().equalsIgnoreCase("Đã hủy") ||
                booking.getStatus().equalsIgnoreCase("cancelled")
            );

            if (isCancelled && booking.getCancelReason() != null && !booking.getCancelReason().isEmpty()) {
                binding.llCancelReason.setVisibility(View.VISIBLE);
                binding.tvCancelReason.setText(booking.getCancelReason());
            } else {
                binding.llCancelReason.setVisibility(View.GONE);
            }

            // Setup Badge Status
            setupStatusBadge(booking.getStatus());

            // Customer feedback for completed bookings
            bindCustomerFeedback(booking);

            // Setup Actions layout
            setupActions(booking);

            binding.getRoot().setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(booking);
                }
            });
        }

        private void bindCustomerFeedback(BookingEntity booking) {
            String status = booking.getStatus() != null ? booking.getStatus().trim() : "";
            boolean isCompleted = status.equalsIgnoreCase("Đã hoàn thành") || status.equalsIgnoreCase("completed");
            boolean hasFeedback = booking.hasCustomerFeedback();

            if (!isCompleted) {
                binding.llCustomerFeedback.setVisibility(View.GONE);
                binding.tvFeedbackBadge.setVisibility(View.GONE);
                return;
            }

            binding.llCustomerFeedback.setVisibility(View.VISIBLE);

            if (!hasFeedback) {
                binding.tvFeedbackStars.setVisibility(View.GONE);
                binding.tvFeedbackScore.setVisibility(View.GONE);
                binding.tvFeedbackText.setVisibility(View.GONE);
                binding.imgFeedbackPhoto.setVisibility(View.GONE);
                binding.tvFeedbackDate.setVisibility(View.GONE);
                binding.tvFeedbackEmpty.setVisibility(View.VISIBLE);
                binding.tvFeedbackBadge.setVisibility(View.GONE);
                return;
            }

            binding.tvFeedbackEmpty.setVisibility(View.GONE);
            binding.tvFeedbackStars.setVisibility(View.VISIBLE);
            binding.tvFeedbackScore.setVisibility(View.VISIBLE);

            int stars = Math.max(0, Math.min(5, Math.round(booking.getUserRating())));
            binding.tvFeedbackStars.setText(buildStarText(stars));
            binding.tvFeedbackScore.setText(stars + "/5");
            binding.tvFeedbackBadge.setVisibility(View.VISIBLE);
            binding.tvFeedbackBadge.setText("Đã phản hồi " + stars + "/5");

            String reviewText = booking.getUserReview();
            if (reviewText != null && !reviewText.trim().isEmpty()) {
                binding.tvFeedbackText.setVisibility(View.VISIBLE);
                binding.tvFeedbackText.setText(reviewText.trim());
            } else {
                binding.tvFeedbackText.setVisibility(View.GONE);
            }

            List<String> allImages = ReviewImageParser.parseLoadableUrls(booking.getFeedbackImageUrls());
            String firstImageUrl = allImages.isEmpty() ? "" : allImages.get(0);
            if (!firstImageUrl.isEmpty()) {
                binding.imgFeedbackPhoto.setVisibility(View.VISIBLE);
                ImageUtils.loadImage(
                        binding.getRoot().getContext(),
                        binding.imgFeedbackPhoto,
                        firstImageUrl,
                        R.drawable.nuoc_sen_hau_giang
                );
                binding.imgFeedbackPhoto.setOnClickListener(v ->
                        ImageViewerDialog.show(binding.getRoot().getContext(), allImages, 0)
                );
            } else {
                binding.imgFeedbackPhoto.setVisibility(View.GONE);
            }

            String reviewDate = booking.getReviewDate();
            if (reviewDate != null && !reviewDate.trim().isEmpty()) {
                binding.tvFeedbackDate.setVisibility(View.VISIBLE);
                binding.tvFeedbackDate.setText("Đánh giá ngày " + reviewDate.trim());
            } else {
                binding.tvFeedbackDate.setVisibility(View.GONE);
            }
        }

        private String buildStarText(int stars) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                builder.append(i < stars ? "★" : "☆");
            }
            return builder.toString();
        }

        private void setupStatusBadge(String status) {
            if (status == null) status = "";
            boolean isPending = status.equalsIgnoreCase("Chờ xác nhận") || status.equalsIgnoreCase("pending");
            boolean isUpcoming = status.equalsIgnoreCase("Sắp diễn ra") ||
                status.equalsIgnoreCase("confirmed") ||
                status.equalsIgnoreCase("upcoming");
            boolean isCompleted = status.equalsIgnoreCase("Đã hoàn thành") || status.equalsIgnoreCase("completed");
            boolean isCancelled = status.equalsIgnoreCase("Đã huỷ") ||
                status.equalsIgnoreCase("Đã hủy") ||
                status.equalsIgnoreCase("cancelled");

            String badgeText;
            String textColor;
            String bgColor;

            if (isPending) {
                badgeText = "Chờ xác nhận";
                textColor = "#FF9F1C";
                bgColor = "#FFEED6";
            } else if (isUpcoming) {
                badgeText = "Sắp diễn ra";
                textColor = "#3498DB";
                bgColor = "#E8F4F8";
            } else if (isCompleted) {
                badgeText = "Hoàn tất";
                textColor = "#2ECC71";
                bgColor = "#E8F8F5";
            } else if (isCancelled) {
                badgeText = "Đã hủy";
                textColor = "#E74C3C";
                bgColor = "#FADBD8";
            } else {
                badgeText = status;
                textColor = "#95A192";
                bgColor = "#F2F4EB";
            }

            binding.tvStatusBadge.setText(badgeText);
            binding.tvStatusBadge.setTextColor(Color.parseColor(textColor));
            binding.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(bgColor)));
        }

        private void setupActions(BookingEntity booking) {
            String status = booking.getStatus();
            if (status == null) status = "";
            boolean isPending = status.equalsIgnoreCase("Chờ xác nhận") || status.equalsIgnoreCase("pending");
            boolean isUpcoming = status.equalsIgnoreCase("Sắp diễn ra") ||
                status.equalsIgnoreCase("confirmed") ||
                status.equalsIgnoreCase("upcoming");

            if (isPending) {
                binding.llActions.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                binding.btnAction.setVisibility(View.VISIBLE);
                binding.btnAction.setText("Xác nhận");
                binding.btnAction.setOnClickListener(v -> {
                    if (onActionClickListener != null) {
                        onActionClickListener.onActionClick(booking, "Sắp diễn ra");
                    }
                });
                binding.btnCancel.setOnClickListener(v -> {
                    if (onCancelClickListener != null) {
                        onCancelClickListener.onCancelClick(booking);
                    }
                });
            } else if (isUpcoming) {
                binding.llActions.setVisibility(View.VISIBLE);
                binding.btnCancel.setVisibility(View.VISIBLE);
                binding.btnAction.setVisibility(View.VISIBLE);
                binding.btnAction.setText("Hoàn tất");
                binding.btnAction.setOnClickListener(v -> {
                    if (onActionClickListener != null) {
                        onActionClickListener.onActionClick(booking, "Đã hoàn thành");
                    }
                });
                binding.btnCancel.setOnClickListener(v -> {
                    if (onCancelClickListener != null) {
                        onCancelClickListener.onCancelClick(booking);
                    }
                });
            } else {
                // Completed or Cancelled
                binding.llActions.setVisibility(View.GONE);
            }
        }
    }
}

class BookingDiffCallback extends DiffUtil.ItemCallback<BookingEntity> {
    @Override
    public boolean areItemsTheSame(@NonNull BookingEntity oldItem, @NonNull BookingEntity newItem) {
        return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull BookingEntity oldItem, @NonNull BookingEntity newItem) {
        return oldItem.equals(newItem);
    }
}
