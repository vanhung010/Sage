import type { ReactNode } from "react";

export type DataTableRowKey = string | number;

export type DataTableAlign = "left" | "center" | "right";

export type DataTableSortDirection = "asc" | "desc";

export interface DataTableSort {
  columnId: string;
  direction: DataTableSortDirection;
}

export interface DataTableColumn<T> {
  id: string;
  header: string;

  /** Chiều rộng cột, đơn vị px. */
  width: number;

  align?: DataTableAlign;
  sortable?: boolean;
  className?: string;

  /**
   * Chỉ trả về nội dung bên trong ô.
   * DataTableRow sẽ tự bọc bằng DataTableCell.
   */
  render: (row: T, index: number) => ReactNode;
}

export interface DataTableSelection<T> {
  selectedKeys: ReadonlySet<DataTableRowKey>;

  onChange: (keys: Set<DataTableRowKey>) => void;

  /** Ví dụ: "Chọn đơn SAGE-001". */
  getLabel: (row: T) => string;
}

export interface DataTablePagination {
  /** Trang bắt đầu từ 0. */
  page: number;

  /** Kích thước trang, phải lớn hơn 0. */
  size: number;

  totalElements: number;

  onPageChange: (page: number) => void;
}

export interface DataTableProps<T> {
  columns: readonly DataTableColumn<T>[];
  data: readonly T[];

  getRowKey: (row: T) => DataTableRowKey;

  caption: string;

  loading?: boolean;
  loadingRows?: number;

  /** null hoặc undefined nghĩa là không có lỗi. */
  error?: string | null;
  onRetry?: () => void;

  emptyTitle?: string;
  emptyDescription?: string;
  emptyIcon?: ReactNode;

  selection?: DataTableSelection<T>;
  pagination?: DataTablePagination;

  sort?: DataTableSort;
  onSortChange?: (sort: DataTableSort) => void;

  summary?: ReactNode;
  itemLabel?: string;

  showFooter?: boolean;
  showPagination?: boolean;

  className?: string;
}