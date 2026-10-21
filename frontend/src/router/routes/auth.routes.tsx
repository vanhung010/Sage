import type { RouteObject } from "react-router-dom";
import { LoginPage } from "../../pages/auth/Login";
import { RegisterPage } from "../../pages/auth/Register";
import { ForgotPasswordPage } from "../../pages/auth/ForgotPassword";

export const authRoutes: RouteObject[] = [
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },
  {
    path: "/forgot-password",
    element: <ForgotPasswordPage />,
  },
];
