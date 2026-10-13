import { apiClient } from './client';
import type { ApiResponse, ImageAnalysisResponse } from '../types';

export const aiApi = {
  analyzeImage: async (file: File): Promise<ImageAnalysisResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const res = await apiClient.post<ApiResponse<ImageAnalysisResponse>>('/ai/analyze-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data.data;
  },
};
