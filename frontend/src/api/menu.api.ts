import { apiClient } from "./client";
import type { PageResponse } from "../types/api";
import type {
    
    DishFilters,
    DishSummary
} from "../types/menu"

export const menuApi = {
    async listDishes(filter?: DishFilters, signal?: AbortSignal) : Promise<PageResponse<DishSummary>> {

        const response = await apiClient.get<PageResponse<DishSummary>>("/api/menu/dishes",{
                params: filter,
                signal,
            },
        );
        
        return response.data;
    }
}