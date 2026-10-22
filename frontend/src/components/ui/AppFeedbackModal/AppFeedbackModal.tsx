import {
  useEffect,
  useId,
  useRef,
  useState,
  type FormEvent,
  type MouseEvent,
  type ReactNode,
} from "react";

import { Button, type ButtonVariant } from "../Button";
import { IconButton } from "../IconButton";
import { Textarea } from "../Textarea";
import { FormField } from "../FormField";

import successIcon from "../../../assets/feedback/success.svg";
import infoIcon from "../../../assets/feedback/info.svg";
import warningIcon from "../../../assets/feedback/warning.svg";
import errorIcon from "../../../assets/feedback/error.svg";
import confirmIcon from "../../../assets/feedback/confirm.svg";
import closeIcon from "../../../assets/feedback/close.svg";
import loadingIcon from "../../../assets/feedback/loading.svg";
import buttonLoadingIcon from "../../../assets/feedback/button-loading.svg";

import "./AppFeedbackModal.css";

export type FeedbackSeverity =
  | "success"
  | "info"
  | "warning"
  | "error"
  | "confirm"
  | "destructive"
  | "loading";

interface BaseModalProps {
  open: boolean;
  onClose: () => void;

  title: string;
  description?: ReactNode;
  children?: ReactNode;
  severity?: FeedbackSeverity;
  className?: string;

  primaryButtonLabel?: string;
  secondaryButtonLabel?: string;
  primaryVariant?: ButtonVariant;
  primaryDisabled?: boolean;

  // Trang gọi chịu trách nhiệm gửi request và xử lý kết quả.
  onPrimaryAction: (reason: string) => void;

  showCloseButton?: boolean;
  showSecondaryButton?: boolean;
  closeOnBackdropClick?: boolean;

  loading?: boolean;
  loadingLabel?: string;
}

interface ReasonFieldProps {
  showReason: true;

  // Controlled: không mất nội dung khi loading hoặc request lỗi.
  reasonValue: string;
  onReasonChange: (value: string) => void;

  reasonLabel?: string;
  reasonPlaceholder?: string;
  reasonRequired?: boolean;

  showReasonHint?: boolean;
  reasonHint?: string;

  showReasonError?: boolean;
  reasonError?: string;
}

interface WithoutReasonFieldProps {
  showReason?: false;
}

export type AppFeedbackModalProps = BaseModalProps &
  (ReasonFieldProps | WithoutReasonFieldProps);

const severityIcons: Record<FeedbackSeverity, string> = {
  success: successIcon,
  info: infoIcon,
  warning: warningIcon,
  error: errorIcon,
  confirm: confirmIcon,
  destructive: errorIcon,
  loading: loadingIcon,
};

export function AppFeedbackModal(props: AppFeedbackModalProps) {
  const {
    open,
    onClose,
    title,
    description,
    children,
    severity = "info",
    className = "",
    primaryButtonLabel,
    secondaryButtonLabel,
    primaryVariant,
    primaryDisabled = false,
    onPrimaryAction,
    showCloseButton = true,
    showSecondaryButton = true,
    closeOnBackdropClick = false,
    loading = false,
    loadingLabel = "Đang xử lý...",
  } = props;

  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const [submitted, setSubmitted] = useState(false);

  const id = useId();
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;
  const reasonId = `${id}-reason`;
  const reasonHintId = `${id}-reason-hint`;
  const reasonErrorId = `${id}-reason-error`;

  const isBusy = loading || severity === "loading";
  const displayedSeverity = isBusy ? "loading" : severity;
  const reason = props.showReason ? props : undefined;

  const reasonRequired = reason?.reasonRequired ?? true;
  const reasonValue = reason?.reasonValue ?? "";
  const reasonHint =
    reason?.reasonHint ?? "Lý do được lưu trong nhật ký thay đổi.";
  const reasonError =
    reason?.reasonError ?? "Vui lòng nhập lý do trước khi tiếp tục.";

  const missingReason =
    Boolean(reason) && reasonRequired && !reasonValue.trim();

  const hasReasonError =
    Boolean(reason) &&
    (Boolean(reason?.showReasonError) || (submitted && missingReason));

  const showReasonHint =
    Boolean(reason) &&
    (reason?.showReasonHint ?? true) &&
    Boolean(reasonHint);

  const reasonDescribedBy =
    [
      showReasonHint ? reasonHintId : null,
      hasReasonError ? reasonErrorId : null,
    ]
      .filter(Boolean)
      .join(" ") || undefined;

  const isConfirmation =
    severity === "confirm" || severity === "destructive";

  const primaryLabel =
    primaryButtonLabel ??
    (severity === "destructive"
      ? "Xác nhận hủy"
      : severity === "confirm"
        ? "Xác nhận"
        : "Tiếp tục");

  const secondaryLabel =
    secondaryButtonLabel ??
    (isConfirmation || severity === "loading" ? "Quay lại" : "Đóng");

  // Loading dùng nút primary theo variant loading trong Figma.
  const buttonVariant: ButtonVariant = isBusy
    ? "primary"
    : primaryVariant ??
      (severity === "destructive" ? "destructive" : "primary");

  const hasDescription =
    description != null || children != null;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!open || !dialog) return;

    const previousFocus =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;

    const previousOverflow = document.body.style.overflow;

    setSubmitted(false);

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

  useEffect(() => {
    // Tránh giữ focus trên nút vừa bị disabled khi gửi.
    if (open && isBusy) {
      titleRef.current?.focus();
    }
  }, [open, isBusy]);

  function requestClose() {
    if (!isBusy) {
      onClose();
    }
  }

  function handleBackdropClick(event: MouseEvent<HTMLDialogElement>) {
    if (
      isBusy ||
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
      requestClose();
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isBusy || primaryDisabled) return;

    setSubmitted(true);

    if (missingReason || reason?.showReasonError) {
      dialogRef.current
        ?.querySelector<HTMLTextAreaElement>("textarea")
        ?.focus();

      return;
    }

    onPrimaryAction(reasonValue.trim());
  }

  return (
    <dialog
      ref={dialogRef}
      className={[
        "app-feedback-modal",
        `app-feedback-modal--${displayedSeverity}`,
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      aria-labelledby={titleId}
      aria-describedby={hasDescription ? descriptionId : undefined}
      aria-modal="true"
      aria-busy={isBusy}
      tabIndex={-1}
      onCancel={(event) => {
        event.preventDefault();
        requestClose();
      }}
      onClick={handleBackdropClick}
      onKeyDown={(event) => {
        if (isBusy && event.key === "Tab") {
          event.preventDefault();
          titleRef.current?.focus();
        }
      }}
    >
      <form
        className="app-feedback-modal__form"
        onSubmit={handleSubmit}
        noValidate
      >
        <div className="app-feedback-modal__header">
          <span
            className="app-feedback-modal__severity"
            aria-hidden="true"
          >
            <img
              src={severityIcons[displayedSeverity]}
              alt=""
              width={isBusy ? 48 : 24}
              height={isBusy ? 48 : 24}
              className={isBusy ? "app-feedback-modal__spinner" : undefined}
            />
          </span>

          {showCloseButton && !isBusy && (
            <IconButton
              type="button"
              variant="ghost"
              size="sm"
              aria-label="Đóng"
              icon={
                <img
                  src={closeIcon}
                  alt=""
                  width={16}
                  height={16}
                />
              }
              onClick={requestClose}
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

          {hasDescription && (
            <div
              id={descriptionId}
              className="app-feedback-modal__description"
            >
              {description != null && <div>{description}</div>}
              {children}
            </div>
          )}
        </div>

        {reason && (
          <FormField
            className="app-feedback-modal__reason"
            label={reason.reasonLabel ?? "Lý do"}
            required={reasonRequired}
            htmlFor={reasonId}
          >
            <Textarea
              id={reasonId}
              name="reason"
              className="app-feedback-modal__reason-input"
              value={reasonValue}
              onChange={(event) => {
                reason.onReasonChange(event.target.value);
              }}
              placeholder={reason.reasonPlaceholder ?? "Nhập lý do..."}
              required={reasonRequired}
              readOnly={isBusy}
              error={hasReasonError}
              aria-describedby={reasonDescribedBy}
            />

            {showReasonHint && (
              <p
                id={reasonHintId}
                className="app-feedback-modal__hint"
              >
                {reasonHint}
              </p>
            )}

            {hasReasonError && (
              <p
                id={reasonErrorId}
                className="app-feedback-modal__reason-error"
                role="alert"
              >
                {reasonError}
              </p>
            )}
          </FormField>
        )}

        <div className="app-feedback-modal__actions">
          {showSecondaryButton && (
            <Button
              type="button"
              variant="outline"
              size="md"
              className="app-feedback-modal__secondary"
              disabled={isBusy}
              onClick={requestClose}
            >
              {secondaryLabel}
            </Button>
          )}

          <Button
            type="submit"
            variant={buttonVariant}
            size="md"
            className={[
              "app-feedback-modal__primary",
              isBusy ? "app-feedback-modal__primary--loading" : "",
            ]
              .filter(Boolean)
              .join(" ")}
            disabled={isBusy || primaryDisabled}
            aria-busy={isBusy}
            aria-label={isBusy ? loadingLabel : undefined}
          >
            <span className="app-feedback-modal__primary-label">
              {isBusy ? loadingLabel : primaryLabel}
            </span>

            {isBusy && (
              <img
                className="app-feedback-modal__button-spinner"
                src={buttonLoadingIcon}
                alt=""
                width={16}
                height={16}
                aria-hidden="true"
              />
            )}
          </Button>
        </div>
      </form>
    </dialog>
  );
}