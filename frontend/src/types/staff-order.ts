import type { OrderResponse } from "./order";

export type StaffOrderStatus =
  OrderResponse["foodOrderStatus"];

export interface StaffOrderSummaryResponse {
  id: number;

  customerId: number;
  customerName: string;
  customerPhone: string | null;
  deliveryPhone: string;

  createdAt: string;
  status: StaffOrderStatus;

  totalQuantity: number;
  totalAmount: number;

  paymentMethod: "COD";
  cancelledAt: string | null;
}

export interface StaffOrderFilters {
  search: string;
  date: string;
  status: StaffOrderStatus | "";
}

export type StaffOrderFilterKey =
  keyof StaffOrderFilters;

export interface StaffOrderDetailItem {
  id: number;
  dishId: number;
  dishName: string;

  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface StaffOrderDetailResponse
  extends StaffOrderSummaryResponse {
  deliveryAddress: string;
  dishCount: number;
  items: StaffOrderDetailItem[];
}