import { apiClient } from './client';
import type {
  AdminStatsDto,
  ApiResponse,
  AuditLogDto,
  ClaimResponse,
  PageData,
  ReportDto,
  UserProfile,
} from '../types';

export const adminApi = {
  getStats: async (): Promise<AdminStatsDto> => {
    const res = await apiClient.get<ApiResponse<AdminStatsDto>>('/admin/stats');
    return res.data.data;
  },

  getReports: async (page = 0, size = 20): Promise<PageData<ReportDto>> => {
    const res = await apiClient.get<ApiResponse<PageData<ReportDto>>>('/admin/reports', {
      params: { page, size },
    });
    return res.data.data;
  },

  dismissReports: async (itemId: number): Promise<void> => {
    await apiClient.post<ApiResponse<null>>(`/admin/reports/${itemId}/dismiss`);
  },

  deleteItem: async (id: number): Promise<void> => {
    await apiClient.delete<ApiResponse<null>>(`/admin/items/${id}`);
  },

  getUsers: async (search?: string, page = 0, size = 20): Promise<PageData<UserProfile>> => {
    const res = await apiClient.get<ApiResponse<PageData<UserProfile>>>('/admin/users', {
      params: { search: search || undefined, page, size },
    });
    return res.data.data;
  },

  banUser: async (id: number): Promise<UserProfile> => {
    const res = await apiClient.post<ApiResponse<UserProfile>>(`/admin/users/${id}/ban`);
    return res.data.data;
  },

  unbanUser: async (id: number): Promise<UserProfile> => {
    const res = await apiClient.post<ApiResponse<UserProfile>>(`/admin/users/${id}/unban`);
    return res.data.data;
  },

  unhideItem: async (id: number): Promise<void> => {
    await apiClient.post<ApiResponse<null>>(`/admin/items/${id}/unhide`);
  },

  approveOfficeClaim: async (matchId: number, claimId: number): Promise<ClaimResponse> => {
    const res = await apiClient.post<ApiResponse<ClaimResponse>>(
      `/admin/matches/${matchId}/claims/${claimId}/office-approve`
    );
    return res.data.data;
  },

  getAuditLogs: async (page = 0, size = 20): Promise<PageData<AuditLogDto>> => {
    const res = await apiClient.get<ApiResponse<PageData<AuditLogDto>>>('/admin/audit-logs', {
      params: { page, size },
    });
    return res.data.data;
  },
};
