import { apiClient } from "./client";

import type {
  CurrentUserResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from "../types/auth";

export const authApi = {
  async login(request: LoginRequest,): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>("/api/auth/login",request,);

    return response.data;
  },

  async register(request: RegisterRequest): Promise<CurrentUserResponse> {
    const response = await apiClient.post<CurrentUserResponse>("/api/auth/register",request,);

    return response.data;
  },

  async getCurrentUser(): Promise<CurrentUserResponse> {
    const response = await apiClient.get<CurrentUserResponse>("/api/auth/me",);
    return response.data;
  },
};