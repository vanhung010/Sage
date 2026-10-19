import type { ReactNode } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import type { AccountRole } from "../../types/auth";

interface RoleRouteProps {
  allowedRoles: readonly AccountRole[];
  fallback?: ReactNode;
}

export function RoleRoute({
  allowedRoles,
  fallback = <Navigate to="/" replace />,
}: RoleRouteProps) {
  const { user } = useAuth();
  const { pathname } = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ returnTo: pathname }} />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <>{fallback}</>;
  }

  return <Outlet />;
}
