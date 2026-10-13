import { apiClient } from './client';
import type { ApiResponse, ChatMessageDto, PageData, SendChatMessagePayload } from '../types';

export const chatApi = {
  getMessages: async (matchId: number, page = 0, size = 50): Promise<PageData<ChatMessageDto>> => {
    const res = await apiClient.get<ApiResponse<PageData<ChatMessageDto>>>(`/matches/${matchId}/messages`, {
      params: { page, size },
    });
    return res.data.data;
  },

  sendMessageRest: async (matchId: number, payload: SendChatMessagePayload): Promise<ChatMessageDto> => {
    const res = await apiClient.post<ApiResponse<ChatMessageDto>>(`/matches/${matchId}/messages`, payload);
    return res.data.data;
  },

  markMessagesRead: async (matchId: number): Promise<void> => {
    await apiClient.post<ApiResponse<null>>(`/matches/${matchId}/messages/read`);
  },
};
