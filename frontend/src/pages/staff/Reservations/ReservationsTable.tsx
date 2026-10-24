import { Eye, MoreHorizontal } from "lucide-react";
import { useNavigate } from "react-router-dom";

import {
  DataTable,
  DataTableText,
  type DataTableColumn,
  type DataTableRowKey,
} from "../../../components/ui/DataTable";

import { Badge } from "../../../components/ui/Badge";
import { IconButton } from "../../../components/ui/IconButton";

import type {
  StaffReservationItem,
} from "../../../types/staff-reservation";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import { RESERVATION_STATUS_META } from "./reservationPresentation";

interface ReservationsTableProps {
  data: StaffReservationItem[];

  loading: boolean;
  error: string | null;
  onRetry: () => void;

  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;

  selectedKeys: ReadonlySet<DataTableRowKey>;
  onSelectionChange: (keys: Set<DataTableRowKey>) => void;
}

function ReservationDetailButton({ row }: { row: StaffReservationItem }) {
  const navigate = useNavigate();

  return (
    <IconButton
      size="sm"
      variant="ghost"
      icon={<Eye />}
      aria-label={`Chi tiết đơn ${row.code}`}
      title="Xem chi tiết đặt bàn"
      onClick={() => void navigate(`/staff/reservations/${row.id}`)}
    />
  );
}

const columns: DataTableColumn<StaffReservationItem>[] = [
  {
    id: "code",
    header: "Mã đặt bàn",
    width: 160,
    className: "sage-data-table__code",
    render: (row) => (
      <DataTableText primary={row.code} />
    ),
  },
  {
    id: "customer",
    header: "Khách hàng",
    width: 172,
    render: (row) => (
      <DataTableText
        primary={row.customerName ?? "—"}
        secondary={row.customerPhone ?? "—"}
      />
    ),
  },
  {
    id: "startTime",
    header: "Thời gian",
    width: 168,
    render: (row) => (
      <DataTableText
        primary={displayDate(dateInputValue(row.startTime))}
        secondary={
          `${displayTime(row.startTime)} – ` +
          displayTime(row.endTime)
        }
      />
    ),
  },
  {
    id: "partySize",
    header: "Số khách",
    width: 88,
    align: "center",
    render: (row) => row.partySize,
  },
  {
    id: "table",
    header: "Bàn / Khu vực",
    width: 140,
    render: (row) => (
      <DataTableText
        primary={row.tableCode ?? "Chưa xếp bàn"}
        secondary={row.areaName ?? "—"}
      />
    ),
  },
  {
    id: "status",
    header: "Trạng thái",
    width: 164,
    render: (row) => {
      const status = RESERVATION_STATUS_META[row.status];

      return (
        <Badge variant={status.variant}>
          {status.label}
        </Badge>
      );
    },
  },
  {
    id: "actions",
    header: "Thao tác",
    width: 96,
    align: "center",
    render: (row) => (
      <div className="sage-data-table__actions">
        <ReservationDetailButton row={row} />

        <IconButton
          size="sm"
          variant="ghost"
          icon={<MoreHorizontal />}
          aria-label={`Thao tác với đơn ${row.code} chưa khả dụng`}
          title="Các thao tác xử lý đơn chưa khả dụng"
          disabled
        />
      </div>
    ),
  },
];

export function ReservationsTable({
  data,
  loading,
  error,
  onRetry,
  page,
  pageSize,
  totalElements,
  onPageChange,
  selectedKeys,
  onSelectionChange,
}: ReservationsTableProps) {
  return (
    <DataTable
      caption="Danh sách đặt bàn"
      columns={columns}
      data={data}
      getRowKey={(row) => row.id}
      loading={loading}
      loadingRows={pageSize}
      error={error}
      onRetry={onRetry}
      emptyTitle="Không tìm thấy đơn đặt bàn"
      emptyDescription="Thử thay đổi ngày hoặc điều kiện lọc."
      itemLabel="đơn đặt bàn"
      pagination={{
        page,
        size: pageSize,
        totalElements,
        onPageChange,
      }}
      selection={{
        selectedKeys,
        onChange: onSelectionChange,
        getLabel: (row) => `Chọn đơn ${row.code}`,
      }}
      sort={{
        columnId: "startTime",
        direction: "asc",
      }}
    />
  );
}
