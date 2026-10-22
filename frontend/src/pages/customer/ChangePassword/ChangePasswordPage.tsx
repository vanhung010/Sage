// Các hook dùng để quản lý dữ liệu, request và focus.
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";

// Hàm điều hướng của React Router.
import { useNavigate } from "react-router-dom";

// Toàn bộ icon của trang lấy từ lucide-react.
import {
  Eye, // Hiện mật khẩu.
  EyeOff, // Ẩn mật khẩu.
  Menu, // Mở menu mobile.
  LoaderCircle, // Spinner khi đang lưu.
} from "lucide-react";

// Tái sử dụng các component có sẵn trong dự án.
import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { NavigationBreadcrumb } from "../../../components/ui/NavigationBreadcrumb";
import { FormField } from "../../../components/ui/FormField";
import { Input } from "../../../components/ui/Input";
import { Button } from "../../../components/ui/Button";

// Hàm gọi API và kiểu dữ liệu của request.
import {
  accountApi,
  type ChangePasswordRequest,
} from "../../../api/account.api";

// Chuyển lỗi Axios về định dạng lỗi của ứng dụng.
import { getApiError } from "../../../api/error";

// Logo là ảnh thương hiệu, không phải icon giao diện.
import headerLogo from "../../../assets/logo/sage-logo-light.svg";
import footerLogo from "../../../assets/logo/sage-logo-dark.svg";

// CSS riêng của trang.
import "./ChangePasswordPage.css";

// Chỉ cho phép tên của ba trường mật khẩu.
type PasswordField = keyof ChangePasswordRequest;

// Mỗi trường có thể có hoặc không có lỗi.
type FieldErrors = Partial<Record<PasswordField, string>>;

// Dữ liệu khi người dùng mới mở trang.
const emptyForm: ChangePasswordRequest = {
  currentPassword: "",
  newPassword: "",
  repeatPassword: "",
};

// Cấu hình dùng để tạo ba ô nhập theo cùng một cấu trúc.
const fields = [
  {
    name: "currentPassword",
    label: "Mật khẩu hiện tại",
    placeholder: "Nhập mật khẩu hiện tại",
    autoComplete: "current-password",
  },
  {
    name: "newPassword",
    label: "Mật khẩu mới",
    placeholder: "Nhập mật khẩu mới",
    autoComplete: "new-password",
  },
  {
    name: "repeatPassword",
    label: "Xác nhận mật khẩu mới",
    placeholder: "Nhập lại mật khẩu mới",
    autoComplete: "new-password",
  },
] as const;

// Dùng cùng thông tin footer đang có trên HomePage.
const restaurantInfo = {
  description:
    "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

export function ChangePasswordPage() {
  const navigate = useNavigate(); // Dùng để chuyển sang trang khác.

  const [form, setForm] = useState(emptyForm); // Dữ liệu ba input.
  const [errors, setErrors] = useState<FieldErrors>({}); // Lỗi từng input.
  const [formError, setFormError] = useState(""); // Lỗi chung của form.
  const [saving, setSaving] = useState(false); // Đang gọi API hay chưa.

  // Từng input có thể hiện/ẩn mật khẩu độc lập.
  const [visible, setVisible] = useState<Record<PasswordField, boolean>>({
    currentPassword: false,
    newPassword: false,
    repeatPassword: false,
  });

  // Giữ tham chiếu tới phần tử form thật trong DOM.
  const formRef = useRef<HTMLFormElement>(null);

  // Request đang chạy; đồng thời là khóa chống submit liên tiếp.
  const requestRef = useRef<AbortController | null>(null);

  // Ghi nhớ input cần focus sau khi cập nhật lỗi.
  const focusFieldRef = useRef<PasswordField | null>(null);

  useEffect(() => {
    // Rời trang thì ngừng chờ request và bỏ qua kết quả trả về muộn.
    return () => requestRef.current?.abort();
  }, []);

  useEffect(() => {
    // Không focus khi input vẫn đang disabled trong lúc gửi.
    if (saving || !focusFieldRef.current) return;

    const field = focusFieldRef.current; // Lấy tên input cần focus.
    focusFieldRef.current = null; // Đánh dấu đã xử lý yêu cầu focus.

    // Đưa con trỏ vào input đang có lỗi.
    formRef.current
      ?.querySelector<HTMLInputElement>(`input[name="${field}"]`)
      ?.focus();
  }, [errors, saving]); // Chạy lại khi lỗi hoặc trạng thái gửi thay đổi.

  function changeField(field: PasswordField, value: string) {
    // Giữ nguyên các trường khác, cập nhật trường đang được nhập.
    setForm((current) => ({ ...current, [field]: value }));

    // Bỏ lỗi cũ của trường vừa sửa.
    setErrors((current) => ({
      ...current,
      [field]: undefined,

      // Nếu sửa mật khẩu mới thì cần kiểm tra lại xác nhận.
      ...(field === "newPassword"
        ? { repeatPassword: undefined }
        : {}),
    }));

    setFormError(""); // Bỏ lỗi API cũ khi người dùng chỉnh dữ liệu.
  }

  function showFieldErrors(nextErrors: FieldErrors) {
    // Lấy tên trường lỗi đầu tiên.
    const firstField = Object.keys(nextErrors)[0] as
      | PasswordField
      | undefined;

    // Lưu yêu cầu focus; effect sẽ xử lý khi input có thể nhận focus.
    focusFieldRef.current = firstField ?? null;

    setErrors(nextErrors); // Cập nhật lỗi để React render lại.
  }

  function validateForm(): boolean {
    const nextErrors: FieldErrors = {}; // Danh sách lỗi mới.
    const password = form.newPassword; // Giữ nguyên mật khẩu người dùng nhập.

    // trim chỉ dùng để phát hiện chuỗi rỗng/toàn khoảng trắng.
    if (!form.currentPassword.trim()) {
      nextErrors.currentPassword = "Vui lòng nhập mật khẩu hiện tại.";
    }

    if (!password.trim()) {
      nextErrors.newPassword = "Vui lòng nhập mật khẩu mới.";
    } else if (password.length < 8 || password.length > 72) {
      nextErrors.newPassword =
        "Mật khẩu mới phải có từ 8 đến 72 ký tự.";
    } else if (!/^(?=.*[A-Za-z])(?=.*[0-9]).+$/.test(password)) {
      // Cùng điều kiện có chữ và số với DTO backend.
      nextErrors.newPassword =
        "Mật khẩu mới phải chứa ít nhất một chữ cái và một chữ số.";
    } else if (new TextEncoder().encode(password).length > 72) {
      // Kiểm tra thêm giới hạn byte của BCrypt với ký tự Unicode.
      nextErrors.newPassword =
        "Mật khẩu mới không được vượt quá 72 byte.";
    }

    if (!form.repeatPassword.trim()) {
      nextErrors.repeatPassword = "Vui lòng nhập lại mật khẩu mới.";
    } else if (form.repeatPassword !== password) {
      nextErrors.repeatPassword =
        "Mật khẩu xác nhận chưa khớp với mật khẩu mới.";
    }

    showFieldErrors(nextErrors); // Hiển thị lỗi và chuẩn bị focus.

    // Không có key nào nghĩa là không có lỗi.
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); // Không tải lại trang khi submit.

    if (requestRef.current) return; // Đang gửi thì không gửi thêm.

    setFormError(""); // Xóa lỗi API của lần gửi trước.

    if (!validateForm()) return; // Dữ liệu sai thì dừng tại frontend.

    const controller = new AbortController(); // Tạo bộ điều khiển request.
    requestRef.current = controller; // Khóa submit ngay lập tức.
    setSaving(true); // Hiện loading và khóa các điều khiển.

    try {
      // Gọi backend với ba trường mật khẩu.
      await accountApi.changePassword(form, controller.signal);

      if (controller.signal.aborted) return; // Đã rời trang thì bỏ qua.

      setForm({ ...emptyForm }); // Xóa mật khẩu khỏi state trang này.

      // Chuyển về trang chủ ngay khi backend xác nhận thành công.
      navigate("/", {
        replace: true, // Thay entry trang đổi mật khẩu trong history.
        state: { passwordChanged: true }, // Cờ để Home mở modal.
      });
    } catch (error) {
      if (controller.signal.aborted) return; // Bỏ lỗi do ngừng chờ request.

      const problem = getApiError(error); // Lấy status, code, message.

      // Backend dùng cùng code cho hai lỗi nên phải kiểm tra message.
      if (
        problem.code === "PASSWORD NOT MATCH" &&
        problem.message === "Mật khẩu hiện tại không khớp"
      ) {
        showFieldErrors({
          currentPassword:
            "Mật khẩu hiện tại không đúng. Vui lòng thử lại.",
        });
      } else if (
        problem.code === "PASSWORD NOT MATCH" &&
        problem.message === "2 mật khẩu không khớp"
      ) {
        showFieldErrors({
          repeatPassword:
            "Mật khẩu xác nhận chưa khớp với mật khẩu mới.",
        });
      } else if (problem.status === 0 || problem.status >= 500) {
        // Không tự động retry vì server có thể đã đổi mật khẩu.
        setFormError(
          "Chưa nhận được xác nhận từ máy chủ. Mật khẩu có thể đã được thay đổi; hãy kiểm tra bằng cách đăng nhập trước khi thử lại.",
        );
      } else {
        // Các lỗi còn lại hiển thị ở cuối form.
        setFormError(problem.message || "Không thể đổi mật khẩu.");
      }
    } finally {
      requestRef.current = null; // Mở khóa submit.

      if (!controller.signal.aborted) {
        setSaving(false); // Mở lại input nếu request chưa bị bỏ qua.
      }
    }
  }

  return (
    <div className="change-password-page">
      {/* Header dùng tài khoản đăng nhập và icon menu thư viện. */}
      <Header
        variant="customer"
        active={null}
        logoSrc={headerLogo}
        mobileMenuIcon={
          <Menu size={20} strokeWidth={1.5} aria-hidden="true" />
        }
      />

      <main>
        {/* Dải tiêu đề có nền be theo Figma. */}
        <section className="change-password-page__heading">
          <div className="change-password-page__container">
            <NavigationBreadcrumb
              items={[
                { label: "Tài khoản" },
                { label: "Đổi mật khẩu" },
              ]}
              separator={<span aria-hidden="true">/</span>}
            />

            <h1>Đổi mật khẩu</h1>
            <p>Cập nhật mật khẩu để bảo vệ tài khoản Sage của bạn.</p>
          </div>
        </section>

        <section
          className="change-password-page__main"
          aria-label="Biểu mẫu đổi mật khẩu"
        >
          <form
            ref={formRef}
            className="change-password-page__form"
            onSubmit={handleSubmit}
            noValidate
            aria-busy={saving}
          >
            {/* Tạo ba trường từ cấu hình, tránh lặp JSX. */}
            {fields.map((field) => {
              const error = errors[field.name]; // Lỗi của trường này.
              const isNewPassword = field.name === "newPassword";
              const messageId = `change-password-${field.name}-message`;

              // Ưu tiên lỗi; nếu không có thì hiện hướng dẫn mật khẩu mới.
              const message =
                error ||
                (isNewPassword
                  ? "8–72 ký tự, gồm ít nhất một chữ cái và một chữ số."
                  : "");

              return (
                <div
                  key={field.name}
                  className="change-password-page__field"
                >
                  <FormField
                    label={field.label}
                    htmlFor={`change-password-${field.name}`}
                    required
                  >
                    <Input
                      id={`change-password-${field.name}`}
                      name={field.name}
                      type={visible[field.name] ? "text" : "password"}
                      size="lg"
                      value={form[field.name]}
                      placeholder={field.placeholder}
                      autoComplete={field.autoComplete}
                      required
                      disabled={saving}
                      error={Boolean(error)}
                      aria-describedby={message ? messageId : undefined}
                      onChange={(event) =>
                        changeField(field.name, event.target.value)
                      }
                      trailingIcon={
                        <button
                          type="button"
                          className="change-password-page__toggle"
                          disabled={saving}
                          aria-label={`${
                            visible[field.name] ? "Ẩn" : "Hiện"
                          } ${field.label.toLowerCase()}`}
                          aria-pressed={visible[field.name]}
                          onClick={() =>
                            setVisible((current) => ({
                              ...current,
                              [field.name]: !current[field.name],
                            }))
                          }
                        >
                          {visible[field.name] ? (
                            <EyeOff
                              size={20}
                              strokeWidth={1.5}
                              aria-hidden="true"
                            />
                          ) : (
                            <Eye
                              size={20}
                              strokeWidth={1.5}
                              aria-hidden="true"
                            />
                          )}
                        </button>
                      }
                    />

                    {/* Thông báo có id để input tham chiếu qua aria. */}
                    {message && (
                      <p
                        id={messageId}
                        className={
                          error
                            ? "change-password-page__field-error"
                            : "change-password-page__hint"
                        }
                        role={error ? "alert" : undefined}
                      >
                        {message}
                      </p>
                    )}
                  </FormField>

                  {/* Chỉ trường mật khẩu hiện tại có liên kết khôi phục. */}
                  {field.name === "currentPassword" && (
                    <button
                      type="button"
                      className="change-password-page__recovery"
                      disabled={saving}
                      onClick={() => navigate("/forgot-password")}
                    >
                      Quên mật khẩu?
                    </button>
                  )}
                </div>
              );
            })}

            {/* Lỗi API không gắn với một trường cụ thể. */}
            {formError && (
              <p
                className="change-password-page__form-error"
                role="alert"
              >
                {formError}
              </p>
            )}

            <div className="change-password-page__actions">
              <Button
                type="button"
                variant="outline"
                size="lg"
                disabled={saving}
                onClick={() => navigate("/")}
              >
                Hủy
              </Button>

              <Button
                type="submit"
                size="lg"
                disabled={saving}
                aria-busy={saving}
                aria-label={saving ? "Đang lưu mật khẩu mới" : undefined}
                className={`change-password-page__submit${
                  saving ? " is-loading" : ""
                }`}
              >
                {/* Giữ diện tích chữ để nút loading không co quá nhỏ. */}
                <span className="change-password-page__submit-label">
                  {saving ? "Đang lưu..." : "Lưu mật khẩu mới"}
                </span>

                {saving && (
                  <LoaderCircle
                    className="change-password-page__spinner"
                    size={16}
                    strokeWidth={2}
                    aria-hidden="true"
                  />
                )}
              </Button>
            </div>

            {/* Thông báo loading cho cả người dùng và trình đọc màn hình. */}
            {saving && (
              <p className="change-password-page__hint" role="status">
                Đang lưu mật khẩu mới...
              </p>
            )}
          </form>

          <p className="change-password-page__security-note">
            Chỉ bạn mới biết mật khẩu của mình.
            <br />
            Không chia sẻ mật khẩu với bất kỳ ai.
          </p>
        </section>
      </main>

      {/* Không truyền contactIcons: Footer tự dùng MapPin, Phone, Mail. */}
      <Footer
        restaurantInfo={restaurantInfo}
        logoSrc={footerLogo}
        mobileSystemLabel="Hệ thống đặt bàn nhà hàng"
      />
    </div>
  );
}