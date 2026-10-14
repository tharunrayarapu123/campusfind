import axios from 'axios';

const TOKEN_KEY = 'campusfind_token';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const setStoredToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearStoredToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem('campusfind_user');
};

export const getApiBaseUrl = (): string => {
  const url = import.meta.env.VITE_API_URL;
  return url ? url.replace(/\/+$/, '') : '';
};

export const apiClient = axios.create({
  baseURL: getApiBaseUrl() ? `${getApiBaseUrl()}/api/v1` : '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT bearer token
apiClient.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: 401 redirection and clean error message extraction
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const hadToken = !!getStoredToken();
      clearStoredToken();
      const currentPath = window.location.pathname;
      const isProtectedRoute = ['/post', '/matches', '/profile', '/notifications', '/admin'].some(
        (prefix) => currentPath.startsWith(prefix)
      );
      if ((hadToken || isProtectedRoute) && currentPath !== '/login' && currentPath !== '/register') {
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
      }
    }

    const message =
      error.response?.data?.message ||
      (typeof error.response?.data === 'string' ? error.response.data : null) ||
      error.message ||
      'An unexpected error occurred';

    return Promise.reject(new Error(message));
  }
);
