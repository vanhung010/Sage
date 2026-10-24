import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, Clock } from "lucide-react";

import { areaApi } from "../../../api/area.api";
import { getApiError } from "../../../api/error";
import { staffReservationApi } from "../../../api/staff-reservation.api";

import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { DateInput } from "../../../components/ui/DateInput";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";
import { Select } from "../../../components/ui/Select";
import { Textarea } from "../../../components/ui/Textarea";

import type { AreaResponse } from "../../../types/area";
import type {
  CreateGuestReservationRequest,
  StaffReservationNavigationState,
} from "../../../types/staff-reservation";

import {
  dateInputValue,
  displayDate,
} from "../../../utils/reservation";

import "./GuestReservationPage.css";

interface FormValues {
  name: string;
  phone: string;
  date: string;
  timeStart: string;
  timeEnd: string;
  partySize: string;
  areaId: string;
  description: string;
}

type FieldName = keyof FormValues;
type FormErrors = Partial<Record<FieldName, string>>;

const STAFF_HOME = "/staff/reservations";

const TIME_OPTIONS = Array.from({ length: 96 }, (_, index) => {
  const hours = String(Math.floor(index / 4)).padStart(2, "0");
  const minutes = String((index % 4) * 15).padStart(2, "0");
  const value = `${hours}:${minutes}`;

  return { value, label: value };
});

const TIME_PATTERN = /^([01]\d|2[0-3]):(00|15|30|45)$/;

function initialForm(): FormValues {
  return {
    name: "",
    phone: "",
    date: dateInputValue(),
    timeStart: "",
    timeEnd: "",
    partySize: "2",
    areaId: "",
    description: "",
  };
}

function normalizePhone(value: string) {
  // Cho phép nhập "0912 345 678" như thiết kế.
  return value.replace(/\s+/g, "");
}

function validate(values: FormValues): FormErrors {
  const errors: FormErrors = {};
  const phone = normalizePhone(values.phone);
  const partySize = Number(values.partySize);

  if (!values.name.trim()) {
    errors.name = "Vui lòng nhập họ và tên khách.";
  } else if (values.name.trim().length > 255) {
    errors.name = "Tên khách tối đa 255 ký tự.";
  }

  if (!/^[0-9]{10}$/.test(phone)) {
    errors.phone = "Số điện thoại phải gồm 10 chữ số.";
  }

  if (!values.date) {
    errors.date = "Vui lòng chọn ngày đặt.";
  } else if (values.date < dateInputValue()) {
    errors.date = "Ngày đặt không được thuộc quá khứ.";
  }

  if (!TIME_PATTERN.test(values.timeStart)) {
    errors.timeStart = "Chọn giờ bắt đầu theo bước 15 phút.";
  }

  if (!TIME_PATTERN.test(values.timeEnd)) {
    errors.timeEnd = "Chọn giờ kết thúc theo bước 15 phút.";
  }

  if (
    !errors.timeStart &&
    !errors.timeEnd &&
    values.timeEnd <= values.timeStart
  ) {
    errors.timeEnd = "Giờ kết thúc phải sau giờ bắt đầu.";
  }

  if (!errors.date && !errors.timeStart) {
    const start = Date.parse(
      `${values.date}T${values.timeStart}:00+07:00`,
    );
    const currentMinute = Math.floor(Date.now() / 60_000) * 60_000;

    if (!Number.isFinite(start) || start < currentMinute) {
      errors.timeStart = "Giờ bắt đầu không được thuộc phút đã qua.";
    }
  }

  if (
    !Number.isInteger(partySize) ||
    partySize < 1 ||
    partySize > 12
  ) {
    errors.partySize = "Số khách phải từ 1 đến 12.";
  }

  return errors;
}

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section className="guest-booking__card">
      <header className="guest-booking__card-header">
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </header>

      <div className="guest-booking__card-content">
        {children}
      </div>
    </section>
  );
}

export function GuestReservationPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState<FormValues>(initialForm);
  const [errors, setErrors] = useState<FormErrors>({});
  const [requestError, setRequestError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [areas, setAreas] = useState<AreaResponse[]>([]);
  const [areasLoading, setAreasLoading] = useState(true);
  const [areasError, setAreasError] = useState("");
  const [areasAttempt, setAreasAttempt] = useState(0);

  // Chặn hai lần submit trước khi React cập nhật loading.
  const submittingRef = useRef(false);
  const mountedRef = useRef(false);

  useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    setAreasLoading(true);
    setAreasError("");

    void areaApi.list(controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return;

        setAreas(data);
        setAreasLoading(false);
      },
      (error: unknown) => {
        if (controller.signal.aborted) return;

        setAreasError(getApiError(error).message);
        setAreasLoading(false);
      },
    );

    return () => controller.abort();
  }, [areasAttempt]);

  function changeField(key: FieldName, value: string) {
    setForm((current) => ({
      ...current,
      [key]: value,
      // Khi giờ bắt đầu mới vượt giờ kết thúc, yêu cầu chọn lại.
      ...(key === "timeStart" &&
      current.timeEnd &&
      current.timeEnd <= value
        ? { timeEnd: "" }
        : {}),
    }));

    setErrors((current) => ({
      ...current,
      [key]: undefined,
      ...(key === "timeStart" ? { timeEnd: undefined } : {}),
    }));

    setRequestError("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submittingRef.current) return;

    const nextErrors = validate(form);
    setErrors(nextErrors);
    setRequestError("");

    const firstInvalid = Object.keys(nextErrors)[0] as
      | FieldName
      | undefined;

    if (firstInvalid) {
      document.getElementById(`guest-${firstInvalid}`)?.focus();
      return;
    }

    const payload: CreateGuestReservationRequest = {
      name: form.name.trim(),
      phone: normalizePhone(form.phone),
      date: form.date,
      timeStart: form.timeStart,
      timeEnd: form.timeEnd,
      partySize: Number(form.partySize),
      areaId: form.areaId ? Number(form.areaId) : null,
      description: form.description.trim() || null,
    };

    submittingRef.current = true;
    setSubmitting(true);

    try {
      const result = await staffReservationApi.createGuest(payload);

      if (!mountedRef.current) return;

      const state: StaffReservationNavigationState = {
        guestCreated: result,
      };

      // Chỉ chuyển trang sau khi API xác nhận thành công.
      // replace tránh quay lại form đã gửi bằng nút Back.
      void navigate(STAFF_HOME, {
        replace: true,
        state,
      });
    } catch (error: unknown) {
      if (!mountedRef.current) return;

      const apiError = getApiError(error);

      setRequestError(
        apiError.status === 0 || apiError.status >= 500
          ? "Chưa xác định được đơn đã tạo thành công hay chưa. " +
            "Vui lòng kiểm tra danh sách trước khi gửi lại."
          : apiError.message || "Không thể tạo đơn. Vui lòng thử lại.",
      );
    } finally {
      submittingRef.current = false;

      if (mountedRef.current) {
        setSubmitting(false);
      }
    }
  }

  const selectedArea = areas.find(
    (area) => String(area.id) === form.areaId,
  );

  const areaLabel = form.areaId
    ? selectedArea?.name ?? "Khu vực đã chọn"
    : "Không yêu cầu khu vực";

  const endTimeOptions = TIME_OPTIONS.map((option) => ({
    ...option,
    disabled: Boolean(form.timeStart) && option.value <= form.timeStart,
  }));

  return (
    <div className="guest-booking">
      <header className="guest-booking__header">
        <button
          type="button"
          className="guest-booking__back"
          disabled={submitting}
          onClick={() => void navigate(STAFF_HOME)}
        >
          <ChevronLeft size={16} aria-hidden="true" />
          Danh sách đặt bàn
        </button>

        <h1>Đặt bàn hộ khách</h1>
        <p>Tạo đơn tại quầy hoặc qua điện thoại.</p>
      </header>

      <form
        className="guest-booking__form"
        noValidate
        aria-busy={submitting}
        onSubmit={handleSubmit}
      >
        <div className="guest-booking__grid">
          <div className="guest-booking__fields">
            <Section
              title="Thông tin khách hàng"
              description="Không cần tài khoản khách hàng."
            >
              <div className="guest-booking__row">
                <FormField
                  label="Họ và tên"
                  htmlFor="guest-name"
                  required
                  error={errors.name}
                >
                  <Input
                    id="guest-name"
                    name="name"
                    autoComplete="off"
                    placeholder="Nhập họ và tên khách"
                    maxLength={255}
                    value={form.name}
                    error={Boolean(errors.name)}
                    disabled={submitting}
                    required
                    onChange={(event) =>
                      changeField("name", event.target.value)
                    }
                  />
                </FormField>

                <FormField
                  label="Số điện thoại"
                  htmlFor="guest-phone"
                  required
                  error={errors.phone}
                >
                  <Input
                    id="guest-phone"
                    name="phone"
                    type="tel"
                    inputMode="tel"
                    autoComplete="off"
                    placeholder="Nhập số điện thoại"
                    maxLength={20}
                    value={form.phone}
                    error={Boolean(errors.phone)}
                    disabled={submitting}
                    required
                    onChange={(event) =>
                      changeField("phone", event.target.value)
                    }
                  />
                </FormField>
              </div>
            </Section>

            <Section
              title="Thông tin lượt đặt"
              description="Chọn thời gian và khu vực phù hợp với khách."
            >
              <FormField
                label="Ngày đặt"
                htmlFor="guest-date"
                required
              >
                <DateInput
                  id="guest-date"
                  name="date"
                  placeholder="Chọn ngày đặt"
                  value={form.date}
                  min={dateInputValue()}
                  error={errors.date}
                  required
                  disabled={submitting}
                  onChange={(value) => changeField("date", value)}
                />
              </FormField>

              <div className="guest-booking__row">
                <FormField
                  label="Giờ bắt đầu"
                  htmlFor="guest-timeStart"
                  required
                  error={errors.timeStart}
                >
                  <Select
                    id="guest-timeStart"
                    name="timeStart"
                    aria-label="Giờ bắt đầu, bắt buộc"
                    className="guest-booking__time"
                    placeholder="HH:mm"
                    leadingIcon={<Clock size={24} aria-hidden="true" />}
                    options={TIME_OPTIONS}
                    value={form.timeStart}
                    error={Boolean(errors.timeStart)}
                    disabled={submitting}
                    onValueChange={(value) =>
                      changeField("timeStart", value)
                    }
                  />
                </FormField>

                <FormField
                  label="Giờ kết thúc"
                  htmlFor="guest-timeEnd"
                  required
                  error={errors.timeEnd}
                >
                  <Select
                    id="guest-timeEnd"
                    name="timeEnd"
                    aria-label="Giờ kết thúc, bắt buộc"
                    className="guest-booking__time"
                    placeholder="HH:mm"
                    leadingIcon={<Clock size={24} aria-hidden="true" />}
                    options={endTimeOptions}
                    value={form.timeEnd}
                    error={Boolean(errors.timeEnd)}
                    disabled={submitting}
                    onValueChange={(value) =>
                      changeField("timeEnd", value)
                    }
                  />
                </FormField>
              </div>

              <div className="guest-booking__row">
                <FormField
                  label="Số khách"
                  htmlFor="guest-partySize"
                  required
                  error={errors.partySize}
                >
                  <Input
                    id="guest-partySize"
                    name="partySize"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    max={12}
                    step={1}
                    value={form.partySize}
                    error={Boolean(errors.partySize)}
                    disabled={submitting}
                    required
                    onChange={(event) =>
                      changeField("partySize", event.target.value)
                    }
                  />
                </FormField>

                <FormField
                  label="Khu vực yêu cầu"
                  htmlFor="guest-areaId"
                >
                  <Select
                    id="guest-areaId"
                    name="areaId"
                    aria-label="Khu vực yêu cầu"
                    placeholder={
                      areasLoading ? "Đang tải khu vực..." : "Chọn khu vực"
                    }
                    value={form.areaId}
                    options={[
                      { value: "", label: "Không yêu cầu khu vực" },
                      ...areas.map((area) => ({
                        value: String(area.id),
                        label: area.name,
                      })),
                    ]}
                    disabled={
                      submitting || areasLoading || Boolean(areasError)
                    }
                    onValueChange={(value) =>
                      changeField("areaId", value)
                    }
                  />
                </FormField>
              </div>

              {areasError && (
                <div className="guest-booking__notice" role="alert">
                  <p>
                    Không tải được khu vực. Bạn vẫn có thể đặt bàn
                    không yêu cầu khu vực.
                  </p>

                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={submitting || areasLoading}
                    onClick={() => setAreasAttempt((value) => value + 1)}
                  >
                    Tải lại khu vực
                  </Button>
                </div>
              )}

              <FormField label="Ghi chú" htmlFor="guest-description">
                <Textarea
                  id="guest-description"
                  name="description"
                  className="guest-booking__description"
                  placeholder="Yêu cầu đặc biệt hoặc thông tin cần lưu ý"
                  value={form.description}
                  disabled={submitting}
                  onChange={(event) =>
                    changeField("description", event.target.value)
                  }
                />
              </FormField>
            </Section>
          </div>

          <aside className="guest-booking__summary">
            <Section title="Tóm tắt lượt đặt">
              <dl className="guest-booking__details">
                <div>
                  <dt>Khách hàng</dt>
                  <dd>{form.name.trim() || "Chưa nhập tên khách"}</dd>
                </div>

                <div>
                  <dt>Thời gian</dt>
                  <dd>
                    {displayDate(form.date)} ·{" "}
                    {form.timeStart || "--:--"}–{form.timeEnd || "--:--"}
                  </dd>
                </div>

                <div>
                  <dt>Số người / khu vực</dt>
                  <dd>
                    {form.partySize || "—"} khách · {areaLabel}
                  </dd>
                </div>
              </dl>

              <div>
                <Badge variant="success">Đã xác nhận</Badge>
              </div>

              <p className="guest-booking__muted">
                Bàn được hệ thống xếp khi tạo đơn thành công.
              </p>
            </Section>

            <div className="guest-booking__info">
              <strong>Xác nhận trực tiếp với khách</strong>
              <p>
                Đơn đặt hộ không lưu email. Nhân viên đọc lại mã đặt bàn
                cho khách sau khi tạo đơn.
              </p>
            </div>

            <p className="guest-booking__hint">
              Tối đa 12 khách. Thời lượng linh hoạt trong giờ mở cửa;
              không áp dụng thời gian đặt trước tối thiểu của khách tự đặt.
            </p>
          </aside>
        </div>

        {requestError && (
          <div className="guest-booking__error" role="alert">
            <strong>Chưa thể xác nhận kết quả tạo đơn</strong>
            <p>{requestError}</p>

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void navigate(STAFF_HOME)}
            >
              Kiểm tra danh sách
            </Button>
          </div>
        )}

        <footer className="guest-booking__actions">
          <p>Kiểm tra lại thời gian và thông tin liên hệ trước khi tạo.</p>

          <div className="guest-booking__buttons">
            <Button
              type="button"
              variant="outline"
              disabled={submitting}
              onClick={() => void navigate(STAFF_HOME)}
            >
              Hủy
            </Button>

            <Button
              type="submit"
              loading={submitting}
              loading-label="Đang tạo đơn"
            >
              {submitting ? "Đang tạo đơn..." : "Tạo đơn đặt bàn"}
            </Button>
          </div>
        </footer>
      </form>
    </div>
  );
}