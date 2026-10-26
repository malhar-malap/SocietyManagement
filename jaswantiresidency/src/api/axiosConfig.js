import axios from 'axios';

// Set REACT_APP_API_BASE_URL at build time to point a deployed frontend at its backend
export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const SESSION_KEYS = ['accessToken', 'refreshToken', 'role', 'name', 'userId', 'roomId', 'forcePasswordChange'];

// One refresh at a time: a page that fires several requests when the access token expires shares a single refresh
// instead of racing one per request
let refreshPromise = null;

const refreshAccessToken = () => {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem('refreshToken');
    refreshPromise = (refreshToken
      ? axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
      : Promise.reject(new Error('No refresh token')))
      .then((res) => {
        if (!res.data?.accessToken) throw new Error('Refresh returned no access token');
        localStorage.setItem('accessToken', res.data.accessToken);
        if (res.data.refreshToken) {
          localStorage.setItem('refreshToken', res.data.refreshToken);
        }
        return res.data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config || {};
    // A 401 from /auth/* (e.g. a wrong password at sign-in) is an answer, not an expired session: refreshing and
    // reloading the login page here used to wipe the "invalid credentials" message
    const isAuthCall = (originalRequest.url || '').includes('/auth/');
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthCall) {
      originalRequest._retry = true;
      try {
        const accessToken = await refreshAccessToken();
        originalRequest.headers = { ...(originalRequest.headers || {}), Authorization: `Bearer ${accessToken}` };
        return api(originalRequest);
      } catch (refreshError) {
        SESSION_KEYS.forEach((key) => localStorage.removeItem(key));
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
