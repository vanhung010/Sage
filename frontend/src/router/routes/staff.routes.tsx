import {
  Navigate,
  type RouteObject, useNavigate
} from "react-router-dom";

import { RoleRoute } from "../guards/RoleRoute";
import { GuestReservationPage } from "../../pages/staff/GuestReservation/GuestReservationPage";
import {
  StaffLayout,
  type StaffRouteHandle,
} from "../../components/layout/StaffLayout";

import { StaffReservationsPage } from "../../pages/staff/Reservations/StaffReservationsPage";
import { StaffReservationDetailPage } from "../../pages/staff/ReservationDetail/StaffReservationDetailPage";
import { StaffReservationDurationPage } from "../../pages/staff/ChangeDuration/StaffReservationDurationPage";
import { StaffMoveTablePage,} from "../../pages/staff/MoveTable/StaffMoveTablePage";
import { StaffReservationTimelinePage, } from "../../pages/staff/ReservationTimeline/StaffReservationTimelinePage";
import { StaffOrdersPage } from
  "../../pages/staff/Orders/StaffOrdersPage";
  import { StaffOrderDetailPage } from
  "../../pages/staff/OrderDetail/StaffOrderDetailPage";
export const staffRoutes: RouteObject[] = [
  {
    path: "/staff",
    element: (
      <RoleRoute allowedRoles={["STAFF", "ADMIN"]} />
    ),
    children: [
      {
        element: <StaffLayout ordersEnabled/>,
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
          path: "reservations/timeline",
          element: <StaffReservationTimelinePage />,
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
      {
        path: "reservations/:id/change-duration",
        element: <StaffReservationDurationPage />,
        handle: {
          breadcrumb: "Đổi thời lượng",
        } satisfies StaffRouteHandle,
      },
      {
      path: "reservations/:id/move-table",
      element: <StaffMoveTablePage />,
      handle: {
        breadcrumb: "Đặt bàn",
      } satisfies StaffRouteHandle,
    },
    {
  path: "orders",
  element: <StaffOrdersPage />,
  handle: {
    breadcrumb: "Đơn đặt món",
  } satisfies StaffRouteHandle,
},

{
  path: "orders/:id",
  element: <StaffOrderDetailPage />,
  handle: {
    breadcrumb: "Đơn đặt món",
  } satisfies StaffRouteHandle,
},
        ],
      },
    ],
  },
];
