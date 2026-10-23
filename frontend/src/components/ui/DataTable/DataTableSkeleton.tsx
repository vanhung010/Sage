import { DataTableCell } from "./DataTableCell";

import type { DataTableColumn } from "./DataTable.types";

interface DataTableSkeletonProps<T> {
  columns: readonly DataTableColumn<T>[];

  rows?: number;
  selectable?: boolean;
}

export function DataTableSkeleton<T>({
  columns,
  rows = 7,
  selectable = false,
}: DataTableSkeletonProps<T>) {
  return (
    <>
      {Array.from(
        { length: Math.max(1, rows) },
        (_, index) => (
          <tr key={index}>
            {selectable && (
              <DataTableCell
                align="center"
                className="sage-data-table__selection"
              >
                <span className="sage-data-table__skeleton sage-data-table__skeleton--checkbox" />
              </DataTableCell>
            )}

            {columns.map((column) => (
              <DataTableCell key={column.id}>
                <span className="sage-data-table__skeleton" />
              </DataTableCell>
            ))}
          </tr>
        ),
      )}
    </>
  );
}