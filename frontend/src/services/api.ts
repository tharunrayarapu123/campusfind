import axios from 'axios';
import { ApiResponse, HealthCheckResponse } from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const checkBackendHealth = async (): Promise<ApiResponse<HealthCheckResponse>> => {
  const response = await api.get<ApiResponse<HealthCheckResponse>>('/health');
  return response.data;
};

export default api;
