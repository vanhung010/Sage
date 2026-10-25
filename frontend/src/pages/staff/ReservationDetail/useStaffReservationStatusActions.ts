import { useEffect, useRef, useState } from "react";

import { getApiError } from "../../../api/error";
import { staffReservationApi } from "../../../api/staff-reservation.api";
import type {
  AppFeedbackModalProps,
} from "../../../components/ui/AppFeedbackModal";
import type { ApiErrorResponse } from "../../../types/api";
import type { ReservationStatus } from "../../../types/reservation";
import type {
  StaffReservationAction,
  StaffReservationCommand,
  StaffReservationDetail,
} from "../../../types/staff-reservation";
import { dateInputValue } from "../../../utils/reservation";
import {
  cleanupEndsAt,
  detailDate,
  detailDateTime,
  detailTime,
} from "./reservationDetailPresentation";

type DialogAction = Exclude<StaffReservationAction, "arrive">;

const ALLOWED_STATUSES: Record<
  StaffReservationAction,
  readonly ReservationStatus[]
> = {
  approve: ["PENDING"],
  reject: ["PENDING"],
  cancel: ["PENDING", "CONFIRMED"],
  arrive: ["CONFIRMED"],
  "no-show": ["CONFIRMED"],
  complete: ["ARRIVED"],
};

const DIALOG_LABELS: Record<
  DialogAction,
  { title: string; primary: string }
> = {
  approve: {
    title: "Duyệt đơn đặt bàn?",
    primary: "Duyệt đơn",
  },
  reject: {
    title: "Từ chối đơn đặt bàn?",
    primary: "Từ chối đơn",
  },
  cancel: {
    title: "Hủy đơn thay khách?",
    primary: "Xác nhận hủy",
  },
  "no-show": {
    title: "Đánh dấu khách không đến?",
    primary: "Xác nhận không đến",
  },
  complete: {
    title: "Hoàn thành lượt đặt bàn?",
    primary: "Hoàn thành",
  },
};

function blockedReason(
  action: StaffReservationAction,
  reservation: StaffReservationDetail,
  now: number,
): string {
  if (!ALLOWED_STATUSES[action].includes(reservation.status)) {
    return "Trạng thái hiện tại không cho phép thao tác này.";
  }

  if (action === "approve") {
    const deadline = Date.parse(reservation.approvalDeadline ?? "");

    if (!Number.isFinite(deadline)) {
      return "Chưa có thông tin hạn duyệt.";
    }

    if (now >= deadline) {
      return "Đơn đã hết hạn duyệt.";
    }
  }

  if (action === "arrive") {
    if (!Number.isFinite(Date.parse(reservation.startTime))) {
      return "Ngày hẹn không hợp lệ.";
    }

    // Dùng ngày tại nhà hàng, không dùng múi giờ của trình duyệt.
    if (dateInputValue(now) !== dateInputValue(reservation.startTime)) {
      return "Chỉ ghi nhận khách đến trong ngày hẹn.";
    }
  }

  if (action === "no-show") {
    const allowedAt = Date.parse(reservation.noShowAllowedAt ?? "");

    if (!Number.isFinite(allowedAt)) {
      return "Chưa có thời điểm cho phép ghi nhận không đến.";
    }

    if (now < allowedAt) {
      return `Có thể thực hiện từ ${
        detailDateTime(reservation.noShowAllowedAt!)
      }.`;
    }
  }

  return "";
}

function dialogDescription(
  action: DialogAction,
  reservation: StaffReservationDetail,
  now: number,
): string {
  const customer = reservation.customerName?.trim() || "Khách hàng";
  const table = reservation.table?.code || "đã được xếp";

  switch (action) {
    case "approve":
      return (
        `Xác nhận giữ ${table} cho ${reservation.partySize} khách vào ` +
        `${detailTime(reservation.startTime)}–${detailTime(reservation.endTime)} ` +
        `ngày ${detailDate(reservation.startTime)}.`
      );

    case "reject":
      return (
        `${reservation.code} • ${customer} • ${reservation.partySize} khách. ` +
        "Bàn sẽ được giải phóng sau khi từ chối."
      );

    case "cancel":
      return (
        `${reservation.code} • ${customer}. ` +
        "Nhập lý do để lưu vào nhật ký" +
        (reservation.source === "ONLINE"
          ? " và thông báo cho khách."
          : ".")
      );

    case "no-show":
      return (
        `Đơn hẹn ${detailDateTime(reservation.startTime)}, ` +
        `hiện tại ${detailDateTime(new Date(now).toISOString())}. ` +
        (reservation.noShowAllowedAt
          ? `Có thể ghi nhận không đến từ ${
              detailDateTime(reservation.noShowAllowedAt)
            }. `
          : "") +
        "Bàn sẽ được giải phóng ngay; trạng thái này không thể hoàn tác."
      );

    case "complete":
      return (
        `Khách đã dùng bữa xong. Bàn ${table} vẫn được giữ đến ` +
        `${detailDateTime(cleanupEndsAt(reservation))}, ` +
        `gồm thời gian đã cam kết và ${reservation.bufferMinutes} phút dọn bàn. ` +
        "Trạng thái này không thể hoàn tác."
      );
  }
}

export function useStaffReservationStatusActions(
  reservation: StaffReservationDetail,
  onUpdated: (updated: StaffReservationDetail) => void,
) {
  const [dialog, setDialog] = useState<DialogAction | null>(null);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiErrorResponse | null>(null);
  const [pending, setPending] = useState<StaffReservationAction | null>(null);
  const [now, setNow] = useState(() => Date.now());

  // Ref chặn cả hai lần click xảy ra trước khi React render lại.
  const locked = useRef(false);
  const mounted = useRef(false);

  useEffect(() => {
    mounted.current = true;

    // Tự cập nhật điều kiện thời gian, không gọi API mỗi giây.
    const timer = window.setInterval(() => setNow(Date.now()), 1000);

    return () => {
      mounted.current = false;
      window.clearInterval(timer);
    };
  }, []);

  function disabledReason(action: StaffReservationAction) {
    return pending
      ? "Đang xử lý thao tác trước."
      : blockedReason(action, reservation, now);
  }

  function openDialog(action: DialogAction) {
    if (locked.current) return;

    const blocked = blockedReason(action, reservation, Date.now());

    if (blocked) {
      setError({
        status: 409,
        code: "ACTION_NOT_ALLOWED",
        message: blocked,
      });
      return;
    }

    setReason("");
    setError(null);
    setDialog(action);
  }

  function closeDialog() {
    if (locked.current) return;

    setDialog(null);
    setError(null);
  }

  async function execute(
    action: StaffReservationAction,
    enteredReason = "",
  ) {
    if (locked.current) return;

    // Kiểm tra lại ngay lúc gửi vì dialog có thể mở khá lâu.
    const blocked = blockedReason(action, reservation, Date.now());

    if (blocked) {
      setError({
        status: 409,
        code: "ACTION_NOT_ALLOWED",
        message: blocked,
      });
      return;
    }

    const needsReason = action === "reject" || action === "cancel";
    const trimmedReason = enteredReason.trim();

    if (needsReason && !trimmedReason) {
      setError({
        status: 400,
        code: "REASON_REQUIRED",
        message: "Vui lòng nhập lý do trước khi tiếp tục.",
      });
      return;
    }

    const command: StaffReservationCommand =
      action === "reject" || action === "cancel"
        ? { action, reason: trimmedReason }
        : { action };

    locked.current = true;
    setPending(action);
    setError(null);

    try {
      const updated = await staffReservationApi.changeStatus(
        reservation.id,
        command,
      );

      if (!mounted.current) return;

      setDialog(null);
      setReason("");
      onUpdated(updated);
    } catch (cause: unknown) {
      if (!mounted.current) return;

      const apiError = getApiError(cause);
      setError(apiError);

      // Giữ dialog và lý do đã nhập khi request lỗi.
      //
      // 409: đơn có thể đã bị người khác cập nhật.
      // Lỗi mạng/5xx: request có thể đã tới server.
      // Chỉ đọc lại trạng thái, không tự gửi lại POST.
      if (
        apiError.status === 409 ||
        apiError.status === 0 ||
        apiError.status >= 500
      ) {
        try {
          const latest = await staffReservationApi.getDetail(
            reservation.id,
          );

          if (mounted.current) {
            onUpdated(latest);
            setError({
              ...apiError,
              message:
                apiError.message +
                " Đã tải lại thông tin mới nhất; hãy kiểm tra trước khi tiếp tục.",
            });
          }
        } catch {
          if (mounted.current) {
            setError({
              ...apiError,
              message:
                apiError.message +
                " Chưa thể tải lại thông tin đơn đặt bàn.",
            });
          }
        }
      }
    } finally {
      locked.current = false;

      if (mounted.current) {
        setPending(null);
      }
    }
  }

  const needsReason = dialog === "reject" || dialog === "cancel";
  const blocked = dialog
    ? blockedReason(dialog, reservation, now)
    : "";

  const reasonError = needsReason && error?.status === 400;

  const dialogProps: AppFeedbackModalProps | null = dialog
    ? {
        open: true,
        title: DIALOG_LABELS[dialog].title,
        description: dialogDescription(dialog, reservation, now),
        severity: needsReason ? "destructive" : "confirm",
        primaryButtonLabel: DIALOG_LABELS[dialog].primary,
        secondaryButtonLabel: "Quay lại",
        primaryDisabled: Boolean(blocked),
        loading: pending !== null,
        loadingLabel: "Đang xử lý...",
        closeOnBackdropClick: false,
        onClose: closeDialog,
        onPrimaryAction: (value) => {
          if (dialog) void execute(dialog, value);
        },
        ...(needsReason
          ? {
              showReason: true,
              reasonValue: reason,
              onReasonChange: (value: string) => {
                setReason(value);
                setError(null);
              },
              reasonRequired: true,
              reasonLabel: "Lý do",
              reasonPlaceholder:
                dialog === "reject"
                  ? "Nhập lý do từ chối..."
                  : "Nhập lý do hủy...",
              reasonHint: "Lý do được lưu vào nhật ký thay đổi.",
              showReasonError: reasonError,
              reasonError:
                error?.message || "Vui lòng nhập lý do trước khi tiếp tục.",
            }
          : { showReason: false }),
      }
    : null;

  return {
    dialog,
    dialogProps,
    pending,
    error,
    modalError: reasonError ? null : error,
    blocked,
    disabledReason,
    openDialog,
    execute,
  };
}