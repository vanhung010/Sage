import { useId } from "react";
import { CalendarDays } from "lucide-react";
import "./DateInput.css";

interface DateInputProps {
  value: string; // YYYY-MM-DD; chuỗi rỗng nếu chưa chọn
  onChange: (value: string) => void;
  label?: string;
  placeholder?: string;
  min?: string; // YYYY-MM-DD
  max?: string; // YYYY-MM-DD
  size?: "md" | "lg";
  disabled?: boolean;
  readOnly?: boolean;
  required?: boolean;
  error?: string;
  name?: string;
  className?: string;
}

function formatDate(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  if (!match) return value;

  const [, year, month, day] = match;
  return `${day}/${month}/${year}`;
}

export function DateInput({
  value,
  onChange,
  label,
  placeholder = "Chọn ngày",
  min,
  max,
  size = "md",
  disabled = false,
  readOnly = false,
  required = false,
  error,
  name,
  className = "",
}: DateInputProps) {
  const inputId = useId();
  const errorId = `${inputId}-error`;

  return (
    <div className={`date-input ${className}`}>
      {label && (
        <label className="date-input__label" htmlFor={inputId}>
          {label}
        </label>
      )}

      <div
        className={[
          "date-input__field",
          `date-input__field--${size}`,
          error ? "date-input__field--error" : "",
          disabled ? "date-input__field--disabled" : "",
          readOnly ? "date-input__field--readonly" : "",
        ]
          .filter(Boolean)
          .join(" ")}
      >
        <span
          className={`date-input__text ${
            value ? "" : "date-input__text--placeholder"
          }`}
          aria-hidden="true"
        >
          {value ? formatDate(value) : placeholder}
        </span>

        <CalendarDays
          className="date-input__icon"
          size={20}
          aria-hidden="true"
        />

        {/* Input phủ toàn bộ ô: bấm vào chữ hoặc icon đều chọn được ngày. */}
        <input
          id={inputId}
          className="date-input__native"
          type="date"
          name={name}
          value={value}
          min={min}
          max={max}
          required={required}
          disabled={disabled}
          readOnly={readOnly}
          aria-label={label ? undefined : placeholder}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          onChange={(event) => onChange(event.target.value)}
          onClick={(event) => {
            if (disabled || readOnly) return;

            try {
              event.currentTarget.showPicker?.();
            } catch {
              // Trình duyệt vẫn có thể xử lý click theo cách mặc định.
            }
          }}
        />
      </div>

      {error && (
        <p id={errorId} className="date-input__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}