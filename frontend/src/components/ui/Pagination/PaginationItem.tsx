import type { ButtonHTMLAttributes } from "react";

import "./Pagination.css";

interface PaginationItemProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  page: number;
  active?: boolean;
}

export function PaginationItem({
  page,
  active = false,
  className = "",
  ...props
}: PaginationItemProps) {
  const classes = [
    "sage-pagination-item",
    active ? "sage-pagination-item--active" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type="button"
      className={classes}
      aria-current={active ? "page" : undefined}
      {...props}
    >
      {page}
    </button>
  );
}