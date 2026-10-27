import {
  useEffect,
  useId,
  useState,
} from "react";

import {
  Eye,
  Search,
  ShoppingBag,
} from "lucide-react";

import { Button } from "../../../components/ui/Button";
import { IconButton } from "../../../components/ui/IconButton";
import { Badge } from "../../../components/ui/Badge";
import { Input } from "../../../components/ui/Input";
import { DateInput } from "../../../components/ui/DateInput";
import { Select } from "../../../components/ui/Select";
import { useNavigate } from "react-router-dom";
import {
  FilterToolbar,
  FilterToolbarField,
  type FilterToolbarActiveFilter,
} from "../../../components/ui/FilterToolbar";

import {
  DataTable,
  DataTableText,
  type DataTableColumn,
} from "../../../components/ui/DataTable";

import type {
  StaffOrderFilters,
  StaffOrderFilterKey,
  StaffOrderSummaryResponse,
} from "../../../types/staff-order";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import { useStaffOrders } from "./useStaffOrders";

import "./StaffOrdersPage.css";

const STATUS_META = {
  PLACED: {
    label: "Đã đặt",
    variant: "placed",
  },

  CANCELLED: {
    label: "Đã hủy",
    variant: "cancelled",
  },
} as const;

const FILTER_KEYS: StaffOrderFilterKey[] = [
  "search",
  "date",
  "status",
];

const FILTER_LABELS: Record<StaffOrderFilterKey, string> = {
  search: "Từ khóa",
  date: "Ngày",
  status: "Trạng thái",
};

const numberFormatter = new Intl.NumberFormat("vi-VN");

function emptyFilters(): StaffOrderFilters {
  return {
    search: "",
    date: "",
    status: "",
  };
}

function hasFilters(values: StaffOrderFilters): boolean {
  return FILTER_KEYS.some((key) => values[key] !== "");
}

interface OrderFiltersProps {
  draft: StaffOrderFilters;
  applied: StaffOrderFilters;
  loading: boolean;

  onChange: (value: StaffOrderFilters) => void;
  onApply: () => void;
  onRemove: (key: StaffOrderFilterKey) => void;
}

function OrderFilters({
  draft,
  applied,
  loading,
  onChange,
  onApply,
  onRemove,
}: OrderFiltersProps) {
  const id = useId();

  function updateField<K extends StaffOrderFilterKey>(
    key: K,
    value: StaffOrderFilters[K],
  ) {
    onChange({
      ...draft,
      [key]: value,
    });
  }

  function filterValue(key: StaffOrderFilterKey): string {
    if (key === "date") {
      return displayDate(applied.date);
    }

    if (key === "status") {
      return applied.status
        ? STATUS_META[applied.status].label
        : "";
    }

    return applied.search;
  }

  const activeFilters: FilterToolbarActiveFilter[] =
    FILTER_KEYS
      .filter((key) => applied[key] !== "")
      .map((key) => ({
        id: key,
        label: `${FILTER_LABELS[key]}: ${filterValue(key)}`,
        onRemove: () => onRemove(key),
      }));

  return (
    <FilterToolbar
      aria-label="Bộ lọc đơn đặt món"
      loading={loading}
      activeFilters={activeFilters}
      showClear={false}
      onApply={onApply}
      filters={({ disabled }) => (
        <>
          <FilterToolbarField
            label="Tìm kiếm"
            htmlFor={`${id}-search`}
            grow
          >
            <Input
              id={`${id}-search`}
              size="md"
              value={draft.search}
              placeholder="Mã đơn, tên hoặc số điện thoại"
              leadingIcon={<Search aria-hidden="true" />}
              disabled={disabled}
              onChange={(event) =>
                updateField("search", event.target.value)
              }
            />
          </FilterToolbarField>

          <FilterToolbarField
            label="Ngày đặt"
            htmlFor={`${id}-date`}
            width={200}
          >
            <DateInput
              id={`${id}-date`}
              size="md"
              value={draft.date}
              placeholder="Tất cả ngày"
              disabled={disabled}
              onChange={(value) =>
                updateField("date", value)
              }
            />
          </FilterToolbarField>

          <FilterToolbarField
            label="Trạng thái"
            htmlFor={`${id}-status`}
            width={220}
          >
            <Select
              id={`${id}-status`}
              size="md"
              value={draft.status}
              disabled={disabled}
              options={[
                {
                  value: "",
                  label: "Tất cả trạng thái",
                },
                {
                  value: "PLACED",
                  label: STATUS_META.PLACED.label,
                },
                {
                  value: "CANCELLED",
                  label: STATUS_META.CANCELLED.label,
                },
              ]}
              onValueChange={(value) => {
                if (
                  value === "" ||
                  value === "PLACED" ||
                  value === "CANCELLED"
                ) {
                  updateField("status", value);
                }
              }}
            />
          </FilterToolbarField>
        </>
      )}
    />
  );
}


function OrderDetailButton({
  order,
}: {
  order: StaffOrderSummaryResponse;
}) {
  const navigate = useNavigate();

  return (
    <IconButton
      size="sm"
      variant="ghost"
      icon={<Eye aria-hidden="true" />}
      aria-label={`Xem chi tiết đơn #${order.id}`}
      title="Xem chi tiết đơn đặt món"
      onClick={() =>
        void navigate(`/staff/orders/${order.id}`)
      }
    />
  );
}


export function StaffOrdersPage() {
  // filters: điều kiện đã gửi lên API.
  const [filters, setFilters] =
    useState<StaffOrderFilters>(emptyFilters);

  // draft: điều kiện đang nhập, chưa bấm Áp dụng.
  const [draft, setDraft] =
    useState<StaffOrderFilters>(emptyFilters);

  const [page, setPage] = useState(0);

  const {
    data,
    loading,
    error,
    retry,
    pageSize,
  } = useStaffOrders(filters, page);

  const filtered = hasFilters(filters);

  // Khi số lượng đơn giảm, quay về trang cuối còn tồn tại.
  useEffect(() => {
    if (!data) return;

    const lastPage = Math.max(0, data.totalPages - 1);

    if (page > lastPage) {
      setPage(lastPage);
    }
  }, [data, page]);

  function commitFilters(next: StaffOrderFilters) {
    setPage(0);
    setFilters(next);
  }

  function applyFilters() {
    const next: StaffOrderFilters = {
      ...draft,
      search: draft.search.trim(),
    };

    setDraft(next);
    commitFilters(next);
  }

  function removeFilter(key: StaffOrderFilterKey) {
    // Chỉ xóa trường được chọn, giữ các thay đổi nháp khác.
    setDraft((current) => ({
      ...current,
      [key]: "",
    }));

    commitFilters({
      ...filters,
      [key]: "",
    });
  }

  const columns: DataTableColumn<StaffOrderSummaryResponse>[] = [
    {
      id: "id",
      header: "Mã đơn",
      width: 112,
      render: (row) => (
        <DataTableText primary={`#${row.id}`} />
      ),
    },

    {
      id: "customer",
      header: "Khách hàng",
      width: 256,
      render: (row) => (
        <DataTableText
          primary={row.customerName || "—"}
          secondary={row.customerPhone || "—"}
        />
      ),
    },

    {
      id: "createdAt",
      header: "Ngày đặt",
      width: 192,
      render: (row) => (
        <DataTableText
          primary={displayDate(dateInputValue(row.createdAt))}
          secondary={displayTime(row.createdAt)}
        />
      ),
    },

    {
      id: "totalQuantity",
      header: "Số phần",
      width: 96,
      render: (row) => (
        <DataTableText
          primary={numberFormatter.format(row.totalQuantity)}
        />
      ),
    },

    {
      id: "totalAmount",
      header: "Tổng tiền",
      width: 160,
      align: "right",
      render: (row) => (
        <span className="staff-orders__amount">
          {numberFormatter.format(row.totalAmount)}đ
        </span>
      ),
    },

    {
      id: "status",
      header: "Trạng thái",
      width: 176,
      render: (row) => {
        const status = STATUS_META[row.status];

        return (
          <Badge variant={status.variant}>
            {status.label}
          </Badge>
        );
      },
    },

    {
  id: "actions",
  header: "Chi tiết",
  width: 144,
  align: "center",
  render: (row) => (
    <OrderDetailButton order={row} />
  ),
},
  ];

  return (
    <section
      className="staff-orders"
      aria-labelledby="staff-orders-title"
    >
      <div className="staff-orders__header">
        <div>
          <h1 id="staff-orders-title">
            Quản lý đơn đặt món
          </h1>

          <p className="staff-orders__description">
            Tra cứu thông tin các đơn đặt món của khách hàng.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="md"
          disabled={loading}
          onClick={retry}
        >
          Làm mới
        </Button>
      </div>

      <OrderFilters
        draft={draft}
        applied={filters}
        loading={loading}
        onChange={setDraft}
        onApply={applyFilters}
        onRemove={removeFilter}
      />

      {!loading && !error && data && data.totalElements > 0 && (
        <p
          className="staff-orders__count"
          role="status"
          aria-live="polite"
        >
          {numberFormatter.format(data.totalElements)}
          {" đơn đặt món"}
          {filtered ? " phù hợp" : ""}
        </p>
      )}

      <DataTable
        caption="Danh sách đơn đặt món"
        columns={columns}
        data={data?.content ?? []}
        getRowKey={(row) => row.id}
        loading={loading}
        loadingRows={pageSize}
        error={error}
        onRetry={retry}
        emptyTitle={
          filtered
            ? "Không có đơn phù hợp"
            : "Chưa có đơn đặt món"
        }
        emptyDescription={
          filtered
            ? "Thử thay đổi từ khóa, ngày đặt hoặc trạng thái để tìm lại."
            : "Các đơn đặt món của khách hàng sẽ xuất hiện tại đây."
        }
        emptyIcon={<ShoppingBag size={32} />}
        itemLabel="đơn đặt món"
        pagination={{
          page,
          size: pageSize,
          totalElements: data?.totalElements ?? 0,
          onPageChange: setPage,
        }}
        sort={{
          columnId: "createdAt",
          direction: "desc",
        }}
      />

     <p className="staff-orders__footnote">
  Chọn biểu tượng mắt để xem chi tiết đơn đặt món.
</p>
    </section>
  );
}