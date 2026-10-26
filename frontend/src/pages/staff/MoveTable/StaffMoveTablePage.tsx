import { Link, useNavigate, useParams } from "react-router-dom";

import { TablePicker } from "../../../components/common/TablePicker";
import {
  pickerRange,
} from "../../../components/common/TablePicker/tablePickerPresentation";
import { Button } from "../../../components/ui/Button";
import { FormField } from "../../../components/ui/FormField";
import { Textarea } from "../../../components/ui/Textarea";

import backIcon from "../../../assets/staff-reservation-detail/back.svg";
import type {
  MoveTableNavigationState,
} from "../../../types/staff-move-table";

import { useStaffMoveTable } from "./useStaffMoveTable";
import "./StaffMoveTablePage.css";

export function StaffMoveTablePage() {
  const { id } = useParams<{ id: string }>();
  const reservationId = Number(id);

  const valid =
    /^\d+$/.test(id ?? "") &&
    Number.isSafeInteger(reservationId) &&
    reservationId > 0;

  if (!valid) {
    return (
      <div className="staff-move-table">
        <h1>Không thể mở đơn đặt bàn</h1>
        <p role="alert">ID đơn đặt bàn không hợp lệ.</p>
        <Link to="/staff/reservations">
          Danh sách đặt bàn
        </Link>
      </div>
    );
  }

  return (
    <MoveTableEditor
      key={reservationId}
      reservationId={reservationId}
    />
  );
}

function MoveTableEditor({
  reservationId,
}: {
  reservationId: number;
}) {
  const editor = useStaffMoveTable(reservationId);
  const navigate = useNavigate();
  const reservation = editor.options?.reservation;
  const detailPath = `/staff/reservations/${reservationId}`;

  async function handleSubmit() {
    const result = await editor.submit();
    if (!result) return;

    const { detail, fromTableCode } = result;

    const state: MoveTableNavigationState = {
      tableMoved: {
        reservationId: detail.id,
        fromTableCode,
        toTableId: detail.table.id,
        toTableCode: detail.table.code,
        toAreaName: detail.table.area.name,
        startTime: detail.startTime,
        endTime: detail.endTime,
        requestedAreaName: detail.requestedArea?.name ?? null,
        updatedAt: detail.updatedAt,
      },
    };

    void navigate(detailPath, {
      replace: true,
      state,
    });
  }

  return (
    <div className="staff-move-table">
      <header className="staff-move-table__heading">
        <Link
          className="staff-move-table__back"
          to="/staff/reservations"
          aria-disabled={editor.saving || undefined}
          onClick={(event) => {
            if (editor.saving) event.preventDefault();
          }}
        >
          <img
            src={backIcon}
            width={16}
            height={16}
            alt=""
          />
          Danh sách đặt bàn
        </Link>

        <h1>Chuyển bàn</h1>

        {reservation && (
          <p>
            {reservation.code} ·{" "}
            {reservation.customerName?.trim() ||
              "Khách đặt bàn"} ·{" "}
            {reservation.partySize} khách
          </p>
        )}
      </header>

      <form
        className="staff-move-table__form"
        aria-busy={editor.saving}
        onSubmit={(event) => {
          event.preventDefault();
          void handleSubmit();
        }}
      >
        <TablePicker
          reservation={reservation}
          description={
            reservation
              ? `Bàn hiện tại: ${reservation.currentTable.code} · ${reservation.currentTable.area.name} · ${reservation.partySize} khách`
              : undefined
          }
          areas={editor.options?.areas ?? []}
          tables={editor.options?.tables ?? []}
          selectedAreaId={editor.areaId}
          selectedTableId={editor.selectedTableId}
          loading={editor.loading}
          error={editor.loadError?.message}
          disabled={editor.saving}
          onAreaChange={editor.changeArea}
          onSelect={editor.selectTable}
          onRetry={editor.reload}
        />

        {reservation && (
          <>
            <section className="staff-move-table__notice">
              <h2>Giữ nguyên yêu cầu khu vực</h2>
              <p>
                {editor.selectedTable?.outsideRequestedArea
                  ? `Bàn được chọn thuộc ${editor.selectedTable.area.name}. Hãy xác nhận lại với khách. `
                  : "Nếu chuyển sang khu vực khác, hãy xác nhận lại với khách. "}
                Giờ hẹn và trạng thái đơn được giữ nguyên.
              </p>
            </section>

            <FormField
              label="Ghi chú chuyển bàn"
              htmlFor="move-table-reason"
              supportingText={
                `Không bắt buộc · ${editor.reason.length}/1000 ký tự`
              }
              error={
                editor.reason.length > 1000
                  ? "Ghi chú không được vượt quá 1000 ký tự."
                  : undefined
              }
            >
              <Textarea
                id="move-table-reason"
                name="reason"
                className="staff-move-table__reason"
                value={editor.reason}
                maxLength={1000}
                disabled={editor.saving}
                placeholder="Ví dụ: Khách muốn ngồi ở vị trí yên tĩnh hơn."
                onChange={(event) =>
                  editor.changeReason(event.target.value)
                }
              />
            </FormField>
          </>
        )}

        {editor.saveError && (
          <section
            className="staff-move-table__notice"
            role="alert"
          >
            <h2>Chưa thể xác nhận kết quả chuyển bàn</h2>
            <p>{editor.saveError.message}</p>
            <p>
              Ghi chú và lựa chọn vẫn được giữ. Kiểm tra lại
              dữ liệu trước khi xác nhận tiếp.
            </p>

            <div className="staff-move-table__buttons">
              <Button
                type="button"
                variant="outline"
                disabled={editor.loading || editor.saving}
                onClick={editor.reload}
              >
                Kiểm tra lại danh sách
              </Button>

              <Button
                type="button"
                variant="outline"
                disabled={editor.saving}
                onClick={() => void navigate(detailPath)}
              >
                Xem chi tiết đơn
              </Button>
            </div>
          </section>
        )}

        <footer className="staff-move-table__actions">
          <p>
            {editor.needsReview
              ? "Kiểm tra lại dữ liệu trước khi tiếp tục."
              : reservation && editor.selectedTable
                ? `${reservation.currentTable.code} → ${editor.selectedTable.code} · Giữ nguyên ${pickerRange(reservation.startTime, reservation.endTime)} và ${reservation.bufferMinutes} phút dọn bàn.`
                : "Chọn một bàn phù hợp để tiếp tục."}
          </p>

          <div className="staff-move-table__buttons">
            <Button
              type="button"
              variant="outline"
              disabled={editor.saving}
              onClick={() => void navigate(detailPath)}
            >
              Quay lại
            </Button>

            <Button
              type="submit"
              loading={editor.saving}
              disabled={!editor.canSubmit}
            >
              Xác nhận chuyển bàn
            </Button>
          </div>
        </footer>
      </form>
    </div>
  );
}