import { apiClient } from './client';
import type { ApiResponse, AuthResponse, UserProfile } from '../types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  department: string;
  year: number;
  phone?: string;
}

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', credentials);
    return res.data.data;
  },

  register: async (data: RegisterRequest): Promise<UserProfile> => {
    const res = await apiClient.post<ApiResponse<UserProfile>>('/auth/register', data);
    return res.data.data;
  },

  getMe: async (): Promise<UserProfile> => {
    const res = await apiClient.get<ApiResponse<UserProfile>>('/auth/me');
    return res.data.data;
  },

  updateMe: async (data: Partial<UserProfile>): Promise<UserProfile> => {
    const res = await apiClient.put<ApiResponse<UserProfile>>('/users/me', data);
    return res.data.data;
  },
};
