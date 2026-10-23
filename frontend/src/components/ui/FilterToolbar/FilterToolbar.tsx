import type { ReactNode } from "react";
import { LoaderCircle } from "lucide-react";

import { Button } from "../Button";
import { FilterChip } from "./FilterChip";

import "./FilterToolbar.css";

export interface FilterToolbarActiveFilter {
  id: string;
  label: string;
  onRemove: () => void;
}

export interface FilterToolbarRenderContext {
  disabled: boolean;
}

export interface FilterToolbarProps {
  filters: (
    context: FilterToolbarRenderContext,
  ) => ReactNode;

  activeFilters?: readonly FilterToolbarActiveFilter[];

  onApply: () => void;
  onClear: () => void;

  loading?: boolean;
  disabled?: boolean;

  /**
   * Có thể bật khi người dùng đã nhập điều kiện
   * nhưng chưa bấm Áp dụng.
   */
  canClear?: boolean;

  applyLabel?: string;
  clearLabel?: string;
  helperText?: string;

  "aria-label"?: string;
  className?: string;
}

export function FilterToolbar({
  filters,
  activeFilters = [],
  onApply,
  onClear,
  loading = false,
  disabled = false,
  canClear = activeFilters.length > 0,
  applyLabel = "Áp dụng",
  clearLabel = "Xóa bộ lọc",
  helperText = "Nhập điều kiện rồi chọn Áp dụng.",
  "aria-label": ariaLabel = "Bộ lọc danh sách",
  className = "",
}: FilterToolbarProps) {
  const blocked = disabled || loading;
  const hasFilters = activeFilters.length > 0;

  return (
    <form
      className={`sage-filter-toolbar ${className}`.trim()}
      aria-label={ariaLabel}
      aria-busy={loading}
      onSubmit={(event) => {
        event.preventDefault();

        if (blocked) return;

        onApply();
      }}
    >
      <fieldset
        className="sage-filter-toolbar__controls"
        disabled={blocked}
      >
        {filters({ disabled: blocked })}

        <Button
          type="submit"
          variant="primary"
          size="md"
          className="sage-filter-toolbar__apply"
          disabled={blocked}
          data-loading={loading || undefined}
          aria-label={
            loading
              ? "Đang áp dụng bộ lọc"
              : applyLabel
          }
          leadingIcon={
            loading ? (
              <LoaderCircle
                size={16}
                className="sage-filter-toolbar__spinner"
                aria-hidden="true"
              />
            ) : undefined
          }
        >
          {applyLabel}
        </Button>
      </fieldset>

      <div className="sage-filter-toolbar__footer">
        {hasFilters ? (
          <ul
            className="sage-filter-toolbar__chips"
            aria-label="Điều kiện đang áp dụng"
          >
            {activeFilters.map((filter) => (
              <li key={filter.id}>
                <FilterChip
                  label={filter.label}
                  disabled={blocked}
                  onRemove={filter.onRemove}
                />
              </li>
            ))}
          </ul>
        ) : (
          <p className="sage-filter-toolbar__helper">
            {helperText}
          </p>
        )}

        <Button
          type="button"
          variant="ghost"
          size="md"
          className="sage-filter-toolbar__clear"
          disabled={blocked || !canClear}
          onClick={onClear}
        >
          {clearLabel}
        </Button>
      </div>

      {loading && (
        <span
          role="status"
          className="sage-filter-toolbar__sr-only"
        >
          Đang áp dụng bộ lọc…
        </span>
      )}
    </form>
  );
}