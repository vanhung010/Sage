import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
} from "lucide-react";

import type {
  DataTableAlign,
  DataTableSortDirection,
} from "./DataTable.types";

export interface DataTableHeaderCellProps {
  label: string;
  align?: DataTableAlign;

  showSort?: boolean;
  direction?: DataTableSortDirection;

  disabled?: boolean;
  onSort?: () => void;
}

export function DataTableHeaderCell({
  label,
  align = "left",
  showSort = false,
  direction,
  disabled = false,
  onSort,
}: DataTableHeaderCellProps) {
  const SortIcon =
    direction === "asc"
      ? ArrowUp
      : direction === "desc"
        ? ArrowDown
        : ArrowUpDown;

  const content = (
    <>
      <span title={label}>{label}</span>

      {showSort && (
        <SortIcon
          size={16}
          aria-hidden="true"
          className={
            direction
              ? "sage-data-table__sort-icon--active"
              : undefined
          }
        />
      )}
    </>
  );

  return (
    <th
      scope="col"
      className="sage-data-table__header-cell"
      data-align={align}
      aria-sort={
        direction === "asc"
          ? "ascending"
          : direction === "desc"
            ? "descending"
            : showSort
              ? "none"
              : undefined
      }
    >
      {showSort && onSort ? (
        <button
          type="button"
          className="sage-data-table__heading sage-data-table__sort"
          disabled={disabled}
          onClick={onSort}
          aria-label={`${label}: sắp xếp ${
            direction === "asc" ? "giảm dần" : "tăng dần"
          }`}
        >
          {content}
        </button>
      ) : (
        <div className="sage-data-table__heading">
          {content}
        </div>
      )}
    </th>
  );
}