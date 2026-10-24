import {
  useEffect,
  useState,
} from "react";

import { Button } from "../../../components/ui/Button";

import type {
  DataTableRowKey,
} from "../../../components/ui/DataTable";

import type {
  StaffReservationFilterForm,
  StaffReservationFilterKey,
} from "../../../types/staff-reservation";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import { ReservationFilters } from "./ReservationFilters";
import { ReservationsTable } from "./ReservationsTable";
import { useStaffReservations } from "./useStaffReservations";

import { useLocation, useNavigate } from "react-router-dom";

import { AppFeedbackModal } from
  "../../../components/ui/AppFeedbackModal";

import type {
  CreateGuestReservationResponse,
  StaffReservationNavigationState,
} from "../../../types/staff-reservation";

import "./StaffReservationsPage.css";

function defaultFilters(): StaffReservationFilterForm {
  return {
    date: dateInputValue(),
    search: "",
    status: "",
    areaId: "",
  };
}

export function StaffReservationsPage() {
 
   const navigate = useNavigate();
  const location = useLocation();

  const [createdReservation, setCreatedReservation] =
    useState<CreateGuestReservationResponse | null>(() => {
      const state =
        location.state as StaffReservationNavigationState | null;

      return state?.guestCreated ?? null;
    });

  const [filters, setFilters] =
    useState<StaffReservationFilterForm>(() => ({
      ...defaultFilters(),

      // Mở đúng ngày của đơn vừa tạo.
      date: createdReservation
        ? dateInputValue(createdReservation.startTime)
        : dateInputValue(),
    }));


  const [draft, setDraft] =
    useState<StaffReservationFilterForm>(
      () => ({ ...filters }),
    );

  const [page, setPage] = useState(0);

  const [selectedKeys, setSelectedKeys] =
    useState<Set<DataTableRowKey>>(() => new Set());

  const {
    list,
    summary,
    areas,
    pageSize,
  } = useStaffReservations(filters, page);

  // Dữ liệu thay đổi thì bỏ lựa chọn của danh sách trước.
  useEffect(() => {
    setSelectedKeys(new Set());
  }, [list.data]);

  // Nếu số trang giảm, chuyển về trang cuối còn tồn tại.
  useEffect(() => {
    if (!list.data) return;

    const lastPage = Math.max(0, list.data.totalPages - 1);

    if (page > lastPage) {
      setPage(lastPage);
    }
  }, [list.data, page]);

  function commitFilters(next: StaffReservationFilterForm) {
    setPage(0);
    setSelectedKeys(new Set());
    setFilters(next);
  }

  function applyFilters() {
    if (!draft.date) return;

    const next = {
      ...draft,
      search: draft.search.trim(),
    };

    setDraft(next);
    commitFilters(next);
  }

  function clearFilters() {
    const next = defaultFilters();

    setDraft(next);
    commitFilters(next);
  }

  function removeFilter(key: StaffReservationFilterKey) {
    const replacement = key === "date" ? dateInputValue() : "";

    // Bỏ đúng trường trong draft, giữ các thay đổi chưa áp dụng khác.
    setDraft((current) => ({
      ...current,
      [key]: replacement,
    }));

    commitFilters({
      ...filters,
      [key]: replacement,
    });
  }

  function changePage(nextPage: number) {
    setSelectedKeys(new Set());
    setPage(nextPage);
  }

  const busy = list.loading || summary.loading;
  const pendingCount = summary.data?.pendingCount;

  useEffect(() => {
  const state =
    location.state as StaffReservationNavigationState | null;

  if (!state?.guestCreated) return;

  // Đã đưa kết quả vào state của trang.
  // Xóa navigation state để refresh không mở lại thông báo cũ.
        void navigate(
          {
            pathname: location.pathname,
            search: location.search,
            hash: location.hash,
          },
          {
            replace: true,
            state: null,
          },
        );
      }, [
        location.pathname,
        location.search,
        location.hash,
        location.state,
        navigate,
      ]);


  return (
    <section
      className="staff-reservations"
      aria-labelledby="staff-reservations-title"
    >
      <div className="staff-reservations__header">
        <div>
          <h1 id="staff-reservations-title">
            Quản lý đặt bàn
          </h1>

          <p className="staff-reservations__description">
            Tra cứu và xử lý các đơn đặt bàn trong ngày.
          </p>
        </div>

       <Button
        type="button"
        size="md"
        onClick={() => void navigate("/staff/reservations/guest")}
      >
        Đặt bàn hộ
      </Button>
      </div>

      <div
        className="staff-reservations__views"
        role="group"
        aria-label="Chế độ xem đặt bàn"
      >
        <Button
          type="button"
          variant="primary"
          size="md"
          aria-pressed="true"
        >
          Danh sách
        </Button>

        <Button
          type="button"
          variant="ghost"
          size="md"
        
          title="Lịch theo bàn chưa được triển khai"
        >
          Lịch theo bàn
        </Button>
      </div>

      {summary.loading ? (
        <div
          className="staff-reservations__notice"
          role="status"
        >
          Đang tải số đơn chờ duyệt…
        </div>
      ) : summary.error ? (
        <div
          className="staff-reservations__notice staff-reservations__notice--error"
          role="alert"
        >
          <div>
            <strong>Không thể tải số đơn chờ duyệt.</strong>
            <p>{summary.error}</p>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={summary.retry}
          >
            Thử lại
          </Button>
        </div>
      ) : pendingCount != null && pendingCount > 0 ? (
        <aside
          className="staff-reservations__notice staff-reservations__notice--warning"
          aria-label="Đơn chờ duyệt"
        >
          <div>
            <strong>
              {pendingCount} đơn đang chờ duyệt trong kết quả đang xem
            </strong>

            <p>
              Ưu tiên xử lý trước hạn duyệt để bảo đảm bàn cho khách.
            </p>
          </div>
        </aside>
      ) : null}

      {areas.error && (
        <div
          className="staff-reservations__notice staff-reservations__notice--error"
          role="alert"
        >
          <div>
            <strong>Không thể tải lựa chọn khu vực.</strong>
            <p>Bạn vẫn có thể tìm kiếm và lọc theo ngày, trạng thái.</p>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={areas.retry}
          >
            Thử lại
          </Button>
        </div>
      )}

      <ReservationFilters
        draft={draft}
        applied={filters}
        areas={areas.data ?? []}
        areasDisabled={areas.loading || !!areas.error}
        loading={busy}
        onChange={setDraft}
        onApply={applyFilters}
        onClear={clearFilters}
        onRemove={removeFilter}
      />

      <ReservationsTable
        data={list.data?.content ?? []}
        loading={list.loading}
        error={list.error}
        onRetry={list.retry}
        page={page}
        pageSize={pageSize}
        totalElements={list.data?.totalElements ?? 0}
        onPageChange={changePage}
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
      />

      <p
        className="staff-reservations__footnote"
        role="status"
      >
        {selectedKeys.size > 0
          ? `Đã chọn ${selectedKeys.size} đơn trên trang hiện tại.`
          : "Danh sách được sắp xếp theo thời gian khách đến tăng dần."}
      </p>
      <AppFeedbackModal
        open={createdReservation !== null}
        severity="success"
        title="Đặt bàn hộ thành công"
        description={
          createdReservation
            ? [
                `Mã ${createdReservation.reservationCode}`,
                createdReservation.name,
                createdReservation.tableCode,
                `${createdReservation.partySize} khách`,
                `${displayDate(
                  dateInputValue(createdReservation.startTime),
                )} · ${displayTime(
                  createdReservation.startTime,
                )}–${displayTime(createdReservation.endTime)}`,
              ].join(" • ") + ". Đọc lại mã và lịch hẹn cho khách."
            : undefined
        }
        primaryButtonLabel="Về danh sách"
        showSecondaryButton={false}
        onClose={() => setCreatedReservation(null)}
        onPrimaryAction={() => setCreatedReservation(null)}
      />
    </section>
  );
}