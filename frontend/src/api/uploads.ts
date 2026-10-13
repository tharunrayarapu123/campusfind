import { apiClient } from './client';
import type { ApiResponse } from '../types';

export interface UploadResponse {
  url: string;
  format: string;
  size: number;
}

export const uploadsApi = {
  uploadImage: async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append('file', file);

    const res = await apiClient.post<ApiResponse<UploadResponse>>('/uploads/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data.data.url;
  },
};
