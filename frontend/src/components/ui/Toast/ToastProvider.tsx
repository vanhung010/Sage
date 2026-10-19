import {
  useCallback,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { Toast, type ToastProps } from "./Toast";
import { ToastContext } from "./useToast";

type ToastOptions = Omit<ToastProps, "onDismiss">;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<(ToastOptions & { id: number }) | null>(null);
  const nextId = useRef(0);
  const showToast = useCallback((options: ToastOptions) => {
    setToast({ ...options, id: ++nextId.current });
  }, []);
  const dismissToast = useCallback(() => setToast(null), []);

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      {createPortal(
        <div
          className="sage-toast-viewport"
          role="status"
          aria-live="polite"
          aria-atomic="true"
        >
          {toast && <Toast key={toast.id} {...toast} onDismiss={dismissToast} />}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

