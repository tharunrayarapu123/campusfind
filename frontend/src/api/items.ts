import { apiClient } from './client';
import type {
  ApiResponse,
  CreateItemPayload,
  ItemCategory,
  ItemDetail,
  ItemStatus,
  ItemSummary,
  ItemType,
  MatchSummary,
  PageData,
  SimilarItem,
  UpdateItemPayload,
} from '../types';

export interface ItemFilterParams {
  type?: ItemType;
  category?: ItemCategory;
  blockId?: number;
  roomId?: number;
  status?: ItemStatus;
  keyword?: string;
  mine?: boolean;
  page?: number;
  size?: number;
}

export const itemsApi = {
  getItems: async (params?: ItemFilterParams): Promise<PageData<ItemSummary>> => {
    const res = await apiClient.get<ApiResponse<PageData<ItemSummary>>>('/items', { params });
    return res.data.data;
  },

  getItemById: async (id: number): Promise<ItemDetail> => {
    const res = await apiClient.get<ApiResponse<ItemDetail>>(`/items/${id}`);
    return res.data.data;
  },

  createItem: async (payload: CreateItemPayload): Promise<ItemDetail> => {
    const res = await apiClient.post<ApiResponse<ItemDetail>>('/items', payload);
    return res.data.data;
  },

  updateItem: async (id: number, payload: UpdateItemPayload): Promise<ItemDetail> => {
    const res = await apiClient.put<ApiResponse<ItemDetail>>(`/items/${id}`, payload);
    return res.data.data;
  },

  deleteItem: async (id: number): Promise<void> => {
    await apiClient.delete<ApiResponse<null>>(`/items/${id}`);
  },

  reportItem: async (id: number, reason: string): Promise<void> => {
    await apiClient.post<ApiResponse<null>>(`/items/${id}/report`, { reason });
  },

  getItemMatches: async (id: number): Promise<MatchSummary[]> => {
    const res = await apiClient.get<ApiResponse<MatchSummary[]>>(`/items/${id}/matches`);
    return res.data.data;
  },

  getSimilarItems: async (id: number): Promise<SimilarItem[]> => {
    const res = await apiClient.get<ApiResponse<SimilarItem[]>>(`/items/${id}/similar`);
    return res.data.data;
  },
};
