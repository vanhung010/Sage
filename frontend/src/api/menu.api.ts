import { apiClient } from "./client";
import type { PageResponse } from "../types/api";
import type {
    DishDetail,
    DishFilters,
    DishSummary,
    Category,
    Tag
} from "../types/menu"

export const menuApi = {
    async listDishes(filter?: DishFilters, signal?: AbortSignal) : Promise<PageResponse<DishSummary>> {
        const response = await apiClient.get<PageResponse<DishSummary>>("/api/menu/dishes",{
                params: filter,
                signal,
            },
        );
        return response.data;
    },

    async listCatories(signal?: AbortSignal) : Promise<Category[]>{
        const response = await apiClient.get<Category[]>("/api/menu/categories", {
            signal: signal
        })
        return response.data;
    },

    async listTags(signal?: AbortSignal) : Promise<Tag[]>{
        const response = await apiClient.get<Tag[]>("/api/menu/tags", {
            signal: signal
        })
      
        return response.data;
    },
            async getDish(
        id: string,
        signal?: AbortSignal,
        ): Promise<DishDetail> {
        const response = await apiClient.get<DishDetail>(
            `/api/menu/dishes/${encodeURIComponent(id)}`,
            { signal },
        );

        return response.data;
        },

}