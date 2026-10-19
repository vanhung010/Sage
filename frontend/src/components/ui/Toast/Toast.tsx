import { useEffect, useState } from "react";
import { CheckCircle2, X } from "lucide-react";
import { IconButton } from "../IconButton";
import "./Toast.css";

export interface ToastProps {
  message: string;
  title?: string;
  duration?: number;
  onDismiss: () => void;
}

export function Toast({
  message,
  title = "Thành công",
  duration = 3500,
  onDismiss,
}: ToastProps) {
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);

  useEffect(() => {
    if (hovered || focused) return;
    const timer = window.setTimeout(onDismiss, duration);
    return () => window.clearTimeout(timer);
  }, [duration, focused, hovered, onDismiss]);

  return (
    <div
      className="sage-toast"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => setFocused(true)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
          setFocused(false);
        }
      }}
    >
      <span className="sage-toast__icon" aria-hidden="true">
        <CheckCircle2 size={22} strokeWidth={1.8} />
      </span>
      <div className="sage-toast__content">
        <p className="sage-toast__title">{title}</p>
        <p className="sage-toast__message">{message}</p>
      </div>
      <IconButton
        className="sage-toast__close"
        icon={<X size={18} aria-hidden="true" />}
        variant="ghost"
        size="sm"
        aria-label="Đóng thông báo"
        onClick={onDismiss}
      />
    </div>
  );
}
