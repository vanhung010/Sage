import { useId } from "react";

import { Button } from "../Button";

import infoRail from "../../../assets/activity-timeline/info.svg";
import successRail from "../../../assets/activity-timeline/success.svg";
import warningRail from "../../../assets/activity-timeline/warning.svg";
import errorRail from "../../../assets/activity-timeline/error.svg";

import "./ActivityTimeline.css";

export type ActivityTimelineTone =
  | "info"
  | "success"
  | "warning"
  | "error";

export type ActivityTimelineState =
  | "default"
  | "loading"
  | "empty"
  | "error";

export interface ActivityTimelineEvent {
  id: string | number;
  title: string;
  occurredAt: string;

  actorName: string;
  actorRole?: "STAFF" | "ADMIN" | "CUSTOMER" | null;

  description?: string | null;
  reason?: string | null;
  tone?: ActivityTimelineTone;
}

export interface ActivityTimelineProps {
  events: readonly ActivityTimelineEvent[];
  title?: string;
  description?: string;

  state?: ActivityTimelineState;
  errorMessage?: string;
  onRetry?: () => void;

  className?: string;
}

interface ActivityTimelineItemProps {
  event: ActivityTimelineEvent;
  isLast: boolean;
}

const rails: Record<ActivityTimelineTone, string> = {
  info: infoRail,
  success: successRail,
  warning: warningRail,
  error: errorRail,
};

const dateFormatter = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

const timeFormatter = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

function ActivityTimelineItem({
  event,
  isLast,
}: ActivityTimelineItemProps) {
  const date = new Date(event.occurredAt);
  const validDate = Number.isFinite(date.getTime());

  const tone = event.tone ?? "info";
  const reason = event.reason?.trim();

  return (
    <li
      className={[
        "activity-timeline__item",
        isLast ? "activity-timeline__item--last" : "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <div
        className="activity-timeline__rail"
        aria-hidden="true"
      >
        <img
          src={rails[tone]}
          alt=""
          width={16}
          height={116}
        />
      </div>

      <div className="activity-timeline__event">
        <p className="activity-timeline__event-title">
          {event.title}
        </p>

        <p className="activity-timeline__metadata">
          {validDate ? (
            <time dateTime={date.toISOString()}>
              {dateFormatter.format(date)}
              {" • "}
              {timeFormatter.format(date)}
            </time>
          ) : (
            <span>Chưa có thời gian</span>
          )}

          {" • "}
          {event.actorName.trim() || "Hệ thống"}

          {event.actorRole && ` (${event.actorRole})`}
        </p>

        {event.description && (
          <p className="activity-timeline__event-description">
            {event.description}
          </p>
        )}

        {reason && (
          <p className="activity-timeline__reason">
            Lý do: {reason}
          </p>
        )}
      </div>
    </li>
  );
}

export function ActivityTimeline({
  events,
  title = "Lịch sử thay đổi",
  description,
  state = "default",
  errorMessage =
    "Kiểm tra kết nối rồi thử lại. Dữ liệu đã nhập được giữ nguyên.",
  onRetry,
  className = "",
}: ActivityTimelineProps) {
  const titleId = useId();

  const currentState =
    state === "default" && events.length === 0
      ? "empty"
      : state;

  const loading = currentState === "loading";
  const error = currentState === "error";

  return (
    <section
      className={`activity-timeline ${className}`.trim()}
      aria-labelledby={titleId}
      aria-busy={loading}
    >
      <h2
        id={titleId}
        className="activity-timeline__title"
      >
        {title}
      </h2>

      {description && (
        <p className="activity-timeline__description">
          {description}
        </p>
      )}

      {currentState === "default" ? (
        <>
          <ol className="activity-timeline__list" role="list">
            {events.map((event, index) => (
              <ActivityTimelineItem
                key={event.id}
                event={event}
                isLast={index === events.length - 1}
              />
            ))}
          </ol>

          <p className="activity-timeline__timezone">
            Giờ hiển thị: Việt Nam (UTC+7)
          </p>
        </>
      ) : (
        <div
          className="activity-timeline__state"
          role={error ? "alert" : "status"}
        >
          <p className="activity-timeline__state-title">
            {loading
              ? "Đang tải…"
              : error
                ? "Không tải được hoạt động"
                : "Chưa có hoạt động"}
          </p>

          <p className="activity-timeline__state-description">
            {loading
              ? "Vui lòng chờ trong giây lát."
              : error
                ? errorMessage
                : "Dữ liệu sẽ xuất hiện tại đây khi có phát sinh."}
          </p>

          {loading && (
            <div
              className="activity-timeline__skeleton"
              aria-hidden="true"
            >
              <span />
              <span />
              <span />
            </div>
          )}

          {error && onRetry && (
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
      )}
    </section>
  );
}