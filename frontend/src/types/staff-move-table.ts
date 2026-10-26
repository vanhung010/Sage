import type { ReservationStatus } from "./reservation";

export interface MoveTableArea {
  id: number;
  name: string;
}

export type MoveTableAvailability =
  | "AVAILABLE"
  | "BUSY"
  | "CAPACITY_MISMATCH"
  | "SUSPENDED"
  | "INACTIVE";

export interface MoveTableOption {
  id: number;
  code: string;
  area: MoveTableArea;
  minCapacity: number;
  maxCapacity: number;
  availability: MoveTableAvailability;
  selectable: boolean;
  outsideRequestedArea: boolean;
  blockingPeriod: {
    startTime: string;
    endTime: string;
  } | null;
  unavailableReason: string | null;
}

export interface MoveTableContext {
  id: number;
  code: string;
  version: number;
  status: ReservationStatus;
  customerName: string | null;
  partySize: number;
  startTime: string;
  endTime: string;
  bufferMinutes: number;
  occupiedUntil: string;
  currentTable: {
    id: number;
    code: string;
    area: MoveTableArea;
  };
  requestedArea: MoveTableArea | null;
}

export interface MoveTableOptionsResponse {
  reservation: MoveTableContext;
  areas: MoveTableArea[];
  selectedAreaId: number | null;
  suitableTableCount: number;
  totalTableCount: number;
  tables: MoveTableOption[];
}