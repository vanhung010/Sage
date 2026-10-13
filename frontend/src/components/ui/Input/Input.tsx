import type {
  InputHTMLAttributes,
  ReactNode,
} from "react";

import "./Input.css";

export type InputSize = "md" | "lg";

interface InputProps
  extends Omit<
    InputHTMLAttributes<HTMLInputElement>,
    "size"
  > {
  size?: InputSize;
  error?: boolean;

  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
}

export function Input({
  size = "md",
  error = false,
  leadingIcon,
  trailingIcon,
  disabled,
  readOnly,
  className = "",
  ...props
}: InputProps) {
  const classes = [
    "sage-input",
    `sage-input--${size}`,
    error ? "sage-input--error" : "",
    disabled ? "sage-input--disabled" : "",
    readOnly ? "sage-input--readonly" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classes}>
      {leadingIcon && (
        <span className="sage-input__icon">
          {leadingIcon}
        </span>
      )}

      <input
        className="sage-input__field"
        disabled={disabled}
        readOnly={readOnly}
        aria-invalid={error || undefined}
        {...props}
      />

      {trailingIcon && (
        <span className="sage-input__icon">
          {trailingIcon}
        </span>
      )}
    </div>
  );
}