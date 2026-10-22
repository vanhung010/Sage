import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";

import {
  useBeforeUnload,
  useBlocker,
  useNavigate,
  useParams,
} from "react-router-dom";

import { areaApi } from "../../../api/area.api";
import { getApiError } from "../../../api/error";
import { reservationApi } from "../../../api/reservation.api";

import { Footer } from "../../../components/layout/Footer";
import { Header } from "../../../components/layout/Header";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { DateInput } from "../../../components/ui/DateInput";
import { FormField } from "../../../components/ui/FormField";
import { NavigationBreadcrumb } from "../../../components/ui/NavigationBreadcrumb";
import { Select } from "../../../components/ui/Select";

import { useReservationDetail } from "../../../hooks/useReservationDetail";
import { AppFeedbackModal } from "../../../components/ui/AppFeedbackModal";
import type { AreaResponse } from "../../../types/area";

import type {
  AvailabilityResponse,
  AvailabilitySlot,
  ReservationResponse,
} from "../../../types/reservation";

import {
  addDays,
  dateInputValue,
  displayDate,
  displayTime,
  remainingReschedules,
  rescheduleIneligibility,
  reservationDeadline,
  reservationFingerprint,
  sameSlot,
  timeRange,
} from "../../../utils/reservation";

import { ReservationPageState } from "../ReservationDetail/ReservationPageState";

import "./ReservationReschedulePage.css";

interface SearchForm {
  date: string;
  partySize: string;
  durationMinutes: string;
  areaId: string;
}

const restaurantInfo = {
  description:
    "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address:
    "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

const partyOptions = Array.from(
  { length: 12 },
  (_, index) => ({
    value: String(index + 1),
    label: `${index + 1} khách`,
  }),
);

const durationOptions = Array.from(
  { length: 9 },
  (_, index) => ({
    value: String(60 + index * 15),
    label: `${60 + index * 15} phút`,
  }),
);

const eligibilityCodes = new Set([
  "RESERVATION_NOT_RESCHEDULABLE",
  "RESCHEDULE_LIMIT_REACHED",
  "RESCHEDULE_DEADLINE_PASSED",
]);

function InfoRows({
  rows,
}: {
  rows: [string, string][];
}) {
  return (
    <dl className="reschedule-page__rows">
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function ReservationReschedulePage() {
  const { id } = useParams();
  const detail = useReservationDetail(Number(id));

  if (detail.loading) {
    return (
      <ReservationPageState
        message="Đang tải đơn đặt bàn…"
        loading
      />
    );
  }

  if (detail.error || !detail.reservation) {
    return (
      <ReservationPageState
        message={
          detail.error || "Không tìm thấy đơn đặt bàn"
        }
        onRetry={detail.reload}
      />
    );
  }

  return (
    <RescheduleEditor
      key={detail.reservation.id}
      initialReservation={detail.reservation}
    />
  );
}

function RescheduleEditor({
  initialReservation,
}: {
  initialReservation: ReservationResponse;
}) {
  const navigate = useNavigate();

  const [reservation, setReservation] =
    useState(initialReservation);

  // Chỉ điền sẵn tiêu chí tìm kiếm.
  // Không tự coi giờ của lịch cũ là một giờ mới đã chọn.
  const [form, setForm] = useState<SearchForm>(() => ({
    date: dateInputValue(initialReservation.startTime),
    partySize: String(initialReservation.partySize),

    durationMinutes: String(
      Math.round(
        (
          Date.parse(initialReservation.endTime) -
          Date.parse(initialReservation.startTime)
        ) / 60_000,
      ),
    ),

    areaId: initialReservation.requestedArea
      ? String(initialReservation.requestedArea.id)
      : "",
  }));

  const [areas, setAreas] = useState<AreaResponse[]>([]);
  const [areasLoading, setAreasLoading] = useState(true);
  const [areaError, setAreaError] = useState("");
  const [areaAttempt, setAreaAttempt] = useState(0);

  const [availability, setAvailability] =
    useState<AvailabilityResponse | null>(null);

  const [selectedSlot, setSelectedSlot] =
    useState<AvailabilitySlot | null>(null);

  const [lostSlot, setLostSlot] =
    useState<AvailabilitySlot | null>(null);

  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  const [feedback, setFeedback] = useState("");
  const [serverBlock, setServerBlock] = useState("");
  const [uncertain, setUncertain] = useState(false);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [now, setNow] = useState(Date.now);

  const searchRef =
    useRef<AbortController | null>(null);

  // Ref chặn gửi lặp ngay cả trước khi React render lại.
  const submittingRef = useRef(false);
  const committedRef = useRef(false);

  const blocker = useBlocker(
    () =>
      submittingRef.current &&
      !committedRef.current,
  );

  useBeforeUnload((event) => {
    if (
      submittingRef.current &&
      !committedRef.current
    ) {
      event.preventDefault();
      event.returnValue = "";
    }
  });

  useEffect(() => {
    if (blocker.state === "blocked") {
      blocker.reset();
    }
  }, [blocker]);

  useEffect(() => {
    const interval = window.setInterval(
      () => setNow(Date.now()),
      1000,
    );

    return () => {
      window.clearInterval(interval);
      searchRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    areaApi
      .list(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setAreas(data);
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setAreaError(getApiError(error).message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setAreasLoading(false);
        }
      });

    return () => controller.abort();
  }, [areaAttempt]);

  const detailUrl = `/reservations/${reservation.id}`;
  const remaining = remainingReschedules(reservation);

  const blockedReason =
    serverBlock ||
    rescheduleIneligibility(reservation, now);

  const locked =
    submitting ||
    Boolean(blockedReason) ||
    uncertain;

  const today = dateInputValue(now);

  const areaValid =
    !form.areaId ||
    areas.some(
      (area) => String(area.id) === form.areaId,
    );

  const selectedAreaName = form.areaId
    ? (
        areas.find(
          (area) => String(area.id) === form.areaId,
        )?.name ??
        reservation.requestedArea?.name ??
        "Khu vực không còn khả dụng"
      )
    : "Không yêu cầu";

  const areaOptions = [
    {
      value: "",
      label: "Không yêu cầu",
    },

    ...areas.map((area) => ({
      value: String(area.id),
      label: area.name,
    })),
  ];

  // Vẫn hiển thị lựa chọn cũ để khách hiểu tại sao phải chọn lại.
  if (!areaValid && reservation.requestedArea) {
    areaOptions.push({
      value: form.areaId,
      label:
        `${reservation.requestedArea.name} ` +
        "(không còn khả dụng)",
    });
  }

  const unchanged = Boolean(
    selectedSlot &&
    sameSlot(selectedSlot, reservation) &&
    Number(form.partySize) === reservation.partySize &&
    (form.areaId ? Number(form.areaId) : null) ===
      (reservation.requestedArea?.id ?? null),
  );

  const canContinue =
    !locked &&
    !searching &&
    !areasLoading &&
    !areaError &&
    areaValid &&
    selectedSlot !== null &&
    !unchanged;

  const deadline = new Date(
    reservationDeadline(reservation),
  ).toISOString();

  function params(criteria: SearchForm) {
    return {
      date: criteria.date,
      partySize: Number(criteria.partySize),
      durationMinutes: Number(criteria.durationMinutes),

      areaId: criteria.areaId
        ? Number(criteria.areaId)
        : undefined,
    };
  }

  function updateField(
    field: keyof SearchForm,
    value: string,
  ) {
    // Response của tiêu chí cũ không được ghi đè tiêu chí mới.
    searchRef.current?.abort();

    setSearching(false);

    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    setAvailability(null);
    setSelectedSlot(null);
    setLostSlot(null);
    setSearchError("");
    setFeedback("");
  }

  async function search(event: FormEvent) {
    event.preventDefault();

    if (
      locked ||
      searching ||
      areasLoading ||
      areaError ||
      !areaValid
    ) {
      return;
    }

    const eligibility =
      rescheduleIneligibility(reservation);

    if (eligibility) {
      setServerBlock(eligibility);
      return;
    }

    searchRef.current?.abort();

    const controller = new AbortController();
    searchRef.current = controller;

    setSearching(true);
    setAvailability(null);
    setSelectedSlot(null);
    setLostSlot(null);
    setFeedback("");
    setSearchError("");

    try {
      const response =
        await reservationApi.getRescheduleAvailability(
          reservation.id,
          params(form),
          controller.signal,
        );

      if (!controller.signal.aborted) {
        setAvailability(response);
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        const problem = getApiError(error);

        setSearchError(problem.message);

        if (
          eligibilityCodes.has(problem.code) ||
          problem.status === 404
        ) {
          setServerBlock(problem.message);
        }
      }
    } finally {
      if (!controller.signal.aborted) {
        setSearching(false);
      }
    }
  }

  function openConfirmation() {
    if (!canContinue) return;

    const reason =
      rescheduleIneligibility(reservation);

    if (reason) {
      setServerBlock(reason);
      return;
    }

    setFeedback("");
    setConfirmOpen(true);
  }

  async function confirm() {
    if (
      submittingRef.current ||
      !selectedSlot
    ) {
      return;
    }

    const reason =
      rescheduleIneligibility(reservation);

    if (reason) {
      setServerBlock(reason);
      setConfirmOpen(false);
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setFeedback("");

    let mutationStarted = false;

    try {
      // Kiểm tra lại đơn trước khi gửi thay đổi.
      const fresh =
        await reservationApi.getDetail(reservation.id);

      const eligibility =
        rescheduleIneligibility(fresh);

      if (eligibility) {
        setReservation(fresh);
        setServerBlock(eligibility);
        setConfirmOpen(false);
        return;
      }

      // Đơn có thể vừa thay đổi trong tab/request khác.
      if (
        reservationFingerprint(fresh) !==
        reservationFingerprint(reservation)
      ) {
        setReservation(fresh);
        setAvailability(null);
        setSelectedSlot(null);
        setConfirmOpen(false);

        setFeedback(
          "Đơn vừa được cập nhật. Vui lòng kiểm tra lịch hiện tại và tìm lại giờ trống.",
        );

        return;
      }

      // Giờ đã chọn phải còn trong kết quả kiểm tra mới nhất.
      const slots =
        await reservationApi.getRescheduleAvailability(
          reservation.id,
          params(form),
        );

      const validSlot = slots.slots.find(
        (slot) => sameSlot(slot, selectedSlot),
      );

      setAvailability(slots);

      if (!validSlot) {
        setLostSlot(selectedSlot);
        setSelectedSlot(null);
        setConfirmOpen(false);

        setFeedback(
          "Khung giờ này vừa hết chỗ. Vui lòng chọn giờ khác.",
        );

        return;
      }

      mutationStarted = true;

      const updated = await reservationApi.reschedule(
        reservation.id,
        {
          startTime: validSlot.startTime,
          endTime: validSlot.endTime,
          partySize: Number(form.partySize),

          requestedAreaId: form.areaId
            ? Number(form.areaId)
            : null,
        },
      );

      committedRef.current = true;
      submittingRef.current = false;

      // Giữ nguyên ID của đơn, quay lại trang chi tiết.
      navigate(detailUrl, {
        replace: true,
        state: {
          rescheduled: updated,
        },
      });
    } catch (error) {
      const problem = getApiError(error);

      setConfirmOpen(false);

      if (
        mutationStarted &&
        (
          problem.status === 0 ||
          problem.status >= 500
        )
      ) {
        // Timeout có thể xảy ra sau khi server đã commit.
        // Không tự gửi lại POST và làm tăng số lần đổi.
        setUncertain(true);

        setFeedback(
          "Chưa xác định được kết quả đổi lịch. Vui lòng về chi tiết đơn để kiểm tra trước khi thử lại.",
        );
      } else {
        setFeedback(problem.message);

        if (
          eligibilityCodes.has(problem.code) ||
          problem.status === 404
        ) {
          setServerBlock(problem.message);
        }

        if (
          problem.status === 409 ||
          problem.status === 400
        ) {
          setLostSlot(selectedSlot);
          setSelectedSlot(null);
          setAvailability(null);
        }
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="reschedule-page">
      <Header
        variant="customer"
        active="reservation"
      />

      <main>
        <section className="reschedule-page__heading">
          <div className="container">
            <NavigationBreadcrumb
              items={[
                {
                  label: "Đơn đặt bàn của tôi",
                  to: "/reservations",
                },
                {
                  label: "Chi tiết đơn",
                  to: detailUrl,
                },
                {
                  label: "Đổi lịch",
                },
              ]}
            />

            <h1>Đổi lịch đặt bàn</h1>

            <p>
              Chọn lịch hẹn mới phù hợp với bạn. Lịch hiện
              tại được giữ cho đến khi thay đổi thành công.
            </p>
          </div>
        </section>

        <div className="container reschedule-page__content">
          {(blockedReason || uncertain) && (
            <div
              className="reschedule-page__notice reschedule-page__notice--warning"
              role="alert"
            >
              <h2>
                {uncertain
                  ? "Kiểm tra kết quả đổi lịch"
                  : "Không còn đủ điều kiện đổi lịch"}
              </h2>

              <p>{blockedReason || feedback}</p>

              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(detailUrl)}
              >
                Về chi tiết đơn
              </Button>
            </div>
          )}

          <div className="reschedule-page__grid">
            <div className="reschedule-page__column">
              <section className="reschedule-page__card reschedule-page__editor">
                <div>
                  <h2>Chọn lịch hẹn mới</h2>

                  <p className="reschedule-page__intro">
                    Chọn ngày, số khách và khu vực để tìm
                    giờ phù hợp.
                  </p>
                </div>

                <form
                  onSubmit={(event) => void search(event)}
                >
                  <fieldset disabled={locked}>
                    <legend className="reschedule-page__sr-only">
                      Tiêu chí tìm giờ trống
                    </legend>

                    <div className="reschedule-page__fields">
                      <FormField
                        label="Ngày đặt bàn"
                        required
                        htmlFor="reschedule-date"
                      >
                        <DateInput
                          id="reschedule-date"
                          name="reschedule-date"
                          placeholder="Ngày đặt bàn"
                          size="lg"
                          value={form.date}
                          min={today}
                          max={addDays(today, 30)}
                          required
                          disabled={locked}
                          onChange={(value) =>
                            updateField("date", value)
                          }
                        />
                      </FormField>

                      <FormField
                        label="Số khách"
                        required
                        htmlFor="reschedule-party"
                      >
                        <Select
                          name="reschedule-party"
                          size="lg"
                          value={form.partySize}
                          options={partyOptions}
                          disabled={locked}
                          aria-label="Số khách"
                          onValueChange={(value) =>
                            updateField("partySize", value)
                          }
                        />
                      </FormField>

                      <FormField
                        label="Thời lượng"
                        required
                        htmlFor="reschedule-duration"
                      >
                        <Select
                          name="reschedule-duration"
                          size="lg"
                          value={form.durationMinutes}
                          options={durationOptions}
                          disabled={locked}
                          aria-label="Thời lượng"
                          onValueChange={(value) =>
                            updateField(
                              "durationMinutes",
                              value,
                            )
                          }
                        />
                      </FormField>

                      <FormField
                        label="Khu vực"
                        supportingText="Không bắt buộc"
                        htmlFor="reschedule-area"
                      >
                        <Select
                          name="reschedule-area"
                          size="lg"
                          value={form.areaId}
                          options={areaOptions}
                          aria-label="Khu vực"
                          placeholder={
                            areasLoading
                              ? "Đang tải khu vực…"
                              : "Không yêu cầu"
                          }
                          disabled={
                            locked ||
                            areasLoading ||
                            Boolean(areaError)
                          }
                          onValueChange={(value) =>
                            updateField("areaId", value)
                          }
                        />
                      </FormField>
                    </div>

                    {areaError && (
                      <div
                        className="reschedule-page__field-error"
                        role="alert"
                      >
                        <p>
                          Không tải được khu vực: {areaError}
                        </p>

                        <Button
                          type="button"
                          variant="outline"
                          onClick={() => {
                            setAreasLoading(true);
                            setAreaError("");

                            setAreaAttempt(
                              (value) => value + 1,
                            );
                          }}
                        >
                          Tải lại khu vực
                        </Button>
                      </div>
                    )}

                    <div className="reschedule-page__search-action">
                      <Button
                        type="submit"
                        size="lg"
                        loading={searching}
                        disabled={
                          locked ||
                          areasLoading ||
                          Boolean(areaError) ||
                          !areaValid ||
                          !form.date
                        }
                      >
                        {searching
                          ? "Đang tìm giờ…"
                          : "Tìm giờ trống"}
                      </Button>
                    </div>
                  </fieldset>
                </form>

                <section
                  className="reschedule-page__availability"
                  aria-label="Kết quả tìm giờ trống"
                  aria-busy={searching}
                >
                  <div className="reschedule-page__availability-heading">
                    <h2>Giờ còn trống</h2>

                    <span>
                      {displayDate(
                        availability?.date ?? form.date,
                      )}
                    </span>
                  </div>

                  {searching ? (
                    <div
                      className="reschedule-page__notice"
                      role="status"
                    >
                      <h3>Đang tìm giờ phù hợp…</h3>

                      <p>
                        Vui lòng chờ trong giây lát. Chưa
                        chọn giờ hẹn mới.
                      </p>
                    </div>
                  ) : searchError ? (
                    <p
                      className="reschedule-page__error"
                      role="alert"
                    >
                      {searchError}
                    </p>
                  ) : !availability ? (
                    <div
                      className="reschedule-page__notice"
                      role="status"
                    >
                      <h3>
                        {lostSlot
                          ? "Vui lòng chọn lại giờ hẹn"
                          : "Chọn tiêu chí, rồi tìm giờ trống"}
                      </h3>

                      <p>
                        {lostSlot
                          ? "Tìm lại giờ trống để chọn lịch mới phù hợp."
                          : "Lịch hiện tại chưa được xác thực là giờ mới."}
                      </p>
                    </div>
                  ) : availability.slots.length === 0 ? (
                    <div
                      className="reschedule-page__notice"
                      role="status"
                    >
                      <h3>Chưa có giờ phù hợp</h3>

                      <p>
                        Bạn có thể chọn ngày, thời lượng
                        hoặc khu vực khác.
                      </p>
                    </div>
                  ) : (
                    <>
                      <p>
                        Chọn một giờ bắt đầu · Thời lượng{" "}
                        {availability.durationMinutes} phút
                      </p>

                      <div className="reschedule-page__slots">
                        {availability.slots.map((slot) => (
                          <button
                            key={slot.startTime}
                            type="button"
                            className="reschedule-page__slot"
                            aria-pressed={Boolean(
                              selectedSlot &&
                              sameSlot(slot, selectedSlot),
                            )}
                            disabled={locked}
                            onClick={() => {
                              setSelectedSlot(slot);
                              setLostSlot(null);
                              setFeedback("");
                            }}
                          >
                            {displayTime(slot.startTime)}
                          </button>
                        ))}
                      </div>
                    </>
                  )}

                  {lostSlot && !selectedSlot && (
                    <div className="reschedule-page__lost-slot">
                      <button
                        type="button"
                        className="reschedule-page__slot"
                        disabled
                      >
                        {displayTime(lostSlot.startTime)}
                      </button>

                      <span>
                        Giờ đã chọn vừa hết chỗ hoặc không
                        còn hợp lệ.
                      </span>
                    </div>
                  )}

                  <div
                    className="reschedule-page__selected"
                    aria-live="polite"
                  >
                    <strong>
                      Giờ hẹn mới:{" "}
                      {selectedSlot
                        ? timeRange(selectedSlot)
                        : "Chưa chọn giờ"}
                    </strong>

                    <p>
                      Bàn cụ thể sẽ được Sage sắp xếp dựa
                      trên số khách và tình trạng bàn trống.
                    </p>
                  </div>

                  <p>
                    Nếu thay đổi thông tin ở trên, vui
                    lòng tìm giờ trống và chọn lại giờ hẹn.
                  </p>

                  {unchanged && (
                    <p
                      className="reschedule-page__error"
                      role="status"
                    >
                      Lịch mới giống lịch hiện tại. Vui
                      lòng thay đổi thông tin trước khi
                      tiếp tục.
                    </p>
                  )}

                  {feedback && !uncertain && (
                    <p
                      className="reschedule-page__error"
                      role="alert"
                    >
                      {feedback}
                    </p>
                  )}
                </section>
              </section>

              <aside className="reschedule-page__card reschedule-page__note">
                <h3>Ghi chú đặc biệt · Giữ nguyên</h3>

                <p>
                  {reservation.specialNote ||
                    "Không có ghi chú đặc biệt."}
                </p>
              </aside>
            </div>

            <aside className="reschedule-page__column">
              <section className="reschedule-page__card reschedule-page__comparison">
                <h2>Lịch hẹn hiện tại</h2>

                <div className="reschedule-page__identity">
                  <span>Mã đặt bàn</span>

                  <strong>{reservation.code}</strong>

                  <Badge
                    variant={
                      reservation.status === "CONFIRMED"
                        ? "success"
                        : "warning"
                    }
                  >
                    {reservation.status === "CONFIRMED"
                      ? "Đã xác nhận"
                      : reservation.status === "PENDING"
                        ? "Chờ duyệt"
                        : "Không thể đổi lịch"}
                  </Badge>
                </div>

                <InfoRows
                  rows={[
                    [
                      "Ngày đặt bàn",
                      displayDate(
                        dateInputValue(
                          reservation.startTime,
                        ),
                      ),
                    ],
                    [
                      "Thời gian",
                      timeRange(reservation),
                    ],
                    [
                      "Số khách",
                      `${reservation.partySize} khách`,
                    ],
                    [
                      "Khu vực yêu cầu",
                      reservation.requestedArea?.name ??
                        "Không yêu cầu",
                    ],
                  ]}
                />

                <p>Bạn còn {remaining} lần đổi lịch.</p>
              </section>

              <section className="reschedule-page__card reschedule-page__comparison">
                <h2>Lịch hẹn mới</h2>

                <p>Thông tin bạn sắp xác nhận</p>

                <InfoRows
                  rows={[
                    [
                      "Ngày đặt bàn",
                      displayDate(form.date),
                    ],
                    [
                      "Thời gian",
                      selectedSlot
                        ? timeRange(selectedSlot)
                        : "Chưa chọn giờ",
                    ],
                    [
                      "Số khách",
                      `${form.partySize} khách`,
                    ],
                    [
                      "Thời lượng",
                      `${form.durationMinutes} phút`,
                    ],
                    [
                      "Khu vực yêu cầu",
                      selectedAreaName,
                    ],
                  ]}
                />

                <div className="reschedule-page__allowance">
                  Sau khi đổi thành công, bạn còn{" "}
                  {Math.max(0, remaining - 1)} lần đổi lịch.
                </div>

                {Number(form.partySize) >= 7 && (
                  <div
                    className="reschedule-page__approval"
                    role="status"
                  >
                    <Badge variant="warning">
                      Chờ duyệt
                    </Badge>

                    <p>
                      Sau khi đổi lịch thành công, đơn{" "}
                      {form.partySize} khách sẽ chờ nhà
                      hàng duyệt lại.
                    </p>
                  </div>
                )}

                <Button
                  type="button"
                  size="lg"
                  fullWidth
                  disabled={!canContinue}
                  onClick={openConfirmation}
                >
                  Tiếp tục
                </Button>

                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  fullWidth
                  disabled={submitting}
                  onClick={() => navigate(detailUrl)}
                >
                  {uncertain
                    ? "Về chi tiết đơn"
                    : "Giữ lịch cũ"}
                </Button>

                <p>
                  Có thể đổi lịch đến{" "}
                  {displayTime(deadline)} ngày{" "}
                  {displayDate(dateInputValue(deadline))},
                  ít nhất 2 giờ trước lịch hẹn hiện tại.
                  Mỗi đơn được đổi tối đa 2 lần.
                </p>

                <p>
                  Trạng thái sẽ được xét lại sau khi đổi
                  lịch. Nhóm từ 7 khách cần nhà hàng duyệt.
                </p>
              </section>
            </aside>
          </div>
        </div>
      </main>

      <Footer restaurantInfo={restaurantInfo} />

      <AppFeedbackModal
  open={confirmOpen}
  severity="confirm"
  title="Xác nhận đổi lịch?"
  primaryButtonLabel="Xác nhận đổi lịch"
  secondaryButtonLabel="Quay lại chỉnh sửa"
  primaryDisabled={!selectedSlot}
  loading={submitting}
  loadingLabel="Đang đổi lịch..."
  onPrimaryAction={() => void confirm()}
  onClose={() => {
    if (!submittingRef.current) {
      setConfirmOpen(false);
    }
  }}
>
  <p>Đơn {reservation.code}</p>

  <p>
    Lịch hiện tại:{" "}
    {displayDate(dateInputValue(reservation.startTime))}
    {" · "}
    {timeRange(reservation)}.
  </p>

  {selectedSlot && (
    <p>
      Lịch mới: {displayDate(form.date)}
      {" · "}
      {timeRange(selectedSlot)}.
    </p>
  )}

  <p>
    {form.partySize} khách · {form.durationMinutes} phút
    {" · "}
    {selectedAreaName}.
  </p>

  <p>
    Sau khi đổi thành công, bạn còn{" "}
    {Math.max(0, remaining - 1)} lần đổi lịch.
  </p>

  {Number(form.partySize) >= 7 && (
    <p>Đơn sẽ chờ nhà hàng duyệt lại.</p>
  )}
</AppFeedbackModal>
    </div>
  );
}