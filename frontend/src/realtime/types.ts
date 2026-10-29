export type RealtimeEventType =
  | "RESERVATION_CREATED"
  | "RESERVATION_UPDATED"
  | "FOOD_ORDER_CREATED"
  | "FOOD_ORDER_UPDATED";

export interface RealtimeEvent {
  eventId: string;
  type: RealtimeEventType;
  resourceId: number;
  occurredAt: string;
}

// SYNC chỉ dùng nội bộ React khi reconnect/focus hoặc đối chiếu định kỳ.
export type RealtimeSignal = RealtimeEvent | { type: "SYNC" };

const allowedTypes: ReadonlySet<string> = new Set([
  "RESERVATION_CREATED",
  "RESERVATION_UPDATED",
  "FOOD_ORDER_CREATED",
  "FOOD_ORDER_UPDATED",
]);

export function parseRealtimeEvent(body: string): RealtimeEvent | null {
  try {
    const value: unknown = JSON.parse(body);
    if (typeof value !== "object" || value === null) return null;
    const item = value as Record<string, unknown>;
    if (
      typeof item.eventId !== "string" ||
      typeof item.type !== "string" ||
      !allowedTypes.has(item.type) ||
      typeof item.resourceId !== "number" ||
      !Number.isSafeInteger(item.resourceId) ||
      item.resourceId <= 0 ||
      typeof item.occurredAt !== "string"
    ) return null;
    return item as unknown as RealtimeEvent;
  } catch {
    return null;
  }
}