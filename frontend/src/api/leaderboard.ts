import { apiClient } from './client';
import type { ApiResponse, LeaderboardUserDto } from '../types';

export const leaderboardApi = {
  getLeaderboard: async (): Promise<LeaderboardUserDto[]> => {
    const res = await apiClient.get<ApiResponse<LeaderboardUserDto[]>>('/leaderboard');
    return res.data.data;
  },
};
