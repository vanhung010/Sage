import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useAuth } from "../../auth/AuthContext";
import { getApiError } from "../../api/error";
import {
  reservationApi,
  type AvailabilityResponse,
  type AvailabilitySlot,
  type ReservationResponse,
} from "../../api/reservation.api";

import { Header } from "../../components/layout/Header";
import {
  Footer,
  type FooterRestaurantInfo,
} from "../../components/layout/Footer";
import { Button } from "../../components/ui/Button";
import { FormField } from "../../components/ui/FormField";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { DateInput } from "../../components/ui/DateInput";

import { areaApi } from "../../api/area.api";
import type { AreaResponse, PublicArea } from "../../types/area";

import "./ReservationPage.css";



interface SearchForm {
  date: string;
  partySize: string;
  durationMinutes: string;
  areaId: string;
  specialNote: string;
}

const DRAFT_KEY = "sage-reservation-draft";

const initialForm: SearchForm = {
  date: "",
  partySize: "2",
  durationMinutes: "90",
  areaId: "",
  specialNote: "",
};

const restaurantInfo: FooterRestaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

const partySizeOptions = Array.from({ length: 12 }, (_, index) => {
  const count = index + 1;

  return {
    value: String(count),
    label: `${count} người`,
  };
});

const durationOptions = Array.from({ length: 9 }, (_, index) => {
  const minutes = 60 + index * 15;

  return {
    value: String(minutes),
    label: `${minutes} phút`,
  };
});

function loadDraft(): SearchForm {
  try {
    const stored = sessionStorage.getItem(DRAFT_KEY);
    if (!stored) return initialForm;

    const parsed = JSON.parse(stored) as Partial<SearchForm>;

    return {
      date: parsed.date ?? "",
      partySize: parsed.partySize ?? "2",
      durationMinutes: parsed.durationMinutes ?? "90",
      areaId: parsed.areaId ?? "",
      specialNote: parsed.specialNote ?? "",
    };
  } catch {
    return initialForm;
  }
}

function todayInVietnam(): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());

  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value ?? "";

  return `${part("year")}-${part("month")}-${part("day")}`;
}

function addDays(date: string, days: number): string {
  const [year, month, day] = date.split("-").map(Number);

  return new Date(Date.UTC(year, month - 1, day + days))
    .toISOString()
    .slice(0, 10);
}

function displayDate(date: string): string {
  if (!date) return "—";

  const [year, month, day] = date.split("-");
  return `${day}/${month}/${year}`;
}

function displayLongDate(date: string): string {
  const dateAtNoon = new Date(`${date}T12:00:00+07:00`);

  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(dateAtNoon);
}

function displayTime(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(value));
}

export function ReservationPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [form, setForm] = useState<SearchForm>(loadDraft);
  const [availability, setAvailability] =
    useState<AvailabilityResponse | null>(null);
  const [selectedSlot, setSelectedSlot] =
    useState<AvailabilitySlot | null>(null);

  const [searching, setSearching] = useState(false);
  const [booking, setBooking] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [bookingError, setBookingError] = useState("");
  const [createdReservation, setCreatedReservation] =
    useState<ReservationResponse | null>(null);


  const [areas, setAreas] = useState<AreaResponse[]>([]);
  const [areasLoading, setAreasLoading] = useState(true);
  const [areasError, setAreasError] = useState("");
  const [areasRetryCount, setAreasRetryCount] = useState(0);

  const searchController = useRef<AbortController | null>(null);

  const today = todayInVietnam();
  const latestDate = addDays(today, 30);

  const areaOptions = [
    { value: "", label: "Không yêu cầu" },
    ...areas.map((area) => ({
      value: String(area.id),
      label: area.name,
    })),
  ];

  const selectedAreaName =
    areas.find((area) => String(area.id) === form.areaId)?.name ??
    "Không yêu cầu";

  useEffect(() => {
    return () => searchController.current?.abort();
  }, []);

  function updateSearchField(
    field: "date" | "partySize" | "durationMinutes" | "areaId",
    value: string,
  ) {
    // Kết quả tra cứu cũ không còn đúng khi tiêu chí thay đổi.
    searchController.current?.abort();
    setSearching(false);
    setAvailability(null);
    setSelectedSlot(null);
    setSearchError("");
    setBookingError("");
    setCreatedReservation(null);

    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  }

  async function handleSearch() {
    if (!form.date) {
      setSearchError("Vui lòng chọn ngày đặt bàn.");
      return;
    }

    if (form.date < today || form.date > latestDate) {
      setSearchError(
        `Vui lòng chọn ngày từ ${displayDate(today)} đến ${displayDate(
          latestDate,
        )}.`,
      );
      return;
    }

    searchController.current?.abort();
    const controller = new AbortController();
    searchController.current = controller;

    setSearching(true);
    setSearchError("");
    setBookingError("");
    setAvailability(null);
    setSelectedSlot(null);
    setCreatedReservation(null);

    try {
      const result = await reservationApi.getAvailability(
        {
          date: form.date,
          partySize: Number(form.partySize),
          durationMinutes: Number(form.durationMinutes),
          areaId: form.areaId ? Number(form.areaId) : undefined,
        },
        controller.signal,
      );

      if (!controller.signal.aborted) {
        setAvailability(result);
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setSearchError(getApiError(error).message);
      }
    } finally {
      if (!controller.signal.aborted) {
        setSearching(false);
      }
    }
  }

  async function handleBook() {
    if (!selectedSlot || booking || createdReservation) return;

    if (!user) {
      // Giữ form khi khách chuyển sang trang đăng nhập.
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(form));

      navigate("/login", {
        state: { returnTo: "/reservation" },
      });
      return;
    }

    setBooking(true);
    setBookingError("");

    try {
      // Gửi đúng khoảng giờ mà API availability đã trả về.
      const result = await reservationApi.create({
        startTime: selectedSlot.startTime,
        endTime: selectedSlot.endTime,
        partySize: Number(form.partySize),
        requestedAreaId: form.areaId
          ? Number(form.areaId)
          : null,
        specialNote: form.specialNote.trim() || null,
      });

      sessionStorage.removeItem(DRAFT_KEY);

      navigate("/reservation/success", {
        replace: true,
        state: {
          reservation: result,
        },
      });
    } catch (error) {
      const apiError = getApiError(error);

      setBookingError(
        apiError.status === 404
          ? "API tạo đơn đặt bàn chưa sẵn sàng."
          : apiError.message,
      );
    } finally {
      setBooking(false);
    }
  }

  useEffect(() => {
  const controller = new AbortController();

  setAreasLoading(true);
  setAreasError("");

  async function loadAreas() {
    try {
      const result = await areaApi.list(
        controller.signal,
      );

      if (!controller.signal.aborted) {
        setAreas(result);
      }
    } catch (cause) {
      if (!controller.signal.aborted) {
        setAreasError(
          getApiError(cause).message ||
            "Không thể tải danh sách khu vực.",
        );
      }
    } finally {
      if (!controller.signal.aborted) {
        setAreasLoading(false);
      }
    }
  }

  void loadAreas();

  return () => controller.abort();
}, [areasRetryCount]);

  return (
    <div className="reservation-page">
      <Header variant="guest" active="reservation" />

      <main className="reservation-page__main">
        <section className="reservation-page__hero">
          <div className="container">
            <p className="reservation-page__eyebrow">ĐẶT BÀN</p>
            <h1>Đặt bàn tại Sage</h1>
            <p>
              Chọn thời gian phù hợp và Sage sẽ sắp xếp bàn dành cho bạn.
            </p>
          </div>
        </section>

        <div className="reservation-page__content">
          <div className="container reservation-page__sections">
            <section
              className="reservation-page__card reservation-page__search"
              aria-labelledby="reservation-search-title"
            >
              <div className="reservation-page__section-heading">
                <h2 id="reservation-search-title">Tìm bàn trống</h2>
                <p>Chọn thông tin để xem các khung giờ còn phù hợp.</p>
              </div>

              <div className="reservation-page__fields">
                <DateInput
                  label="Ngày đặt bàn"
                  size="lg"
                  value={form.date}
                  min={today}
                  max={latestDate}
                  disabled={booking}
                 onChange={(value: string) => updateSearchField("date", value)}
                />

                <div role="group" aria-label="Số người">
                  <FormField label="Số người">
                    <Select
                      size="lg"
                      value={form.partySize}
                      options={partySizeOptions}
                      disabled={booking}
                      onValueChange={(value) =>
                        updateSearchField("partySize", value)
                      }
                    />
                  </FormField>
                </div>

                <div role="group" aria-label="Thời lượng">
                  <FormField label="Thời lượng">
                    <Select
                      size="lg"
                      value={form.durationMinutes}
                      options={durationOptions}
                      disabled={booking}
                      onValueChange={(value) =>
                        updateSearchField("durationMinutes", value)
                      }
                    />
                  </FormField>
                </div>

               <div role="group" aria-label="Khu vực">
                  <FormField
                    label="Khu vực"
                    supportingText={
                      areasLoading
                        ? "Đang tải danh sách khu vực..."
                        : "Không bắt buộc"
                    }
                  >
                    <Select
                      size="lg"
                      value={form.areaId}
                      options={areaOptions}
                      disabled={
                        booking ||
                        areasLoading ||
                        Boolean(areasError)
                      }
                      error={Boolean(areasError)}
                      onValueChange={(value) =>
                        updateSearchField("areaId", value)
                      }
                    />
                  </FormField>

                  {areasError && (
                    <div>
                      <p role="alert">{areasError}</p>

                      <Button
                        type="button"
                        variant="outline"
                        size="md"
                        disabled={booking}
                        onClick={() =>
                          setAreasRetryCount((count) => count + 1)
                        }
                      >
                        Thử lại
                      </Button>
                    </div>
                  )}
                </div>
              </div>

              <div className="reservation-page__search-actions">
                <p>
                  Sage sẽ tự động sắp xếp bàn phù hợp với số lượng khách.
                </p>

                <Button
                  type="button"
                  size="lg"
                  loading={searching}
                  disabled={booking}
                  onClick={() => void handleSearch()}
                >
                  Kiểm tra bàn trống
                </Button>
              </div>

              {searchError && (
                <p className="reservation-page__error" role="alert">
                  {searchError}
                </p>
              )}
            </section>

            {availability && (
              <>
                <section
                  className="reservation-page__times"
                  aria-labelledby="reservation-times-title"
                >
                  <div className="reservation-page__times-heading">
                    <div>
                      <h2 id="reservation-times-title">Giờ còn trống</h2>
                      <p>
                        {displayLongDate(availability.date)} ·{" "}
                        {availability.partySize} khách ·{" "}
                        {availability.durationMinutes} phút
                      </p>
                    </div>

                    {availability.slots.length > 0 && (
                      <span>Chọn một giờ bắt đầu</span>
                    )}
                  </div>

                  {availability.slots.length > 0 ? (
                    <div className="reservation-page__slot-grid">
                      {availability.slots.map((slot) => {
                        const selected =
                          selectedSlot?.startTime === slot.startTime;

                        return (
                          <button
                            key={slot.startTime}
                            type="button"
                            className={[
                              "reservation-page__slot",
                              selected
                                ? "reservation-page__slot--selected"
                                : "",
                            ]
                              .filter(Boolean)
                              .join(" ")}
                            aria-pressed={selected}
                            disabled={booking}
                            onClick={() => {
                              setSelectedSlot(slot);
                              setBookingError("");
                            }}
                          >
                            {displayTime(slot.startTime)}
                          </button>
                        );
                      })}
                    </div>
                  ) : (
                    <p className="reservation-page__empty">
                      Không còn giờ phù hợp trong ngày này. Vui lòng thử
                      ngày hoặc khu vực khác.
                    </p>
                  )}
                </section>

                {selectedSlot && (
                  <section className="reservation-page__booking">
                    <div className="reservation-page__card reservation-page__note">
                      <div className="reservation-page__section-heading">
                        <h2>Hoàn tất đặt bàn</h2>
                        <p>
                          Kiểm tra thông tin và thêm ghi chú nếu cần.
                        </p>
                      </div>

                      <FormField
                        label="Ghi chú đặc biệt"
                        supportingText="Không bắt buộc"
                      >
                        <Textarea
                          value={form.specialNote}
                          maxLength={500}
                          disabled={booking || Boolean(createdReservation)}
                          placeholder="Ví dụ: bàn gần cửa sổ, ghế trẻ em..."
                          onChange={(event) =>
                            setForm((current) => ({
                              ...current,
                              specialNote: event.target.value,
                            }))
                          }
                        />
                      </FormField>
                    </div>

                    <div className="reservation-page__card reservation-page__summary">
                      <h2>Thông tin đặt bàn</h2>

                      <dl className="reservation-page__summary-rows">
                        <div>
                          <dt>Ngày</dt>
                          <dd>{displayDate(form.date)}</dd>
                        </div>
                        <div>
                          <dt>Thời gian</dt>
                          <dd>
                            {displayTime(selectedSlot.startTime)} –{" "}
                            {displayTime(selectedSlot.endTime)}
                          </dd>
                        </div>
                        <div>
                          <dt>Số người</dt>
                          <dd>{form.partySize} khách</dd>
                        </div>
                        <div>
                          <dt>Thời lượng</dt>
                          <dd>{form.durationMinutes} phút</dd>
                        </div>
                        <div>
                          <dt>Khu vực</dt>
                          <dd>{selectedAreaName}</dd>
                        </div>
                      </dl>

                      <p className="reservation-page__summary-help">
                        Bàn cụ thể sẽ được Sage tự động sắp xếp dựa trên
                        số lượng khách và tình trạng bàn trống.
                      </p>

                      {bookingError && (
                        <p
                          className="reservation-page__error"
                          role="alert"
                        >
                          {bookingError}
                        </p>
                      )}

                      {createdReservation ? (
                        <p
                          className="reservation-page__confirmation"
                          role="status"
                        >
                          Đã nhận đơn {createdReservation.code}. Trạng
                          thái:{" "}
                          {createdReservation.status === "PENDING"
                            ? "Chờ nhà hàng xác nhận"
                            : "Đã xác nhận"}
                          .
                        </p>
                      ) : (
                        <Button
                          type="button"
                          size="lg"
                          fullWidth
                          loading={booking}
                          onClick={() => void handleBook()}
                        >
                          Đặt bàn
                        </Button>
                      )}
                    </div>
                  </section>
                )}
              </>
            )}
          </div>
        </div>
      </main>

      <Footer restaurantInfo={restaurantInfo} />
    </div>
  );
}

