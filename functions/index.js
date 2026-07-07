const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();
const AUTO_CANCEL_PREFIX = "Hệ thống tự động";
const REASON_PENDING =
  "Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.";
const REASON_UPCOMING =
  "Hệ thống tự động huỷ do đã quá thời gian hẹn mà khách không đến Spa.";
const ACTIVE_STATUSES = new Set([
  "chờ xác nhận",
  "pending",
  "sắp diễn ra",
  "confirmed",
  "upcoming",
]);

function isPendingStatus(status) {
  const value = (status || "").trim().toLowerCase();
  return value === "chờ xác nhận" || value === "pending";
}

function isUpcomingStatus(status) {
  const value = (status || "").trim().toLowerCase();
  return value === "sắp diễn ra" || value === "confirmed" || value === "upcoming";
}

function isCancelledStatus(status) {
  const value = (status || "").trim().toLowerCase();
  return value === "đã huỷ" || value === "đã hủy" || value === "cancelled";
}

function parseBookingTime(booking) {
  try {
    const now = new Date();
    let year = now.getFullYear();
    let month = now.getMonth();
    let day = 1;
    let hour = 0;
    let minute = 0;

    const dateDisplay = booking.dateDisplay || "";
    const time = booking.time || "";

    if (dateDisplay.includes("/")) {
      const parts = dateDisplay.split("/").map((part) => part.trim());
      if (parts[0]) day = parseInt(parts[0], 10);
      if (parts[1]) month = parseInt(parts[1], 10) - 1;
      if (parts[2]) year = parseInt(parts[2], 10);
    } else if (dateDisplay) {
      day = parseInt(dateDisplay.trim().split(" ")[0], 10);
      const monthDisplay = booking.monthDisplay || "";
      const match = monthDisplay.match(/(\d+)/);
      if (match) {
        month = parseInt(match[1], 10) - 1;
      }
    }

    if (time) {
      const startTime = time.split("-")[0].trim();
      const timeParts = startTime.split(":");
      if (timeParts.length >= 2) {
        hour = parseInt(timeParts[0], 10);
        minute = parseInt(timeParts[1], 10);
      }
    }

    return new Date(year, month, day, hour, minute, 0, 0);
  } catch (error) {
    return null;
  }
}

function resolveAutoCancelReason(booking, now = new Date()) {
  const status = booking.status || "";
  const normalized = status.trim().toLowerCase();
  if (!ACTIVE_STATUSES.has(normalized)) {
    return null;
  }

  const reason = booking.cancelReason || "";
  if (reason.startsWith(AUTO_CANCEL_PREFIX)) {
    return null;
  }

  const bookingTime = parseBookingTime(booking);
  if (!bookingTime || bookingTime >= now) {
    return null;
  }

  return isPendingStatus(status) ? REASON_PENDING : REASON_UPCOMING;
}

function buildBookingMessage(booking, bookingId) {
  const service = booking.serviceName || "Soi da";
  const date = booking.dateDisplay || "";
  const slot = booking.time || "";
  const store = booking.storeName || "Rootie";
  const when = date && slot ? `${slot} ngày ${date}` : date || slot || "thời gian đã đặt";
  return `Lịch ${service} (#${bookingId}) lúc ${when} tại ${store} đã bị huỷ`;
}

async function sendBookingCancelPush(booking, bookingId, cancelReason) {
  const userId = (booking.userId || "").trim();
  if (!userId) {
    return;
  }

  const userSnap = await db.collection("users").doc(userId).get();
  if (!userSnap.exists) {
    return;
  }

  const token = userSnap.get("fcm_token");
  if (!token) {
    return;
  }

  const title = "Lịch hẹn soi da đã bị huỷ";
  const baseMessage = buildBookingMessage(booking, bookingId);
  const content = cancelReason
    ? `${baseMessage} (${cancelReason})`
    : `${baseMessage}.`;

  await getMessaging().send({
    token,
    data: {
      id: `booking_cancel_${bookingId}`,
      title,
      content,
      category: "Lịch hẹn",
      actionText: "XEM LỊCH HẸN",
      notificationType: "schedule date",
      scheduleId: bookingId,
      iconResName: "ic_bell",
    },
    android: {
      priority: "high",
    },
  });
}

async function expireOverdueBookings() {
  const now = new Date();
  const snapshot = await db.collection("bookings").get();
  let expiredCount = 0;

  for (const doc of snapshot.docs) {
    const booking = doc.data();
    const reason = resolveAutoCancelReason(booking, now);
    if (!reason) {
      continue;
    }

    await doc.ref.update({
      status: "Đã huỷ",
      cancelReason: reason,
      cancelledAt: formatCancelledAt(now),
      autoCancelNotified: false,
    });
    expiredCount += 1;
  }

  return expiredCount;
}

function formatCancelledAt(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

exports.expireOverdueBookings = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Ho_Chi_Minh",
  },
  async () => {
    const expiredCount = await expireOverdueBookings();
    console.log(`Auto-expired ${expiredCount} overdue booking(s).`);
  }
);

exports.notifyBookingAutoCancel = onDocumentUpdated(
  "bookings/{bookingId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after) {
      return;
    }

    if (isCancelledStatus(before.status) || !isCancelledStatus(after.status)) {
      return;
    }

    const cancelReason = after.cancelReason || "";
    if (!cancelReason.startsWith(AUTO_CANCEL_PREFIX)) {
      return;
    }

    if (after.autoCancelNotified === true) {
      return;
    }

    const bookingId = event.params.bookingId;
    await sendBookingCancelPush(after, bookingId, cancelReason);

    await event.data.after.ref.update({
      autoCancelNotified: true,
      autoCancelNotifiedAt: FieldValue.serverTimestamp(),
    });
  }
);
