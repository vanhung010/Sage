import type { ReactNode } from "react";

import { Pagination } from "../Pagination";

import type { DataTablePagination } from "./DataTable.types";

interface DataTableFooterProps {
  rowCount: number;

  pagination?: DataTablePagination;

  loading?: boolean;
  showPagination?: boolean;

  summary?: ReactNode;
  itemLabel?: string;
}

export function DataTableFooter({
  rowCount,
  pagination,
  loading = false,
  showPagination = true,
  summary,
  itemLabel = "bản ghi",
}: DataTableFooterProps) {
  const totalItems = pagination?.totalElements ?? rowCount;

  const totalPages = pagination
    ? Math.ceil(totalItems / pagination.size)
    : 0;

  const offset = pagination
    ? pagination.page * pagination.size
    : 0;

  const defaultSummary =
    rowCount > 0
      ? `Hiển thị ${offset + 1}–${Math.min(
          offset + rowCount,
          totalItems,
        )} trong ${totalItems} ${itemLabel}`
      : `Trang hiện tại không có dữ liệu · Tổng ${totalItems} ${itemLabel}`;

  return (
    <div className="sage-data-table__footer">
      {loading ? (
        <div
          className="sage-data-table__loading-footer"
          aria-hidden="true"
        >
          <span className="sage-data-table__skeleton sage-data-table__skeleton--summary" />

          {showPagination && pagination && (
            <div className="sage-data-table__loading-pages">
              {Array.from({ length: 5 }, (_, index) => (
                <span
                  key={index}
                  className="sage-data-table__skeleton sage-data-table__skeleton--page"
                />
              ))}
            </div>
          )}
        </div>
      ) : (
        <>
          <div className="sage-data-table__summary">
            {summary ?? defaultSummary}
          </div>

          {showPagination && pagination && totalPages > 1 && (
            <Pagination
              currentPage={pagination.page + 1}
              totalPages={totalPages}
              onPageChange={(page) =>
                pagination.onPageChange(page - 1)
              }
            />
          )}
        </>
      )}
    </div>
  );
}