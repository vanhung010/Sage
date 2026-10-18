import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
} from "react-router-dom";

import { HomePage } from "../pages/Home";
import { MenuPage } from "../pages/Menu";
import { LoginPage } from "../pages/Login";
import { RegisterPage } from "../pages/Register";

import { DishDetailPage } from "../pages/DishDetail/DishDetailPage";
import { ReservationPage } from "../pages/Reservation/ReservationPage";
import { ReservationSuccessPage } from "../pages/ReservationSuccess/ReservationSuccessPage";
import { ReservationDetailRoute } from "../pages/ReservationDetail/ReservationDetailRoute";
import { ReservationReschedulePage } from "../pages/ReservationReschedule/ReservationReschedulePage";

import { CustomerRoute } from "./CustomerRoute";

const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />,
  },
  {
    path: "/menu",
    element: <MenuPage />,
  },
  {
    path: "/menu/:id",
    element: <DishDetailPage />,
  },
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
  },
  {
    path: "/reservation",
    element: <ReservationPage />,
  },
  {
    path: "/reservation/success",
    element: <ReservationSuccessPage />,
  },
  {
    element: <CustomerRoute />,

    children: [
      {
        path: "/reservations/:id",
        element: <ReservationDetailRoute />,
      },
      {
        path: "/reservations/:id/reschedule",
        element: <ReservationReschedulePage />,
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/" replace />,
  },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}