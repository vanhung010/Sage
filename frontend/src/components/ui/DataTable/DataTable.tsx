import { DataTableHeaderRow } from "./DataTableHeaderRow";
import { DataTableRow } from "./DataTableRow";
import { DataTableSkeleton } from "./DataTableSkeleton";
import { DataTableState } from "./DataTableState";
import { DataTableFooter } from "./DataTableFooter";

import type {
  DataTableProps,
  DataTableRowKey,
} from "./DataTable.types";

import "./DataTable.css";

export function DataTable<T>({
  columns,
  data,
  getRowKey,
  caption,
  loading = false,
  loadingRows = 7,
  error,
  onRetry,
  emptyTitle = "Chưa có dữ liệu",
  emptyDescription = "Dữ liệu sẽ xuất hiện tại đây.",
  emptyIcon,
  selection,
  pagination,
  sort,
  onSortChange,
  summary,
  itemLabel = "bản ghi",
  showFooter = true,
  showPagination = true,
  className = "",
}: DataTableProps<T>) {
  const hasError = error != null;

  const state = loading
    ? "loading"
    : hasError
      ? "error"
      : data.length === 0
        ? "empty"
        : "default";

  const pageKeys = data.map(getRowKey);

  const selectedOnPage = selection
    ? pageKeys.filter((key) => selection.selectedKeys.has(key)).length
    : 0;

  const allSelected =
    pageKeys.length > 0 && selectedOnPage === pageKeys.length;

  const someSelected = selectedOnPage > 0 && !allSelected;

  const tableWidth =
    columns.reduce((total, column) => total + column.width, 0) +
    (selection ? 48 : 0);

  const canSelect = state === "default";

  // Giữ footer nếu trang hiện tại rỗng nhưng vẫn còn dữ liệu
  // ở các trang khác để người dùng có thể chuyển trang.
  const hasFooter =
    showFooter &&
    (state === "loading" ||
      state === "default" ||
      (state === "empty" && (pagination?.totalElements ?? 0) > 0));

  function toggleRow(key: DataTableRowKey, checked: boolean) {
    if (!selection) return;

    const next = new Set(selection.selectedKeys);

    if (checked) {
      next.add(key);
    } else {
      next.delete(key);
    }

    selection.onChange(next);
  }

  function togglePage(checked: boolean) {
    if (!selection) return;

    const next = new Set(selection.selectedKeys);

    // Chỉ tác động tới các hàng của trang đang hiển thị.
    for (const key of pageKeys) {
      if (checked) {
        next.add(key);
      } else {
        next.delete(key);
      }
    }

    selection.onChange(next);
  }

  return (
    <div
      className={`sage-data-table ${className}`.trim()}
      aria-busy={loading}
    >
      <div
        className="sage-data-table__scroll"
        role="region"
        aria-label={caption}
        tabIndex={0}
      >
        <table
          className="sage-data-table__table"
          style={{ minWidth: tableWidth }}
        >
          <caption className="sage-data-table__sr-only">
            {caption}
          </caption>

          <colgroup>
            {selection && <col style={{ width: 48 }} />}

            {columns.map((column) => (
              <col
                key={column.id}
                style={{ width: column.width }}
              />
            ))}
          </colgroup>

          <thead>
            <DataTableHeaderRow
              columns={columns}
              sort={sort}
              onSortChange={onSortChange}
              disabled={loading}
              selection={
                selection
                  ? {
                      checked: canSelect && allSelected,
                      indeterminate: canSelect && someSelected,
                      disabled: !canSelect,
                      onChange: togglePage,
                    }
                  : undefined
              }
            />
          </thead>

          <tbody aria-hidden={loading || undefined}>
            {state === "loading" && (
              <DataTableSkeleton
                columns={columns}
                rows={loadingRows}
                selectable={!!selection}
              />
            )}

            {state === "default" &&
              data.map((row, index) => {
                const key = getRowKey(row);

                return (
                  <DataTableRow
                    key={key}
                    row={row}
                    index={index}
                    columns={columns}
                    selected={selection?.selectedKeys.has(key)}
                    selectionLabel={selection?.getLabel(row)}
                    onSelect={
                      selection
                        ? (checked) => toggleRow(key, checked)
                        : undefined
                    }
                  />
                );
              })}
          </tbody>
        </table>
      </div>

      {state === "loading" && (
        <span
          role="status"
          className="sage-data-table__sr-only"
        >
          Đang tải dữ liệu…
        </span>
      )}

      {state === "empty" && (
        <DataTableState
          variant="empty"
          title={emptyTitle}
          description={emptyDescription}
          icon={emptyIcon}
        />
      )}

      {state === "error" && (
        <DataTableState
          variant="error"
          title="Không thể tải danh sách"
          description={error || "Vui lòng thử lại để tải danh sách."}
          onRetry={onRetry}
        />
      )}

      {hasFooter && (
        <DataTableFooter
          rowCount={data.length}
          loading={loading}
          pagination={pagination}
          showPagination={showPagination}
          summary={summary}
          itemLabel={itemLabel}
        />
      )}
    </div>
  );
}