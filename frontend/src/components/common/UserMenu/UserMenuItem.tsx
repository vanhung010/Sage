import type { ButtonHTMLAttributes } from "react";

import "./UserMenu.css";

export type UserMenuItemVariant =
  | "default"
  | "danger";

interface UserMenuItemProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: UserMenuItemVariant;
}

export function UserMenuItem({
  children,
  variant = "default",
  className = "",
  ...props
}: UserMenuItemProps) {
  const classes = [
    "sage-user-menu-item",
    `sage-user-menu-item--${variant}`,
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type="button"
      role="menuitem"
      className={classes}
      {...props}
    >
      {children}
    </button>
  );
}