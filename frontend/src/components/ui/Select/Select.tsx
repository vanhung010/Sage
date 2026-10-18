import {
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
} from "react";

import { ChevronDown } from "lucide-react";

import "./Select.css";

export type SelectSize = "md" | "lg";

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectProps {
  options: SelectOption[];

  id?: string;
"aria-label"?: string;

  value?: string;
  defaultValue?: string;

  placeholder?: string;

  size?: SelectSize;
  error?: boolean;
  disabled?: boolean;

  leadingIcon?: ReactNode;

  name?: string;

  onValueChange?: (value: string) => void;

  className?: string;
}

export function Select({
  options,
  value,
  defaultValue = "",
  placeholder = "Chọn danh mục",
  size = "md",
  error = false,
  disabled = false,
  leadingIcon,
  name,
    id,
  "aria-label": ariaLabel,
  onValueChange,
  className = "",
}: SelectProps) {
  const [open, setOpen] = useState(false);

  const [internalValue, setInternalValue] =
    useState(defaultValue);

  const containerRef = useRef<HTMLDivElement>(null);

  const listboxId = useId();


  const selectedValue =
    value !== undefined ? value : internalValue;

  const selectedOption = options.find(
    (option) => option.value === selectedValue
  );

  const classes = [
    "sage-select",
    `sage-select--${size}`,
    open ? "sage-select--open" : "",
    error ? "sage-select--error" : "",
    disabled ? "sage-select--disabled" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  function handleToggle() {
    if (disabled) return;

    setOpen((current) => !current);
  }

  function handleSelect(option: SelectOption) {
    if (option.disabled) return;

    if (value === undefined) {
      setInternalValue(option.value);
    }

    onValueChange?.(option.value);

    setOpen(false);
  }

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(
          event.target as Node
        )
      ) {
        setOpen(false);
      }
    }

    document.addEventListener(
      "mousedown",
      handleClickOutside
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        handleClickOutside
      );
    };
  }, []);

  function handleKeyDown(
    event: React.KeyboardEvent<HTMLButtonElement>
  ) {
    if (event.key === "Escape") {
      setOpen(false);
    }

    if (
      event.key === "Enter" ||
      event.key === " "
    ) {
      event.preventDefault();
      handleToggle();
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
    }
  }

  return (
    <div
      ref={containerRef}
      className="sage-select-container"
    >
      {name && (
        <input
          type="hidden"
          name={name}
          value={selectedValue}
        />
      )}

      <button
        id={id ?? name}
  aria-label={ariaLabel}
  type="button"
  className={classes}
  disabled={disabled}
  aria-haspopup="listbox"
  aria-expanded={open}
  aria-controls={listboxId}
  aria-invalid={error || undefined}
  onClick={handleToggle}
  onKeyDown={handleKeyDown}
      >
        {leadingIcon && (
          <span className="sage-select__icon">
            {leadingIcon}
          </span>
        )}

        <span
          className={
            selectedOption
              ? "sage-select__value"
              : "sage-select__placeholder"
          }
        >
          {selectedOption
            ? selectedOption.label
            : placeholder}
        </span>

        <ChevronDown
          className="sage-select__chevron"
          size={20}
          strokeWidth={2}
          aria-hidden="true"
        />
      </button>

      {open && !disabled && (
        <div
          id={listboxId}
          className="sage-select__dropdown"
          role="listbox"
        >
          {options.map((option) => {
            const selected =
              option.value === selectedValue;

            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={selected}
                disabled={option.disabled}
                className={[
                  "sage-select__option",
                  selected
                    ? "sage-select__option--selected"
                    : "",
                ]
                  .filter(Boolean)
                  .join(" ")}
                onClick={() =>
                  handleSelect(option)
                }
              >
                {option.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}