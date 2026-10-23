import type { ReactNode } from "react";

import {
  Navigate,
  Outlet,
  useLocation,
} from "react-router-dom";

import { useAuth } from "../../auth/AuthContext";
import { getRoleHome } from "../../auth/redirects";

import type { AccountRole } from "../../types/auth";

interface RoleRouteProps {
  allowedRoles: readonly AccountRole[];
  fallback?: ReactNode;
}

export function RoleRoute({
  allowedRoles,
  fallback,
}: RoleRouteProps) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          returnTo:
            location.pathname +
            location.search +
            location.hash,
        }}
      />
    );
  }

  if (!allowedRoles.includes(user.role)) {
    return (
      <>
        {fallback ?? (
          <Navigate
            to={getRoleHome(user.role)}
            replace
          />
        )}
      </>
    );
  }

  return <Outlet />;
}