package com.veganbeauty.admin.core.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;

public class ImageUtils {

    public static void loadImage(Context context, ImageView imageView, Object data, int placeholderRes, int errorRes) {
        if (context == null || imageView == null) return;

        Object loadTarget = resolveLoadTarget(data);
        if (loadTarget == null) {
            if (errorRes != 0) {
                imageView.setImageResource(errorRes);
            }
            return;
        }

        ImageRequest.Builder builder = new ImageRequest.Builder(context)
                .data(loadTarget)
                .crossfade(true)
                .target(imageView);

        if (placeholderRes != 0) {
            builder.placeholder(placeholderRes);
        }
        if (errorRes != 0) {
            builder.error(errorRes);
        }

        Coil.imageLoader(context).enqueue(builder.build());
    }

    public static void loadImage(Context context, ImageView imageView, Object data, int placeholderErrorRes) {
        loadImage(context, imageView, data, placeholderErrorRes, placeholderErrorRes);
    }

    private static Object resolveLoadTarget(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof File) {
            File file = (File) data;
            return file.exists() ? file : null;
        }
        if (!(data instanceof String)) {
            return data;
        }

        String value = ((String) data).trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.startsWith("[")) {
            List<String> parsed = ReviewImageParser.parseLoadableUrls(value);
            if (parsed.isEmpty()) {
                return null;
            }
            return parsed.get(0);
        }

        if (ReviewImageParser.isLoadableRemoteUrl(value)) {
            return value;
        }

        if (value.startsWith("content://")) {
            return Uri.parse(value);
        }

        if (value.startsWith("file://")) {
            File file = new File(Uri.parse(value).getPath());
            return file.exists() ? file : null;
        }

        if (value.startsWith("/")) {
            File file = new File(value);
            return file.exists() ? file : null;
        }

        return value;
    }
}
