import { useEffect, useRef } from "react";
import { useRealtime } from "./RealtimeProvider";
import type { RealtimeSignal } from "./types";

export function useRealtimeRefresh(
  accepts: (signal: RealtimeSignal) => boolean,
  refresh: () => void,
) {
  const { subscribe } = useRealtime();
  const callbacks = useRef({ accepts, refresh });

  // Cập nhật callback mới nhất mà không đăng ký lại mỗi lần render.
  useEffect(() => { callbacks.current = { accepts, refresh }; }, [accepts, refresh]);

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    const unsubscribe = subscribe((signal) => {
      if (!callbacks.current.accepts(signal) || timer !== undefined) return;
      // Gộp các event gần nhau thành một lần tải lại.
      timer = setTimeout(() => {
        timer = undefined;
        callbacks.current.refresh();
      }, 150);
    });
    return () => {
      unsubscribe();
      if (timer !== undefined) clearTimeout(timer);
    };
  }, [subscribe]);
}