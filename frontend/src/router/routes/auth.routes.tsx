import type { RouteObject } from "react-router-dom";
import { LoginPage } from "../../pages/auth/Login";
import { RegisterPage } from "../../pages/auth/Register";

export const authRoutes: RouteObject[] = [
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
];
