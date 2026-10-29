import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  type ReactNode,
} from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import { useAuth } from "../auth/AuthContext";
import { authApi } from "../api/auth.api";
import { authStorage } from "../storage/authStorage";
import { parseRealtimeEvent, type RealtimeSignal } from "./types";

type Listener = (signal: RealtimeSignal) => void;
interface RealtimeContextValue {
  subscribe: (listener: Listener) => () => void;
}

const RealtimeContext = createContext<RealtimeContextValue | null>(null);

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const listeners = useRef(new Set<Listener>());

  const subscribe = useCallback((listener: Listener) => {
    listeners.current.add(listener);
    return () => { listeners.current.delete(listener); };
  }, []);

  const emit = useCallback((signal: RealtimeSignal) => {
    for (const listener of listeners.current) {
      try {
        listener(signal);
      } catch {
        console.error("Không thể xử lý một cập nhật realtime.");
      }
    }
  }, []);

  useEffect(() => {
    const token = authStorage.getAccessToken();
    if (!user || !token) return;

    let disposed = false;
    let lastSessionCheck = 0;

    const client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL,
      reconnectDelay: 3000,
      connectionTimeout: 10000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {}, // Không log STOMP CONNECT chứa JWT.
    });

    const syncVisibleScreen = () => {
      if (!disposed && document.visibilityState === "visible"
          && authStorage.getAccessToken() === token) {
        emit({ type: "SYNC" });
      }
    };

    const verifySession = async () => {
      if (disposed || authStorage.getAccessToken() !== token) return;
      if (Date.now() - lastSessionCheck < 3000) return;
      lastSessionCheck = Date.now();
      try {
        // HTTP interceptor trong AuthContext đang xử lý 401 + logout.
        await authApi.getCurrentUser();
      } catch {
        // 401 được auth layer xử lý; lỗi mạng cho phép thử reconnect.
      }
    };

    client.beforeConnect = () => {
      if (disposed || authStorage.getAccessToken() !== token) {
        void client.deactivate();
        return;
      }
      client.connectHeaders = { Authorization: "Bearer " + token };
    };

    client.onConnect = () => {
      if (disposed) return;

      const destinations = user.role === "CUSTOMER"
        ? ["/user/queue/reservations", "/user/queue/food-orders"]
        : ["/topic/staff/reservations", "/topic/staff/food-orders"];

      const receive = (message: IMessage) => {
        if (disposed) return;
        const event = parseRealtimeEvent(message.body);
        if (event) emit(event);
      };

      // onConnect chạy lại sau mỗi reconnect, do đó luôn đăng ký lại.
      for (const destination of destinations) {
        client.subscribe(destination, receive);
      }
      syncVisibleScreen();
    };

    client.onStompError = () => { void verifySession(); };
    client.onWebSocketClose = () => { void verifySession(); };

    const handleStorage = (event: StorageEvent) => {
      if ((event.key === "sage_access_token" || event.key === null)
          && authStorage.getAccessToken() !== token) {
        void client.deactivate();
        // Đồng bộ cả AuthContext khi đăng nhập/đăng xuất ở tab khác.
        window.location.reload();
      }
    };

    window.addEventListener("focus", syncVisibleScreen);
    document.addEventListener("visibilitychange", syncVisibleScreen);
    window.addEventListener("storage", handleStorage);

    // Đối chiếu REST để bù thông báo bỏ lỡ trong MVP không có outbox/replay.
    const interval = window.setInterval(syncVisibleScreen, 30000);
    client.activate();

    return () => {
      disposed = true;
      window.clearInterval(interval);
      window.removeEventListener("focus", syncVisibleScreen);
      document.removeEventListener("visibilitychange", syncVisibleScreen);
      window.removeEventListener("storage", handleStorage);
      void client.deactivate();
    };
  }, [user, emit]);

  const value = useMemo(() => ({ subscribe }), [subscribe]);
  return (
    <RealtimeContext.Provider value={value}>
      {children}
    </RealtimeContext.Provider>
  );
}

export function useRealtime() {
  const context = useContext(RealtimeContext);
  if (!context) throw new Error("Thiếu RealtimeProvider");
  return context;
}