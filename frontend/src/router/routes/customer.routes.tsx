import type { RouteObject } from "react-router-dom";
import { CustomerRoute } from "../guards/CustomerRoute";
import { OrderPage } from "../../pages/customer/Order/OrderPage";
import { OrderDetailPage } from "../../pages/customer/OrderDetail/OrderDetailPage";
import { ReservationDetailRoute } from "../../pages/customer/ReservationDetail/ReservationDetailRoute";
import { ReservationReschedulePage } from "../../pages/customer/ReservationReschedule/ReservationReschedulePage";
import { MyReservationsPage } from "../../pages/customer/MyOrders/MyReservationsPage";
import { MyFoodOrdersPage } from "../../pages/customer/MyOrders/MyFoodOrdersPage";

export const customerRoutes: RouteObject[] = [
  {
    element: <CustomerRoute />,
    children: [
      { path: "/reservations", element: <MyReservationsPage /> },
      { path: "/orders", element: <MyFoodOrdersPage /> },
      { path: "/reservations/:id", element: <ReservationDetailRoute /> },
      { path: "/reservations/:id/reschedule", element: <ReservationReschedulePage /> },
      { path: "/order", element: <OrderPage /> },
      { path: "/orders/:id", element: <OrderDetailPage /> },
    ],
  },
];
