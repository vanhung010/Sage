import { ChevronRight } from "lucide-react";
import type { ReactNode } from "react";

import { NavigationBreadcrumbItem } from "./NavigationBreadcrumbItem";

import "./NavigationBreadcrumb.css";

export interface BreadcrumbEntry {
  label: string;
  to?: string;
}

interface NavigationBreadcrumbProps {
  items: BreadcrumbEntry[];
  className?: string;
  separator?: ReactNode;
}

export function NavigationBreadcrumb({
  items,
  className = "",
  separator,
}: NavigationBreadcrumbProps) {
  if (items.length === 0) return null;

  const classes = ["sage-navigation-breadcrumb", className]
    .filter(Boolean)
    .join(" ");

  return (
    <nav className={classes} aria-label="Đường dẫn trang">
      <ol className="sage-navigation-breadcrumb__list">
        {items.map((item, index) => (
          <li
            className="sage-navigation-breadcrumb__step"
            key={`${index}-${item.label}`}
          >
            {index > 0 && (
              separator ?? <ChevronRight
                className="sage-navigation-breadcrumb__separator"
                size={16}
                strokeWidth={2}
                aria-hidden="true"
              />
            )}
            <NavigationBreadcrumbItem
              label={item.label}
              to={item.to}
              current={index === items.length - 1}
            />
          </li>
        ))}
      </ol>
    </nav>
  );
}
