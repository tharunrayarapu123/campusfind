import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ShieldAlert,
  BarChart3,
  Users,
  Flag,
  Activity,
  CheckCircle,
  Trash2,
  UserX,
  UserCheck,
  Building,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { adminApi } from '../api/admin';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { toast } from 'sonner';
import { cn } from '../lib/utils';
import type { ReportDto, AuditLogDto, UserProfile } from '../types';

export const AdminPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'STATS' | 'REPORTS' | 'USERS' | 'LOGS'>('STATS');

  // Search state for users
  const [userSearch, setUserSearch] = useState('');

  // 1. Fetch system statistics
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['adminStats'],
    queryFn: adminApi.getStats,
  });

  // 2. Fetch reports queue
  const { data: reportsPage, isLoading: reportsLoading, refetch: refetchReports } = useQuery({
    queryKey: ['adminReports'],
    queryFn: () => adminApi.getReports(0, 50),
    enabled: activeTab === 'REPORTS',
  });

  // 3. Fetch users list
  const { data: usersPage, isLoading: usersLoading, refetch: refetchUsers } = useQuery({
    queryKey: ['adminUsers', userSearch],
    queryFn: () => adminApi.getUsers(userSearch, 0, 50),
    enabled: activeTab === 'USERS',
  });

  // 4. Fetch audit logs
  const { data: logsPage, isLoading: logsLoading } = useQuery({
    queryKey: ['adminAuditLogs'],
    queryFn: () => adminApi.getAuditLogs(0, 50),
    enabled: activeTab === 'LOGS',
  });

  // Dismiss reports mutation
  const dismissReportsMutation = useMutation({
    mutationFn: (itemId: number) => adminApi.dismissReports(itemId),
    onSuccess: () => {
      toast.success('Reports dismissed.');
      refetchReports();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to dismiss reports');
    },
  });

  // Hard delete item post
  const deleteItemMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteItem(id),
    onSuccess: () => {
      toast.success('Item permanently deleted by admin.');
      refetchReports();
      queryClient.invalidateQueries({ queryKey: ['adminStats'] });
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to delete item');
    },
  });

  // Ban user mutation
  const banUserMutation = useMutation({
    mutationFn: (id: number) => adminApi.banUser(id),
    onSuccess: () => {
      toast.success('User account has been banned.');
      refetchUsers();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to ban user');
    },
  });

  // Unban user mutation
  const unbanUserMutation = useMutation({
    mutationFn: (id: number) => adminApi.unbanUser(id),
    onSuccess: () => {
      toast.success('User account unbanned.');
      refetchUsers();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to unban user');
    },
  });

  // Format chart data from itemsPerBlock map
  const chartData = stats?.itemsPerBlock
    ? Object.entries(stats.itemsPerBlock).map(([block, count]) => ({
        block: block.replace('Block ', 'B-'),
        count,
      }))
    : [];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 text-xs font-bold border border-rose-200 dark:border-rose-800 mb-2">
            <ShieldAlert className="w-3.5 h-3.5" />
            Administrative Portal
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
            Campus Moderation Console
          </h1>
          <p className="text-xs sm:text-sm text-slate-500">
            Monitor campus reports, items distribution, user governance, and security audit logs.
          </p>
        </div>
      </div>

      {/* Admin Tabs */}
      <div className="flex gap-2 overflow-x-auto border-b border-slate-200 dark:border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('STATS')}
          className={cn(
            'px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center gap-2 min-h-touch whitespace-nowrap',
            activeTab === 'STATS'
              ? 'bg-brand-600 text-white shadow-xs'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-800'
          )}
        >
          <BarChart3 className="w-4 h-4" />
          Overview & Stats
        </button>

        <button
          onClick={() => setActiveTab('REPORTS')}
          className={cn(
            'px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center gap-2 min-h-touch whitespace-nowrap',
            activeTab === 'REPORTS'
              ? 'bg-brand-600 text-white shadow-xs'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-800'
          )}
        >
          <Flag className="w-4 h-4 text-rose-500" />
          Reports Moderation
        </button>

        <button
          onClick={() => setActiveTab('USERS')}
          className={cn(
            'px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center gap-2 min-h-touch whitespace-nowrap',
            activeTab === 'USERS'
              ? 'bg-brand-600 text-white shadow-xs'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-800'
          )}
        >
          <Users className="w-4 h-4" />
          User Governance
        </button>

        <button
          onClick={() => setActiveTab('LOGS')}
          className={cn(
            'px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center gap-2 min-h-touch whitespace-nowrap',
            activeTab === 'LOGS'
              ? 'bg-brand-600 text-white shadow-xs'
              : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-800'
          )}
        >
          <Activity className="w-4 h-4" />
          Audit Trail
        </button>
      </div>

      {/* Tab 1: Stats & Overview */}
      {activeTab === 'STATS' && (
        <div className="space-y-6">
          {statsLoading ? (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[1, 2, 3, 4].map((n) => (
                <Skeleton key={n} className="h-24 rounded-2xl" />
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Total Users
                </span>
                <span className="text-2xl font-black text-slate-900 dark:text-white">
                  {stats?.totalUsers ?? 0}
                </span>
              </Card>

              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Lost Items
                </span>
                <span className="text-2xl font-black text-rose-600 dark:text-rose-400">
                  {stats?.totalLost ?? 0}
                </span>
              </Card>

              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Found Items
                </span>
                <span className="text-2xl font-black text-emerald-600 dark:text-emerald-400">
                  {stats?.totalFound ?? 0}
                </span>
              </Card>

              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Returned Items
                </span>
                <span className="text-2xl font-black text-brand-600 dark:text-brand-400">
                  {stats?.returned ?? 0}
                </span>
              </Card>

              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Recovery Rate
                </span>
                <span className="text-2xl font-black text-amber-600 dark:text-amber-400">
                  {stats?.returnRatePercentage ? `${stats.returnRatePercentage.toFixed(1)}%` : '0%'}
                </span>
              </Card>

              <Card className="p-4 text-center">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  AI Matches
                </span>
                <span className="text-2xl font-black text-indigo-600 dark:text-indigo-400">
                  {stats?.matchesCount ?? 0}
                </span>
              </Card>
            </div>
          )}

          {/* Recharts Bar Chart: Items Per Block */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-slate-900 dark:text-white mb-1 flex items-center gap-2">
              <Building className="w-5 h-5 text-brand-600" />
              Items Distribution Across Campus Blocks
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Geographic concentration of reported items across college buildings.
            </p>

            <div className="h-72 w-full">
              {chartData.length === 0 ? (
                <div className="h-full flex items-center justify-center text-xs text-slate-400">
                  No block distribution data available yet.
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
                    <XAxis
                      dataKey="block"
                      tick={{ fontSize: 11 }}
                      interval={0}
                      angle={-20}
                      textAnchor="end"
                    />
                    <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                    <Tooltip
                      contentStyle={{
                        borderRadius: '0.75rem',
                        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                        fontSize: '12px',
                      }}
                    />
                    <Bar dataKey="count" fill="#2563eb" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </Card>
        </div>
      )}

      {/* Tab 2: Reports Queue */}
      {activeTab === 'REPORTS' && (
        <div className="space-y-4">
          {reportsLoading ? (
            <Skeleton className="h-40 w-full rounded-2xl" />
          ) : !reportsPage || reportsPage.content.length === 0 ? (
            <EmptyState
              icon={<CheckCircle className="w-8 h-8 text-emerald-500" />}
              title="Reports queue is clean!"
              description="No unresolved student reports for inappropriate items or spam."
            />
          ) : (
            <div className="space-y-3">
              {reportsPage.content.map((rep: ReportDto) => (
                <Card key={rep.id} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300 text-xs font-bold">
                        Reported Post
                      </span>
                      <span className="text-xs text-slate-400">
                        {new Date(rep.createdAt).toLocaleString()}
                      </span>
                    </div>

                    <h4 className="font-bold text-sm text-slate-900 dark:text-white">
                      Item: "{rep.itemTitle}" (ID #{rep.itemId})
                    </h4>

                    <p className="text-xs text-slate-600 dark:text-slate-400 bg-slate-50 dark:bg-slate-800/80 p-2.5 rounded-xl border border-slate-200/60 dark:border-slate-700">
                      <strong>Report Reason:</strong> {rep.reason}
                    </p>

                    <span className="text-[11px] text-slate-400 block">
                      Reported by: {rep.reporter?.fullName || 'Anonymous Student'} ({rep.reporter?.department || 'Student'})
                    </span>
                  </div>

                  <div className="flex items-center gap-2 pt-2 md:pt-0">
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-xs"
                      onClick={() => dismissReportsMutation.mutate(rep.itemId)}
                      isLoading={dismissReportsMutation.isPending}
                    >
                      Dismiss Reports
                    </Button>

                    <Button
                      size="sm"
                      variant="danger"
                      className="text-xs gap-1"
                      onClick={() => {
                        if (confirm(`Permanently delete post #${rep.itemId}?`)) {
                          deleteItemMutation.mutate(rep.itemId);
                        }
                      }}
                      isLoading={deleteItemMutation.isPending}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Delete Post
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Users Governance */}
      {activeTab === 'USERS' && (
        <div className="space-y-4">
          <div className="max-w-md">
            <Input
              placeholder="Search by student name or email..."
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
            />
          </div>

          {usersLoading ? (
            <Skeleton className="h-40 w-full rounded-2xl" />
          ) : !usersPage || usersPage.content.length === 0 ? (
            <EmptyState
              icon={<Users className="w-8 h-8 text-slate-400" />}
              title="No users found"
              description="No registered students match your search criteria."
            />
          ) : (
            <Card className="divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
              {usersPage.content.map((u: UserProfile) => (
                <div key={u.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm text-slate-900 dark:text-white">
                        {u.fullName}
                      </span>
                      <Badge variant={u.role === 'ADMIN' ? 'brand' : 'neutral'}>
                        {u.role}
                      </Badge>
                    </div>

                    <div className="text-xs text-slate-500">
                      {u.email} • {u.department || 'Dept not set'} • {u.karmaPoints} karma pts
                    </div>
                  </div>

                  {u.role !== 'ADMIN' && (
                    <div className="flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        className="text-xs text-rose-600 hover:text-rose-700"
                        onClick={() => banUserMutation.mutate(u.id)}
                        isLoading={banUserMutation.isPending}
                      >
                        <UserX className="w-3.5 h-3.5 mr-1" />
                        Ban Student
                      </Button>

                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-xs text-emerald-600 hover:text-emerald-700"
                        onClick={() => unbanUserMutation.mutate(u.id)}
                        isLoading={unbanUserMutation.isPending}
                      >
                        <UserCheck className="w-3.5 h-3.5 mr-1" />
                        Unban
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </Card>
          )}
        </div>
      )}

      {/* Tab 4: Audit Logs */}
      {activeTab === 'LOGS' && (
        <div className="space-y-4">
          {logsLoading ? (
            <Skeleton className="h-40 w-full rounded-2xl" />
          ) : !logsPage || logsPage.content.length === 0 ? (
            <EmptyState
              icon={<Activity className="w-8 h-8 text-slate-400" />}
              title="No audit entries"
              description="System administrative actions will be recorded here."
            />
          ) : (
            <Card className="divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
              {logsPage.content.map((log: AuditLogDto) => (
                <div key={log.id} className="p-4 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-bold text-brand-600 dark:text-brand-400 uppercase">
                      {log.action}
                    </span>
                    <span className="text-[11px] text-slate-400">
                      {new Date(log.createdAt).toLocaleString()}
                    </span>
                  </div>

                  <p className="text-xs text-slate-700 dark:text-slate-300">
                    {log.details || `Admin ${log.adminEmail} performed ${log.action} on ${log.targetType} #${log.targetId}`}
                  </p>

                  <span className="text-[10px] text-slate-400 block">
                    Admin: {log.adminEmail}
                  </span>
                </div>
              ))}
            </Card>
          )}
        </div>
      )}
    </div>
  );
};

export default AdminPage;
