import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Bell,
  CheckCheck,
  Sparkles,
  MessageSquare,
  ShieldCheck,
  ChevronRight,
  Calendar,
  AlertCircle,
  Inbox,
} from 'lucide-react';
import { notificationsApi } from '../api/notifications';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { toast } from 'sonner';
import { cn } from '../lib/utils';
import type { NotificationDto } from '../types';

export const NotificationsPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    data: pageData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => notificationsApi.getNotifications(page, 20),
  });

  const notifications = pageData?.content || [];

  const markAsReadMutation = useMutation({
    mutationFn: (id: number) => notificationsApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unreadNotificationsCount'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationsApi.markAllAsRead(),
    onSuccess: () => {
      toast.success('All notifications marked as read');
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unreadNotificationsCount'] });
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to mark notifications as read');
    },
  });

  const handleNotificationClick = async (notif: NotificationDto) => {
    if (!notif.read) {
      await markAsReadMutation.mutateAsync(notif.id);
    }
    if (notif.link) {
      navigate(notif.link);
    }
  };

  const getNotificationIcon = (title: string) => {
    const lower = title.toLowerCase();
    if (lower.includes('match')) {
      return <Sparkles className="w-5 h-5 text-amber-500" />;
    }
    if (lower.includes('chat') || lower.includes('message')) {
      return <MessageSquare className="w-5 h-5 text-brand-600" />;
    }
    if (lower.includes('claim') || lower.includes('approved')) {
      return <ShieldCheck className="w-5 h-5 text-emerald-600" />;
    }
    return <Bell className="w-5 h-5 text-slate-500" />;
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
      {/* Top Header */}
      <div className="flex items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight flex items-center gap-2">
            <Bell className="w-6 h-6 text-brand-600" />
            Notifications
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Realtime updates for item matches, claim verifications, and chat messages.
          </p>
        </div>

        {notifications.length > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => markAllMutation.mutate()}
            isLoading={markAllMutation.isPending}
            className="text-xs gap-1.5 text-slate-600 dark:text-slate-400"
          >
            <CheckCheck className="w-4 h-4 text-brand-600" />
            Mark all read
          </Button>
        )}
      </div>

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="space-y-3">
          {[1, 2, 3, 4].map((n) => (
            <Card key={n} className="p-4">
              <div className="flex gap-3 items-center">
                <Skeleton className="w-10 h-10 rounded-xl" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/2" />
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Error State */}
      {error && !isLoading && (
        <EmptyState
          icon={<AlertCircle className="w-8 h-8 text-rose-500" />}
          title="Could not load notifications"
          description="A network problem prevented loading your notifications. Please try again."
          actionText="Retry"
          onAction={() => refetch()}
        />
      )}

      {/* Empty State */}
      {!isLoading && !error && notifications.length === 0 && (
        <EmptyState
          icon={<Inbox className="w-8 h-8 text-slate-400" />}
          title="All caught up!"
          description="You don't have any notifications right now. Activity on your posts and matches will appear here."
        />
      )}

      {/* Notifications List */}
      {!isLoading && !error && notifications.length > 0 && (
        <div className="space-y-2.5">
          {notifications.map((notif) => (
            <div
              key={notif.id}
              onClick={() => handleNotificationClick(notif)}
              className={cn(
                'group relative p-4 rounded-2xl border transition-all cursor-pointer flex items-start gap-3.5',
                notif.read
                  ? 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-850'
                  : 'bg-brand-50/40 dark:bg-brand-950/20 border-brand-200 dark:border-brand-900/60 shadow-xs hover:bg-brand-50/70'
              )}
            >
              {/* Unread indicator dot */}
              {!notif.read && (
                <span className="absolute top-4 right-4 w-2.5 h-2.5 rounded-full bg-brand-600 ring-4 ring-brand-100 dark:ring-brand-950" />
              )}

              {/* Icon */}
              <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center shrink-0">
                {getNotificationIcon(notif.title)}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0 pr-4">
                <div className="flex items-center gap-2 mb-0.5">
                  <h4
                    className={cn(
                      'text-sm truncate',
                      notif.read
                        ? 'font-medium text-slate-800 dark:text-slate-200'
                        : 'font-bold text-slate-900 dark:text-white'
                    )}
                  >
                    {notif.title}
                  </h4>
                </div>

                <p className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2 leading-relaxed">
                  {notif.body}
                </p>

                <div className="flex items-center gap-2 mt-2 text-[10px] text-slate-400">
                  <Calendar className="w-3 h-3" />
                  <span>{new Date(notif.createdAt).toLocaleString()}</span>
                </div>
              </div>

              <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-brand-600 group-hover:translate-x-0.5 transition-all shrink-0 self-center" />
            </div>
          ))}

          {/* Pagination controls if more than 1 page */}
          {pageData && pageData.totalPages > 1 && (
            <div className="flex justify-between items-center pt-4">
              <Button
                variant="outline"
                size="sm"
                disabled={pageData.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <span className="text-xs text-slate-500">
                Page {page + 1} of {pageData.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={pageData.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;
