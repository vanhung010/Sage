import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";

import { Eye, EyeOff } from "lucide-react";
import { Link } from "react-router-dom";

import { Header } from "../../../components/layout/Header";
import { Button } from "../../../components/ui/Button";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";

import { getApiError } from "../../../api/error";
import { passwordResetApi } from "../../../api/passwordReset.api";

import "./ForgotPasswordPage.css";

type RecoveryStep =
  | "email"
  | "code"
  | "password"
  | "success";

type PasswordField = "newPassword" | "confirmPassword";

interface RecoveryForm {
  email: string;
  newPassword: string;
  confirmPassword: string;
}

type RecoveryErrors = Partial<
  Record<keyof RecoveryForm | "code", string>
>;

const RESEND_COOLDOWN_SECONDS = 60;

const stepContent = {
  email: {
    number: 1,
    title: "Quên mật khẩu?",
    description:
      "Nhập email đã đăng ký. Sage sẽ gửi mã xác thực để bạn đặt lại mật khẩu.",
    button: "Gửi mã xác thực",
  },
  code: {
    number: 2,
    title: "Nhập mã xác thực",
    description:
      "Nhập mã gồm 6 chữ số được gửi tới email của bạn.",
    button: "Xác thực mã",
  },
  password: {
    number: 3,
    title: "Đặt lại mật khẩu",
    description:
      "Tạo mật khẩu mới để tiếp tục sử dụng tài khoản Sage.",
    button: "Lưu mật khẩu mới",
  },
} as const;

const passwordFields: {
  name: PasswordField;
  label: string;
  placeholder: string;
  supportingText?: string;
}[] = [
  {
    name: "newPassword",
    label: "Mật khẩu mới",
    placeholder: "Nhập mật khẩu mới",
    supportingText: "Tối thiểu 8 ký tự, gồm chữ và số.",
  },
  {
    name: "confirmPassword",
    label: "Xác nhận mật khẩu mới",
    placeholder: "Nhập lại mật khẩu mới",
  },
];

function normalizeOtp(value: string) {
  return value
    .normalize("NFKC")
    .replace(/[^0-9]/g, "")
    .slice(0, 6);
}

export function ForgotPasswordPage() {
  const [step, setStep] = useState<RecoveryStep>("email");

  const [form, setForm] = useState<RecoveryForm>({
    email: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [errors, setErrors] = useState<RecoveryErrors>({});
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");

  const [loading, setLoading] = useState(false);
  const requestInProgress = useRef(false);

  const [showPasswords, setShowPasswords] = useState<
    Record<PasswordField, boolean>
  >({
    newPassword: false,
    confirmPassword: false,
  });

  // OTP dùng một input thật.
  const [otpValue, setOtpValue] = useState("");
  const otpInputRef = useRef<HTMLInputElement | null>(null);

  // Theo dõi quá trình ghép ký tự của bộ gõ.
  const otpComposingRef = useRef(false);

  // Token đặt lại mật khẩu chỉ lưu trong bộ nhớ.
  const [resetToken, setResetToken] = useState("");
  const [resetExpiresAt, setResetExpiresAt] = useState(0);

  const [resendAvailableAt, setResendAvailableAt] = useState(0);
  const [resendSeconds, setResendSeconds] = useState(0);

  const otpCode = normalizeOtp(otpValue);
  const activeOtpIndex = Math.min(otpCode.length, 5);

  const content =
    step === "success" ? null : stepContent[step];

  useEffect(() => {
    if (!resendAvailableAt) return;

    const updateCountdown = () => {
      setResendSeconds(
        Math.max(
          0,
          Math.ceil((resendAvailableAt - Date.now()) / 1000),
        ),
      );
    };

    updateCountdown();

    const timer = window.setInterval(updateCountdown, 1000);

    return () => window.clearInterval(timer);
  }, [resendAvailableAt]);

  useEffect(() => {
    if (step === "code") {
      otpInputRef.current?.focus();
    }
  }, [step]);

  function clearFeedback() {
    setErrors({});
    setFormError("");
    setNotice("");
  }

  function changeField(
    field: keyof RecoveryForm,
    value: string,
  ) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    setErrors((current) => ({
      ...current,
      [field]: undefined,
    }));

    setFormError("");
    setNotice("");
  }

  function updateOtp(value: string, composing = false) {
    // Giữ giá trị tạm trong lúc bộ gõ đang ghép ký tự.
    // Khi kết thúc mới lọc số và giới hạn độ dài.
    setOtpValue(composing ? value : normalizeOtp(value));

    setErrors((current) => ({
      ...current,
      code: undefined,
    }));

    setFormError("");
    setNotice("");
  }

  function restartRecovery(message = "") {
    setStep("email");

    setOtpValue("");
    otpComposingRef.current = false;

    setResetToken("");
    setResetExpiresAt(0);

    setForm((current) => ({
      ...current,
      newPassword: "",
      confirmPassword: "",
    }));

    setShowPasswords({
      newPassword: false,
      confirmPassword: false,
    });

    clearFeedback();
    setNotice(message);
  }

  async function runRequest(action: () => Promise<void>) {
    if (requestInProgress.current) return;

    requestInProgress.current = true;
    setLoading(true);
    clearFeedback();

    try {
      await action();
    } catch (error) {
      const apiError = getApiError(error);

      if (apiError.code === "INVALID_RESET_TOKEN") {
        restartRecovery(
          "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới.",
        );
      } else {
        setFormError(
          apiError.message || "Không thể thực hiện yêu cầu.",
        );
      }
    } finally {
      requestInProgress.current = false;
      setLoading(false);
    }
  }

  async function sendCode() {
    await runRequest(async () => {
      const email = form.email.trim();

      const response = await passwordResetApi.forgotPassword({
        email,
      });

      setForm((current) => ({
        ...current,
        email,
      }));

      setOtpValue("");
      otpComposingRef.current = false;

      setResetToken("");
      setResetExpiresAt(0);

      setResendSeconds(RESEND_COOLDOWN_SECONDS);

      setResendAvailableAt(
        Date.now() + RESEND_COOLDOWN_SECONDS * 1000,
      );

      setNotice(response.message);
      setStep("code");

      // Có tác dụng khi gửi lại mã và step vẫn là "code".
      otpInputRef.current?.focus();
    });
  }

  function validateForm() {
    const nextErrors: RecoveryErrors = {};

    if (step === "email") {
      const email = form.email.trim();

      if (!email) {
        nextErrors.email = "Vui lòng nhập email.";
      } else if (
        email.length > 255 ||
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      ) {
        nextErrors.email = "Email không đúng định dạng.";
      }
    }

    if (step === "code") {
      if (!/^[0-9]{6}$/.test(otpCode)) {
        nextErrors.code = "Vui lòng nhập đủ 6 chữ số.";
      }
    }

    if (step === "password") {
      const password = form.newPassword;

      if (!password) {
        nextErrors.newPassword =
          "Vui lòng nhập mật khẩu mới.";
      } else if (password.length < 8) {
        nextErrors.newPassword =
          "Mật khẩu phải có ít nhất 8 ký tự.";
      } else if (
        password.length > 72 ||
        new TextEncoder().encode(password).length > 72
      ) {
        // BCrypt giới hạn theo số byte UTF-8.
        nextErrors.newPassword =
          "Mật khẩu không được vượt quá 72 byte.";
      } else if (
        !/[A-Za-z]/.test(password) ||
        !/[0-9]/.test(password)
      ) {
        nextErrors.newPassword =
          "Mật khẩu phải chứa ít nhất 1 chữ và 1 số.";
      }

      if (!form.confirmPassword) {
        nextErrors.confirmPassword =
          "Vui lòng xác nhận mật khẩu mới.";
      } else if (form.confirmPassword !== password) {
        nextErrors.confirmPassword =
          "Mật khẩu xác nhận không khớp.";
      }
    }

    setErrors(nextErrors);

    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    if (requestInProgress.current) return;

    // Tránh gửi form khi bộ gõ chưa chốt ký tự OTP.
    if (step === "code" && otpComposingRef.current) {
      return;
    }

    clearFeedback();

    if (!validateForm()) return;

    if (step === "email") {
      await sendCode();
      return;
    }

    if (step === "code") {
      await runRequest(async () => {
        const response =
          await passwordResetApi.verifyResetCode({
            email: form.email,
            code: otpCode,
          });

        setResetToken(response.resetToken);

        setResetExpiresAt(
          Date.now() + response.expiresInSeconds * 1000,
        );

        setOtpValue("");
        setStep("password");
      });

      return;
    }

    if (step === "password") {
      if (!resetToken || Date.now() >= resetExpiresAt) {
        restartRecovery(
          "Yêu cầu đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu mã mới.",
        );
        return;
      }

      await runRequest(async () => {
        const response =
          await passwordResetApi.resetPassword({
            resetToken,
            newPassword: form.newPassword,
            confirmPassword: form.confirmPassword,
          });

        setResetToken("");
        setResetExpiresAt(0);

        setForm((current) => ({
          ...current,
          newPassword: "",
          confirmPassword: "",
        }));

        setShowPasswords({
          newPassword: false,
          confirmPassword: false,
        });

        setNotice(response.message);
        setStep("success");
      });
    }
  }

  return (
    <div className="recovery-page">
      <Header variant="auth" />

      <main className="recovery-page__main">
        <section
          className="recovery-page__card"
          aria-labelledby="recovery-title"
        >
          {step === "success" ? (
            <>
              <div className="recovery-page__heading">
                <p className="recovery-page__eyebrow">
                  KHÔI PHỤC TÀI KHOẢN
                </p>

                <h1 id="recovery-title">
                  Đặt lại thành công
                </h1>

                <p
                  className="recovery-page__description"
                  role="status"
                >
                  {notice}
                </p>
              </div>

              <Link
                to="/login"
                replace
                className="recovery-page__login-action"
              >
                Đăng nhập
              </Link>
            </>
          ) : content && (
            <>
              <div className="recovery-page__heading">
                <p className="recovery-page__eyebrow">
                  KHÔI PHỤC TÀI KHOẢN · {content.number}/3
                </p>

                <h1 id="recovery-title">
                  {content.title}
                </h1>

                <p className="recovery-page__description">
                  {content.description}
                </p>

                {step === "code" && (
                  <>
                    <p className="recovery-page__email">
                      {form.email}
                    </p>

                    <button
                      type="button"
                      className="recovery-page__text-button"
                      disabled={loading}
                      onClick={() => restartRecovery()}
                    >
                      Thay đổi email
                    </button>
                  </>
                )}
              </div>

              <form
                className="recovery-page__form"
                onSubmit={handleSubmit}
                noValidate
                aria-busy={loading}
              >
                <fieldset
                  className="recovery-page__fields"
                  disabled={loading}
                >
                  {/* Bước 1: Nhập email */}
                  {step === "email" && (
                    <FormField
                      label="Email"
                      htmlFor="recovery-email"
                      required
                      error={errors.email}
                    >
                      <Input
                        id="recovery-email"
                        name="email"
                        type="email"
                        size="lg"
                        value={form.email}
                        placeholder="customer@example.com"
                        autoComplete="email"
                        maxLength={255}
                        required
                        autoFocus
                        error={Boolean(errors.email)}
                        onChange={(event) =>
                          changeField(
                            "email",
                            event.target.value,
                          )
                        }
                      />
                    </FormField>
                  )}

                  {/* Bước 2: Nhập OTP */}
                  {step === "code" && (
                    <div className="recovery-page__code-field">
                      <label htmlFor="recovery-code">
                        Mã xác thực
                      </label>

                      <div className="recovery-page__otp">
                        {/*
                          Một input thật nhận toàn bộ mã.
                          Không chuyển focus giữa các chữ số.
                        */}
                        <input
                          ref={otpInputRef}
                          id="recovery-code"
                          name="code"
                          className="recovery-page__otp-native"
                          type="text"
                          inputMode="numeric"
                          autoComplete="one-time-code"
                          value={otpValue}
                          required
                          spellCheck={false}
                          aria-invalid={Boolean(errors.code)}
                          aria-describedby={
                            errors.code
                              ? "recovery-code-error"
                              : undefined
                          }
                          onCompositionStart={() => {
                            otpComposingRef.current = true;
                          }}
                          onCompositionEnd={(event) => {
                            otpComposingRef.current = false;

                            updateOtp(
                              event.currentTarget.value,
                            );
                          }}
                          onChange={(event) => {
                            const nativeEvent =
                              event.nativeEvent as InputEvent;

                            const composing =
                              otpComposingRef.current ||
                              nativeEvent.isComposing === true;

                            updateOtp(
                              event.currentTarget.value,
                              composing,
                            );
                          }}
                          onPaste={(event) => {
                            // Để trình duyệt xử lý nếu IME
                            // đang có một phiên ghép ký tự.
                            if (otpComposingRef.current) return;

                            event.preventDefault();

                            updateOtp(
                              event.clipboardData.getData(
                                "text",
                              ),
                            );
                          }}
                        />

                        <div
                          className="recovery-page__otp-cells"
                          aria-hidden="true"
                        >
                          {Array.from(
                            { length: 6 },
                            (_, index) => (
                              <span
                                key={index}
                                className={[
                                  "recovery-page__otp-cell",
                                  index === activeOtpIndex
                                    ? "recovery-page__otp-cell--active"
                                    : "",
                                  errors.code
                                    ? "recovery-page__otp-cell--error"
                                    : "",
                                ]
                                  .filter(Boolean)
                                  .join(" ")}
                              >
                                {otpCode[index] ?? ""}
                              </span>
                            ),
                          )}
                        </div>
                      </div>

                      {errors.code && (
                        <p
                          id="recovery-code-error"
                          className="recovery-page__field-error"
                          role="alert"
                        >
                          {errors.code}
                        </p>
                      )}
                    </div>
                  )}

                  {/* Bước 3: Nhập mật khẩu mới */}
                  {step === "password" &&
                    passwordFields.map((field) => {
                      const visible =
                        showPasswords[field.name];

                      return (
                        <FormField
                          key={field.name}
                          label={field.label}
                          htmlFor={`recovery-${field.name}`}
                          required
                          error={errors[field.name]}
                          supportingText={
                            field.supportingText
                          }
                        >
                          <Input
                            id={`recovery-${field.name}`}
                            name={field.name}
                            type={
                              visible ? "text" : "password"
                            }
                            size="lg"
                            value={form[field.name]}
                            placeholder={field.placeholder}
                            autoComplete="new-password"
                            maxLength={72}
                            required
                            autoFocus={
                              field.name === "newPassword"
                            }
                            error={Boolean(
                              errors[field.name],
                            )}
                            trailingIcon={
                              <button
                                type="button"
                                className="recovery-page__password-toggle"
                                aria-label={`${
                                  visible ? "Ẩn" : "Hiện"
                                } ${field.label.toLowerCase()}`}
                                aria-pressed={visible}
                                onClick={() => {
                                  setShowPasswords(
                                    (current) => ({
                                      ...current,
                                      [field.name]:
                                        !current[
                                          field.name
                                        ],
                                    }),
                                  );
                                }}
                              >
                                {visible ? (
                                  <EyeOff
                                    size={20}
                                    aria-hidden="true"
                                  />
                                ) : (
                                  <Eye
                                    size={20}
                                    aria-hidden="true"
                                  />
                                )}
                              </button>
                            }
                            onChange={(event) =>
                              changeField(
                                field.name,
                                event.target.value,
                              )
                            }
                          />
                        </FormField>
                      );
                    })}
                </fieldset>

                {formError && (
                  <p
                    className="recovery-page__feedback recovery-page__feedback--error"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                {notice && (
                  <p
                    className="recovery-page__feedback"
                    role="status"
                  >
                    {notice}
                  </p>
                )}

                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  fullWidth
                  loading={loading}
                >
                  {content.button}
                </Button>
              </form>

              {step === "code" && (
                <div className="recovery-page__resend">
                  <span>Chưa nhận được mã?</span>

                  <button
                    type="button"
                    className="recovery-page__text-button"
                    disabled={
                      loading || resendSeconds > 0
                    }
                    onClick={() => {
                      if (
                        Date.now() < resendAvailableAt
                      ) {
                        return;
                      }

                      void sendCode();
                    }}
                  >
                    {resendSeconds > 0
                      ? `Gửi lại sau ${resendSeconds}s`
                      : "Gửi lại mã"}
                  </button>
                </div>
              )}

              <Link
                to="/login"
                className="recovery-page__back"
              >
                Quay lại đăng nhập
              </Link>
            </>
          )}
        </section>
      </main>
    </div>
  );
}