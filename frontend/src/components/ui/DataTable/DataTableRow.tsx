import { DataTableCell } from "./DataTableCell";
import { DataTableCheckbox } from "./DataTableCheckbox";

import type { DataTableColumn } from "./DataTable.types";

interface DataTableRowProps<T> {
  row: T;
  index: number;

  columns: readonly DataTableColumn<T>[];

  selected?: boolean;
  selectionLabel?: string;
  onSelect?: (checked: boolean) => void;
}

export function DataTableRow<T>({
  row,
  index,
  columns,
  selected = false,
  selectionLabel = "Chọn hàng",
  onSelect,
}: DataTableRowProps<T>) {
  return (
    <tr
      className="sage-data-table__row"
      data-selected={selected || undefined}
    >
      {onSelect && (
        <DataTableCell
          align="center"
          className="sage-data-table__selection"
        >
          <DataTableCheckbox
            label={selectionLabel}
            checked={selected}
            onChange={onSelect}
          />
        </DataTableCell>
      )}

      {columns.map((column) => (
        <DataTableCell
          key={column.id}
          align={column.align}
          className={column.className}
        >
          {column.render(row, index)}
        </DataTableCell>
      ))}
    </tr>
  );
}