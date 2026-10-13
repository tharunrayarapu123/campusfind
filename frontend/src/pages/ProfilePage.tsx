import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  User,
  Award,
  Settings,
  Lock,
  Download,
  Camera,
  Package,
  Mail,
  Sparkles,
  Trash2,
  Loader2,
  Eye,
} from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { usersApi } from '../api/users';
import { itemsApi } from '../api/items';
import { apiClient } from '../api/client';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { toast } from 'sonner';
import { cn } from '../lib/utils';
import type { ItemSummary, ItemStatus, UpdateUserRequest, ChangePasswordRequest } from '../types';

export const ProfilePage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'PROFILE' | 'POSTS' | 'SECURITY'>('PROFILE');
  const [postStatusFilter, setPostStatusFilter] = useState<'ALL' | ItemStatus>('ALL');

  // Profile form state
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [department, setDepartment] = useState(user?.department || '');
  const [year, setYear] = useState<number | undefined>(user?.year);
  const [phone, setPhone] = useState(user?.phone || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);

  // Email notifications state
  const [emailNotifications, setEmailNotifications] = useState(
    user?.emailNotifications !== undefined ? user.emailNotifications : true
  );

  // Password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // PWA install prompt state
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [installPrompt, setInstallPrompt] = useState<any>(null);

  useEffect(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleBeforeInstall = (e: any) => {
      e.preventDefault();
      setInstallPrompt(e);
    };
    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
  }, []);

  const handleInstallPwa = async () => {
    if (!installPrompt) return;
    installPrompt.prompt();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const choice = await installPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      toast.success('CampusFind installed to your device!');
    }
    setInstallPrompt(null);
  };

  // Sync form when user changes
  useEffect(() => {
    if (user) {
      setFullName(user.fullName);
      setDepartment(user.department || '');
      setYear(user.year);
      setPhone(user.phone || '');
      setAvatarUrl(user.avatarUrl || '');
      setEmailNotifications(user.emailNotifications !== undefined ? user.emailNotifications : true);
    }
  }, [user]);

  // 1. Fetch user stats
  const { data: stats } = useQuery({
    queryKey: ['myStats'],
    queryFn: usersApi.getMyStats,
  });

  // 2. Fetch my items
  const {
    data: itemsPage,
    isLoading: itemsLoading,
    refetch: refetchItems,
  } = useQuery({
    queryKey: ['myItems'],
    queryFn: () => itemsApi.getItems({ mine: true, size: 50 }),
  });

  const myItems: ItemSummary[] = itemsPage?.content || [];
  const filteredItems = myItems.filter((it) => {
    if (postStatusFilter === 'ALL') return true;
    return it.status === postStatusFilter;
  });

  // Update Profile Mutation
  const updateProfileMutation = useMutation({
    mutationFn: (data: UpdateUserRequest) => usersApi.updateProfile(data),
    onSuccess: async () => {
      toast.success('Profile updated successfully!');
      await refreshUser();
      queryClient.invalidateQueries({ queryKey: ['myStats'] });
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to update profile');
    },
  });

  // Update Settings Mutation
  const updateSettingsMutation = useMutation({
    mutationFn: (enabled: boolean) => usersApi.updateSettings({ emailNotifications: enabled }),
    onSuccess: async () => {
      toast.success('Notification preferences saved!');
      await refreshUser();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to update settings');
    },
  });

  // Change Password Mutation
  const changePasswordMutation = useMutation({
    mutationFn: (data: ChangePasswordRequest) => usersApi.changePassword(data),
    onSuccess: () => {
      toast.success('Password changed successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to change password');
    },
  });

  // Delete item mutation
  const deleteItemMutation = useMutation({
    mutationFn: (id: number) => itemsApi.deleteItem(id),
    onSuccess: () => {
      toast.success('Item post deleted.');
      refetchItems();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to delete item');
    },
  });

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      toast.error('Avatar size must be less than 5 MB');
      return;
    }

    try {
      setIsUploadingAvatar(true);
      const formData = new FormData();
      formData.append('file', file);

      const res = await apiClient.post<{ data: { url: string } }>('/uploads/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const url = res.data.data?.url;
      if (url) {
        setAvatarUrl(url);
        await updateProfileMutation.mutateAsync({
          fullName,
          department,
          year,
          phone,
          avatarUrl: url,
        });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to upload avatar';
      toast.error(msg);
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    updateProfileMutation.mutate({
      fullName,
      department,
      year: year ? Number(year) : undefined,
      phone: phone.trim() ? phone.trim() : undefined,
      avatarUrl,
    });
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) {
      toast.error('New password must be at least 8 characters long');
      return;
    }
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }
    changePasswordMutation.mutate({
      currentPassword,
      newPassword,
    });
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      {/* Profile Header Hero */}
      <Card className="p-6 bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-950 border border-slate-200/80 dark:border-slate-800">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5">
          {/* Avatar with Upload Button */}
          <div className="relative group shrink-0">
            <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-full overflow-hidden bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 border-2 border-brand-200 dark:border-brand-800 flex items-center justify-center font-black text-2xl shadow-sm">
              {avatarUrl ? (
                <img src={avatarUrl} alt={fullName} className="w-full h-full object-cover" />
              ) : (
                fullName.charAt(0).toUpperCase()
              )}
            </div>

            <label className="absolute bottom-0 right-0 p-2 rounded-full bg-brand-600 text-white shadow-md hover:bg-brand-700 cursor-pointer transition-transform group-hover:scale-105">
              {isUploadingAvatar ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Camera className="w-4 h-4" />
              )}
              <input
                type="file"
                className="hidden"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleAvatarUpload}
                disabled={isUploadingAvatar}
              />
            </label>
          </div>

          {/* User Info */}
          <div className="flex-1 text-center sm:text-left space-y-1">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white">
                {user?.fullName}
              </h1>
              <Badge variant="neutral">{user?.role}</Badge>
            </div>

            <p className="text-xs text-slate-500 flex items-center justify-center sm:justify-start gap-1">
              <Mail className="w-3.5 h-3.5" />
              {user?.email}
            </p>

            <p className="text-xs text-slate-600 dark:text-slate-400">
              {user?.department || 'Department not set'} • Year {user?.year || '1'}
            </p>

            {/* Karma & Badges summary */}
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-2">
              <div className="px-3 py-1 rounded-full bg-brand-50 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-bold text-xs flex items-center gap-1.5 border border-brand-200 dark:border-brand-800">
                <Sparkles className="w-3.5 h-3.5 text-brand-600" />
                {user?.karmaPoints ?? 0} Karma Points
              </div>

              {stats?.badges?.map((b) => (
                <span
                  key={b}
                  className="px-2.5 py-1 rounded-full bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800 text-xs font-semibold flex items-center gap-1.5"
                >
                  <Award className="w-3 h-3 text-amber-500" />
                  {b}
                </span>
              ))}
            </div>
          </div>

          {/* Optional PWA Install Button */}
          {installPrompt && (
            <div className="shrink-0 pt-2 sm:pt-0">
              <Button
                variant="outline"
                size="sm"
                onClick={handleInstallPwa}
                className="text-xs gap-1.5"
              >
                <Download className="w-4 h-4 text-brand-600" />
                Install App
              </Button>
            </div>
          )}
        </div>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <Card className="p-4 text-center">
          <span className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
            Total Posts
          </span>
          <span className="text-2xl font-black text-slate-900 dark:text-white">
            {stats?.postsCount ?? 0}
          </span>
        </Card>

        <Card className="p-4 text-center">
          <span className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
            Items Returned
          </span>
          <span className="text-2xl font-black text-emerald-600 dark:text-emerald-400">
            {stats?.returnsCount ?? 0}
          </span>
        </Card>

        <Card className="p-4 text-center">
          <span className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
            Karma Points
          </span>
          <span className="text-2xl font-black text-amber-600 dark:text-amber-400">
            {stats?.karmaPoints ?? user?.karmaPoints ?? 0}
          </span>
        </Card>

        <Card className="p-4 text-center">
          <span className="text-[11px] font-semibold text-slate-500 uppercase block mb-1">
            Badges Earned
          </span>
          <span className="text-2xl font-black text-brand-600 dark:text-brand-400">
            {stats?.badges?.length ?? 0}
          </span>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-800 gap-2">
        <button
          onClick={() => setActiveTab('PROFILE')}
          className={cn(
            'px-4 py-2.5 text-xs sm:text-sm font-semibold border-b-2 transition-colors min-h-touch flex items-center gap-1.5',
            activeTab === 'PROFILE'
              ? 'border-brand-600 text-brand-600 dark:text-brand-400'
              : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
          )}
        >
          <User className="w-4 h-4" />
          Edit Profile
        </button>

        <button
          onClick={() => setActiveTab('POSTS')}
          className={cn(
            'px-4 py-2.5 text-xs sm:text-sm font-semibold border-b-2 transition-colors min-h-touch flex items-center gap-1.5',
            activeTab === 'POSTS'
              ? 'border-brand-600 text-brand-600 dark:text-brand-400'
              : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
          )}
        >
          <Package className="w-4 h-4" />
          My Posts ({myItems.length})
        </button>

        <button
          onClick={() => setActiveTab('SECURITY')}
          className={cn(
            'px-4 py-2.5 text-xs sm:text-sm font-semibold border-b-2 transition-colors min-h-touch flex items-center gap-1.5',
            activeTab === 'SECURITY'
              ? 'border-brand-600 text-brand-600 dark:text-brand-400'
              : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
          )}
        >
          <Settings className="w-4 h-4" />
          Preferences & Security
        </button>
      </div>

      {/* Tab Content 1: Edit Profile */}
      {activeTab === 'PROFILE' && (
        <Card className="p-6">
          <form onSubmit={handleSaveProfile} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />

              <Input
                label="College Email"
                value={user?.email || ''}
                disabled
                helperText="College email cannot be changed"
              />

              <Input
                label="Department"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                placeholder="E.g., Computer Science & Engineering"
              />

              <Select
                label="Year of Study"
                value={year !== undefined ? String(year) : '1'}
                onChange={(e) => setYear(Number(e.target.value))}
                options={[
                  { value: '1', label: '1st Year' },
                  { value: '2', label: '2nd Year' },
                  { value: '3', label: '3rd Year' },
                  { value: '4', label: '4th Year' },
                  { value: '5', label: '5th Year / Postgrad' },
                ]}
              />

              <Input
                label="Phone Number (Optional)"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1234567890"
                helperText="Kept private from other students"
              />
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-100 dark:border-slate-800">
              <Button
                type="submit"
                variant="primary"
                isLoading={updateProfileMutation.isPending}
              >
                Save Changes
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Tab Content 2: My Posts */}
      {activeTab === 'POSTS' && (
        <div className="space-y-4">
          {/* Subfilters */}
          <div className="flex gap-2">
            {(['ALL', 'OPEN', 'MATCHED', 'RETURNED'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setPostStatusFilter(st)}
                className={cn(
                  'px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors',
                  postStatusFilter === st
                    ? 'bg-brand-600 text-white'
                    : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                )}
              >
                {st}
              </button>
            ))}
          </div>

          {itemsLoading ? (
            <Skeleton className="h-32 w-full rounded-2xl" />
          ) : filteredItems.length === 0 ? (
            <EmptyState
              icon={<Package className="w-8 h-8 text-slate-400" />}
              title="No posts found"
              description="You have not created any items in this category."
              actionText="Post an Item"
              onAction={() => {
                window.location.href = '/post';
              }}
            />
          ) : (
            <div className="space-y-3">
              {filteredItems.map((item) => (
                <Card
                  key={item.id}
                  className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-14 h-14 rounded-xl bg-slate-100 dark:bg-slate-800 overflow-hidden shrink-0 flex items-center justify-center">
                      {item.imageUrl ? (
                        <img
                          src={item.imageUrl}
                          alt={item.title}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Package className="w-6 h-6 text-slate-400" />
                      )}
                    </div>

                    <div>
                      <div className="flex items-center gap-2">
                        <span
                          className={cn(
                            'px-1.5 py-0.2 rounded-md text-[10px] font-bold uppercase',
                            item.type === 'LOST'
                              ? 'bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300'
                              : 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                          )}
                        >
                          {item.type}
                        </span>
                        <Badge status={item.status} />
                      </div>

                      <h4 className="text-sm font-bold text-slate-900 dark:text-white mt-1">
                        {item.title}
                      </h4>

                      <span className="text-xs text-slate-500">
                        {item.blockName || 'Campus'} • {new Date(item.eventTime).toLocaleDateString()}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-auto">
                    <Link to={`/items/${item.id}`}>
                      <Button variant="outline" size="sm" className="text-xs gap-1">
                        <Eye className="w-3.5 h-3.5" />
                        View
                      </Button>
                    </Link>

                    {item.status !== 'RETURNED' && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-xs text-rose-600 hover:text-rose-700"
                        onClick={() => {
                          if (confirm('Are you sure you want to delete this post?')) {
                            deleteItemMutation.mutate(item.id);
                          }
                        }}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </Button>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab Content 3: Preferences & Security */}
      {activeTab === 'SECURITY' && (
        <div className="space-y-6">
          {/* Notification settings */}
          <Card className="p-6">
            <h3 className="font-bold text-base text-slate-900 dark:text-white mb-2">
              Email Notifications
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Receive email updates when high-probability matches or claim answers are received.
            </p>

            <div className="flex items-center justify-between py-2 border-t border-slate-100 dark:border-slate-800">
              <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Send email alerts for new matches
              </span>
              <button
                type="button"
                onClick={() => {
                  const newVal = !emailNotifications;
                  setEmailNotifications(newVal);
                  updateSettingsMutation.mutate(newVal);
                }}
                className={cn(
                  'w-11 h-6 rounded-full transition-colors relative focus:outline-hidden',
                  emailNotifications ? 'bg-brand-600' : 'bg-slate-300 dark:bg-slate-700'
                )}
              >
                <span
                  className={cn(
                    'w-4 h-4 rounded-full bg-white transition-transform block absolute top-1',
                    emailNotifications ? 'left-6' : 'left-1'
                  )}
                />
              </button>
            </div>
          </Card>

          {/* Password update form */}
          <Card className="p-6">
            <h3 className="font-bold text-base text-slate-900 dark:text-white mb-2 flex items-center gap-2">
              <Lock className="w-4 h-4 text-brand-600" />
              Change Password
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Keep your campus account safe by choosing a secure password.
            </p>

            <form onSubmit={handleChangePassword} className="space-y-4 max-w-md">
              <Input
                label="Current Password"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />

              <Input
                label="New Password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                helperText="Must be at least 8 characters"
                required
              />

              <Input
                label="Confirm New Password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />

              <Button
                type="submit"
                variant="primary"
                isLoading={changePasswordMutation.isPending}
                disabled={!currentPassword || !newPassword || !confirmPassword}
              >
                Update Password
              </Button>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
};

export default ProfilePage;
