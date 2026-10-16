export type DishStatus = "AVAILABLE" | "SOLD_OUT" | "HIDDEN";
export type PriceRange =
  | "UNDER_100K"
  | "FROM_100K_TO_200K"
  | "FROM_200K_TO_300K"
  | "OVER_300K";

export interface Category {
    id: number;
    name: string;
    displayOrder: number;
}

export interface Tag {
    id: number;
    name: string;
}

export interface DishSummary {
    id: number;
    name: string;
    price: number;
    status: DishStatus;
    imageUrl: string | null;
    description: string | null;
    category: Category;
    tags: Tag[] | null;
}

export interface DishDetail extends DishSummary {
    displayOrder: number;
}

export interface DishFilters {
    page? : number;
    size? : number;
    search?: string;
    categoryId?: number;
    tagId? : number;
    priceRange?: PriceRange;
}