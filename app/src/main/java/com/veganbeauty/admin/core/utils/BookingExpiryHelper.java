package com.veganbeauty.admin.core.utils;

import com.veganbeauty.admin.data.local.entities.BookingEntity;

import java.util.Calendar;
import java.util.Locale;

public final class BookingExpiryHelper {

    public static final String AUTO_CANCEL_PREFIX = "Hệ thống tự động";
    private static final String REASON_PENDING =
            "Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.";
    private static final String REASON_UPCOMING =
            "Hệ thống tự động huỷ do đã quá thời gian hẹn mà khách không đến Spa.";

    private BookingExpiryHelper() {
    }

    public static boolean isPendingStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "Chờ xác nhận".equalsIgnoreCase(normalized) || "pending".equalsIgnoreCase(normalized);
    }

    public static boolean isUpcomingStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "Sắp diễn ra".equalsIgnoreCase(normalized)
                || "confirmed".equalsIgnoreCase(normalized)
                || "upcoming".equalsIgnoreCase(normalized);
    }

    public static boolean isActiveForExpiry(String status) {
        return isPendingStatus(status) || isUpcomingStatus(status);
    }

    public static boolean isAlreadyAutoCancelled(BookingEntity booking) {
        if (booking == null) {
            return true;
        }
        String reason = booking.getCancelReason();
        return reason != null && reason.startsWith(AUTO_CANCEL_PREFIX);
    }

    /**
     * @return cancel reason when booking should be auto-cancelled, otherwise null
     */
    public static String resolveAutoCancelReason(BookingEntity booking, Calendar now) {
        if (booking == null || now == null) {
            return null;
        }
        String status = booking.getStatus();
        if (!isActiveForExpiry(status) || isAlreadyAutoCancelled(booking)) {
            return null;
        }

        Calendar bookingTime = parseBookingTime(booking);
        if (bookingTime == null || !bookingTime.before(now)) {
            return null;
        }

        if (isPendingStatus(status)) {
            return REASON_PENDING;
        }
        return REASON_UPCOMING;
    }

    public static Calendar parseBookingTime(BookingEntity booking) {
        try {
            Calendar cal = Calendar.getInstance();
            int currentYear = cal.get(Calendar.YEAR);

            String dateDisplay = booking.getDateDisplay();
            String time = booking.getTime();

            int day = 1;
            int month = cal.get(Calendar.MONTH);
            int year = currentYear;

            if (dateDisplay != null && !dateDisplay.isEmpty()) {
                if (dateDisplay.contains("/")) {
                    String[] parts = dateDisplay.split("/");
                    if (parts.length >= 1) {
                        day = Integer.parseInt(parts[0].trim());
                    }
                    if (parts.length >= 2) {
                        month = Integer.parseInt(parts[1].trim()) - 1;
                    }
                    if (parts.length >= 3) {
                        year = Integer.parseInt(parts[2].trim());
                    }
                } else {
                    day = Integer.parseInt(dateDisplay.trim().split(" ")[0]);
                    String monthDisplay = booking.getMonthDisplay();
                    if (monthDisplay != null && monthDisplay.toLowerCase(Locale.ROOT).contains("tháng")) {
                        String firstLine = monthDisplay.split("\n")[0];
                        String monthDigits = firstLine.replaceAll("[^0-9]", "");
                        if (!monthDigits.isEmpty()) {
                            month = Integer.parseInt(monthDigits) - 1;
                        }
                    }
                }
            }

            int hour = 0;
            int minute = 0;
            if (time != null && !time.isEmpty()) {
                String startTime = time.split("-")[0].trim();
                String[] timeParts = startTime.split(":");
                if (timeParts.length >= 2) {
                    hour = Integer.parseInt(timeParts[0].trim());
                    minute = Integer.parseInt(timeParts[1].trim());
                }
            }

            cal.set(year, month, day, hour, minute, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception ignored) {
            return null;
        }
    }
}
