import type {
  HTMLAttributes,
  ReactNode,
} from "react";

import "./Badge.css";

export type BadgeVariant =
  | "neutral"
  | "success"
  | "warning"
  | "info"
  | "error";

interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  leadingIcon?: ReactNode;
}

export function Badge({
  children,
  variant = "neutral",
  leadingIcon,
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
    <span
      className={classes}
      {...props}
    >
      {leadingIcon && (
        <span className="sage-badge__icon">
          {leadingIcon}
        </span>
      )}

      <span className="sage-badge__label">
        {children}
      </span>
    </span>
  );
}