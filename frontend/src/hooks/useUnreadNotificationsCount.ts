import { useQuery } from '@tanstack/react-query';
import { notificationsApi } from '../api/notifications';
import { useAuth } from '../features/auth/AuthContext';

export function useUnreadNotificationsCount() {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['unreadNotificationsCount'],
    queryFn: notificationsApi.getUnreadCount,
    enabled: isAuthenticated,
    refetchInterval: 30000, // Poll every 30 seconds
    staleTime: 15000,
  });
}
