import {
  useState,
  type FormEvent,
} from "react";

import {
  Eye,
  EyeOff,
} from "lucide-react";

import { Header } from "../../components/layout/Header";
import { FormField } from "../../components/ui/FormField";
import { Input } from "../../components/ui/Input";
import { Button } from "../../components/ui/Button";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { getApiError } from "../../api/error";
import "./LoginPage.css";

interface LoginForm {
  email: string;
  password: string;
}

interface LoginErrors {
  email?: string;
  password?: string;
}

type LoginField = keyof LoginForm;

const initialForm: LoginForm = {
  email: "",
  password: "",
};

export function LoginPage() {

  const navigate = useNavigate();
  const { login } = useAuth();

  const location = useLocation();

  const [form, setForm] =
    useState<LoginForm>(initialForm);

  const [errors, setErrors] =
    useState<LoginErrors>({});

  const [formError, setFormError] =
    useState("");

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] = useState(false);

  function handleChange(
    field: LoginField,
    value: string,
  ) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    /*
      Nếu field đang lỗi thì khi user nhập lại,
      xóa lỗi cũ để họ có thể sửa.
    */
    if (errors[field]) {
      setErrors((current) => ({
        ...current,
        [field]: undefined,
      }));
    }

    /*
      Nếu trước đó backend báo sai thông tin đăng nhập,
      user thay đổi email/password thì bỏ lỗi chung cũ.
    */
    if (formError) {
      setFormError("");
    }
  }

  function validateForm() {
    const newErrors: LoginErrors = {};

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

    /* Password */

    if (!form.password) {
      newErrors.password =
        "Vui lòng nhập mật khẩu.";
    }

    setErrors(newErrors);

    return (
      Object.keys(newErrors).length === 0
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>,) {
    event.preventDefault();
    //Đã đăng nhập
    if (loading) return;

    setFormError("");

    //Nếu lỗi prompt
    if (!validateForm()) return;

  try {
    setLoading(true);

    await login({ email: form.email.trim(), password: form.password,});

    const returnTo = (location.state as { returnTo?: string } | null)?.returnTo;
    navigate(returnTo === "/reservation" ? returnTo : "/", {
    replace: true,
});
  } catch (error) {
    setFormError(getApiError(error).message);
  } finally {
    setLoading(false);
  }
  }

  return (
    <div className="login-page">
      <Header variant="auth" />

      <main className="login-page__main">
        <div className="login-page__container">
          <section className="login-page__card">
            {/* Heading */}

            <div className="login-page__heading">
              <p className="login-page__eyebrow">
                CHÀO MỪNG TRỞ LẠI
              </p>

              <h1 className="login-page__title">
                Đăng nhập vào Sage
              </h1>

              <p className="login-page__description">
                Đăng nhập để quản lý đơn đặt bàn
                và đơn món của bạn.
              </p>
            </div>

            {/* Form */}

            <form
              className="login-page__form"
              onSubmit={handleSubmit}
              noValidate
            >
              {/* Email */}

              <FormField
                label="Email"
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

              {/* Password Group */}

              <div className="login-page__password-group">
                <FormField
                  label="Mật khẩu"
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
                    placeholder="Nhập mật khẩu"
                    autoComplete="current-password"
                    error={Boolean(
                      errors.password,
                    )}
                    trailingIcon={
                      <button
                        type="button"
                        className="login-page__password-toggle"
                        aria-label={
                          showPassword
                            ? "Ẩn mật khẩu"
                            : "Hiện mật khẩu"
                        }
                        onClick={() => {
                          setShowPassword(
                            (current) =>
                              !current,
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

                <Link to="/forgot-password" className="login-page__forgot-password"> Quên mật khẩu? </Link>
              </div>

              {/* Form Error */}

              {formError && (
                <div
                  className="login-page__form-error"
                  role="alert"
                  aria-live="polite"
                >
                  {formError}
                </div>
              )}

              {/* Login Button */}

              <Button
                type="submit"
                variant="primary"
                size="lg"
                fullWidth
                loading={loading}
              >
                Đăng nhập
              </Button>
            </form>

            {/* Register */}

            <div className="login-page__register">
              <span>
                Chưa có tài khoản?
              </span>

              <Link to="/register">Đăng kí</Link>

            </div>
          </section>
        </div>
      </main>
    </div>
  );
}