import {
  Mail,
  MapPin,
  Phone,
} from "lucide-react";

import { Link } from "react-router-dom";

import sageLogo from "../../../assets/logo/sage-logo-dark.svg";

import "./Footer.css";

export interface FooterRestaurantInfo {
  description: string;
  address: string;
  phone: string;
  email: string;
}

interface FooterProps {
  logoSrc?: string;
  contactIcons?: { address: string; phone: string; mail: string };
  restaurantInfo: FooterRestaurantInfo;
  className?: string;
}

const navigationItems = [
  {
    label: "Trang chủ",
    to: "/",
  },
  {
    label: "Thực đơn",
    to: "/menu",
  },
  {
    label: "Đặt bàn",
    to: "/reservation",
  },
  {
    label: "Về Sage",
    to: "/#about",
  },
];

export function Footer({
  logoSrc,
  contactIcons,
  restaurantInfo,
  className = "",
}: FooterProps) {
  const classes = [
    "sage-footer",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <footer className={classes}>
      <div className="sage-footer__content">
        {/* Brand */}
        <div className="sage-footer__brand">
          <Link
            to="/"
            className="sage-footer__logo-link"
            aria-label="Sage - Trang chủ"
          >
            <img
              src={logoSrc ?? sageLogo}
              alt="Sage"
              className="sage-footer__logo"
            />
          </Link>

          <p className="sage-footer__description">
            {restaurantInfo.description}
          </p>
        </div>

        {/* Navigation */}
        <div className="sage-footer__navigation">
          <h3 className="sage-footer__title">
            Điều hướng
          </h3>

          <nav
            className="sage-footer__links"
            aria-label="Điều hướng cuối trang"
          >
            {navigationItems.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className="sage-footer__link"
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>

        {/* Contact */}
        <div className="sage-footer__contact">
          <h3 className="sage-footer__title">
            Liên hệ
          </h3>

          <div className="sage-footer__contact-list">
            <div className="sage-footer__contact-item">
              {contactIcons ? <img src={contactIcons.address} width={20} height={20} alt="" /> : <MapPin
                size={20}
                strokeWidth={1.8}
                aria-hidden="true"
              />}

              <span>
                {restaurantInfo.address}
              </span>
            </div>

            <a
              href={`tel:${restaurantInfo.phone}`}
              className="sage-footer__contact-item"
            >
              {contactIcons ? <img src={contactIcons.phone} width={20} height={20} alt="" /> : <Phone
                size={20}
                strokeWidth={1.8}
                aria-hidden="true"
              />}

              <span>
                {restaurantInfo.phone}
              </span>
            </a>

            <a
              href={`mailto:${restaurantInfo.email}`}
              className="sage-footer__contact-item"
            >
              {contactIcons ? <img src={contactIcons.mail} width={20} height={20} alt="" /> : <Mail
                size={20}
                strokeWidth={1.8}
                aria-hidden="true"
              />}

              <span>
                {restaurantInfo.email}
              </span>
            </a>
          </div>
        </div>
      </div>

      <div className="sage-footer__divider" />

      <div className="sage-footer__bottom">
        <span>
          © 2026 Sage. Tất cả quyền được bảo lưu.
        </span>

        <span>
          Restaurant Reservation System
        </span>
      </div>
    </footer>
  );
}
