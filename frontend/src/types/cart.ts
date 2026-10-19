export interface CartItem {
  dishId: number;
  name: string;
  imageUrl: string | null;
  price: number;
  quantity: number;
}