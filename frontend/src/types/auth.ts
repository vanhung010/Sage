export type AccountRole =
  | "CUSTOMER"
  | "STAFF"
  | "ADMIN";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthUser {
  id: number;
  fullName: string;
  role: AccountRole;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: AuthUser;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone: string;
}

export interface CurrentUserResponse {
  id: number;
  email: string;
  fullName: string;
  phone: string;
  role: AccountRole;
}