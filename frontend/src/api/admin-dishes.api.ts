import { apiClient } from "./client";
import type { PageResponse } from "../types/api";
import type { Category, DishStatus, Tag } from "../types/menu";

export type AdminCategory = Category & { active: boolean };

export interface AdminDishRow {
  id: number;
  name: string;
  price: number;
  imageUrl: string | null;
  category: Pick<Category, "id" | "name">;
  status: DishStatus;
  tag: Tag | null;
}

export type AdminDish = Omit<AdminDishRow, "tag"> & {
  description: string | null;
  displayOrder: number;
  tags: Tag[];
};

interface DishQuery {
  page: number;
  size: number;
  categoryId?: number;
  status?: DishStatus;
}

const menu = "/api/admin/menu";
const dishes = `${menu}/dishes`;

// apiClient mặc định gửi JSON. Với FormData, trình duyệt tự tạo multipart boundary.
const multipart = { headers: { "Content-Type": undefined } };

export const adminDishesApi = {
  async list(params: DishQuery, signal: AbortSignal) {
    return (await apiClient.get<PageResponse<AdminDishRow>>(dishes, { params, signal })).data;
  },
  async categories(signal: AbortSignal) {
    return (await apiClient.get<AdminCategory[]>(`${menu}/categories`, { signal })).data;
  },
  async tags(signal: AbortSignal) {
    return (await apiClient.get<Tag[]>(`${menu}/tags`, { signal })).data;
  },
  async detail(id: number, signal: AbortSignal) {
    return (await apiClient.get<AdminDish>(`${dishes}/${id}`, { signal })).data;
  },
  async create(data: FormData) {
    return (await apiClient.post<AdminDish>(dishes, data, multipart)).data;
  },
  async update(id: number, data: FormData) {
    return (await apiClient.patch<AdminDish>(`${dishes}/${id}`, data, multipart)).data;
  },
  async setStatus(id: number, status: DishStatus) {
    return (await apiClient.patch<AdminDish>(`${dishes}/${id}/status`, { status })).data;
  },
};