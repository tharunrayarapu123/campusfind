export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  timestamp: string;
  errors?: Record<string, string>;
}

export interface HealthCheckResponse {
  status: string;
  service: string;
  activeProfile: string;
  databaseStatus: string;
  serverTime: string;
}

export type ItemStatus = 'OPEN' | 'MATCHED' | 'RETURNED' | 'CLOSED';
export type ItemType = 'LOST' | 'FOUND';
export type UserRole = 'ROLE_STUDENT' | 'ROLE_ADMIN';
