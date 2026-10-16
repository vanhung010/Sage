import axios from "axios";

import { authStorage } from "../storage/authStorage";

//Explain
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,

  timeout: 10000,

  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

const publicAuthEndpoints = [
  "/api/auth/login",
  "/api/auth/register",
];

apiClient.interceptors.request.use(
  (config) => {
    const token = authStorage.getAccessToken();

    const isPublicAuthEndpoint = publicAuthEndpoints.some((path) => config.url?.startsWith(path),);

    //Nếu có token và không phải endpoint public
    if ( token && !isPublicAuthEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
);