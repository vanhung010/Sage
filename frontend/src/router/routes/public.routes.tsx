import type { RouteObject } from "react-router-dom";
import { HomePage } from "../../pages/public/Home";
import { MenuPage } from "../../pages/public/Menu";
import { DishDetailPage } from "../../pages/public/DishDetail/DishDetailPage";
import { ReservationPage } from "../../pages/public/Reservation/ReservationPage";
import { ReservationSuccessPage } from "../../pages/public/ReservationSuccess/ReservationSuccessPage";

export const publicRoutes: RouteObject[] = [
  { path: "/", element: <HomePage /> },
  { path: "/menu", element: <MenuPage /> },
  { path: "/menu/:id", element: <DishDetailPage /> },
  { path: "/reservation", element: <ReservationPage /> },
  { path: "/reservation/success", element: <ReservationSuccessPage /> },
];
