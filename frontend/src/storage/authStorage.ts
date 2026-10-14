const ACCESS_TOKEN_KEY = "sage_access_token";

export const authStorage = {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY,);
  },
    setAccessToken(token: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token,);
  },

  clearAccessToken() {
    localStorage.removeItem(ACCESS_TOKEN_KEY,);
  },
};