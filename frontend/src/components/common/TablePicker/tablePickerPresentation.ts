import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";
import type { MoveTableOption } from "../../../types/staff-move-table";

export function isTableSelectable(table: MoveTableOption) {
  return table.selectable && table.availability === "AVAILABLE";
}

export function pickerDate(value: string) {
  return Number.isFinite(Date.parse(value))
    ? displayDate(dateInputValue(value))
    : "—";
}

export function pickerTime(value: string) {
  return Number.isFinite(Date.parse(value))
    ? displayTime(value)
    : "—";
}

export function pickerRange(
  start: string,
  end: string,
  reference = start,
) {
  const valid = [start, end, reference].every((value) =>
    Number.isFinite(Date.parse(value)),
  );

  if (!valid) return "—";

  const referenceDay = dateInputValue(reference);
  const sameDay =
    dateInputValue(start) === referenceDay &&
    dateInputValue(end) === referenceDay;

  return sameDay
    ? pickerTime(start) + "–" + pickerTime(end)
    : pickerDate(start) + " · " + pickerTime(start) +
        " – " + pickerDate(end) + " · " + pickerTime(end);
}