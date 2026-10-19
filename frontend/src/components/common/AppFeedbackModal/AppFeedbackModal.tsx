import {
  useEffect,
  useId,
  useRef,
  type ReactNode,
} from "react";

import { Button } from "../../ui/Button";
import type { ButtonVariant } from "../../ui/Button/Button";

import warningIcon from "../../../assets/reservation/warning.svg";
import closeIcon from "../../../assets/reservation/close.svg";

import "./AppFeedbackModal.css";

interface AppFeedbackModalProps {
  className?: string;
  iconSrc?: string;
  primaryVariant?: ButtonVariant;
  open: boolean;
  title: string;
  children: ReactNode;
  primaryLabel: string;
  secondaryLabel?: string;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function AppFeedbackModal({
  className = "",
  iconSrc = warningIcon,
  primaryVariant = "primary",
  open,
  title,
  children,
  primaryLabel,
  secondaryLabel = "Quay lại chỉnh sửa",
  busy = false,
  onConfirm,
  onClose,
}: AppFeedbackModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;

    if (!open || !dialog) return;

    const trigger = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;

    dialog.showModal();

    dialog
      .querySelector<HTMLButtonElement>(
        "[data-modal-initial-focus]",
      )
      ?.focus();

    document.body.style.overflow = "hidden";

    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;

      // Trả focus về nút đã mở modal.
      trigger?.focus();
    };
  }, [open]);

  useEffect(() => {
    if (open && busy) {
      dialogRef.current?.focus();
    }
  }, [open, busy]);

  return (
    <dialog
      ref={dialogRef}
      className={`app-feedback-modal ${className}`}
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      aria-busy={busy}
      tabIndex={-1}
      onKeyDown={(event) => {
        if (event.key !== "Tab") return;

        const dialog = event.currentTarget;

        const focusable = Array.from(
          dialog.querySelectorAll<HTMLElement>(
            [
              "button:not(:disabled)",
              "a[href]",
              "input:not(:disabled)",
              "select:not(:disabled)",
              "textarea:not(:disabled)",
              "[tabindex]:not([tabindex='-1'])",
            ].join(", "),
          ),
        ).filter(
          (element) => element.getClientRects().length > 0,
        );

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (busy || !first) {
          event.preventDefault();
          dialog.focus();
        } else if (
          event.shiftKey &&
          (
            document.activeElement === first ||
            document.activeElement === dialog
          )
        ) {
          event.preventDefault();
          last.focus();
        } else if (
          !event.shiftKey &&
          (
            document.activeElement === last ||
            document.activeElement === dialog
          )
        ) {
          event.preventDefault();
          first.focus();
        }
      }}
      onCancel={(event) => {
        // Chặn Escape khi đang gửi.
        event.preventDefault();

        if (!busy) {
          onClose();
        }
      }}
      onClick={(event) => {
        if (
          event.target !== event.currentTarget ||
          busy
        ) {
          return;
        }

        const box =
          event.currentTarget.getBoundingClientRect();

        const outside =
          event.clientX < box.left ||
          event.clientX > box.right ||
          event.clientY < box.top ||
          event.clientY > box.bottom;

        if (outside) {
          onClose();
        }
      }}
    >
      <div className="app-feedback-modal__header">
        <span className="app-feedback-modal__severity">
          <img
            src={iconSrc}
            width={24}
            height={24}
            alt=""
          />
        </span>

        <button
          type="button"
          className="app-feedback-modal__close"
          aria-label="Đóng"
          disabled={busy}
          onClick={onClose}
        >
          <img
            src={closeIcon}
            width={16}
            height={16}
            alt=""
          />
        </button>
      </div>

      <div className="app-feedback-modal__content">
        <h2 id={titleId}>{title}</h2>
        <div id={descriptionId}>{children}</div>
      </div>

      <div className="app-feedback-modal__actions">
        <Button
          type="button"
          variant="outline"
          disabled={busy}
          onClick={onClose}
          data-modal-initial-focus
        >
          {secondaryLabel}
        </Button>

        <Button
          type="button"
          loading={busy}
          variant={primaryVariant}
          onClick={onConfirm}
        >
          {primaryLabel}
        </Button>
      </div>
    </dialog>
  );
}
