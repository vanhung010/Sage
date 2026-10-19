import { createContext, useContext } from "react";
import type { ToastProps } from "./Toast";

export const ToastContext = createContext<
  ((options: Omit<ToastProps, "onDismiss">) => void) | null
>(null);

export function useToast() {
  const showToast = useContext(ToastContext);
  if (!showToast) throw new Error("useToast phải nằm trong ToastProvider");
  return showToast;
}
