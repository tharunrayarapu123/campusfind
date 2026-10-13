import { apiClient } from './client';
import type { ApiResponse } from '../types';

export const notificationsApi = {
  getUnreadCount: async (): Promise<number> => {
    try {
      const res = await apiClient.get<ApiResponse<number>>('/notifications/unread-count');
      return res.data.data;
    } catch {
      return 0;
    }
  },
};
