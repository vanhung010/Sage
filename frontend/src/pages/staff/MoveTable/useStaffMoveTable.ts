import { useEffect, useRef, useState } from "react";

import { staffReservationApi } from "../../../api/staff-reservation.api";
import { getApiError } from "../../../api/error";
import { isTableSelectable } from "../../../components/common/TablePicker";

import type { ApiErrorResponse } from "../../../types/api";
import type {
  MoveTableOptionsResponse,
} from "../../../types/staff-move-table";

interface Snapshot {
  key: string;
  data: MoveTableOptionsResponse | null;
  error: ApiErrorResponse | null;
}

function readableError(cause: unknown): ApiErrorResponse {
  const error = getApiError(cause);

  return {
    status: typeof error?.status === "number" ? error.status : 0,
    code:
      typeof error?.code === "string"
        ? error.code
        : "UNKNOWN_ERROR",
    message:
      typeof error?.message === "string"
        ? error.message
        : "Không thể hoàn tất yêu cầu. Vui lòng thử lại.",
  };
}

export function useStaffMoveTable(reservationId: number) {
  const [areaId, setAreaId] = useState<number | null>(null);
  const [selectedTableId, setSelectedTableId] =
    useState<number | null>(null);
  const [reason, setReason] = useState("");
  const [attempt, setAttempt] = useState(0);

  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] =
    useState<ApiErrorResponse | null>(null);
  const [needsReview, setNeedsReview] = useState(false);

  const mounted = useRef(false);
  const saveLock = useRef(false);

  const key = `${reservationId}:${areaId ?? "all"}:${attempt}`;

  useEffect(() => {
    mounted.current = true;

    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    void staffReservationApi.getMoveTableOptions(
      reservationId,
      areaId,
      controller.signal,
    ).then(
      (data) => {
        if (controller.signal.aborted) return;

        setSnapshot({ key, data, error: null });
        setNeedsReview(false);
        setSaveError(null);
      },
      (cause: unknown) => {
        if (controller.signal.aborted) return;

        setSnapshot((previous) => ({
          key,
          data: previous?.data ?? null,
          error: readableError(cause),
        }));
      },
    );

    return () => controller.abort();
  }, [reservationId, areaId, key]);

  // Dữ liệu khu vực trước không được dùng để xác nhận khu vực mới.
  const current = snapshot?.key === key ? snapshot : null;
  const options = snapshot?.data ?? null;
  const loading = current === null;
  const loadError = current?.error ?? null;

  const ready = Boolean(
    current?.data &&
      !current.error &&
      current.data.selectedAreaId === areaId,
  );

  const reservation = options?.reservation;
  const selectedTable = options?.tables.find(
    (table) => table.id === selectedTableId,
  );

  const allowed = Boolean(
    reservation &&
      ["PENDING", "CONFIRMED", "ARRIVED"].includes(
        reservation.status,
      ),
  );

  const canSubmit = Boolean(
    ready &&
      allowed &&
      selectedTable &&
      isTableSelectable(selectedTable) &&
      selectedTable.id !== reservation?.currentTable.id &&
      reason.length <= 1000 &&
      !needsReview &&
      !saving,
  );

  function changeArea(next: number | null) {
    if (!saveLock.current) setAreaId(next);
  }

  function selectTable(id: number) {
    if (saveLock.current || !ready) return;

    const table = options?.tables.find((item) => item.id === id);

    if (table && isTableSelectable(table)) {
      setSelectedTableId(id);
    }
  }

  function changeReason(value: string) {
    if (!saveLock.current) setReason(value);
  }

  function reload() {
    if (!saveLock.current) {
      setAttempt((value) => value + 1);
    }
  }

  async function submit() {
    if (
      !canSubmit ||
      !reservation ||
      !selectedTable ||
      saveLock.current
    ) {
      return null;
    }

    // Chặn hai lần nhấn trước khi React kịp cập nhật saving.
    saveLock.current = true;
    setSaving(true);
    setSaveError(null);

    try {
      const detail = await staffReservationApi.moveTable(
        reservationId,
        {
          tableId: selectedTable.id,
          expectedVersion: reservation.version,
          reason: reason.trim() || null,
        },
      );

      if (!mounted.current) return null;

      return {
        detail,
        fromTableCode: reservation.currentTable.code,
      };
    } catch (cause: unknown) {
      if (mounted.current) {
        setSaveError(readableError(cause));

        // Đọc lại trạng thái server trước khi cho gửi POST tiếp.
        setNeedsReview(true);
      }

      return null;
    } finally {
      saveLock.current = false;

      if (mounted.current) setSaving(false);
    }
  }

  return {
    options,
    areaId,
    selectedTableId,
    selectedTable,
    reason,
    loading,
    loadError,
    saveError,
    saving,
    needsReview,
    canSubmit,
    changeArea,
    selectTable,
    changeReason,
    reload,
    submit,
  };
}