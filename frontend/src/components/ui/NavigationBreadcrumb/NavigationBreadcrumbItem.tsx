import { Link } from "react-router-dom";

import "./NavigationBreadcrumb.css";

export interface NavigationBreadcrumbItemProps {
  label: string;
  to?: string;
  current?: boolean;
  className?: string;
}

export function NavigationBreadcrumbItem({
  label,
  to,
  current = false,
  className = "",
}: NavigationBreadcrumbItemProps) {
  const classes = [
    "sage-navigation-breadcrumb__item",
    current ? "sage-navigation-breadcrumb__item--current" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  if (current) {
    return (
      <span className={classes} aria-current="page">
        {label}
      </span>
    );
  }

  if (to) {
    return (
      <Link className={classes} to={to}>
        {label}
      </Link>
    );
  }

  return <span className={classes}>{label}</span>;
}
