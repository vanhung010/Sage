import { useId } from "react";
import { Search } from "lucide-react";

import {
  FilterToolbar,
  FilterToolbarField,
  type FilterToolbarActiveFilter,
} from "../../../components/ui/FilterToolbar";

import { Input } from "../../../components/ui/Input";
import { Select } from "../../../components/ui/Select";
import { DateInput } from "../../../components/ui/DateInput";

import type { AreaResponse } from "../../../types/area";
import type {
  StaffReservationFilterForm,
  StaffReservationFilterKey,
} from "../../../types/staff-reservation";

import {
  dateInputValue,
  displayDate,
} from "../../../utils/reservation";

import { RESERVATION_STATUS_META } from "./reservationPresentation";

interface ReservationFiltersProps {
  draft: StaffReservationFilterForm;
  applied: StaffReservationFilterForm;

  areas: AreaResponse[];
  areasDisabled?: boolean;

  loading?: boolean;

  onChange: (value: StaffReservationFilterForm) => void;
  onApply: () => void;
  onClear: () => void;
  onRemove: (key: StaffReservationFilterKey) => void;
}

const FILTER_KEYS: StaffReservationFilterKey[] = [
  "search",
  "date",
  "status",
  "areaId",
];

const FILTER_LABELS: Record<
  StaffReservationFilterKey,
  string
> = {
  search: "Từ khóa",
  date: "Ngày",
  status: "Trạng thái",
  areaId: "Khu vực",
};

export function ReservationFilters({
  draft,
  applied,
  areas,
  areasDisabled = false,
  loading = false,
  onChange,
  onApply,
  onClear,
  onRemove,
}: ReservationFiltersProps) {
  const id = useId();
  const today = dateInputValue();

  const statusOptions = [
    { value: "", label: "Tất cả trạng thái" },
    ...Object.entries(RESERVATION_STATUS_META).map(
      ([value, item]) => ({
        value,
        label: item.label,
      }),
    ),
  ];

  const areaOptions = [
    { value: "", label: "Tất cả khu vực" },
    ...areas.map((area) => ({
      value: String(area.id),
      label: area.name,
    })),
  ];

  function updateField<K extends StaffReservationFilterKey>(
    key: K,
    value: StaffReservationFilterForm[K],
  ) {
    onChange({
      ...draft,
      [key]: value,
    });
  }

  function isActive(
    values: StaffReservationFilterForm,
    key: StaffReservationFilterKey,
  ) {
    if (key === "date") {
      return values.date !== today;
    }

    return values[key] !== "";
  }

  function getLabel(key: StaffReservationFilterKey) {
    switch (key) {
      case "date":
        return displayDate(applied.date);

      case "status":
        return applied.status
          ? RESERVATION_STATUS_META[applied.status].label
          : "";

      case "areaId":
        return (
          areas.find(
            (area) => String(area.id) === applied.areaId,
          )?.name ?? applied.areaId
        );

      default:
        return applied.search;
    }
  }

  const activeFilters: FilterToolbarActiveFilter[] =
    FILTER_KEYS
      .filter((key) => isActive(applied, key))
      .map((key) => ({
        id: key,
        label: `${FILTER_LABELS[key]}: ${getLabel(key)}`,
        onRemove: () => onRemove(key),
      }));

  const canClear = FILTER_KEYS.some(
    (key) =>
      isActive(draft, key) ||
      isActive(applied, key),
  );

  return (
    <FilterToolbar
      aria-label="Bộ lọc đơn đặt bàn"
      loading={loading}
      activeFilters={activeFilters}
      canClear={canClear}
      onApply={onApply}
      onClear={onClear}
      filters={({ disabled }) => (
        <>
          <FilterToolbarField
            label="Tìm kiếm"
            htmlFor={`${id}-search`}
            grow
          >
            <Input
              id={`${id}-search`}
              size="md"
              value={draft.search}
              placeholder="Mã đơn, tên hoặc số điện thoại"
              leadingIcon={<Search aria-hidden="true" />}
              disabled={disabled}
              onChange={(event) =>
                updateField("search", event.target.value)
              }
            />
          </FilterToolbarField>

          <FilterToolbarField
            label="Ngày đặt bàn"
            htmlFor={`${id}-date`}
            width={180}
          >
            <DateInput
              id={`${id}-date`}
              size="md"
              value={draft.date}
              placeholder="Chọn ngày"
              required
              disabled={disabled}
              onChange={(value) =>
                updateField("date", value)
              }
            />
          </FilterToolbarField>

          <FilterToolbarField
            label="Trạng thái"
            htmlFor={`${id}-status`}
            width={220}
          >
            <Select
              id={`${id}-status`}
              size="md"
              value={draft.status}
              options={statusOptions}
              disabled={disabled}
              onValueChange={(value) =>
                updateField(
                  "status",
                  value as StaffReservationFilterForm["status"],
                )
              }
            />
          </FilterToolbarField>

          <FilterToolbarField
            label="Khu vực"
            htmlFor={`${id}-area`}
            width={196}
          >
            <Select
              id={`${id}-area`}
              size="md"
              value={draft.areaId}
              options={areaOptions}
              disabled={disabled || areasDisabled}
              onValueChange={(value) =>
                updateField("areaId", value)
              }
            />
          </FilterToolbarField>
        </>
      )}
    />
  );
}