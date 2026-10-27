import {
  Fragment,
  useId,
  type CSSProperties, useLayoutEffect,
useRef,
useState,
} from "react";

import { Badge } from "../Badge";
import { Button } from "../Button";
import { DateInput } from "../DateInput";
import { Select } from "../Select";

import type { PublicArea } from "../../../types/area";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import type {
  ReservationTimelineData,
  TimelineReservation,
  TimelineReservationStatus,
  TimelineState,
  TimelineSuspension,
  TimelineTable,
} from "./ReservationTimeline.types";

import "./ReservationTimeline.css";

type BlockTone =
  | "pending"
  | "confirmed"
  | "arrived"
  | "completed"
  | "cleaning"
  | "suspended";

export interface ReservationTimelineProps {
  date: string;
  areaId: number | null;
  areas: readonly Pick<PublicArea, "id" | "name">[];

  data: ReservationTimelineData | null;
  state?: TimelineState;
  errorMessage?: string;

  title?: string;
  className?: string;

  // Khung giờ đang xem; mặc định giống component Figma.
  viewStart?: string;
  viewEnd?: string;

  selectedReservationId?: number | null;

  onDateChange: (date: string) => void;
  onAreaChange: (areaId: number | null) => void;
  onRefresh: () => void;
  onReservationClick: (reservation: TimelineReservation) => void;
}

export interface ReservationTimelineBlockProps {
  tone: BlockTone;
  label?: string;
  description?: string;
  accessibleLabel: string;
  selected?: boolean;
  style?: CSSProperties;
  onClick?: () => void;
}

const MINUTE_MS = 60_000;
const LABEL_WIDTH = 152;
const PIXELS_PER_MINUTE = 928 / 240;
const LANE_HEIGHT = 92;

const STATUS_PRESENTATION: Record<
  TimelineReservationStatus,
  { label: string; tone: BlockTone }
> = {
  PENDING: {
    label: "Chờ xác nhận",
    tone: "pending",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    tone: "confirmed",
  },
  ARRIVED: {
    label: "Đã đến",
    tone: "arrived",
  },
  COMPLETED: {
    label: "Hoàn thành",
    tone: "completed",
  },
};

type Entry =
  | {
      kind: "reservation";
      item: TimelineReservation;
      start: number;
      end: number;
    }
  | {
      kind: "suspension";
      item: TimelineSuspension;
      start: number;
      end: number;
    };

function clockMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function clockLabel(minutes: number): string {
  const hours = String(Math.floor(minutes / 60)).padStart(2, "0");
  const remainder = String(minutes % 60).padStart(2, "0");

  return `${hours}:${remainder}`;
}

// Cắt khoảng thời gian theo khung nhìn.
// Tất cả vị trí đều dùng cùng một tỉ lệ với trục giờ và đường lưới.
function intervalStyle(
  start: number,
  end: number,
  windowStart: number,
  windowEnd: number,
): CSSProperties | null {
  const visibleStart = Math.max(start, windowStart);
  const visibleEnd = Math.min(end, windowEnd);

  if (
    !Number.isFinite(visibleStart) ||
    !Number.isFinite(visibleEnd) ||
    visibleEnd <= visibleStart
  ) {
    return null;
  }

  const duration = windowEnd - windowStart;

  return {
    left: `${((visibleStart - windowStart) / duration) * 100}%`,
    width: `${((visibleEnd - visibleStart) / duration) * 100}%`,
  };
}

// Xếp các khoảng chồng nhau thành hàng con.
// Reservation và phần dọn bàn dùng chung một hàng.
function layoutEntries(
  table: TimelineTable,
  windowStart: number,
  windowEnd: number,
) {
  const candidates: Entry[] = [
    ...table.reservations.map(
      (item): Entry => ({
        kind: "reservation",
        item,
        start: Date.parse(item.startTime),
        end: Date.parse(item.occupiedUntil),
      }),
    ),

    ...table.suspensions.map(
      (item): Entry => ({
        kind: "suspension",
        item,
        start: Date.parse(item.startTime),
        end: Date.parse(item.endTime),
      }),
    ),
  ];

  const visible = candidates
    .filter(
      (entry) =>
        Number.isFinite(entry.start) &&
        Number.isFinite(entry.end) &&
        entry.end > entry.start &&
        entry.start < windowEnd &&
        entry.end > windowStart,
    )
    .sort(
      (a, b) =>
        a.start - b.start ||
        a.end - b.end ||
        a.item.id - b.item.id,
    );

  const laneEnds: number[] = [];

  const entries = visible.map((entry) => {
    const start = Math.max(entry.start, windowStart);
    const end = Math.min(entry.end, windowEnd);

    let lane = laneEnds.findIndex((laneEnd) => laneEnd <= start);

    if (lane === -1) {
      lane = laneEnds.length;
    }

    laneEnds[lane] = end;

    return { entry, lane };
  });

  return {
    entries,
    laneCount: Math.max(1, laneEnds.length),
  };
}

export function ReservationTimelineBlock({
  tone,
  label,
  description,
  accessibleLabel,
  selected = false,
  style,
  onClick,
}: ReservationTimelineBlockProps) {
  const className = [
    "reservation-timeline__block",
    `reservation-timeline__block--${tone}`,
    selected ? "reservation-timeline__block--selected" : "",
  ]
    .filter(Boolean)
    .join(" ");

  const content = tone !== "cleaning" && (
    <span className="reservation-timeline__block-content">
      <span className="reservation-timeline__block-label">
        {label}
      </span>

      <span className="reservation-timeline__block-description">
        {description}
      </span>
    </span>
  );

  if (onClick) {
    return (
      <button
        type="button"
        className={className}
        style={style}
        title={accessibleLabel}
        aria-label={accessibleLabel}
        aria-pressed={selected}
        onClick={onClick}
      >
        {content}
      </button>
    );
  }

  return (
    <div
      className={className}
      style={style}
      role="img"
      title={accessibleLabel}
      aria-label={accessibleLabel}
    >
      {content}
    </div>
  );
}

interface TimelineRowProps {
  table: TimelineTable;
  startMinute: number;
  endMinute: number;
  gridMinutes: number[];
  windowStart: number;
  windowEnd: number;
  closed: boolean;
  selectedReservationId: number | null;
  onReservationClick: (reservation: TimelineReservation) => void;
}

function TimelineRow({
  table,
  startMinute,
  endMinute,
  gridMinutes,
  windowStart,
  windowEnd,
  closed,
  selectedReservationId,
  onReservationClick,
}: TimelineRowProps) {
  const { entries, laneCount } = layoutEntries(
    table,
    windowStart,
    windowEnd,
  );

  const emptyMessage = !table.active
    ? "Bàn ngừng hoạt động"
    : !table.area.active
      ? "Khu vực ngừng hoạt động"
      : closed
        ? "Nhà hàng đóng cửa"
        : "Còn trống trong khung giờ đang xem";

  return (
    <div className="reservation-timeline__row">
      <div className="reservation-timeline__table-label">
        <span className="reservation-timeline__table-code">
          {table.code}
        </span>

        <span className="reservation-timeline__table-meta">
          {table.area.name} • {table.minCapacity}–
          {table.maxCapacity} khách
        </span>

        {(!table.active || !table.area.active) && (
          <span className="reservation-timeline__table-meta">
            Ngừng hoạt động
          </span>
        )}
      </div>

      <div
        className="reservation-timeline__track"
        style={{ height: laneCount * LANE_HEIGHT }}
      >
        <div
          className="reservation-timeline__grid-lines"
          aria-hidden="true"
        >
          {gridMinutes.map((minute) => (
            <span
              key={minute}
              className={[
                "reservation-timeline__grid-line",
                minute % 60 === 0
                  ? "reservation-timeline__grid-line--hour"
                  : "",
              ]
                .filter(Boolean)
                .join(" ")}
              style={{
                left: `${
                  ((minute - startMinute) /
                    (endMinute - startMinute)) *
                  100
                }%`,
              }}
            />
          ))}
        </div>

        {entries.length === 0 && (
          <p className="reservation-timeline__available">
            {emptyMessage}
          </p>
        )}

        {entries.map(({ entry, lane }) => {
          const top = lane * LANE_HEIGHT + 12;

          if (entry.kind === "suspension") {
            const suspension = entry.item;

            const style = intervalStyle(
              entry.start,
              entry.end,
              windowStart,
              windowEnd,
            );

            if (!style) return null;

            const label = suspension.reason
              ? `Tạm ngưng • ${suspension.reason}`
              : "Tạm ngưng";

            const description =
              `${displayTime(suspension.startTime)}–` +
              displayTime(suspension.endTime);

            return (
              <ReservationTimelineBlock
                key={`suspension-${suspension.id}`}
                tone="suspended"
                label={label}
                description={description}
                accessibleLabel={
                  `${table.code} • ${label} • ${description}`
                }
                style={{ ...style, top }}
              />
            );
          }

          const reservation = entry.item;
          const presentation = STATUS_PRESENTATION[reservation.status];

          const label =
                        `${reservation.code} • ` +
                        `${reservation.customerName ?? "Khách"}`;

                        const description =
                        `${displayTime(reservation.startTime)}–` +
                        `${displayTime(reservation.endTime)} • ` +
                        `${reservation.partySize} khách • ` +
                        presentation.label;

          const reservationStyle = intervalStyle(
            Date.parse(reservation.startTime),
            Date.parse(reservation.endTime),
            windowStart,
            windowEnd,
          );

          const cleaningStyle = intervalStyle(
            Date.parse(reservation.endTime),
            Date.parse(reservation.occupiedUntil),
            windowStart,
            windowEnd,
          );

          return (
            <Fragment key={`reservation-${reservation.id}`}>
              {reservationStyle && (
                <ReservationTimelineBlock
                  tone={presentation.tone}
                  label={label}
                  description={description}
                 accessibleLabel={
                    `${table.code} • ${label} • ${description}`
                    }
                  selected={
                    selectedReservationId === reservation.id
                  }
                  style={{ ...reservationStyle, top }}
                  onClick={() =>
                    onReservationClick(reservation)
                  }
                />
              )}

              {cleaningStyle && (
                <ReservationTimelineBlock
                  tone="cleaning"
                  accessibleLabel={
                    `${table.code} • Dọn bàn sau đơn ` +
                    `${reservation.code} • ` +
                    `${displayTime(reservation.endTime)}–` +
                    displayTime(reservation.occupiedUntil)
                  }
                  style={{ ...cleaningStyle, top }}
                />
              )}
            </Fragment>
          );
        })}
      </div>
    </div>
  );
}

export function ReservationTimeline({
  date,
  areaId,
  areas,
  data,
  state = "default",
  errorMessage,
  title = "Lịch đặt bàn",
  className = "",
  viewStart = "17:00",
  viewEnd = "21:00",
  selectedReservationId = null,
  onDateChange,
  onAreaChange,
  onRefresh,
  onReservationClick,
}: ReservationTimelineProps) {
  const headingId = useId();

  // Không hiển thị dữ liệu của ngày cũ dưới tiêu đề ngày mới.
  const currentData = data?.date === date ? data : null;

  const tables = (currentData?.tables ?? []).filter(
    (table) => areaId === null || table.area.id === areaId,
  );

  const resolvedState: TimelineState =
    state !== "default"
      ? state
      : !currentData
        ? "loading"
        : tables.length === 0
          ? "empty"
          : "default";


  const topScrollRef = useRef<HTMLDivElement>(null);
const gridScrollRef = useRef<HTMLDivElement>(null);

const [scrollWidth, setScrollWidth] = useState(0);

// Đo lại khi tải dữ liệu, đổi khung giờ hoặc đổi kích thước màn hình.
useLayoutEffect(() => {
  const viewport = gridScrollRef.current;
  const canvas = viewport?.querySelector(
    ".reservation-timeline__canvas",
  );

  if (!viewport || !canvas) return;

  function updateWidth() {
    if (viewport) {
      setScrollWidth(viewport.scrollWidth);
    }
  }

  updateWidth();

  const observer = new ResizeObserver(updateWidth);

  observer.observe(viewport);
  observer.observe(canvas);

  return () => observer.disconnect();
}, [resolvedState]);

// Giữ vị trí thanh trên khớp với lưới sau khi chiều rộng thay đổi.
useLayoutEffect(() => {
  if (topScrollRef.current && gridScrollRef.current) {
    topScrollRef.current.scrollLeft =
      gridScrollRef.current.scrollLeft;
  }
}, [scrollWidth, resolvedState]);

  const startMinute = clockMinutes(viewStart);
  const endMinute = clockMinutes(viewEnd);

  if (
    !Number.isFinite(startMinute) ||
    !Number.isFinite(endMinute) ||
    startMinute < 0 ||
    endMinute > 1440 ||
    endMinute <= startMinute
  ) {
    throw new Error(
      "Khung giờ timeline phải nằm trong ngày và giờ kết thúc phải sau giờ bắt đầu.",
    );
  }

  const duration = endMinute - startMinute;
  const step = currentData?.gridStepMinutes ?? 15;

  if (!Number.isFinite(step) || step <= 0) {
    throw new Error("Bước thời gian timeline phải lớn hơn 0.");
  }

  const gridMinutes = Array.from(
    { length: Math.floor(duration / step) + 1 },
    (_, index) => startMinute + index * step,
  );

  if (gridMinutes[gridMinutes.length - 1] !== endMinute) {
    gridMinutes.push(endMinute);
  }

  const axisMinutes = gridMinutes.filter(
    (minute) =>
      minute === startMinute ||
      minute === endMinute ||
      minute % 60 === 0,
  );

  // Sage dùng giờ Việt Nam UTC+7; không phụ thuộc timezone máy khách.
  const dayStart = Date.parse(`${date}T00:00:00+07:00`);

  const windowStart = dayStart + startMinute * MINUTE_MS;
  const windowEnd = dayStart + endMinute * MINUTE_MS;

  const areaOptions = [
    { value: "", label: "Tất cả khu vực" },
    ...areas.map((area) => ({
      value: String(area.id),
      label: area.name,
    })),
  ];

  const closed = currentData?.openingHours.closed ?? false;

  const canvasStyle = {
    "--timeline-min-width":
      `${LABEL_WIDTH + duration * PIXELS_PER_MINUTE}px`,
  } as CSSProperties;

  return (
    <section
      className={`reservation-timeline ${className}`.trim()}
      aria-labelledby={headingId}
      aria-busy={resolvedState === "loading"}
    >
      <h2 id={headingId} className="reservation-timeline__title">
        {title}
      </h2>

      <p className="reservation-timeline__description">
        {displayDate(date)} • Giờ Việt Nam (UTC+7)
      </p>

      <div className="reservation-timeline__toolbar">
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            const today = dateInputValue();

            if (today === date) {
              onRefresh();
            } else {
              onDateChange(today);
            }
          }}
        >
          Hôm nay
        </Button>

        <div className="reservation-timeline__date">
          <DateInput
            value={date}
            placeholder="Chọn ngày xem lịch"
            onChange={(value) => {
              if (value) onDateChange(value);
            }}
          />
        </div>

        <div
          className={[
            "reservation-timeline__area",
            areaId === null
              ? "reservation-timeline__area--all"
              : "",
          ]
            .filter(Boolean)
            .join(" ")}
        >
          <Select
            aria-label="Lọc theo khu vực"
            value={areaId === null ? "" : String(areaId)}
            options={areaOptions}
            onValueChange={(value) =>
              onAreaChange(value === "" ? null : Number(value))
            }
          />
        </div>

        <Button
          type="button"
          variant="ghost"
          disabled={resolvedState === "loading"}
          onClick={onRefresh}
        >
          Làm mới
        </Button>
      </div>

      {resolvedState === "loading" && (
        <div className="reservation-timeline__state" role="status">
          <p className="reservation-timeline__state-title">
            Đang tải…
          </p>

          <p>Vui lòng chờ trong giây lát.</p>

          <div
            className="reservation-timeline__skeletons"
            aria-hidden="true"
          >
            <span />
            <span />
            <span />
          </div>
        </div>
      )}

      {resolvedState === "empty" && (
        <div className="reservation-timeline__state" role="status">
          <p className="reservation-timeline__state-title">
            {closed ? "Nhà hàng đóng cửa" : "Chưa có đơn đặt bàn"}
          </p>

          <p>
            {closed
              ? currentData?.openingHours.closedReason ??
                "Nhà hàng nghỉ vào ngày đã chọn."
              : "Dữ liệu sẽ xuất hiện tại đây khi có phát sinh."}
          </p>
        </div>
      )}

      {resolvedState === "error" && (
        <div className="reservation-timeline__state" role="alert">
          <p className="reservation-timeline__state-title">
            Không tải được đơn đặt bàn
          </p>

          <p>
            {errorMessage ??
              "Kiểm tra kết nối rồi thử lại. Dữ liệu đã nhập được giữ nguyên."}
          </p>

          <Button
            type="button"
            variant="outline"
            onClick={onRefresh}
          >
            Thử lại
          </Button>
        </div>
      )}

      {resolvedState === "default" && (
        <>
          {closed && (
            <div
              className="reservation-timeline__closed"
              role="status"
            >
              <strong>Nhà hàng đóng cửa.</strong>{" "}
              {currentData?.openingHours.closedReason}
            </div>
          )}

          <div
            className="reservation-timeline__legend"
            aria-label="Chú thích trạng thái"
          >
            <Badge variant="warning">Chờ xác nhận</Badge>
            <Badge variant="success">Đã xác nhận</Badge>
            <Badge variant="info">Đã đến</Badge>
            <Badge variant="neutral">Dọn bàn</Badge>
            <Badge variant="warning">Tạm ngưng</Badge>
          </div>
            <div
  ref={topScrollRef}
  className="reservation-timeline__top-scroll"
  role="region"
  aria-label="Thanh cuộn ngang lịch đặt bàn"
  tabIndex={0}
  onScroll={(event) => {
    const grid = gridScrollRef.current;
    const position = event.currentTarget.scrollLeft;

    if (grid && grid.scrollLeft !== position) {
      grid.scrollLeft = position;
    }
  }}
>
  <div
    className="reservation-timeline__top-scroll-content"
    style={{ width: scrollWidth }}
    aria-hidden="true"
  />
</div>
          <div
  ref={gridScrollRef}
  className="reservation-timeline__scroll"
  role="region"
  aria-label="Lịch theo bàn, có thể cuộn ngang"
  tabIndex={0}
  onScroll={(event) => {
    const top = topScrollRef.current;
    const position = event.currentTarget.scrollLeft;

    if (top && top.scrollLeft !== position) {
      top.scrollLeft = position;
    }
  }}
>
            <div
              className="reservation-timeline__canvas"
              style={canvasStyle}
            >
              <div className="reservation-timeline__axis">
                <div className="reservation-timeline__corner">
                  Bàn / Khu vực
                </div>

                <div className="reservation-timeline__axis-track">
                  {axisMinutes.map((minute) => (
                    <span
                      key={minute}
                      className="reservation-timeline__axis-label"
                      data-edge={
                        minute === startMinute
                          ? "start"
                          : minute === endMinute
                            ? "end"
                            : undefined
                      }
                      style={{
                        left:
                          `${((minute - startMinute) / duration) * 100}%`,
                      }}
                    >
                      {clockLabel(minute)}
                    </span>
                  ))}
                </div>
              </div>

              {tables.map((table) => (
                <TimelineRow
                  key={table.id}
                  table={table}
                  startMinute={startMinute}
                  endMinute={endMinute}
                  gridMinutes={gridMinutes}
                  windowStart={windowStart}
                  windowEnd={windowEnd}
                  closed={closed}
                  selectedReservationId={selectedReservationId}
                  onReservationClick={onReservationClick}
                />
              ))}
            </div>
          </div>

          <p className="reservation-timeline__footnote">
            Mỗi vạch nhỏ = {step} phút • Viền nổi = đơn đang chọn
            {" • "}Khoảng xám sau đơn = dọn bàn
          </p>
        </>
      )}
    </section>
  );
}