import { apiClient } from './client';
import type {
  ApiResponse,
  ChangePasswordRequest,
  PublicUser,
  UpdateSettingsRequest,
  UpdateUserRequest,
  UserProfile,
  UserStatsDto,
} from '../types';

export const usersApi = {
  getMyStats: async (): Promise<UserStatsDto> => {
    const res = await apiClient.get<ApiResponse<UserStatsDto>>('/users/me/stats');
    return res.data.data;
  },

  updateProfile: async (payload: UpdateUserRequest): Promise<UserProfile> => {
    const res = await apiClient.put<ApiResponse<UserProfile>>('/users/me', payload);
    return res.data.data;
  },

  changePassword: async (payload: ChangePasswordRequest): Promise<void> => {
    await apiClient.post<ApiResponse<null>>('/users/me/change-password', payload);
  },

  updateSettings: async (payload: UpdateSettingsRequest): Promise<UserProfile> => {
    const res = await apiClient.put<ApiResponse<UserProfile>>('/users/me/settings', payload);
    return res.data.data;
  },

  getPublicUser: async (id: number): Promise<PublicUser> => {
    const res = await apiClient.get<ApiResponse<PublicUser>>(`/users/${id}`);
    return res.data.data;
  },
};
