import type { TextareaHTMLAttributes } from "react";

import "./Textarea.css";

interface TextareaProps
  extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: boolean;
}

export function Textarea({
  error = false,
  disabled,
  readOnly,
  className = "",
  ...props
}: TextareaProps) {
  const classes = [
    "sage-textarea",
    error ? "sage-textarea--error" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <textarea
      className={classes}
      disabled={disabled}
      readOnly={readOnly}
      aria-invalid={error || undefined}
      {...props}
    />
  );
}