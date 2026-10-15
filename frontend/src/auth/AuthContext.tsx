import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

import { authApi } from "../api/auth.api";
import { apiClient } from "../api/client";
import { authStorage } from "../storage/authStorage";
import type { AuthUser, LoginRequest } from "../types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [initializationError, setInitializationError] = useState("");

  useEffect(() => {
    //Lấy token hiện tại
    let active = true;
    const token = authStorage.getAccessToken();

    // Xử lý token không còn hợp lệ trên các request có xác thực.
    const interceptorId = apiClient.interceptors.response.use(
      (response) => response,
      (error) => {
        const sentAuthorization = error.config?.headers?.Authorization;
        const currentToken = authStorage.getAccessToken();

        if (
          error.response?.status === 401 && currentToken && sentAuthorization === `Bearer ${currentToken}`
        ) {
          authStorage.clearAccessToken();
          setUser(null);
          window.location.replace("/");
        }

        return Promise.reject(error);
      },
    );

    async function restoreSession() {
      try {
        if (token) {
          const currentUser = await authApi.getCurrentUser();

          if (active && authStorage.getAccessToken() === token) {
            setUser(currentUser);
          }
        }
      } catch {
        // Lỗi mạng/server: giữ token và cho người dùng thử lại.
        if (active && authStorage.getAccessToken()) {
          setInitializationError(
            "Không thể kiểm tra phiên đăng nhập. Vui lòng thử lại.",
          );
        }
      } finally {
        if (active) setInitializing(false);
      }
    }

    void restoreSession();

    return () => {
      active = false;
      apiClient.interceptors.response.eject(interceptorId);
    };
  }, []);

  async function login(request: LoginRequest) {
    const response = await authApi.login(request);

    authStorage.setAccessToken(response.accessToken);
    setUser(response.user);
  }

  function logout() {
    authStorage.clearAccessToken();
    setUser(null);
  }

  if (initializing) {
    return <p>Đang kiểm tra phiên đăng nhập...</p>;
  }

  if (initializationError) {
    return (
      <div role="alert">
        <p>{initializationError}</p>
        <button onClick={() => window.location.reload()}>
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth phải nằm trong AuthProvider");
  }

  return context;
}