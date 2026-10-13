import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  ArrowRight,
  MessageSquare,
  XCircle,
  ChevronRight,
  Package,
} from 'lucide-react';
import { matchesApi } from '../api/matches';
import type { MatchResponseDto, MatchStatus } from '../types';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Button } from '../components/ui/Button';
import { cn } from '../lib/utils';

export const MatchesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'ALL' | MatchStatus>('ALL');

  const { data: pageData, isLoading, error, refetch } = useQuery({
    queryKey: ['myMatches'],
    queryFn: () => matchesApi.getMyMatches(0, 50),
  });

  const matches: MatchResponseDto[] = pageData?.content || [];

  const filteredMatches = matches.filter((m) => {
    if (activeTab === 'ALL') return true;
    return m.status === activeTab;
  });

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-emerald-600 dark:text-emerald-400 border-emerald-500';
    if (score >= 60) return 'text-amber-600 dark:text-amber-400 border-amber-500';
    return 'text-slate-500 dark:text-slate-400 border-slate-400';
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight flex items-center gap-2">
            <Sparkles className="w-7 h-7 text-brand-600 dark:text-brand-400" />
            My Matches
          </h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            Potential lost & found matches automatically identified by our matching engine.
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-2 mb-6 border-b border-slate-200 dark:border-slate-800 scrollbar-none">
        {(['ALL', 'SUGGESTED', 'CONFIRMED', 'RETURNED', 'REJECTED'] as const).map((tab) => {
          const count =
            tab === 'ALL'
              ? matches.length
              : matches.filter((m) => m.status === tab).length;
          return (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                'px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all whitespace-nowrap flex items-center gap-1.5 min-h-touch',
                activeTab === tab
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200/60 dark:border-slate-800'
              )}
            >
              <span>{tab.charAt(0) + tab.slice(1).toLowerCase()}</span>
              <span
                className={cn(
                  'px-1.5 py-0.2 rounded-full text-[10px] font-bold',
                  activeTab === tab
                    ? 'bg-white/20 text-white'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                )}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="space-y-4">
          {[1, 2, 3].map((n) => (
            <Card key={n} className="p-5">
              <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
                <div className="flex items-center gap-4 w-full sm:w-auto">
                  <Skeleton className="w-16 h-16 rounded-xl" />
                  <Skeleton className="w-16 h-16 rounded-xl" />
                  <div className="space-y-2 flex-1">
                    <Skeleton className="h-5 w-40" />
                    <Skeleton className="h-4 w-28" />
                  </div>
                </div>
                <Skeleton className="h-10 w-28 rounded-xl" />
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Error state */}
      {error && !isLoading && (
        <EmptyState
          icon={<XCircle className="w-8 h-8 text-rose-500" />}
          title="Could not load matches"
          description="There was a network issue loading your item matches. Please check your connection and retry."
          actionText="Try Again"
          onAction={() => refetch()}
        />
      )}

      {/* Empty State */}
      {!isLoading && !error && filteredMatches.length === 0 && (
        <EmptyState
          icon={<Sparkles className="w-8 h-8 text-brand-600" />}
          title={
            activeTab === 'ALL'
              ? 'No matches found yet'
              : `No ${activeTab.toLowerCase()} matches`
          }
          description={
            activeTab === 'ALL'
              ? "When you post a lost or found item, our engine automatically searches for high-probability pairs across campus."
              : `You don't currently have any matches in the ${activeTab.toLowerCase()} status.`
          }
          actionText="Post an Item"
          onAction={() => {
            window.location.href = '/post';
          }}
        />
      )}

      {/* Matches List */}
      {!isLoading && !error && filteredMatches.length > 0 && (
        <div className="space-y-4">
          {filteredMatches.map((match) => {
            const otherFirstName = match.otherUser?.fullName
              ? match.otherUser.fullName.split(' ')[0]
              : 'Student';
            const otherDept = match.otherUser?.department || 'Campus Student';

            return (
              <Card
                key={match.id}
                className="p-5 hover:shadow-md transition-shadow group border border-slate-200/80 dark:border-slate-800"
              >
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-5">
                  {/* Left: Dual Items Visuals & Info */}
                  <div className="flex flex-col sm:flex-row sm:items-center gap-4 flex-1">
                    {/* Items pair avatar display */}
                    <div className="flex items-center gap-2 shrink-0">
                      {/* Lost Item Photo */}
                      <div className="relative group/photo">
                        <div className="w-16 h-16 rounded-2xl overflow-hidden bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 flex items-center justify-center">
                          {match.lostItem?.imageUrl ? (
                            <img
                              src={match.lostItem.imageUrl}
                              alt={match.lostItem.title}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <Package className="w-6 h-6 text-rose-500" />
                          )}
                        </div>
                        <span className="absolute -top-1.5 -left-1 px-1.5 py-0.5 rounded-full bg-rose-500 text-white text-[9px] font-bold shadow-xs">
                          LOST
                        </span>
                      </div>

                      {/* Matching Arrow */}
                      <div className="p-1.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-400">
                        <ArrowRight className="w-3.5 h-3.5" />
                      </div>

                      {/* Found Item Photo */}
                      <div className="relative group/photo">
                        <div className="w-16 h-16 rounded-2xl overflow-hidden bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 flex items-center justify-center">
                          {match.foundItem?.imageUrl ? (
                            <img
                              src={match.foundItem.imageUrl}
                              alt={match.foundItem.title}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <Package className="w-6 h-6 text-emerald-500" />
                          )}
                        </div>
                        <span className="absolute -top-1.5 -right-1 px-1.5 py-0.5 rounded-full bg-emerald-600 text-white text-[9px] font-bold shadow-xs">
                          FOUND
                        </span>
                      </div>
                    </div>

                    {/* Titles and Details */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <Badge status={match.status} />
                        <span className="text-xs text-slate-500 dark:text-slate-400">
                          Matched with <span className="font-semibold text-slate-700 dark:text-slate-300">{otherFirstName}</span> ({otherDept})
                        </span>
                      </div>

                      <div className="text-sm font-semibold text-slate-900 dark:text-white truncate">
                        {match.lostItem?.title} & {match.foundItem?.title}
                      </div>

                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-1">
                        Lost at {match.lostItem?.blockName || 'Campus'} • Found at {match.foundItem?.blockName || 'Campus'}
                      </p>
                    </div>
                  </div>

                  {/* Right: Score Ring and Action Buttons */}
                  <div className="flex items-center justify-between md:justify-end gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-slate-100 dark:border-slate-800/80">
                    {/* Score Ring */}
                    <div className="flex items-center gap-2">
                      <div
                        className={cn(
                          'w-11 h-11 rounded-full border-2 flex flex-col items-center justify-center font-extrabold text-xs',
                          getScoreColor(match.score)
                        )}
                        title={`Match confidence: ${match.score}%`}
                      >
                        <span>{match.score}%</span>
                      </div>
                      <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 hidden sm:inline">
                        Match
                      </span>
                    </div>

                    {/* Quick CTA */}
                    <div className="flex items-center gap-2">
                      {match.status === 'CONFIRMED' && (
                        <Link to={`/chat/${match.id}`}>
                          <Button size="sm" variant="outline" className="gap-1.5 text-xs">
                            <MessageSquare className="w-3.5 h-3.5 text-brand-600" />
                            Chat
                          </Button>
                        </Link>
                      )}

                      <Link to={`/matches/${match.id}`}>
                        <Button size="sm" variant="primary" className="gap-1 text-xs">
                          {match.status === 'SUGGESTED' ? 'Verify & Claim' : 'View Details'}
                          <ChevronRight className="w-3.5 h-3.5" />
                        </Button>
                      </Link>
                    </div>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MatchesPage;
