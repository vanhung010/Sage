import { DataTableCheckbox } from "./DataTableCheckbox";
import { DataTableHeaderCell } from "./DataTableHeaderCell";

import type {
  DataTableColumn,
  DataTableSort,
} from "./DataTable.types";

interface HeaderSelection {
  checked: boolean;
  indeterminate: boolean;
  disabled: boolean;

  onChange: (checked: boolean) => void;
}

interface DataTableHeaderRowProps<T> {
  columns: readonly DataTableColumn<T>[];

  selection?: HeaderSelection;

  sort?: DataTableSort;
  onSortChange?: (sort: DataTableSort) => void;

  disabled?: boolean;
}

export function DataTableHeaderRow<T>({
  columns,
  selection,
  sort,
  onSortChange,
  disabled = false,
}: DataTableHeaderRowProps<T>) {
  return (
    <tr>
      {selection && (
        <th
          scope="col"
          className="sage-data-table__header-cell sage-data-table__selection"
        >
          <DataTableCheckbox
            label="Chọn tất cả hàng trên trang hiện tại"
            checked={selection.checked}
            indeterminate={selection.indeterminate}
            disabled={selection.disabled}
            onChange={selection.onChange}
          />
        </th>
      )}

      {columns.map((column) => {
        const direction =
          sort?.columnId === column.id
            ? sort.direction
            : undefined;

        const canSort = column.sortable && !!onSortChange;

        return (
          <DataTableHeaderCell
            key={column.id}
            label={column.header}
            align={column.align}
            showSort={!!column.sortable || !!direction}
            direction={direction}
            disabled={disabled}
            onSort={
              canSort
                ? () => {
                    onSortChange?.({
                      columnId: column.id,
                      direction:
                        direction === "asc" ? "desc" : "asc",
                    });
                  }
                : undefined
            }
          />
        );
      })}
    </tr>
  );
}