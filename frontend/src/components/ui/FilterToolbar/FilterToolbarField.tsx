import type {
  CSSProperties,
  ReactNode,
} from "react";

import { FormField } from "../FormField";

export interface FilterToolbarFieldProps {
  label: string;
  htmlFor: string;

  children: ReactNode;

  width?: number;
  grow?: boolean;
}

export function FilterToolbarField({
  label,
  htmlFor,
  children,
  width = 180,
  grow = false,
}: FilterToolbarFieldProps) {
  const style = {
    "--filter-field-width": `${width}px`,
  } as CSSProperties;

  return (
    <div
      className={[
        "sage-filter-toolbar__field",
        grow ? "sage-filter-toolbar__field--grow" : "",
      ]
        .filter(Boolean)
        .join(" ")}
      style={style}
    >
      <FormField
        label={label}
        htmlFor={htmlFor}
      >
        {children}
      </FormField>
    </div>
  );
}