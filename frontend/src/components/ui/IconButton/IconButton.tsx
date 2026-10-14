import type {
  ButtonHTMLAttributes,
  ReactNode,
} from "react";

import "./IconButton.css";

export type IconButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "destructive";

export type IconButtonSize =
  | "sm"
  | "md"
  | "lg";

interface IconButtonProps
  extends Omit<
    ButtonHTMLAttributes<HTMLButtonElement>,
    "children"
  > {
  icon: ReactNode;

  variant?: IconButtonVariant;

  size?: IconButtonSize;

  "aria-label": string;
}

export function IconButton({
  icon,
  variant = "ghost",
  size = "md",
  className = "",
  type = "button",
  ...props
}: IconButtonProps) {
  const classes = [
    "sage-icon-button",
    `sage-icon-button--${variant}`,
    `sage-icon-button--${size}`,
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type={type}
      className={classes}
      {...props}
    >
      <span
        className="sage-icon-button__icon"
        aria-hidden="true"
      >
        {icon}
      </span>
    </button>
  );
}