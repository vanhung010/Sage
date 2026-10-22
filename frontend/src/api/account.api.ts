import { apiClient } from "./client";

export interface ChangePasswordRequest {
  currentPassword: string; // Mật khẩu hiện tại.
  newPassword: string; // Mật khẩu mới.
  repeatPassword: string; // Nhập lại mật khẩu mới.
}

export const accountApi = {
    async changePassword (request: ChangePasswordRequest,
        signal?: AbortSignal,
    ) : Promise<void>{
        await apiClient.post<void>("api/me/changePasswords", 
            request, 
            {signal},
        )
    }
}