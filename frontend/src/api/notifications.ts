import { apiClient } from './client';
import type { ApiResponse, NotificationDto, PageData, UnreadCountDto } from '../types';

export const notificationsApi = {
  getNotifications: async (page = 0, size = 20): Promise<PageData<NotificationDto>> => {
    const res = await apiClient.get<ApiResponse<PageData<NotificationDto>>>('/notifications', {
      params: { page, size },
    });
    return res.data.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const res = await apiClient.get<ApiResponse<UnreadCountDto>>('/notifications/unread-count');
    return res.data.data.unreadCount;
  },

  markAsRead: async (id: number): Promise<NotificationDto> => {
    const res = await apiClient.post<ApiResponse<NotificationDto>>(`/notifications/${id}/read`);
    return res.data.data;
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.post<ApiResponse<null>>('/notifications/read-all');
  },
};
