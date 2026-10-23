import { Check, Minus } from "lucide-react";

interface DataTableCheckboxProps {
  label: string;
  checked: boolean;
  indeterminate?: boolean;
  disabled?: boolean;

  onChange: (checked: boolean) => void;
}

export function DataTableCheckbox({
  label,
  checked,
  indeterminate = false,
  disabled = false,
  onChange,
}: DataTableCheckboxProps) {
  return (
    <label className="sage-data-table__checkbox">
      <input
        type="checkbox"
        aria-label={label}
        checked={checked}
        disabled={disabled}
        ref={(element) => {
          if (element) {
            element.indeterminate = indeterminate;
          }
        }}
        onChange={(event) => onChange(event.target.checked)}
      />

      <span
        className="sage-data-table__checkbox-box"
        aria-hidden="true"
      >
        {indeterminate ? (
          <Minus size={12} strokeWidth={3} />
        ) : checked ? (
          <Check size={12} strokeWidth={3} />
        ) : null}
      </span>
    </label>
  );
}