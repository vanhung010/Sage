import { useId } from "react";
import { Badge } from "../../ui/Badge";
import type {
  MoveTableContext,
  MoveTableOption,
} from "../../../types/staff-move-table";
import {
  isTableSelectable,
  pickerRange,
} from "./tablePickerPresentation";
import "./TablePicker.css";

const META = {
  AVAILABLE: { label: "Còn trống", variant: "success" },
  BUSY: { label: "Đang bận", variant: "warning" },
  CAPACITY_MISMATCH: {
    label: "Không phù hợp",
    variant: "neutral",
  },
  SUSPENDED: { label: "Tạm ngưng", variant: "warning" },
  INACTIVE: { label: "Ngừng dùng", variant: "neutral" },
} as const;

export interface TableOptionProps {
  table: MoveTableOption;
  reservation: MoveTableContext;
  name: string;
  selected: boolean;
  disabled?: boolean;
  onSelect: (tableId: number) => void;
}

function description(
  table: MoveTableOption,
  reservation: MoveTableContext,
  selected: boolean,
) {
  const period = table.blockingPeriod;

  switch (table.availability) {
    case "AVAILABLE":
      if (selected) return "Sẽ chuyển đơn sang bàn này";

      if (table.outsideRequestedArea && reservation.requestedArea) {
        return (
          "Khác khu vực khách yêu cầu: " +
          reservation.requestedArea.name
        );
      }

      return (
        "Phù hợp " +
        pickerRange(reservation.startTime, reservation.endTime) +
        " (+" + reservation.bufferMinutes + " phút dọn)"
      );

    case "BUSY":
      return period
        ? "Đã giữ chỗ " +
            pickerRange(
              period.startTime,
              period.endTime,
              reservation.startTime,
            )
        : table.unavailableReason || "Bàn đang có lịch trùng";

    case "SUSPENDED":
      return (
        (table.unavailableReason || "Bàn tạm ngưng") +
        (period
          ? " · " +
            pickerRange(
              period.startTime,
              period.endTime,
              reservation.startTime,
            )
          : "")
      );

    case "CAPACITY_MISMATCH":
      return (
        table.unavailableReason ||
        "Bàn nhận " +
          table.minCapacity + "–" + table.maxCapacity +
          " khách; đơn cần " + reservation.partySize + " khách"
      );

    case "INACTIVE":
      return table.unavailableReason || "Bàn đã ngừng sử dụng";
  }
}

export function TableOption({
  table,
  reservation,
  name,
  selected,
  disabled = false,
  onSelect,
}: TableOptionProps) {
  const id = useId();
  const selectable = isTableSelectable(table);

  // Bàn vừa trở thành bận sẽ không còn hiển thị là đã chọn.
  const checked = selected && selectable;
  const blocked = disabled || !selectable;

  const badge = checked
    ? { label: "Đã chọn", variant: "info" as const }
    : META[table.availability];

  return (
    <label
      className={[
        "sage-table-option",
        blocked ? "sage-table-option--disabled" : "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <input
        className="sage-table-option__radio"
        type="radio"
        name={name}
        value={table.id}
        checked={checked}
        disabled={blocked}
        aria-labelledby={id + "-code"}
        aria-describedby={id + "-capacity " + id + "-reason"}
        onChange={() => {
          if (!blocked) onSelect(table.id);
        }}
      />

      <span className="sage-table-option__card">
        <span className="sage-table-option__identity">
          <span
            id={id + "-code"}
            className="sage-table-option__code"
          >
            {table.code}
          </span>

          <Badge variant={badge.variant}>
            {badge.label}
          </Badge>
        </span>

        <span
          id={id + "-capacity"}
          className="sage-table-option__capacity"
        >
          {table.area.name} · {table.minCapacity}–{table.maxCapacity} khách
        </span>

        <span
          id={id + "-reason"}
          className="sage-table-option__reason"
        >
          {description(table, reservation, checked)}
        </span>

        {!selectable && (
          <span className="sage-table-option__unavailable">
            Không thể chọn
          </span>
        )}
      </span>
    </label>
  );
}