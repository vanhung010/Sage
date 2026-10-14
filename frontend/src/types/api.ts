export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];

  page: number;
  size: number;

  totalElements: number;
  totalPages: number;

  first: boolean;
  last: boolean;
}