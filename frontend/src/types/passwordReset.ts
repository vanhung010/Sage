export interface ForgotPasswordRequest {
  email: string;
}

export interface PasswordResetMessageResponse {
  message: string;
}

export interface VerifyResetCodeRequest {
  email: string;
  code: string;
}


export interface VerifyResetCodeResponse {
  resetToken: string;
  expiresInSeconds: number;
}

export interface ResetPasswordRequest {
  resetToken: string;
  newPassword: string;
  confirmPassword: string;
}