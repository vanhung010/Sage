import type { ReactNode } from "react";

import type { DataTableAlign } from "./DataTable.types";

export interface DataTableCellProps {
  children: ReactNode;
  align?: DataTableAlign;
  className?: string;
}

export function DataTableCell({
  children,
  align = "left",
  className = "",
}: DataTableCellProps) {
  return (
    <td
      className={`sage-data-table__cell ${className}`.trim()}
      data-align={align}
    >
      {children}
    </td>
  );
}

export interface DataTableTextProps {
  primary: ReactNode;
  secondary?: ReactNode;
}

export function DataTableText({
  primary,
  secondary,
}: DataTableTextProps) {
  return (
    <div className="sage-data-table__text">
      <span
        className="sage-data-table__primary"
        title={typeof primary === "string" ? primary : undefined}
      >
        {primary}
      </span>

      {secondary != null && (
        <span
          className="sage-data-table__secondary"
          title={
            typeof secondary === "string"
              ? secondary
              : undefined
          }
        >
          {secondary}
        </span>
      )}
    </div>
  );
}