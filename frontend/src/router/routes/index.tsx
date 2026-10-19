import { Navigate, type RouteObject } from "react-router-dom";
import { publicRoutes } from "./public.routes";
import { authRoutes } from "./auth.routes";
import { customerRoutes } from "./customer.routes";
import { adminRoutes } from "./admin.routes";
import { staffRoutes } from "./staff.routes";

export const appRoutes: RouteObject[] = [
  ...publicRoutes,
  ...authRoutes,
  ...customerRoutes,
  ...adminRoutes,
  ...staffRoutes,
  { path: "*", element: <Navigate to="/" replace /> },
];
