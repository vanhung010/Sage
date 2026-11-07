import { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { UserMenuTrigger } from "../../common/UserMenu/UserMenuTrigger";

import "./StaffUserMenu.css";

interface StaffUserMenuProps {
  userName: string;
  onLogout: () => void;
}

export function StaffUserMenu({
  userName,
  onLogout,
}: StaffUserMenuProps) {
  const [open, setOpen] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const dropdownId = useId();

  useEffect(() => {
    if (!open) return;

    function handlePointerDown(event: PointerEvent) {
      if (
        event.target instanceof Node &&
        !containerRef.current?.contains(event.target)
      ) {
        setOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
    };
  }, [open]);

  function handleLogout() {
    setOpen(false);
    onLogout();
  }

  return (
    <div
      ref={containerRef}
      className="staff-user-menu"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
          setOpen(false);
        }
      }}
      onKeyDown={(event) => {
        if (event.key !== "Escape" || !open) return;

        event.preventDefault();
        event.stopPropagation();

        setOpen(false);

        containerRef.current
          ?.querySelector<HTMLButtonElement>("button")
          ?.focus();
      }}
    >
      <UserMenuTrigger
        name={userName}
        open={open}
        className="staff-user-menu__trigger"
        aria-label={`Tài khoản ${userName}`}
        aria-haspopup={undefined}
        aria-controls={open ? dropdownId : undefined}
        onClick={() => setOpen((current) => !current)}
      />

      {open && (
        <div
          id={dropdownId}
          className="sage-user-menu__dropdown staff-user-menu__dropdown"
          role="group"
          aria-label="Các thao tác tài khoản"
        >
          <Link
            to="/"
            className="sage-user-menu-item"
            onClick={() => setOpen(false)}
          >
            Trang khách hàng
          </Link>

          <div className="sage-user-menu__divider" />

          <button
            type="button"
            className="sage-user-menu-item sage-user-menu-item--danger"
            onClick={handleLogout}
          >
            Đăng xuất
          </button>
        </div>
      )}
    </div>
  );
}