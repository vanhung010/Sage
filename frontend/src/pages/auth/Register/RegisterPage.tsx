import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

import { Header } from "../../../components/layout/Header";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";
import { Button } from "../../../components/ui/Button";

import "./RegisterPage.css";

import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../../../api/auth.api";
import { getApiError } from "../../../api/error";

interface RegisterForm {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

interface RegisterErrors {
  fullName?: string;
  email?: string;
  phone?: string;
  password?: string;
  confirmPassword?: string;
}

type RegisterField = keyof RegisterForm;

const initialForm: RegisterForm = {
  fullName: "",
  email: "",
  phone: "",
  password: "",
  confirmPassword: "",
};

export function RegisterPage() {

  const navigate = useNavigate();

  const [formError, setFormError] = useState("");

  const [form, setForm] =
    useState<RegisterForm>(initialForm);

  const [errors, setErrors] =
    useState<RegisterErrors>({});

  const [showPassword, setShowPassword] =
    useState(false);

  const [
    showConfirmPassword,
    setShowConfirmPassword,
  ] = useState(false);

  const [loading, setLoading] =
    useState(false);

  function handleChange(
    field: RegisterField,
    value: string,
  ) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    /*
      Nếu field đang có lỗi,
      khi user nhập lại thì bỏ lỗi cũ.
    */
    if (errors[field]) {
      setErrors((current) => ({
        ...current,
        [field]: undefined,
      }));
    }
  }

  function validateForm() {
    const newErrors: RegisterErrors = {};

    /* Full name */

    if (!form.fullName.trim()) {
      newErrors.fullName =
        "Vui lòng nhập họ và tên.";
    }

    /* Email */

    if (!form.email.trim()) {
      newErrors.email =
        "Vui lòng nhập email.";
    } else {
      const emailRegex =
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      if (!emailRegex.test(form.email)) {
        newErrors.email =
          "Email không đúng định dạng.";
      }
    }

    /* Phone */

    const phoneRegex = /^0\d{9}$/;

    if (!form.phone.trim()) {
      newErrors.phone =
        "Vui lòng nhập số điện thoại.";
    } else if (
      !phoneRegex.test(form.phone)
    ) {
      newErrors.phone =
        "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.";
    }

    /* Password */

    const passwordRegex =
      /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

    if (!form.password) {
      newErrors.password =
        "Vui lòng nhập mật khẩu.";
    } else if (
      !passwordRegex.test(form.password)
    ) {
      newErrors.password =
        "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.";
    }

    /* Confirm password */

    if (!form.confirmPassword) {
      newErrors.confirmPassword =
        "Vui lòng xác nhận mật khẩu.";
    } else if (
      form.password !== form.confirmPassword
    ) {
      newErrors.confirmPassword =
        "Mật khẩu xác nhận không khớp.";
    }

    setErrors(newErrors);

    return (
      Object.keys(newErrors).length === 0
    );
  }

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    if(loading) return;

    setFormError("");

    if(!validateForm()) return

    try {
      setLoading(true);
      await authApi.register({
        fullName: form.fullName.trim(),
         email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
      })

       navigate("/login", { replace: true }); 
  }
   catch (error) {
    setFormError(getApiError(error).message);
    }
    finally {
    setLoading(false);
  }
  }
  return (
    <div className="register-page">
      <Header variant="auth" />

      <main className="register-page__main">
        <div className="register-page__container">
          <section className="register-page__card">
            {/* Heading */}

            <div className="register-page__heading">
              <p className="register-page__eyebrow">
                THAM GIA SAGE
              </p>

              <h1 className="register-page__title">
                Tạo tài khoản
              </h1>

              <p className="register-page__description">
                Tạo tài khoản để đặt bàn, đặt món
                và quản lý các đơn của bạn.
              </p>
            </div>

            {/* Form */}

            <form
              className="register-page__form"
              onSubmit={handleSubmit}
              noValidate
            >
              {/* Full name */}

              <FormField
                label="Họ và tên"
                required
                error={errors.fullName}
                htmlFor="fullName"
              >
                <Input
                  id="fullName"
                  name="fullName"
                  size="lg"
                  value={form.fullName}
                  placeholder="Nhập họ và tên"
                  autoComplete="name"
                  error={Boolean(errors.fullName)}
                  onChange={(event) => {
                    handleChange(
                      "fullName",
                      event.target.value,
                    );
                  }}
                />
              </FormField>

              {/* Email */}

              <FormField
                label="Email"
                required
                error={errors.email}
                htmlFor="email"
              >
                <Input
                  id="email"
                  name="email"
                  type="email"
                  size="lg"
                  value={form.email}
                  placeholder="Nhập email của bạn"
                  autoComplete="email"
                  error={Boolean(errors.email)}
                  onChange={(event) => {
                    handleChange(
                      "email",
                      event.target.value,
                    );
                  }}
                />
              </FormField>

              {/* Phone */}

              <FormField
                label="Số điện thoại"
                required
                error={errors.phone}
                htmlFor="phone"
              >
                <Input
                  id="phone"
                  name="phone"
                  type="tel"
                  size="lg"
                  value={form.phone}
                  placeholder="Ví dụ: 0901234567"
                  autoComplete="tel"
                  inputMode="numeric"
                  error={Boolean(errors.phone)}
                  onChange={(event) => {
                    handleChange(
                      "phone",
                      event.target.value,
                    );
                  }}
                />
              </FormField>

              {/* Password */}

              <FormField
                label="Mật khẩu"
                required
                supportingText="Tối thiểu 8 ký tự, gồm chữ và số."
                error={errors.password}
                htmlFor="password"
              >
                <Input
                  id="password"
                  name="password"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  size="lg"
                  value={form.password}
                  placeholder="Tạo mật khẩu"
                  autoComplete="new-password"
                  error={Boolean(errors.password)}
                  trailingIcon={
                    <button
                      type="button"
                      className="register-page__password-toggle"
                      aria-label={
                        showPassword
                          ? "Ẩn mật khẩu"
                          : "Hiện mật khẩu"
                      }
                      onClick={() => {
                        setShowPassword(
                          (current) => !current,
                        );
                      }}
                    >
                      {showPassword ? (
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
                  onChange={(event) => {
                    handleChange(
                      "password",
                      event.target.value,
                    );
                  }}
                />
              </FormField>

              {/* Confirm password */}

              <FormField
                label="Xác nhận mật khẩu"
                required
                error={errors.confirmPassword}
                htmlFor="confirmPassword"
              >
                <Input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={
                    showConfirmPassword
                      ? "text"
                      : "password"
                  }
                  size="lg"
                  value={form.confirmPassword}
                  placeholder="Nhập lại mật khẩu"
                  autoComplete="new-password"
                  error={Boolean(
                    errors.confirmPassword,
                  )}
                  trailingIcon={
                    <button
                      type="button"
                      className="register-page__password-toggle"
                      aria-label={
                        showConfirmPassword
                          ? "Ẩn mật khẩu"
                          : "Hiện mật khẩu"
                      }
                      onClick={() => {
                        setShowConfirmPassword(
                          (current) => !current,
                        );
                      }}
                    >
                      {showConfirmPassword ? (
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
                  onChange={(event) => {
                    handleChange(
                      "confirmPassword",
                      event.target.value,
                    );
                  }}
                />
              </FormField>

              {/* Submit */}

              <div className="register-page__action">
                {formError && (
                <p role="alert" aria-live="polite">
                  {formError}
                </p>
              )}
                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  fullWidth
                  loading={loading}
                >
                  Đăng ký
                </Button>
              </div>
            </form>

            {/* Login */}

            <div className="register-page__login">
              <span>Đã có tài khoản?</span>

             <Link to = "/login">Đăng nhập</Link>

            </div>
          </section>
        </div>
      </main>
    </div>
  );
}