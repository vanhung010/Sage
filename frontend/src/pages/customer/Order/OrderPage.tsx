import { useEffect, useRef, useState, type FormEvent } from "react";
import { orderApi } from "../../../api/order.api";
import { getApiError } from "../../../api/error";
import { useCart } from "../../../cart/CartContext";
import type { OrderResponse } from "../../../types/order";
import { OrderCheckoutView, type OrderFieldErrors } from "./OrderCheckoutView";

export function OrderPage() {
  const { items, subtotal, totalQuantity, setQuantity, removeItem, clearCart } = useCart();
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [deliveryPhone, setDeliveryPhone] = useState("");
  const [fieldErrors, setFieldErrors] = useState<OrderFieldErrors>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [createdOrder, setCreatedOrder] = useState<OrderResponse | null>(null);
  const submittingRef = useRef(false);

  useEffect(() => {
    if (createdOrder) {
      document.getElementById("order-success-title")?.focus();
      window.scrollTo(0, 0);
    }
  }, [createdOrder]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submittingRef.current || items.length === 0) return;
    const address = deliveryAddress.trim();
    const phone = deliveryPhone.trim();
    const errors: OrderFieldErrors = {};
    if (!address) errors.address = "Vui lòng nhập địa chỉ giao hàng để tiếp tục.";
    if (!phone) errors.phone = "Vui lòng nhập số điện thoại người nhận.";
    else if (!/^\d{1,30}$/.test(phone)) errors.phone = "Số điện thoại phải gồm các chữ số, tối đa 30 số.";
    setError("");
    setFieldErrors(errors);
    if (errors.address || errors.phone) {
      document.getElementById(errors.address ? "delivery-address" : "delivery-phone")?.focus();
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    try {
      const order = await orderApi.create({
        createOrderDishItemRequestList: items.map((item) => ({ idDish: item.dishId, quantity: item.quantity })),
        deliveryAddress: address,
        deliveryPhone: phone,
        paymentMethod: "COD",
      });
      setCreatedOrder(order);
      clearCart();
    } catch (cause) {
      const apiError = getApiError(cause);
      setError(apiError.status === 0
        ? "Chưa xác nhận được kết quả đặt món. Hãy kiểm tra lịch sử đơn trước khi gửi lại."
        : apiError.message || "Không thể đặt món. Vui lòng thử lại.");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return <OrderCheckoutView
    items={items} subtotal={subtotal} totalQuantity={totalQuantity}
    address={deliveryAddress} phone={deliveryPhone} fieldErrors={fieldErrors}
    error={error} submitting={submitting} createdOrder={createdOrder}
    onAddressChange={(value) => {
      setDeliveryAddress(value);
      setFieldErrors((current) => ({ ...current, address: undefined }));
    }}
    onPhoneChange={(value) => {
      setDeliveryPhone(value);
      setFieldErrors((current) => ({ ...current, phone: undefined }));
    }}
    onQuantityChange={(id, value) => { if (!submittingRef.current) setQuantity(id, value); }}
    onRemove={(id) => { if (!submittingRef.current) removeItem(id); }}
    onSubmit={(event) => void handleSubmit(event)}
  />;
}
