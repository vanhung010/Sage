import {
  useEffect,
  useRef,
  useState,
} from "react";

import { UserMenuTrigger } from "./UserMenuTrigger";
import { UserMenuItem } from "./UserMenuItem";

import "./UserMenu.css";

interface UserMenuProps {
  userName: string;

  onProfile?: () => void;
  onReservations?: () => void;
  onOrders?: () => void;
  onChangePassword?: () => void;
  onLogout?: () => void;

  className?: string;
}

export function UserMenu({
  userName,
  onProfile,
  onReservations,
  onOrders,
  onChangePassword,
  onLogout,
  className = "",
}: UserMenuProps) {
  const [open, setOpen] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);

  function handleSelect(
    callback?: () => void
  ) {
    callback?.();

    setOpen(false);
  }

  useEffect(() => {
    function handleClickOutside(
      event: MouseEvent
    ) {
      if (
        containerRef.current &&
        !containerRef.current.contains(
          event.target as Node
        )
      ) {
        setOpen(false);
      }
    }

    function handleEscape(
      event: KeyboardEvent
    ) {
      if (event.key === "Escape") {
        setOpen(false);
      }
    }

    document.addEventListener(
      "mousedown",
      handleClickOutside
    );

    document.addEventListener(
      "keydown",
      handleEscape
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        handleClickOutside
      );

      document.removeEventListener(
        "keydown",
        handleEscape
      );
    };
  }, []);

  const classes = [
    "sage-user-menu",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div
      ref={containerRef}
      className={classes}
    >
      <UserMenuTrigger
        name={userName}
        open={open}
        onClick={() =>
          setOpen((current) => !current)
        }
      />

      {open && (
        <div
          className="sage-user-menu__dropdown"
          role="menu"
        >
          <UserMenuItem
            onClick={() =>
              handleSelect(onProfile)
            }
          >
            Hồ sơ cá nhân
          </UserMenuItem>

          <UserMenuItem
            onClick={() =>
              handleSelect(onReservations)
            }
          >
            Đơn đặt bàn của tôi
          </UserMenuItem>

          <UserMenuItem
            onClick={() =>
              handleSelect(onOrders)
            }
          >
            Đơn đặt món của tôi
          </UserMenuItem>

          <UserMenuItem
            onClick={() =>
              handleSelect(
                onChangePassword
              )
            }
          >
            Đổi mật khẩu
          </UserMenuItem>

          <div className="sage-user-menu__divider" />

          <UserMenuItem
            variant="danger"
            onClick={() =>
              handleSelect(onLogout)
            }
          >
            Đăng xuất
          </UserMenuItem>
        </div>
      )}
    </div>
  );
}