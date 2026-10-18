import {
  Navigate,
  Outlet,
  useLocation,
} from "react-router-dom";

import { useAuth } from "../auth/AuthContext";
import { ReservationPageState } from "../pages/ReservationDetail/ReservationPageState";

export function CustomerRoute() {
  const { user } = useAuth();
  const { pathname } = useLocation();

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ returnTo: pathname }}
      />
    );
  }

  if (user.role !== "CUSTOMER") {
    return (
      <ReservationPageState
        message="Chức năng này chỉ dành cho tài khoản khách hàng."
      />
    );
  }

  return <Outlet />;
}