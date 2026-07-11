package com.veganbeauty.admin.core.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.veganbeauty.admin.R;

import java.util.ArrayList;
import java.util.List;

public final class ImageViewerDialog {

    private ImageViewerDialog() {
    }

    public static void show(@NonNull Context context, @NonNull String imageUrl) {
        List<String> urls = new ArrayList<>();
        urls.add(imageUrl);
        show(context, urls, 0);
    }

    public static void show(@NonNull Context context, @NonNull List<String> imageUrls, int startIndex) {
        if (imageUrls.isEmpty()) {
            return;
        }

        List<String> validUrls = new ArrayList<>();
        for (String url : imageUrls) {
            if (url != null && !url.trim().isEmpty()) {
                validUrls.add(url.trim());
            }
        }
        if (validUrls.isEmpty()) {
            return;
        }

        int safeIndex = Math.max(0, Math.min(startIndex, validUrls.size() - 1));
        final int[] currentIndex = {safeIndex};

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View contentView = LayoutInflater.from(context).inflate(R.layout.dialog_image_viewer, null);
        dialog.setContentView(contentView);

        ImageView imgViewer = contentView.findViewById(R.id.img_viewer);
        ImageView btnClose = contentView.findViewById(R.id.btn_close);
        ImageView btnPrev = contentView.findViewById(R.id.btn_prev);
        ImageView btnNext = contentView.findViewById(R.id.btn_next);
        TextView txtImageIndex = contentView.findViewById(R.id.txt_image_index);

        Runnable renderImage = () -> {
            ImageUtils.loadImage(context, imgViewer, validUrls.get(currentIndex[0]), R.drawable.nuoc_sen_hau_giang);
            if (validUrls.size() > 1) {
                btnPrev.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
                txtImageIndex.setVisibility(View.VISIBLE);
                txtImageIndex.setText((currentIndex[0] + 1) + "/" + validUrls.size());
            } else {
                btnPrev.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
                txtImageIndex.setVisibility(View.GONE);
            }
        };

        renderImage.run();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnPrev.setOnClickListener(v -> {
            currentIndex[0] = (currentIndex[0] - 1 + validUrls.size()) % validUrls.size();
            renderImage.run();
        });
        btnNext.setOnClickListener(v -> {
            currentIndex[0] = (currentIndex[0] + 1) % validUrls.size();
            renderImage.run();
        });
        contentView.setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }
}
