import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { Button } from "../../../components/ui/Button";

import {
  ReservationTimeline,
  type ReservationTimelineData,
  type TimelineReservation,
} from "../../../components/ui/ReservationTimeline";

import { dateInputValue } from "../../../utils/reservation";

import { useStaffReservationTimeline } from "./useStaffReservationTimeline";

import "./StaffReservationTimelinePage.css";



// function formatClock(minutes: number): string {
//   const hours = String(Math.floor(minutes / 60)).padStart(2, "0");
//   const remainder = String(minutes % 60).padStart(2, "0");

//   return `${hours}:${remainder}`;
// }

// function getViewWindow(data: ReservationTimelineData | null) {
//   let startMinute = 17 * 60;
//   let endMinute = 21 * 60;

//   if (!data) {
//     return {
//       viewStart: "17:00",
//       viewEnd: "21:00",
//     };
//   }

//   const dayStart = Date.parse(`${data.date}T00:00:00+07:00`);
//   const dayEnd = dayStart + MINUTES_PER_DAY * MINUTE_MS;
//   const step = data.gridStepMinutes;

//   const intervals = data.tables.flatMap((table) => [
//     ...table.reservations.map(
//       (reservation): [number, number] => [
//         Date.parse(reservation.startTime),
//         Date.parse(reservation.occupiedUntil),
//       ],
//     ),

//     ...table.suspensions.map(
//       (suspension): [number, number] => [
//         Date.parse(suspension.startTime),
//         Date.parse(suspension.endTime),
//       ],
//     ),
//   ]);

//   for (const [start, end] of intervals) {
//     if (
//       !Number.isFinite(start) ||
//       !Number.isFinite(end) ||
//       end <= start ||
//       end <= dayStart ||
//       start >= dayEnd
//     ) {
//       continue;
//     }

//     const visibleStart =
//       (Math.max(start, dayStart) - dayStart) / MINUTE_MS;

//     const visibleEnd =
//       (Math.min(end, dayEnd) - dayStart) / MINUTE_MS;

//     startMinute = Math.min(
//       startMinute,
//       Math.floor(visibleStart / step) * step,
//     );

//     endMinute = Math.max(
//       endMinute,
//       Math.ceil(visibleEnd / step) * step,
//     );
//   }

//   return {
//     viewStart: formatClock(Math.max(0, startMinute)),
//     viewEnd: formatClock(Math.min(MINUTES_PER_DAY, endMinute)),
//   };
// }

function getViewWindow(
  data: ReservationTimelineData | null,
) {
  const openingHours = data?.openingHours;

  return {
    // API có thể trả "10:00:00"; component nhận "10:00".
    viewStart: openingHours?.openTime?.slice(0, 5) ?? "00:00",
    viewEnd: openingHours?.closeTime?.slice(0, 5) ?? "24:00",
  };
}

export function StaffReservationTimelinePage() {
  const navigate = useNavigate();

  const [date, setDate] = useState(() => dateInputValue());
  const [areaId, setAreaId] = useState<number | null>(null);

  const [selectedReservationId, setSelectedReservationId] =
    useState<number | null>(null);

  const {
    data,
    error,
    loading,
    areas,
    areasLoading,
    areasError,
    refresh,
    retryAreas,
  } = useStaffReservationTimeline(date, areaId);

  const { viewStart, viewEnd } = getViewWindow(data);

  function handleDateChange(nextDate: string) {
    setSelectedReservationId(null);
    setDate(nextDate);
  }

  function handleAreaChange(nextAreaId: number | null) {
    setSelectedReservationId(null);
    setAreaId(nextAreaId);
  }

  function handleReservationClick(
    reservation: TimelineReservation,
  ) {
    setSelectedReservationId(reservation.id);

    void navigate(
      `/staff/reservations/${reservation.id}`,
    );
  }

  return (
    <section
      className="staff-reservation-timeline"
      aria-labelledby="staff-reservation-timeline-title"
    >
      <header className="staff-reservation-timeline__header">
        <div>
          <h1 id="staff-reservation-timeline-title">
            Quản lý đặt bàn
          </h1>

          <p className="staff-reservation-timeline__description">
            Theo dõi bàn và khoảng thời gian đã được giữ.
          </p>
        </div>

        <Button
          type="button"
          onClick={() =>
            void navigate("/staff/reservations/guest")
          }
        >
          Đặt bàn hộ
        </Button>
      </header>

      <div
        className="staff-reservation-timeline__views"
        role="group"
        aria-label="Chế độ xem đặt bàn"
      >
        <Button
          type="button"
          variant="ghost"
          aria-pressed={false}
          onClick={() =>
            void navigate("/staff/reservations")
          }
        >
          Danh sách
        </Button>

        <Button
          type="button"
          variant="primary"
          aria-pressed={true}
        >
          Lịch theo bàn
        </Button>
      </div>

      {areasError && (
        <aside
          className="staff-reservation-timeline__notice"
          role="alert"
        >
          <div>
            <strong>Không tải được danh sách khu vực.</strong>

            <p>
              {areasError} Lịch đặt bàn vẫn được tải độc lập.
            </p>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            loading={areasLoading}
            onClick={retryAreas}
          >
            Tải lại khu vực
          </Button>
        </aside>
      )}

      <ReservationTimeline
        title="Lịch theo bàn"
        date={date}
        areaId={areaId}
        areas={areas}
        data={data}
        state={
          loading
            ? "loading"
            : error
              ? "error"
              : "default"
        }
        errorMessage={error ?? undefined}
        selectedReservationId={selectedReservationId}
        onDateChange={handleDateChange}
        onAreaChange={handleAreaChange}
        onRefresh={refresh}
        onReservationClick={handleReservationClick}
        viewStart={viewStart}
        viewEnd={viewEnd}
      />
    </section>
  );
}