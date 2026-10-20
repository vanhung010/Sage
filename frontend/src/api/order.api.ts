import { apiClient } from "./client";
import type { PageResponse } from "../types/api";
import type {
  CreateOrderRequest,
  OrderResponse,
  OrderDetailResponse,
} from "../types/order";

const BASE_URL = "/api/customer/orders";

export const orderApi = {
   async create(
    request: CreateOrderRequest,
  ): Promise<OrderResponse> {
    const response = await apiClient.post<OrderResponse>(
      BASE_URL,
      request,
    );

    return response.data;
  },


  async getDetail(id: number,signal?: AbortSignal,): Promise<OrderDetailResponse> {
    const response = await apiClient.get<OrderDetailResponse>(
      `${BASE_URL}/${id}`,
      { signal },
    );

    return response.data;
  },

  async cancel(id: number): Promise<OrderDetailResponse> {
    const response = await apiClient.patch<OrderDetailResponse>(
      `${BASE_URL}/${id}/cancel`,
    );

    return response.data;
  },

  async listMine(
  params: {
    status?: "PLACED" | "CANCELLED";
    page?: number;
    size?: number;
  },
  signal?: AbortSignal,
): Promise<PageResponse<OrderDetailResponse>> {
  const response = await apiClient.get<PageResponse<OrderDetailResponse>>(
    BASE_URL,
    { params, signal },
  );
  return response.data;
},
};