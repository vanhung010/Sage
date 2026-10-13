import type { ButtonHTMLAttributes } from "react";

import "./MenuCategoryTab.css";

interface MenuCategoryTabProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  active?: boolean;
}

export function MenuCategoryTab({
  children,
  active = false,
  className = "",
  type = "button",
  ...props
}: MenuCategoryTabProps) {
  const classes = [
    "sage-menu-category-tab",
    active ? "sage-menu-category-tab--active" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type={type}
      className={classes}
      aria-pressed={active}
      {...props}
    >
      {children}
    </button>
  );
}