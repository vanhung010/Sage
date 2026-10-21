import { apiClient } from "./client";

import type {
  ForgotPasswordRequest,
  PasswordResetMessageResponse,
  ResetPasswordRequest,
  VerifyResetCodeRequest,
  VerifyResetCodeResponse,
} from "../types/passwordReset";

export const passwordResetApi = {
async forgotPassword(request: ForgotPasswordRequest,): Promise<PasswordResetMessageResponse> {
    const response =
    await apiClient.post<PasswordResetMessageResponse>("/api/auth/forgot-password",request,);

    return response.data;
  },

   async verifyResetCode(
    request: VerifyResetCodeRequest,
  ): Promise<VerifyResetCodeResponse> {
    const response =
      await apiClient.post<VerifyResetCodeResponse>(
        "/api/auth/verify-reset-code",
        request,
      );

    return response.data;
  },

  async resetPassword(
    request: ResetPasswordRequest,
  ): Promise<PasswordResetMessageResponse> {
    const response =
      await apiClient.post<PasswordResetMessageResponse>(
        "/api/auth/reset-password",
        request,
      );

    return response.data;
  },
}