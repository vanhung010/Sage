export interface CreateOrderRequest {
  createOrderDishItemRequestList: {
    idDish: number;
    quantity: number;
  }[];

  deliveryAddress: string;
  deliveryPhone: string;
  paymentMethod: "COD";
}

export interface OrderItemResponse {
  id: number;
  nameDish: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderResponse {
  id: number;
  foodOrderStatus: "PLACED" | "CANCELLED";
  createdAt: string;
  totalAmount: number;
  deleveryAddress: string;
  deliveryPhone: string;
  paymentMethod: "COD";
  orderDishItemResponseList: OrderItemResponse[];
}

export interface OrderDetailResponse {
  id: number;
  foodOrderStatus: "PLACED" | "CANCELLED";
  createdAt: string;
  cancelledAt: string | null;
  totalAmountPrice: number;
  totalQuantity: number;
  deleveryAddress: string;
  deliveryPhone: string;
  paymentMethod: "COD";
  orderDishItemResponseList: OrderItemResponse[];
}