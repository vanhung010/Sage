import { X } from "lucide-react";

import { IconButton } from "../IconButton";

export interface FilterChipProps {
  label: string;
  disabled?: boolean;
  onRemove: () => void;
}

export function FilterChip({
  label,
  disabled = false,
  onRemove,
}: FilterChipProps) {
  return (
    <span
      className="sage-filter-chip"
      data-disabled={disabled || undefined}
    >
      <span
        className="sage-filter-chip__label"
        title={label}
      >
        {label}
      </span>

      <IconButton
        type="button"
        size="sm"
        variant="ghost"
        className="sage-filter-chip__remove"
        icon={<X size={16} />}
        aria-label={`Bỏ bộ lọc ${label}`}
        title="Bỏ điều kiện này"
        disabled={disabled}
        onClick={onRemove}
      />
    </span>
  );
}