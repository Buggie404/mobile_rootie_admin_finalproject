package com.veganbeauty.admin.core.utils;

import android.content.Context;
import android.widget.ImageView;
import coil.Coil;
import coil.request.ImageRequest;

public class ImageUtils {

    public static void loadImage(Context context, ImageView imageView, Object data, int placeholderRes, int errorRes) {
        if (context == null || imageView == null) return;
        
        if (data == null) {
            if (errorRes != 0) {
                imageView.setImageResource(errorRes);
            }
            return;
        }

        ImageRequest.Builder builder = new ImageRequest.Builder(context)
                .data(data)
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
}
