import type { ButtonHTMLAttributes } from "react";
import { ChevronDown } from "lucide-react";

import "./UserMenu.css";

interface UserMenuTriggerProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  name: string;
  open?: boolean;
}

export function UserMenuTrigger({
  name,
  open = false,
  className = "",
  ...props
}: UserMenuTriggerProps) {
  const classes = [
    "sage-user-menu-trigger",
    open ? "sage-user-menu-trigger--open" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type="button"
      className={classes}
      aria-haspopup="menu"
      aria-expanded={open}
      {...props}
    >
      <span>{name}</span>

      <ChevronDown
        size={16}
        strokeWidth={2}
        aria-hidden="true"
        className="sage-user-menu-trigger__icon"
      />
    </button>
  );
}