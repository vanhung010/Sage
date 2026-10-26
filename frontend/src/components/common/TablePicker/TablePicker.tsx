import { useId, type ReactNode } from "react";
import { Button } from "../../ui/Button";
import { Select } from "../../ui/Select";
import type {
  MoveTableArea,
  MoveTableContext,
  MoveTableOption,
} from "../../../types/staff-move-table";
import { TableOption } from "./TableOption";
import {
  isTableSelectable,
  pickerDate,
  pickerRange,
  pickerTime,
} from "./tablePickerPresentation";
import "./TablePicker.css";

export interface TablePickerProps {
  reservation?: MoveTableContext | null;
  areas?: MoveTableArea[];
  tables?: MoveTableOption[];
  selectedAreaId: number | null;
  selectedTableId: number | null;
  loading?: boolean;
  error?: string | null;
  disabled?: boolean;
  title?: string;
  description?: string;
  actions?: ReactNode;
  onAreaChange: (areaId: number | null) => void;
  onSelect: (tableId: number) => void;
  onRetry?: () => void;
}

export function TablePicker({
  reservation,
  areas = [],
  tables = [],
  selectedAreaId,
  selectedTableId,
  loading = false,
  error = null,
  disabled = false,
  title = "Chọn bàn thay thế",
  description,
  actions,
  onAreaChange,
  onSelect,
  onRetry,
}: TablePickerProps) {
  const id = useId();

  const selected = tables.find(
    (table) => table.id === selectedTableId,
  );
  const validSelection = selected && isTableSelectable(selected);
  const count = tables.filter(isTableSelectable).length;
  const ready = Boolean(reservation && !loading && !error);

  return (
    <section
      className="sage-table-picker"
      aria-labelledby={id + "-title"}
      aria-busy={loading}
    >
      <h2
        id={id + "-title"}
        className="sage-table-picker__title"
      >
        {title}
      </h2>

      {reservation && (
        <>
          <p className="sage-table-picker__description">
            {description ??
              "Đơn " + reservation.code +
                " · " + reservation.partySize + " khách"}
          </p>

          <div className="sage-table-picker__context">
            <p className="sage-table-picker__context-time">
              {pickerDate(reservation.startTime)} ·{" "}
              {pickerRange(
                reservation.startTime,
                reservation.endTime,
              )}
            </p>

            <p className="sage-table-picker__caption">
              Tính cả {reservation.bufferMinutes} phút dọn bàn đến{" "}
              {pickerDate(reservation.startTime) ===
              pickerDate(reservation.occupiedUntil)
                ? pickerTime(reservation.occupiedUntil)
                : pickerDate(reservation.occupiedUntil) +
                  " · " +
                  pickerTime(reservation.occupiedUntil)}
            </p>
          </div>
        </>
      )}

      <Select
        id={id + "-area"}
        aria-label="Lọc bàn theo khu vực"
        value={
          selectedAreaId === null ? "" : String(selectedAreaId)
        }
        placeholder="Tất cả khu vực"
        disabled={disabled || loading}
        options={[
          { value: "", label: "Tất cả khu vực" },
          ...areas.map((area) => ({
            value: String(area.id),
            label: area.name,
          })),
        ]}
        onValueChange={(value) =>
          onAreaChange(value === "" ? null : Number(value))
        }
      />

      {loading ? (
        <div className="sage-table-picker__state" role="status">
          <strong>Đang tải…</strong>
          <p>Vui lòng chờ trong giây lát.</p>

          <div
            className="sage-table-picker__skeleton"
            aria-hidden="true"
          >
            <span />
            <span />
            <span />
          </div>
        </div>
      ) : error ? (
        <div className="sage-table-picker__state" role="alert">
          <strong>Không tải được bàn phù hợp</strong>
          <p>{error}</p>
          <p>Dữ liệu đã nhập được giữ nguyên.</p>

          {onRetry && (
            <Button
              type="button"
              variant="outline"
              disabled={disabled}
              onClick={onRetry}
            >
              Thử lại
            </Button>
          )}
        </div>
      ) : !reservation ? (
        <div className="sage-table-picker__state" role="status">
          <strong>Chưa có thông tin đơn đặt bàn</strong>
          <p>Chọn đơn đặt bàn để xem các bàn thay thế.</p>
        </div>
      ) : tables.length === 0 ? (
        <div className="sage-table-picker__state" role="status">
          <strong>Chưa có bàn phù hợp</strong>
          <p>Thử chọn khu vực khác hoặc kiểm tra lại sau.</p>
        </div>
      ) : (
        <>
          <p
            id={id + "-count"}
            className="sage-table-picker__count"
            role="status"
          >
            {count} bàn phù hợp · Chọn 1 bàn
          </p>

          <fieldset
            className="sage-table-picker__grid"
            disabled={disabled}
            aria-describedby={id + "-count"}
          >
            <legend className="sage-table-picker__sr-only">
              Chọn một bàn thay thế
            </legend>

            {tables.map((table) => (
              <TableOption
                key={table.id}
                table={table}
                reservation={reservation}
                name={id + "-table"}
                selected={table.id === selectedTableId}
                disabled={disabled}
                onSelect={onSelect}
              />
            ))}
          </fieldset>
        </>
      )}

      {ready && selectedTableId !== null && (
        <div
          className={
            "sage-table-picker__message" +
            (validSelection
              ? ""
              : " sage-table-picker__message--warning")
          }
          role={validSelection ? "status" : "alert"}
        >
          <strong>
            {validSelection
              ? "Đã chọn " + selected.code
              : selected
                ? "Bàn " + selected.code + " hiện không thể chọn"
                : "Bàn đã chọn không nằm trong danh sách đang xem"}
          </strong>

          <p>
            {validSelection
              ? "Khu vực yêu cầu ban đầu của khách vẫn được giữ. " +
                "Hệ thống kiểm tra lại bàn trống khi xác nhận."
              : "Lựa chọn vẫn được giữ để bạn kiểm tra. " +
                "Vui lòng chọn bàn khả dụng trước khi xác nhận."}
          </p>

          {validSelection &&
            selected.outsideRequestedArea &&
            reservation?.requestedArea && (
              <p>
                Bàn thuộc {selected.area.name}; khách yêu cầu{" "}
                {reservation.requestedArea.name}. Hãy xác nhận lại
                với khách.
              </p>
            )}
        </div>
      )}

      {actions && (
        <footer className="sage-table-picker__actions">
          {actions}
        </footer>
      )}
    </section>
  );
}