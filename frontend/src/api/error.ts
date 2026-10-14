import axios from "axios";

import type { ApiErrorResponse, } from "../types/api";

export function getApiError(error: unknown,): ApiErrorResponse {
  if (axios.isAxiosError<ApiErrorResponse>(error,)) {
    if (error.response?.data) {
      return error.response.data;
    }

    return {
      status: 0,
      code: "NETWORK_ERROR",
      message:
        "Không thể kết nối đến máy chủ.",
    };
  }

  return {
    status: 0,
    code: "UNKNOWN_ERROR",
    message:
      "Đã có lỗi không xác định xảy ra.",
  };
}