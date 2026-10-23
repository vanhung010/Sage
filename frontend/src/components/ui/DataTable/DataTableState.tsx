import type { ReactNode } from "react";
import { CalendarDays, CircleAlert } from "lucide-react";

import { Button } from "../Button";

interface DataTableStateProps {
  variant: "empty" | "error";

  title: string;
  description: string;

  icon?: ReactNode;
  onRetry?: () => void;
}

export function DataTableState({
  variant,
  title,
  description,
  icon,
  onRetry,
}: DataTableStateProps) {
  const isError = variant === "error";

  return (
    <div
      className="sage-data-table__state"
      role={isError ? "alert" : "status"}
    >
      <span
        className={[
          "sage-data-table__state-icon",
          isError ? "sage-data-table__state-icon--error" : "",
        ]
          .filter(Boolean)
          .join(" ")}
        aria-hidden="true"
      >
        {icon ??
          (isError ? (
            <CircleAlert size={24} />
          ) : (
            <CalendarDays size={24} />
          ))}
      </span>

      <p className="sage-data-table__state-title">
        {title}
      </p>

      <p className="sage-data-table__state-description">
        {description}
      </p>

      {isError && onRetry && (
        <Button
          type="button"
          variant="outline"
          size="md"
          onClick={onRetry}
        >
          Thử lại
        </Button>
      )}
    </div>
  );
}