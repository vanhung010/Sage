import type {
  ButtonHTMLAttributes,
  ReactNode,
} from "react";

import "./Button.css";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "destructive";

export type ButtonSize =
  | "sm"
  | "md"
  | "lg";

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
    //dấu ? nghĩa là prop không bắt buộc
  variant?: ButtonVariant;
  size?: ButtonSize;

  fullWidth?: boolean;
  loading?: boolean;

  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
}

export function Button({
  children,
  variant = "primary",
  size = "md",
  fullWidth = false,
  loading = false,
  leadingIcon,
  trailingIcon,
  disabled,
  className = "",
  ...props
}: ButtonProps) {
  
  const classes = [
    "sage-button",
    `sage-button--${variant}`,
    `sage-button--${size}`,
    fullWidth ? "sage-button--full-width" : "",
    loading ? "sage-button--loading" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading}
      {...props}
    >
      {loading ? (
        <span
          className="sage-button__spinner"
          aria-hidden="true"
        />
      ) : (
        leadingIcon && (
          <span className="sage-button__icon">
            {leadingIcon}
          </span>
        )
      )}

      <span className="sage-button__label">
        {children}
      </span>

      {!loading && trailingIcon && (
        <span className="sage-button__icon">
          {trailingIcon}
        </span>
      )}
    </button>
  );
}