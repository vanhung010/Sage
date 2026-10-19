import type { CartItem } from "../types/cart";

export const MAX_QUANTITY = 2_147_483_647;

//explain
function isCartItem(value: unknown): value is CartItem {
     if (!value || typeof value !== "object") return false;

     //explain
     const item = value as Record<string, unknown>;

      return (
    typeof item.dishId === "number" &&
    Number.isSafeInteger(item.dishId) &&
    item.dishId > 0 &&
    typeof item.name === "string" &&
    (typeof item.imageUrl === "string" || item.imageUrl === null) &&
    typeof item.price === "number" &&
    Number.isSafeInteger(item.price) &&
    item.price > 0 &&
    typeof item.quantity === "number" &&
    Number.isInteger(item.quantity) &&
    item.quantity > 0 &&
    item.quantity <= MAX_QUANTITY
  );
}
export function writeCart(key: string | null, items: CartItem[]) {
  if (!key) return;

  try {
    localStorage.setItem(key, JSON.stringify(items));
  } catch {
    // Giỏ vẫn hoạt động trong bộ nhớ nếu trình duyệt chặn storage.
  }
}
//explain
export function readCart(key: string | null): CartItem[] {
  if (!key) return [];

  try {
    const raw = localStorage.getItem(key);
    if (!raw) return [];

    const data: unknown = JSON.parse(raw);
    if (!Array.isArray(data)) return [];

    // Mỗi món chỉ có một dòng trong giỏ.
    const uniqueItems = new Map<number, CartItem>();

    for (const value of data) {
      if (isCartItem(value)) {
        uniqueItems.set(value.dishId, value);
      }
    }

    return [...uniqueItems.values()];
  } catch {
    return [];
  }
}