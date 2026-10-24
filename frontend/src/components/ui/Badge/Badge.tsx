import type {
  HTMLAttributes,
  ReactNode,
} from "react";

import "./Badge.css";

export type BadgeVariant =
  // Tone cơ bản.
  | "neutral"
  | "success"
  | "warning"
  | "info"
  | "error"

  // Đặt bàn.
  | "pending"
  | "confirmed"
  | "arrived"
  | "completed"
  | "cancelled"
  | "rejected"
  | "no-show"

  // Đặt món.
  | "placed"

  // Thực đơn.
  | "available"
  | "sold-out"
  | "hidden"

  // Trạng thái hoạt động / tài khoản.
  | "active"
  | "inactive"
  | "locked";

export interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;

  // Có thể dùng label hoặc children.
  // children được ưu tiên để tương thích cách dùng hiện tại.
  label?: string;

  leadingIcon?: ReactNode;
  showLeadingIcon?: boolean;
}

export function Badge({
  children,
  label = "Badge",
  variant = "neutral",
  leadingIcon,
  showLeadingIcon = true,
  className = "",
  ...props
}: BadgeProps) {
  const classes = [
    "sage-badge",
    `sage-badge--${variant}`,
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <span className={classes} {...props}>
      {showLeadingIcon && leadingIcon != null && (
        <span
          className="sage-badge__icon"
          aria-hidden="true"
        >
          {leadingIcon}
        </span>
      )}

      <span className="sage-badge__label">
        {children ?? label}
      </span>
    </span>
  );
}