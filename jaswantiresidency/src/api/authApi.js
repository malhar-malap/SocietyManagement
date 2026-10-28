import axios from 'axios';
import api, { API_BASE_URL } from './axiosConfig';

export const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  return response.data;
};

export const forgotPassword = async (email) => {
  const response = await api.post('/auth/forgot-password', { email });
  return response.data;
};

export const resetPassword = async (email, otp, newPassword) => {
  const response = await api.post('/auth/reset-password', { email, otp, newPassword });
  return response.data;
};

// Revokes this account's tokens on the server. Best effort: signing out locally must never wait on or fail with it.
export const revokeSession = (refreshToken) => {
    if (!refreshToken) return Promise.resolve();
    return axios.post(`${API_BASE_URL}/auth/logout`, { refreshToken }, { timeout: 5000 }).catch(() => {});
};
