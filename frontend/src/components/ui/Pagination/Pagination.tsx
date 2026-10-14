import {
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

import { IconButton } from "../IconButton";
import { PaginationItem } from "./PaginationItem";

import "./Pagination.css";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

type PaginationValue = number | "ellipsis";

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  className = "",
}: PaginationProps) {
  const pages = getPaginationPages(
    currentPage,
    totalPages,
  );

  const classes = [
    "sage-pagination",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  function handlePrevious() {
    if (currentPage > 1) {
      onPageChange(currentPage - 1);
    }
  }

  function handleNext() {
    if (currentPage < totalPages) {
      onPageChange(currentPage + 1);
    }
  }

  return (
    <nav
      className={classes}
      aria-label="Phân trang"
    >
      <IconButton
        icon={<ChevronLeft />}
        size="md"
        variant="ghost"
        aria-label="Trang trước"
        disabled={currentPage === 1}
        onClick={handlePrevious}
      />

      {pages.map((item, index) => {
        if (item === "ellipsis") {
          return (
            <span
              key={`ellipsis-${index}`}
              className="sage-pagination__ellipsis"
              aria-hidden="true"
            >
              …
            </span>
          );
        }

        return (
          <PaginationItem
            key={item}
            page={item}
            active={item === currentPage}
            onClick={() => {
              onPageChange(item);
            }}
          />
        );
      })}

      <IconButton
        icon={<ChevronRight />}
        size="md"
        variant="ghost"
        aria-label="Trang sau"
        disabled={currentPage === totalPages}
        onClick={handleNext}
      />
    </nav>
  );
}

function getPaginationPages(
  currentPage: number,
  totalPages: number,
): PaginationValue[] {
  if (totalPages <= 5) {
    return Array.from(
      { length: totalPages },
      (_, index) => index + 1,
    );
  }

  if (currentPage <= 3) {
    return [
      1,
      2,
      3,
      "ellipsis",
      totalPages,
    ];
  }

  if (currentPage >= totalPages - 2) {
    return [
      1,
      "ellipsis",
      totalPages - 2,
      totalPages - 1,
      totalPages,
    ];
  }

  return [
    1,
    "ellipsis",
    currentPage - 1,
    currentPage,
    currentPage + 1,
    "ellipsis",
    totalPages,
  ];
}