package com.veganbeauty.admin.core.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse review/feedback image fields from Firestore.
 * User app may store a JSON array, pipe-separated URLs, or a single URL/path.
 */
public final class ReviewImageParser {

    private ReviewImageParser() {
    }

    @NonNull
    public static List<String> parse(@Nullable String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray array = new JSONArray(trimmed);
                for (int i = 0; i < array.length(); i++) {
                    addIfPresent(result, array.optString(i, ""));
                }
                return result;
            } catch (Exception ignored) {
                // fall through to other formats
            }
        }

        if (trimmed.contains("|")) {
            for (String part : trimmed.split("\\|")) {
                addIfPresent(result, part);
            }
            return result;
        }

        addIfPresent(result, trimmed);
        return result;
    }

    @NonNull
    public static List<String> parseLoadableUrls(@Nullable String raw) {
        List<String> loadable = new ArrayList<>();
        for (String item : parse(raw)) {
            if (isLoadableRemoteUrl(item)) {
                loadable.add(item.trim());
            }
        }
        return loadable;
    }

    public static boolean hasLoadableImages(@Nullable String raw) {
        return !parseLoadableUrls(raw).isEmpty();
    }

    /** Images were saved on the customer's device only (local paths), not cloud URLs. */
    public static boolean hasPendingLocalImages(@Nullable String raw) {
        return !parse(raw).isEmpty() && !hasLoadableImages(raw);
    }

    public static int countParsedImages(@Nullable String raw) {
        return parse(raw).size();
    }

    public static boolean isLoadableRemoteUrl(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String lower = value.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static void addIfPresent(List<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.add(value.trim());
        }
    }
}
