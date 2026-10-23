import type { AccountRole } from "../types/auth";

export function getRoleHome(role: AccountRole): string {
  return role === "CUSTOMER"
    ? "/"
    : "/staff/reservations";
}

export function getLoginDestination(
  role: AccountRole,
  returnTo: unknown,
): string {
  const fallback = getRoleHome(role);

  if (
    typeof returnTo !== "string" ||
    !returnTo.startsWith("/") ||
    returnTo.startsWith("//") ||
    /[\\\u0000-\u0020]/.test(returnTo)
  ) {
    return fallback;
  }

  const pathname = returnTo.split(/[?#]/, 1)[0];

  const publicPath =
    ["/", "/about", "/menu"].includes(pathname) ||
    /^\/menu\/[1-9]\d*$/.test(pathname);

  if (publicPath) return returnTo;

  if (role === "CUSTOMER") {
    const customerPath =
      [
        "/reservation",
        "/reservations",
        "/order",
        "/orders",
        "/change-password",
      ].includes(pathname) ||
      /^\/reservations\/[1-9]\d*(?:\/reschedule)?$/.test(
        pathname,
      ) ||
      /^\/orders\/[1-9]\d*$/.test(pathname);

    if (customerPath) return returnTo;
  }

  if (
    (role === "STAFF" || role === "ADMIN") &&
    ["/staff", "/staff/reservations"].includes(pathname)
  ) {
    return returnTo;
  }

  return fallback;
}