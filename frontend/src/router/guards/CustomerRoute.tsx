import { ReservationPageState } from "../../pages/customer/ReservationDetail/ReservationPageState";
import { RoleRoute } from "./RoleRoute";

export function CustomerRoute() {
  return (
    <RoleRoute
      allowedRoles={["CUSTOMER"]}
      fallback={
        <ReservationPageState message="Chức năng này chỉ dành cho tài khoản khách hàng." />
      }
    />
  );
}
