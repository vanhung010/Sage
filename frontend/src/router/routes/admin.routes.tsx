import { Navigate, type RouteObject } from "react-router-dom";
import { StaffLayout } from "../../components/layout/ManagementLayout";
import { AdminDishesPage } from "../../pages/admin/Dishes/AdminDishesPage";
import { RoleRoute } from "../guards/RoleRoute";

export const adminRoutes: RouteObject[] = [
  {
    path: "/admin",
    element: <RoleRoute allowedRoles={["ADMIN"]} />,
    children: [
      {
        element: <StaffLayout ordersEnabled />,
        children: [
          { index: true, element: <Navigate to="menu" replace /> },
          {
            path: "menu",
            element: <AdminDishesPage />,
            handle: { breadcrumb: "Thực đơn" },
          },
        ],
      },
    ],
  },
];