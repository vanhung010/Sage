import type { ReactNode } from "react";

import "./FormField.css";

interface FormFieldProps {
  label?: string;
  required?: boolean;

  supportingText?: string;
  error?: string;

  htmlFor?: string;

  children: ReactNode;

  className?: string;
}

export function FormField({
  label,
  required = false,
  supportingText,
  error,
  htmlFor,
  children,
  className = "",
}: FormFieldProps) {
    //class
  const classes = [
    "sage-form-field",
    error ? "sage-form-field--error" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={classes}>
      {label && (
        <div className="sage-form-field__label-row">
          <label
            className="sage-form-field__label"
            htmlFor={htmlFor}
          >
            {label}
          </label>

          {required && (
            <span
              className="sage-form-field__required"
              aria-hidden="true"
            >
              *
            </span>
          )}
        </div>
      )}

      {children}

      {(error || supportingText) && (
        <p className="sage-form-field__supporting-text">
          {error || supportingText}
        </p>
      )}
    </div>
  );
}