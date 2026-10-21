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

type RecoveryStep = "email" | "code" | "password" | "success";

interface RecoveryForm {
  email: string;
  newPassword: string;
  confirmPassword: string;
}

type RecoveryErrors =
  Partial<Record<keyof RecoveryForm | "code", string>>;

const emptyDigits = () => Array<string>(6).fill("");

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

export function ForgotPasswordPage() {
  const [step, setStep] = useState<RecoveryStep>("email");

  const [form, setForm] = useState<RecoveryForm>({
    email: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [digits, setDigits] = useState<string[]>(emptyDigits);
  const digitRefs = useRef<Array<HTMLInputElement | null>>([]);

  const [errors, setErrors] = useState<RecoveryErrors>({});
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");

  const [loading, setLoading] = useState(false);

  // Ref ngăn hai request được gửi trước khi React cập nhật loading.
  const requestInProgress = useRef(false);

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);

  // Đây là token đặt lại mật khẩu, không phải access token đăng nhập.
  const [resetToken, setResetToken] = useState("");
  const [resetExpiresAt, setResetExpiresAt] = useState(0);
 // Thời điểm được phép gửi lại mã, tính bằng mili giây.
  const [resendAvailableAt, setResendAvailableAt] = useState(0);
   // Số giây còn phải chờ để hiển thị trên giao diện.
  const [resendSeconds, setResendSeconds] = useState(0);

  // Tính theo thời gian thực để không bị lệch khi tab chạy nền.
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
      digitRefs.current[0]?.focus();
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

  function restartRecovery(message = "") {
    setStep("email");
    setResetToken("");
    setResetExpiresAt(0);
    setDigits(emptyDigits());

    setForm((current) => ({
      ...current,
      newPassword: "",
      confirmPassword: "",
    }));

    setShowPassword(false);
    setShowConfirmation(false);

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

      setDigits(emptyDigits());
      setResetToken("");
      setResetExpiresAt(0);

      // Backend hiện cấu hình resend-cooldown=60s.
      setResendSeconds(60);
      setResendAvailableAt(Date.now() + 60_000);

      setNotice(response.message);
      setStep("code");

      digitRefs.current[0]?.focus();
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

    if (step === "code" && !/^\d{6}$/.test(digits.join(""))) {
      nextErrors.code = "Vui lòng nhập đủ 6 chữ số.";
    }

    if (step === "password") {
      const password = form.newPassword;

      if (!password) {
        nextErrors.newPassword = "Vui lòng nhập mật khẩu mới.";
      } else if (password.length < 8) {
        nextErrors.newPassword = "Mật khẩu phải có ít nhất 8 ký tự.";
      } else if (
        password.length > 72 ||
        new TextEncoder().encode(password).length > 72
      ) {
        // Backend dùng BCrypt nên giới hạn theo byte UTF-8.
        nextErrors.newPassword =
          "Mật khẩu không được vượt quá 72 byte.";
      } else if (
        !/[A-Za-z]/.test(password) ||
        !/\d/.test(password)
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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (requestInProgress.current) return;

    clearFeedback();

    if (!validateForm()) return;

    if (step === "email") {
      await sendCode();
      return;
    }

    if (step === "code") {
      await runRequest(async () => {
        const response = await passwordResetApi.verifyResetCode({
          email: form.email,
          code: digits.join(""),
        });

        setResetToken(response.resetToken);

        // Dùng TTL backend trả về, không tự hardcode thời hạn token.
        setResetExpiresAt(
          Date.now() + response.expiresInSeconds * 1000,
        );

        setDigits(emptyDigits());
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
        const response = await passwordResetApi.resetPassword({
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

        setNotice(response.message);
        setStep("success");
      });
    }
  }

  function changeDigit(index: number, value: string) {
    // OTP phải giữ dạng chuỗi để không mất số 0 ở đầu.
    if (!/^\d?$/.test(value)) return;

    setDigits((current) =>
      current.map((digit, position) =>
        position === index ? value : digit,
      ),
    );

    setErrors((current) => ({
      ...current,
      code: undefined,
    }));

    setFormError("");
    setNotice("");

    if (value && index < 5) {
      digitRefs.current[index + 1]?.focus();
    }
  }

  const content =
    step === "success" ? null : stepContent[step];

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

                <p className="recovery-page__description" role="status">
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

                <h1 id="recovery-title">{content.title}</h1>

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
                          changeField("email", event.target.value)
                        }
                      />
                    </FormField>
                  )}

                  {step === "code" && (
                    <div
                      className="recovery-page__code-field"
                      role="group"
                      aria-labelledby="recovery-code-label"
                      aria-describedby={
                        errors.code ? "recovery-code-error" : undefined
                      }
                    >
                      <span id="recovery-code-label">
                        Mã xác thực
                      </span>

                      <div
                        className="recovery-page__otp"
                        onPaste={(event) => {
                          event.preventDefault();

                          const pasted = event.clipboardData
                            .getData("text")
                            .replace(/\D/g, "")
                            .slice(0, 6);

                          if (!pasted) return;

                          setDigits(
                            Array.from(
                              { length: 6 },
                              (_, index) => pasted[index] ?? "",
                            ),
                          );

                          setErrors((current) => ({
                            ...current,
                            code: undefined,
                          }));

                          setFormError("");
                          setNotice("");

                          digitRefs.current[
                            Math.min(pasted.length, 5)
                          ]?.focus();
                        }}
                      >
                        {digits.map((digit, index) => (
                          <input
                            key={index}
                            ref={(element) => {
                              digitRefs.current[index] = element;
                            }}
                            className="recovery-page__otp-input"
                            type="text"
                            inputMode="numeric"
                            autoComplete={
                              index === 0 ? "one-time-code" : "off"
                            }
                            maxLength={1}
                            value={digit}
                            aria-label={`Chữ số ${index + 1}`}
                            aria-invalid={Boolean(errors.code)}
                            onFocus={(event) =>
                              event.currentTarget.select()
                            }
                            onChange={(event) =>
                              changeDigit(index, event.target.value)
                            }
                            onKeyDown={(event) => {
                              if (
                                event.key === "Backspace" &&
                                !digit &&
                                index > 0
                              ) {
                                digitRefs.current[index - 1]?.focus();
                              }

                              if (
                                event.key === "ArrowLeft" &&
                                index > 0
                              ) {
                                event.preventDefault();
                                digitRefs.current[index - 1]?.focus();
                              }

                              if (
                                event.key === "ArrowRight" &&
                                index < 5
                              ) {
                                event.preventDefault();
                                digitRefs.current[index + 1]?.focus();
                              }
                            }}
                          />
                        ))}
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

                  {step === "password" && (
                    <>
                      <FormField
                        label="Mật khẩu mới"
                        htmlFor="recovery-password"
                        required
                        error={errors.newPassword}
                        supportingText="Tối thiểu 8 ký tự, gồm chữ và số."
                      >
                        <Input
                          id="recovery-password"
                          name="newPassword"
                          type={showPassword ? "text" : "password"}
                          size="lg"
                          value={form.newPassword}
                          placeholder="Nhập mật khẩu mới"
                          autoComplete="new-password"
                          maxLength={72}
                          required
                          autoFocus
                          error={Boolean(errors.newPassword)}
                          trailingIcon={
                            <button
                              type="button"
                              className="recovery-page__password-toggle"
                              aria-label={
                                showPassword
                                  ? "Ẩn mật khẩu mới"
                                  : "Hiện mật khẩu mới"
                              }
                              aria-pressed={showPassword}
                              onClick={() =>
                                setShowPassword((current) => !current)
                              }
                            >
                              {showPassword
                                ? <EyeOff size={20} />
                                : <Eye size={20} />}
                            </button>
                          }
                          onChange={(event) =>
                            changeField(
                              "newPassword",
                              event.target.value,
                            )
                          }
                        />
                      </FormField>

                      <FormField
                        label="Xác nhận mật khẩu mới"
                        htmlFor="recovery-confirmation"
                        required
                        error={errors.confirmPassword}
                      >
                        <Input
                          id="recovery-confirmation"
                          name="confirmPassword"
                          type={
                            showConfirmation ? "text" : "password"
                          }
                          size="lg"
                          value={form.confirmPassword}
                          placeholder="Nhập lại mật khẩu mới"
                          autoComplete="new-password"
                          maxLength={72}
                          required
                          error={Boolean(errors.confirmPassword)}
                          trailingIcon={
                            <button
                              type="button"
                              className="recovery-page__password-toggle"
                              aria-label={
                                showConfirmation
                                  ? "Ẩn mật khẩu xác nhận"
                                  : "Hiện mật khẩu xác nhận"
                              }
                              aria-pressed={showConfirmation}
                              onClick={() =>
                                setShowConfirmation(
                                  (current) => !current,
                                )
                              }
                            >
                              {showConfirmation
                                ? <EyeOff size={20} />
                                : <Eye size={20} />}
                            </button>
                          }
                          onChange={(event) =>
                            changeField(
                              "confirmPassword",
                              event.target.value,
                            )
                          }
                        />
                      </FormField>
                    </>
                  )}
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
                    disabled={loading || resendSeconds > 0}
                    onClick={() => {
                      if (Date.now() < resendAvailableAt) return;
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