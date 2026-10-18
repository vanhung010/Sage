import {
  useEffect,
  useId,
  useRef,
  type MouseEvent,
} from "react";
import {
  CircleAlert,
  CircleCheck,
  CircleX,
  Info,
} from "lucide-react";

import { Button } from "../Button";
import { IconButton } from "../IconButton";

import "./AppFeedbackModal.css";

export type FeedbackSeverity =
  | "success"
  | "info"
  | "warning"
  | "error";

export interface AppFeedbackModalProps {
  open: boolean;
  onClose: () => void;

  title: string;
  description: string;
  severity?: FeedbackSeverity;

  primaryButtonLabel?: string;
  secondaryButtonLabel?: string;
  onPrimaryAction: () => void;

  showCloseButton?: boolean;
  showSecondaryButton?: boolean;

  loading?: boolean;
  loadingLabel?: string;
  closeOnBackdropClick?: boolean;
}

const severityIcons = {
  success: CircleCheck,
  info: Info,
  warning: CircleAlert,
  error: CircleX,
};

export function AppFeedbackModal({
  open,
  onClose,
  title,
  description,
  severity = "error",
  primaryButtonLabel = "Tiếp tục",
  secondaryButtonLabel = "Đóng",
  onPrimaryAction,
  showCloseButton = true,
  showSecondaryButton = true,
  loading = false,
  loadingLabel = "Đang xử lý...",
  closeOnBackdropClick = false,
}: AppFeedbackModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);

  const titleId = useId();
  const descriptionId = useId();

  const SeverityIcon = severityIcons[severity];

  useEffect(() => {
    const dialog = dialogRef.current;

    if (!open || !dialog) {
      return;
    }

    const previousFocus =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    const previousOverflow = document.body.style.overflow;

    if (!dialog.open) {
      dialog.showModal();
    }

    document.body.style.overflow = "hidden";
    titleRef.current?.focus();

    return () => {
      if (dialog.open) {
        dialog.close();
      }

      document.body.style.overflow = previousOverflow;

      if (previousFocus?.isConnected) {
        previousFocus.focus();
      }
    };
  }, [open]);

  function handleBackdropClick(
    event: MouseEvent<HTMLDialogElement>,
  ) {
    if (
      loading ||
      !closeOnBackdropClick ||
      event.target !== event.currentTarget
    ) {
      return;
    }

    const bounds = event.currentTarget.getBoundingClientRect();

    const outside =
      event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom;

    if (outside) {
      onClose();
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className={`app-feedback-modal app-feedback-modal--${severity}`}
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      aria-modal="true"
      aria-busy={loading}
      onCancel={(event) => {
        // Kiểm soát đóng bằng prop open, kể cả khi nhấn Escape.
        event.preventDefault();

        if (!loading) {
          onClose();
        }
      }}
      onClick={handleBackdropClick}
    >
      <div className="app-feedback-modal__header">
        <span
          className="app-feedback-modal__severity"
          aria-hidden="true"
        >
          <SeverityIcon size={24} strokeWidth={1.5} />
        </span>

        {showCloseButton && (
          <IconButton
            type="button"
            variant="ghost"
            size="sm"
            aria-label="Đóng"
            icon={<CircleX size={16} strokeWidth={1.5} />}
            disabled={loading}
            onClick={onClose}
          />
        )}
      </div>

      <div className="app-feedback-modal__content">
        <h2
          ref={titleRef}
          id={titleId}
          className="app-feedback-modal__title"
          tabIndex={-1}
        >
          {title}
        </h2>

        <p
          id={descriptionId}
          className="app-feedback-modal__description"
        >
          {description}
        </p>
      </div>

      <div className="app-feedback-modal__actions">
        {showSecondaryButton && (
          <Button
            type="button"
            variant="outline"
            size="md"
            className="app-feedback-modal__secondary"
            disabled={loading}
            onClick={onClose}
          >
            {secondaryButtonLabel}
          </Button>
        )}

        <Button
          type="button"
          variant="primary"
          size="md"
          className="app-feedback-modal__primary"
          loading={loading}
          onClick={onPrimaryAction}
        >
          {loading ? loadingLabel : primaryButtonLabel}
        </Button>
      </div>
    </dialog>
  );
}