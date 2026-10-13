import { apiClient } from './client';
import type { ApiResponse, Block, CategoryInfo } from '../types';

export const referenceApi = {
  getBlocks: async (): Promise<Block[]> => {
    const res = await apiClient.get<ApiResponse<Block[]>>('/blocks');
    return res.data.data;
  },

  getCategories: async (): Promise<CategoryInfo[]> => {
    const res = await apiClient.get<ApiResponse<CategoryInfo[]>>('/categories');
    return res.data.data;
  },
};
