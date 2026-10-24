import {
  Navigate,
  type RouteObject,
} from "react-router-dom";

import { RoleRoute } from "../guards/RoleRoute";
import { GuestReservationPage } from "../../pages/staff/GuestReservation/GuestReservationPage";
import {
  StaffLayout,
  type StaffRouteHandle,
} from "../../components/layout/StaffLayout";

import { StaffReservationsPage } from "../../pages/staff/Reservations/StaffReservationsPage";
import { StaffReservationDetailPage } from "../../pages/staff/ReservationDetail/StaffReservationDetailPage";

export const staffRoutes: RouteObject[] = [
  {
    path: "/staff",
    element: (
      <RoleRoute allowedRoles={["STAFF", "ADMIN"]} />
    ),
    children: [
      {
        element: <StaffLayout />,
        children: [
          {
            index: true,
            element: (
              <Navigate to="reservations" replace />
            ),
          },
          {
            path: "reservations",
            element: <StaffReservationsPage />,
            handle: {
              breadcrumb: "Đặt bàn",
            } satisfies StaffRouteHandle,
          },
          {
            path: "reservations/:id",
            element: <StaffReservationDetailPage />,
            handle: {
              breadcrumb: "Đặt bàn",
            } satisfies StaffRouteHandle,
          },
          {
        path: "reservations/guest",
        element: <GuestReservationPage />,
        handle: {
          breadcrumb: "Đặt bàn hộ",
        } satisfies StaffRouteHandle,
      },
        ],
      },
    ],
  },
];
