import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Sparkles,
  ShieldCheck,
  MessageSquare,
  Building,
  Calendar,
  AlertCircle,
  HelpCircle,
  Check,
  X,
  Award,
  ChevronLeft,
  FileText,
} from 'lucide-react';
import { matchesApi } from '../api/matches';
import { useAuth } from '../features/auth/AuthContext';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { EmptyState } from '../components/ui/EmptyState';
import { Skeleton } from '../components/ui/Skeleton';
import { toast } from 'sonner';
import { cn } from '../lib/utils';
import type { ClaimResponse } from '../types';

export const MatchDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const matchId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  // Modals state
  const [rejectMatchOpen, setRejectMatchOpen] = useState(false);
  const [markReturnedOpen, setMarkReturnedOpen] = useState(false);
  const [approveClaimId, setApproveClaimId] = useState<number | null>(null);

  // Form states
  const [claimAnswer, setClaimAnswer] = useState('');
  const [officeProof, setOfficeProof] = useState('');

  // 1. Fetch match details
  const {
    data: match,
    isLoading: matchLoading,
    error: matchError,
  } = useQuery({
    queryKey: ['match', matchId],
    queryFn: () => matchesApi.getMatchById(matchId),
    enabled: !!matchId,
  });

  // Check roles: Is current user lost-item owner or found-item owner?
  const isLostOwner = !!(user && match && match.lostItem?.owner?.id === user.id);
  const isFinder = !!(user && match && match.foundItem?.owner?.id === user.id);

  // 2. Fetch verification question if user is lost item owner and match is SUGGESTED
  const {
    data: verificationData,
    isLoading: verificationLoading,
    refetch: refetchVerification,
  } = useQuery({
    queryKey: ['verification', matchId],
    queryFn: () => matchesApi.getVerificationQuestion(matchId),
    enabled: !!matchId && isLostOwner && match?.status === 'SUGGESTED',
  });

  // 3. Fetch submitted claims if user is finder
  const {
    data: claimsData = [],
    isLoading: claimsLoading,
    refetch: refetchClaims,
  } = useQuery({
    queryKey: ['claims', matchId],
    queryFn: () => matchesApi.getClaims(matchId),
    enabled: !!matchId && isFinder,
  });

  // Mutations
  const submitClaimMutation = useMutation({
    mutationFn: (answer: string) => matchesApi.submitClaim(matchId, answer),
    onSuccess: () => {
      toast.success('Your claim answer was submitted to the finder!');
      setClaimAnswer('');
      queryClient.invalidateQueries({ queryKey: ['match', matchId] });
      refetchVerification();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to submit claim answer');
    },
  });

  const submitOfficeClaimMutation = useMutation({
    mutationFn: (proof: string) => matchesApi.submitOfficeClaim(matchId, proof),
    onSuccess: () => {
      toast.success('Office claim submitted for administrator review!');
      setOfficeProof('');
      queryClient.invalidateQueries({ queryKey: ['match', matchId] });
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to submit office claim');
    },
  });

  const approveClaimMutation = useMutation({
    mutationFn: (claimId: number) => matchesApi.approveClaim(matchId, claimId),
    onSuccess: () => {
      toast.success('Claim approved! Match is confirmed and chat is unlocked.');
      setApproveClaimId(null);
      queryClient.invalidateQueries({ queryKey: ['match', matchId] });
      refetchClaims();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to approve claim');
    },
  });

  const rejectClaimMutation = useMutation({
    mutationFn: (claimId: number) => matchesApi.rejectClaim(matchId, claimId),
    onSuccess: () => {
      toast.info('Claim answer was rejected.');
      refetchClaims();
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to reject claim');
    },
  });

  const rejectMatchMutation = useMutation({
    mutationFn: () => matchesApi.rejectMatch(matchId),
    onSuccess: () => {
      toast.info('Match declined.');
      setRejectMatchOpen(false);
      navigate('/matches');
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to decline match');
    },
  });

  const markReturnedMutation = useMutation({
    mutationFn: () => matchesApi.markReturned(matchId),
    onSuccess: () => {
      toast.success('Item confirmed as returned! Finder awarded +10 karma.');
      setMarkReturnedOpen(false);
      queryClient.invalidateQueries({ queryKey: ['match', matchId] });
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to mark as returned');
    },
  });

  if (matchLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (matchError || !match) {
    return (
      <div className="max-w-xl mx-auto px-4 py-16">
        <EmptyState
          icon={<AlertCircle className="w-8 h-8 text-rose-500" />}
          title="Match not accessible"
          description="Could not load match details. You must be one of the two owners or an administrator to view this match."
          actionText="Back to Matches"
          onAction={() => navigate('/matches')}
        />
      </div>
    );
  }

  // Stepper Timeline State
  const steps: { label: string; status: 'completed' | 'current' | 'pending' }[] = [
    {
      label: 'Suggested',
      status: match.status === 'SUGGESTED' ? 'current' : 'completed',
    },
    {
      label: 'Claim & Verify',
      status:
        match.status === 'SUGGESTED'
          ? 'pending'
          : match.status === 'CONFIRMED' || match.status === 'RETURNED'
          ? 'completed'
          : 'pending',
    },
    {
      label: 'Confirmed (Chat)',
      status:
        match.status === 'CONFIRMED'
          ? 'current'
          : match.status === 'RETURNED'
          ? 'completed'
          : 'pending',
    },
    {
      label: 'Returned',
      status: match.status === 'RETURNED' ? 'completed' : 'pending',
    },
  ];

  const breakdown = match.scoreBreakdown || {};

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* Top Navigation & Status */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <button
          onClick={() => navigate('/matches')}
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 dark:text-slate-400 hover:text-brand-600 dark:hover:text-brand-400"
        >
          <ChevronLeft className="w-4 h-4" />
          Back to matches
        </button>

        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-500">Status:</span>
          <Badge status={match.status} />
        </div>
      </div>

      {/* Timeline Stepper */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl p-4 sm:p-6 border border-slate-200/80 dark:border-slate-800 shadow-xs">
        <div className="flex items-center justify-between relative">
          <div className="absolute left-6 right-6 top-1/2 -translate-y-1/2 h-0.5 bg-slate-200 dark:bg-slate-800 -z-0" />
          {steps.map((step, idx) => (
            <div key={step.label} className="flex flex-col items-center relative z-10">
              <div
                className={cn(
                  'w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold transition-colors',
                  step.status === 'completed'
                    ? 'bg-brand-600 text-white'
                    : step.status === 'current'
                    ? 'bg-amber-500 text-white ring-4 ring-amber-100 dark:ring-amber-950'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-400 border border-slate-300 dark:border-slate-700'
                )}
              >
                {step.status === 'completed' ? <Check className="w-4 h-4" /> : idx + 1}
              </div>
              <span className="text-[11px] font-semibold mt-1.5 text-slate-700 dark:text-slate-300 hidden sm:inline">
                {step.label}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Side-by-Side Items Comparison */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Lost Item Card */}
        <Card className="p-5 border-l-4 border-l-rose-500">
          <div className="flex items-center justify-between mb-3">
            <span className="px-2.5 py-0.5 rounded-full bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-400 text-xs font-bold uppercase tracking-wider">
              Lost Item
            </span>
            {isLostOwner && (
              <span className="text-xs font-semibold text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 px-2 py-0.5 rounded-md">
                Your Post
              </span>
            )}
          </div>

          <div className="aspect-video w-full rounded-xl overflow-hidden bg-slate-100 dark:bg-slate-800 mb-4 border border-slate-200 dark:border-slate-700 flex items-center justify-center">
            {match.lostItem?.imageUrl ? (
              <img
                src={match.lostItem.imageUrl}
                alt={match.lostItem.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="text-slate-400 text-xs flex flex-col items-center gap-1">
                <FileText className="w-8 h-8 stroke-1" />
                <span>No photo provided</span>
              </div>
            )}
          </div>

          <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-2">
            {match.lostItem?.title}
          </h3>

          <p className="text-xs text-slate-600 dark:text-slate-400 mb-4 line-clamp-2">
            {match.lostItem?.description || 'No description provided.'}
          </p>

          <div className="space-y-1.5 text-xs text-slate-500 dark:text-slate-400 pt-3 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2">
              <Building className="w-3.5 h-3.5 text-slate-400" />
              <span>Location: {match.lostItem?.blockName || 'Campus'}</span>
            </div>
            <div className="flex items-center gap-2">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              <span>Reported: {new Date(match.lostItem?.eventTime || match.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800">
            <Link
              to={`/items/${match.lostItem?.id}`}
              className="text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline"
            >
              View original lost post →
            </Link>
          </div>
        </Card>

        {/* Found Item Card */}
        <Card className="p-5 border-l-4 border-l-emerald-500">
          <div className="flex items-center justify-between mb-3">
            <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400 text-xs font-bold uppercase tracking-wider">
              Found Item
            </span>
            {isFinder && (
              <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded-md">
                Your Post
              </span>
            )}
          </div>

          <div className="aspect-video w-full rounded-xl overflow-hidden bg-slate-100 dark:bg-slate-800 mb-4 border border-slate-200 dark:border-slate-700 flex items-center justify-center">
            {match.foundItem?.imageUrl ? (
              <img
                src={match.foundItem.imageUrl}
                alt={match.foundItem.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="text-slate-400 text-xs flex flex-col items-center gap-1">
                <FileText className="w-8 h-8 stroke-1" />
                <span>No photo provided</span>
              </div>
            )}
          </div>

          <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-2">
            {match.foundItem?.title}
          </h3>

          <p className="text-xs text-slate-600 dark:text-slate-400 mb-4 line-clamp-2">
            {match.foundItem?.description || 'No description provided.'}
          </p>

          <div className="space-y-1.5 text-xs text-slate-500 dark:text-slate-400 pt-3 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2">
              <Building className="w-3.5 h-3.5 text-slate-400" />
              <span>Location: {match.foundItem?.blockName || 'Campus'}</span>
            </div>
            <div className="flex items-center gap-2">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              <span>Found: {new Date(match.foundItem?.eventTime || match.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800">
            <Link
              to={`/items/${match.foundItem?.id}`}
              className="text-xs font-semibold text-brand-600 dark:text-brand-400 hover:underline"
            >
              View original found post →
            </Link>
          </div>
        </Card>
      </div>

      {/* Confidence Score & Breakdown Bars */}
      <Card className="p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
          <div>
            <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-brand-600" />
              Match Confidence Breakdown
            </h3>
            <p className="text-xs text-slate-500">
              Computed by CampusFind multi-signal matching algorithms
            </p>
          </div>
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <div className="text-2xl font-black text-brand-600 dark:text-brand-400">
              {match.score}%
            </div>
            <span className="text-xs font-semibold text-slate-500 uppercase">Overall Match</span>
          </div>
        </div>

        <div className="space-y-3 pt-2">
          {Object.entries({
            Category: Number(breakdown.category || 0),
            'Block & Area': Number(breakdown.block || 0),
            Room: Number(breakdown.room || 0),
            'Date & Timing': Number(breakdown.time || 0),
            'Text Similarity': Number(breakdown.text || 0),
            'AI Vision Match': Number(breakdown.image || 0),
          }).map(([label, score]) => (
            <div key={label} className="space-y-1">
              <div className="flex justify-between text-xs font-medium">
                <span className="text-slate-600 dark:text-slate-300">{label}</span>
                <span className="text-slate-900 dark:text-white font-bold">{score}%</span>
              </div>
              <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-brand-500 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(score, 100)}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Dynamic Claim Flow Sections */}
      {/* 1. If Lost item owner and match is SUGGESTED */}
      {isLostOwner && match.status === 'SUGGESTED' && (
        <Card className="p-6 border-2 border-brand-500/20 bg-brand-50/20 dark:bg-brand-950/20">
          <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2 mb-2">
            <HelpCircle className="w-5 h-5 text-brand-600" />
            Verification & Ownership Proof
          </h3>

          {verificationLoading ? (
            <div className="py-4 space-y-2">
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-20 w-full rounded-xl" />
            </div>
          ) : verificationData?.handedToOffice ? (
            /* Office pickup flow */
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-xs text-amber-800 dark:text-amber-200">
                <p className="font-semibold mb-1">Campus Office Custody</p>
                <p>
                  This item has been deposited at the Campus Security / Administrative Office. Please describe identifying details of your property to request verification from the campus office.
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  Proof of Ownership & Unique Marks (max 500 chars)
                </label>
                <textarea
                  value={officeProof}
                  onChange={(e) => setOfficeProof(e.target.value)}
                  maxLength={500}
                  rows={3}
                  placeholder="E.g., Serial number, scratches, stickers, or wallpaper photo description..."
                  className="w-full text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 p-3 focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <Button
                variant="primary"
                onClick={() => submitOfficeClaimMutation.mutate(officeProof)}
                disabled={!officeProof.trim() || submitOfficeClaimMutation.isPending}
                isLoading={submitOfficeClaimMutation.isPending}
              >
                Submit Office Claim
              </Button>
            </div>
          ) : (
            /* Standard Verification Question Flow */
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Finder's Verification Question
                </span>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                  "{verificationData?.verificationQuestion || 'What are distinguishing marks on this item?'}"
                </p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  Your Answer (Max 3 attempts permitted)
                </label>
                <textarea
                  value={claimAnswer}
                  onChange={(e) => setClaimAnswer(e.target.value)}
                  maxLength={500}
                  rows={3}
                  placeholder="Provide your specific answer to verify you are the rightful owner..."
                  className="w-full text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 p-3 focus:ring-2 focus:ring-brand-500"
                />
                <span className="text-[11px] text-slate-400 block text-right mt-1">
                  {claimAnswer.length}/500 chars
                </span>
              </div>

              <div className="flex items-center justify-between">
                <Button
                  variant="primary"
                  onClick={() => submitClaimMutation.mutate(claimAnswer)}
                  disabled={!claimAnswer.trim() || submitClaimMutation.isPending}
                  isLoading={submitClaimMutation.isPending}
                >
                  Submit Answer to Finder
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* 2. If Finder: review submitted claims */}
      {isFinder && (
        <Card className="p-6">
          <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2 mb-4">
            <ShieldCheck className="w-5 h-5 text-brand-600" />
            Submitted Claims from Potential Owner
          </h3>

          {claimsLoading ? (
            <Skeleton className="h-20 w-full rounded-xl" />
          ) : claimsData.length === 0 ? (
            <p className="text-xs text-slate-500 italic py-2">
              No claims submitted by the lost item owner yet. When they answer your question, their responses will appear here for review.
            </p>
          ) : (
            <div className="space-y-4">
              {claimsData.map((claim: ClaimResponse) => {
                const claimantFirst = claim.claimant?.fullName
                  ? claim.claimant.fullName.split(' ')[0]
                  : 'Student';

                return (
                  <div
                    key={claim.id}
                    className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                        {claimantFirst} ({claim.claimant?.department || 'Student'})
                      </span>
                      <span className="text-[11px] text-slate-400">
                        {new Date(claim.createdAt).toLocaleString()}
                      </span>
                    </div>

                    <p className="text-xs text-slate-800 dark:text-slate-200 bg-white dark:bg-slate-800 p-3 rounded-lg border border-slate-200 dark:border-slate-700">
                      "{claim.answer}"
                    </p>

                    <div className="flex items-center justify-between pt-1">
                      {claim.approved === true ? (
                        <Badge variant="brand">Approved</Badge>
                      ) : claim.approved === false ? (
                        <Badge variant="neutral">Rejected</Badge>
                      ) : (
                        <div className="flex items-center gap-2">
                          <Button
                            size="sm"
                            variant="primary"
                            className="text-xs gap-1"
                            onClick={() => setApproveClaimId(claim.id)}
                          >
                            <Check className="w-3.5 h-3.5" />
                            Approve Answer
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="text-xs gap-1 text-rose-600 hover:text-rose-700"
                            onClick={() => rejectClaimMutation.mutate(claim.id)}
                            isLoading={rejectClaimMutation.isPending}
                          >
                            <X className="w-3.5 h-3.5" />
                            Reject
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      )}

      {/* 3. When CONFIRMED: Big Chat button + Mark returned */}
      {match.status === 'CONFIRMED' && (
        <Card className="p-6 bg-emerald-50/40 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-800 space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white">Match Confirmed!</h3>
              <p className="text-xs text-slate-600 dark:text-slate-400">
                You can now message each other in real-time to arrange a safe public handoff.
              </p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
            <Link to={`/chat/${match.id}`} className="w-full sm:w-auto">
              <Button size="lg" variant="primary" className="w-full gap-2">
                <MessageSquare className="w-5 h-5" />
                Open Realtime Chat
              </Button>
            </Link>

            <Button
              size="lg"
              variant="outline"
              className="w-full sm:w-auto gap-2 text-emerald-700 border-emerald-300 hover:bg-emerald-50 dark:hover:bg-emerald-950"
              onClick={() => setMarkReturnedOpen(true)}
            >
              <Award className="w-5 h-5 text-emerald-600" />
              Mark as Returned (+10 Karma)
            </Button>
          </div>
        </Card>
      )}

      {/* 4. When RETURNED: Success Celebration */}
      {match.status === 'RETURNED' && (
        <Card className="p-8 text-center bg-brand-50/40 dark:bg-brand-950/30 border-brand-200 dark:border-brand-800 space-y-4">
          <div className="w-16 h-16 rounded-full bg-brand-600 text-white flex items-center justify-center mx-auto shadow-md">
            <Award className="w-8 h-8" />
          </div>
          <div>
            <h2 className="text-xl font-black text-slate-900 dark:text-white">
              Item Successfully Returned!
            </h2>
            <p className="text-sm text-slate-600 dark:text-slate-400 max-w-md mx-auto mt-1">
              Thank you for keeping our college community trustworthy and connected. The finder has been awarded +10 karma points!
            </p>
          </div>
          <div className="pt-2">
            <Link to="/leaderboard">
              <Button variant="outline" size="sm">
                View Community Leaderboard
              </Button>
            </Link>
          </div>
        </Card>
      )}

      {/* Bottom Option: Reject Match */}
      {match.status === 'SUGGESTED' && (
        <div className="pt-4 flex justify-end">
          <Button
            variant="ghost"
            size="sm"
            className="text-xs text-rose-600 hover:text-rose-700 hover:bg-rose-50 dark:hover:bg-rose-950/30"
            onClick={() => setRejectMatchOpen(true)}
          >
            Not the right match? Decline match
          </Button>
        </div>
      )}

      {/* Confirmation Modals */}
      {/* 1. Approve Claim Confirmation Modal */}
      <Modal
        isOpen={approveClaimId !== null}
        onClose={() => setApproveClaimId(null)}
        title="Approve Ownership Claim?"
        description="Are you certain this answer correctly identifies your found item? Once approved, match status will become CONFIRMED and realtime chat will unlock."
      >
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="ghost" onClick={() => setApproveClaimId(null)}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={() => approveClaimId && approveClaimMutation.mutate(approveClaimId)}
            isLoading={approveClaimMutation.isPending}
          >
            Yes, Approve & Unlock Chat
          </Button>
        </div>
      </Modal>

      {/* 2. Reject Match Confirmation Modal */}
      <Modal
        isOpen={rejectMatchOpen}
        onClose={() => setRejectMatchOpen(false)}
        title="Decline this Match?"
        description="Are you sure this is not your item? Declining this match will remove it from your active suggestions list."
      >
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="ghost" onClick={() => setRejectMatchOpen(false)}>
            Keep Match
          </Button>
          <Button
            variant="danger"
            onClick={() => rejectMatchMutation.mutate()}
            isLoading={rejectMatchMutation.isPending}
          >
            Decline Match
          </Button>
        </div>
      </Modal>

      {/* 3. Mark as Returned Confirmation Modal */}
      <Modal
        isOpen={markReturnedOpen}
        onClose={() => setMarkReturnedOpen(false)}
        title="Confirm Item Handed Over?"
        description="Have you successfully handed over the item in person? This will complete the match, close both posts, and reward +10 karma points to the finder."
      >
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="ghost" onClick={() => setMarkReturnedOpen(false)}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={() => markReturnedMutation.mutate()}
            isLoading={markReturnedMutation.isPending}
          >
            Confirm & Complete Return
          </Button>
        </div>
      </Modal>
    </div>
  );
};

export default MatchDetailPage;
