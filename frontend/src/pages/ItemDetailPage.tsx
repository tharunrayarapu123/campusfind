import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format, formatDistanceToNow } from 'date-fns';
import { toast } from 'sonner';
import {
  MapPin,
  Calendar,
  Building,
  Flag,
  Trash2,
  ArrowLeft,
  Sparkles,
  Lock,
  ChevronRight,
  Package,
} from 'lucide-react';
import { itemsApi } from '../api/items';
import { useAuth } from '../features/auth/AuthContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { Modal } from '../components/ui/Modal';
import { getCategoryIcon, ItemCard } from '../features/items/ItemCard';

export const ItemDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const itemId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, isAuthenticated } = useAuth();

  // Modals state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [reportReason, setReportReason] = useState('');

  // 1. Fetch Item Details
  const {
    data: item,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['itemDetail', itemId],
    queryFn: () => itemsApi.getItemById(itemId),
    enabled: !isNaN(itemId),
  });

  // Check if current user is owner or admin
  const isOwner = !!(user && item && user.id === item.owner?.id);
  const isAdmin = user?.role === 'ADMIN';
  const canManage = isOwner || isAdmin;

  // 2. Fetch Suggested Matches (Owner / Admin only)
  const { data: matches = [], isLoading: isLoadingMatches } = useQuery({
    queryKey: ['itemMatches', itemId],
    queryFn: () => itemsApi.getItemMatches(itemId),
    enabled: canManage && !isNaN(itemId),
  });

  // 3. Fetch Similar Items ("You may also like")
  const { data: similarItems = [] } = useQuery({
    queryKey: ['similarItems', itemId],
    queryFn: () => itemsApi.getSimilarItems(itemId),
    enabled: !isNaN(itemId),
  });

  // Delete Mutation
  const deleteMutation = useMutation({
    mutationFn: () => itemsApi.deleteItem(itemId),
    onSuccess: () => {
      toast.success('Item deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['itemsFeed'] });
      navigate('/', { replace: true });
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Failed to delete item';
      toast.error(msg);
    },
  });

  // Report Mutation
  const reportMutation = useMutation({
    mutationFn: (reason: string) => itemsApi.reportItem(itemId, reason),
    onSuccess: () => {
      toast.success('Report submitted to campus administrators for review');
      setIsReportModalOpen(false);
      setReportReason('');
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Failed to submit report';
      toast.error(msg);
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        <Skeleton className="h-8 w-48 rounded-lg" />
        <Skeleton className="aspect-video w-full rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-40 md:col-span-2 rounded-2xl" />
          <Skeleton className="h-40 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="w-14 h-14 rounded-2xl bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 mx-auto flex items-center justify-center mb-4">
          <Package className="w-7 h-7" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-2">Item Not Found</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mb-6">
          {error instanceof Error ? error.message : 'This post may have been removed or closed.'}
        </p>
        <Link to="/">
          <Button variant="primary">Return to Home Feed</Button>
        </Link>
      </div>
    );
  }

  const ownerFirstName = item.owner?.fullName ? item.owner.fullName.split(' ')[0] : 'Student';
  const locationText = [item.blockName, item.roomName].filter(Boolean).join(' • ');

  // Timeline steps
  const timelineSteps = [
    { label: 'Open', active: true, completed: item.status !== 'OPEN' },
    {
      label: 'Matched',
      active: item.status === 'MATCHED' || item.status === 'RETURNED',
      completed: item.status === 'RETURNED',
    },
    { label: 'Returned', active: item.status === 'RETURNED', completed: item.status === 'RETURNED' },
  ];

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
      {/* Back button */}
      <Link
        to="/"
        className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200 mb-6 transition-colors"
      >
        <ArrowLeft className="w-3.5 h-3.5" /> Back to feed
      </Link>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Left Column: Photo & Details */}
        <div className="md:col-span-2 space-y-6">
          {/* Main Photo Card */}
          <Card className="p-0 overflow-hidden border-slate-200/90 dark:border-slate-800">
            <div className="relative aspect-[16/10] w-full bg-slate-900 flex items-center justify-center overflow-hidden">
              {item.imageUrl ? (
                <img
                  src={item.imageUrl}
                  alt={item.title}
                  className="w-full h-full object-contain"
                />
              ) : (
                <div className="flex flex-col items-center justify-center text-slate-400 p-8">
                  <div className="w-16 h-16 rounded-2xl bg-slate-800 flex items-center justify-center mb-3">
                    {getCategoryIcon(item.category)}
                  </div>
                  <span className="text-sm font-medium uppercase tracking-wider text-slate-400">
                    {item.category.replace('_', ' ')}
                  </span>
                </div>
              )}

              {/* Badges Overlay */}
              <div className="absolute top-4 left-4 flex gap-2 z-10">
                <Badge type={item.type} />
                <Badge status={item.status} />
              </div>

              {item.handedToOffice && (
                <div className="absolute bottom-4 left-4 z-10">
                  <span className="text-xs font-semibold bg-slate-900/90 backdrop-blur-sm text-white px-3 py-1 rounded-lg flex items-center gap-1.5">
                    <Building className="w-3.5 h-3.5 text-amber-400" /> Handed to Security / Library
                  </span>
                </div>
              )}
            </div>

            {/* Title & Core Meta */}
            <div className="p-6">
              <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
                {item.title}
              </h1>

              <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-600 dark:text-slate-400 border-y border-slate-100 dark:border-slate-800/80 py-3">
                {locationText && (
                  <div className="flex items-center gap-1.5">
                    <MapPin className="w-4 h-4 text-slate-400 shrink-0" />
                    <span className="font-medium text-slate-800 dark:text-slate-200">{locationText}</span>
                  </div>
                )}
                <div className="flex items-center gap-1.5">
                  <Calendar className="w-4 h-4 text-slate-400 shrink-0" />
                  <span>
                    {format(new Date(item.eventTime), 'PPp')} ({formatDistanceToNow(new Date(item.eventTime), { addSuffix: true })})
                  </span>
                </div>
              </div>

              {/* Description */}
              {item.description && (
                <div className="mt-5">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-1.5">
                    Description
                  </h2>
                  <p className="text-sm text-slate-700 dark:text-slate-300 whitespace-pre-wrap leading-relaxed">
                    {item.description}
                  </p>
                </div>
              )}

              {/* Verification Question indicator (for Found items) */}
              {item.type === 'FOUND' && item.verificationQuestion && (
                <div className="mt-6 p-4 rounded-xl bg-brand-50/60 dark:bg-brand-950/30 border border-brand-100 dark:border-brand-900/50">
                  <div className="flex items-center gap-2 text-xs font-bold text-brand-700 dark:text-brand-300 uppercase tracking-wider mb-1">
                    <Lock className="w-3.5 h-3.5" /> Claim Verification Protected
                  </div>
                  <p className="text-xs text-slate-600 dark:text-slate-400">
                    The finder has set a verification challenge. Claimants must answer correctly to unlock contact.
                  </p>
                </div>
              )}
            </div>
          </Card>

          {/* Suggested Matches Section (Visible only to owner / admin) */}
          {canManage && (
            <div className="pt-2">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-brand-50 dark:bg-brand-950/60 text-brand-600 dark:text-brand-400 flex items-center justify-center">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                      Suggested Matches ({matches.length})
                    </h2>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      Evaluated by location, time, category, and visual embeddings
                    </p>
                  </div>
                </div>
              </div>

              {isLoadingMatches ? (
                <Skeleton className="h-28 w-full rounded-2xl" />
              ) : matches.length === 0 ? (
                <Card className="p-6 text-center text-xs text-slate-500 dark:text-slate-400">
                  No automated matches detected yet. We re-scan new posts continuously.
                </Card>
              ) : (
                <div className="space-y-4">
                  {matches.map((m) => {
                    const otherItemTitle = m.isLostItemOwner ? m.foundItemTitle : m.lostItemTitle;
                    const otherItemImage = m.isLostItemOwner ? m.foundItemImageUrl : m.lostItemImageUrl;
                    const b = m.scoreBreakdown;

                    return (
                      <Card key={m.id} className="p-5 border-brand-100 dark:border-brand-950">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                          <div className="flex items-center gap-4">
                            {/* Score Ring / Pill */}
                            <div className="w-14 h-14 rounded-2xl bg-brand-600 text-white flex flex-col items-center justify-center shrink-0 shadow-sm">
                              <span className="text-lg font-extrabold leading-none">{m.score}%</span>
                              <span className="text-[9px] uppercase font-semibold tracking-wider mt-0.5">
                                match
                              </span>
                            </div>

                            {/* Matched item photo thumbnail */}
                            {otherItemImage && (
                              <img
                                src={otherItemImage}
                                alt={otherItemTitle}
                                className="w-12 h-12 rounded-xl object-cover shrink-0 hidden sm:block border border-slate-200 dark:border-slate-800"
                              />
                            )}

                            {/* Matched item summary */}
                            <div>
                              <div className="flex items-center gap-2 mb-1">
                                <Badge status={m.status} />
                                <span className="text-xs text-slate-400 font-medium">
                                  {m.isLostItemOwner ? 'Found Candidate' : 'Lost Candidate'}
                                </span>
                              </div>
                              <h3 className="font-semibold text-sm text-slate-900 dark:text-white line-clamp-1">
                                {otherItemTitle}
                              </h3>
                              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                                Posted by {m.otherUser?.fullName ? m.otherUser.fullName.split(' ')[0] : 'Student'}
                              </p>
                            </div>
                          </div>

                          {/* Link to Match Page */}
                          <Link to={`/matches/${m.id}`}>
                            <Button variant="primary" size="sm" className="w-full sm:w-auto">
                              View Match <ChevronRight className="w-4 h-4 ml-1" />
                            </Button>
                          </Link>
                        </div>

                        {/* Breakdown Bars */}
                        <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-800 grid grid-cols-3 sm:grid-cols-6 gap-2 text-center text-[10px]">
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">Block</span>
                            <span className="font-bold text-slate-700 dark:text-slate-300">
                              {b.block ?? 0}/25
                            </span>
                          </div>
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">Room</span>
                            <span className="font-bold text-slate-700 dark:text-slate-300">
                              {b.room ?? 0}/25
                            </span>
                          </div>
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">Category</span>
                            <span className="font-bold text-slate-700 dark:text-slate-300">
                              {b.category ?? 0}/20
                            </span>
                          </div>
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">Time</span>
                            <span className="font-bold text-slate-700 dark:text-slate-300">
                              {b.time ?? 0}/10
                            </span>
                          </div>
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">Text</span>
                            <span className="font-bold text-slate-700 dark:text-slate-300">
                              {b.text ?? 0}/10
                            </span>
                          </div>
                          <div className="p-1.5 rounded-lg bg-slate-50 dark:bg-slate-800/50">
                            <span className="text-slate-400 block font-medium">AI Image</span>
                            <span className="font-bold text-brand-600 dark:text-brand-400">
                              {b.image ?? 0}/10
                            </span>
                          </div>
                        </div>
                      </Card>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Column: Status Timeline, Owner Card, Actions */}
        <div className="space-y-6">
          {/* Status Timeline */}
          <Card className="p-5">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
              Status Flow
            </h2>

            <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-800">
              {timelineSteps.map((s, idx) => (
                <div key={idx} className="relative flex items-center justify-between">
                  <div
                    className={`absolute -left-6 w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] ${
                      s.active
                        ? 'border-brand-600 bg-brand-600 text-white'
                        : 'border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-400'
                    }`}
                  >
                    ✓
                  </div>
                  <span
                    className={`text-sm ${
                      s.active
                        ? 'font-bold text-slate-900 dark:text-white'
                        : 'text-slate-400 font-medium'
                    }`}
                  >
                    {s.label}
                  </span>
                </div>
              ))}
            </div>
          </Card>

          {/* Owner Profile (Masked) */}
          <Card className="p-5">
            <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
              Posted By
            </h2>
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-bold flex items-center justify-center text-base border border-brand-200 dark:border-brand-800 shrink-0">
                {ownerFirstName.charAt(0).toUpperCase()}
              </div>
              <div>
                <span className="font-semibold text-sm text-slate-900 dark:text-white block">
                  {ownerFirstName}
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  {item.owner?.department || 'Student'} • {item.owner?.karmaPoints ?? 0} karma
                </span>
              </div>
            </div>
            <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 text-[11px] text-slate-400 flex items-center gap-1.5">
              <span>🔒 Contact details are kept strictly private until claim verification.</span>
            </div>
          </Card>

          {/* Action Buttons */}
          <Card className="p-5 space-y-3">
            {canManage ? (
              <>
                <Button
                  variant="danger"
                  className="w-full text-xs"
                  onClick={() => setIsDeleteModalOpen(true)}
                >
                  <Trash2 className="w-4 h-4 mr-1.5" /> Delete Post
                </Button>
              </>
            ) : (
              isAuthenticated && (
                <Button
                  variant="ghost"
                  className="w-full text-xs text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                  onClick={() => setIsReportModalOpen(true)}
                >
                  <Flag className="w-4 h-4 mr-1.5" /> Report Inappropriate Post
                </Button>
              )
            )}
          </Card>
        </div>
      </div>

      {/* "You may also like" Similar Items */}
      {similarItems.length > 0 && (
        <div className="mt-12 pt-8 border-t border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-2 mb-6">
            <div className="w-8 h-8 rounded-lg bg-brand-50 dark:bg-brand-950 text-brand-600 dark:text-brand-400 flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">You May Also Like</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Top similar items of opposite type matched via AI visual & text embeddings
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
            {similarItems.map((sim) => (
              <ItemCard key={sim.item.id} item={sim.item} />
            ))}
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        title="Delete Post"
        description="Are you sure you want to permanently delete this post? This action cannot be undone."
      >
        <div className="flex items-center justify-end gap-3 mt-6">
          <Button
            variant="ghost"
            onClick={() => setIsDeleteModalOpen(false)}
            disabled={deleteMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            isLoading={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate()}
          >
            Yes, Delete Post
          </Button>
        </div>
      </Modal>

      {/* Report Modal */}
      <Modal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        title="Report Post"
        description="Help maintain a safe campus community. Please specify the reason for reporting this post."
      >
        <div className="space-y-4 mt-2">
          <textarea
            rows={3}
            value={reportReason}
            onChange={(e) => setReportReason(e.target.value)}
            placeholder="e.g. Inappropriate photo, personal phone numbers exposed, spam, duplicate..."
            className="w-full rounded-input border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              variant="ghost"
              onClick={() => setIsReportModalOpen(false)}
              disabled={reportMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              disabled={!reportReason.trim()}
              isLoading={reportMutation.isPending}
              onClick={() => reportMutation.mutate(reportReason.trim())}
            >
              Submit Report
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
