import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Trophy,
  Award,
  Star,
  HeartHandshake,
} from 'lucide-react';
import { leaderboardApi } from '../api/leaderboard';
import { useAuth } from '../features/auth/AuthContext';
import { Card } from '../components/ui/Card';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { cn } from '../lib/utils';

export const LeaderboardPage: React.FC = () => {
  const { user: currentUser } = useAuth();

  const {
    data: leaderboard = [],
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['leaderboard'],
    queryFn: leaderboardApi.getLeaderboard,
  });

  const top3 = leaderboard.slice(0, 3);
  const rest = leaderboard.slice(3);

  const getBadgeIcon = (badge: string) => {
    switch (badge.toUpperCase()) {
      case 'FIRST RETURN':
        return <Award className="w-3 h-3 text-amber-500" />;
      case 'CAMPUS HELPER':
      case 'HELPER':
        return <HeartHandshake className="w-3 h-3 text-emerald-500" />;
      case 'TOP FINDER':
        return <Trophy className="w-3 h-3 text-brand-500" />;
      default:
        return <Star className="w-3 h-3 text-indigo-500" />;
    }
  };

  const getRankBadge = (rank: number) => {
    if (rank === 1) {
      return (
        <div className="w-8 h-8 rounded-full bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 font-black flex items-center justify-center text-sm border border-amber-300 dark:border-amber-700 shadow-xs">
          🥇
        </div>
      );
    }
    if (rank === 2) {
      return (
        <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-black flex items-center justify-center text-sm border border-slate-300 dark:border-slate-700 shadow-xs">
          🥈
        </div>
      );
    }
    if (rank === 3) {
      return (
        <div className="w-8 h-8 rounded-full bg-amber-800/20 text-amber-800 dark:text-amber-300 font-black flex items-center justify-center text-sm border border-amber-700/40 shadow-xs">
          🥉
        </div>
      );
    }
    return (
      <div className="w-7 h-7 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 font-bold flex items-center justify-center text-xs">
        #{rank}
      </div>
    );
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* Page Header */}
      <div className="text-center space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 dark:bg-amber-950/50 text-amber-700 dark:text-amber-300 text-xs font-bold border border-amber-200 dark:border-amber-800">
          <Trophy className="w-3.5 h-3.5" />
          Campus Honor Roll
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          Community Karma Leaderboard
        </h1>
        <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 max-w-md mx-auto">
          Honoring students who find and return lost belongings across college campus.
        </p>
      </div>

      {isLoading && (
        <div className="space-y-3">
          <Skeleton className="h-44 w-full rounded-2xl" />
          {[1, 2, 3, 4].map((n) => (
            <Skeleton key={n} className="h-16 w-full rounded-xl" />
          ))}
        </div>
      )}

      {error && !isLoading && (
        <EmptyState
          icon={<Trophy className="w-8 h-8 text-amber-500" />}
          title="Could not load leaderboard"
          description="Check your network connection and try again."
          actionText="Retry"
          onAction={() => refetch()}
        />
      )}

      {!isLoading && !error && leaderboard.length === 0 && (
        <EmptyState
          icon={<Award className="w-8 h-8 text-brand-600" />}
          title="No karma awards yet"
          description="Be the first to return a lost item to earn +10 karma points and claim the #1 spot!"
        />
      )}

      {!isLoading && !error && leaderboard.length > 0 && (
        <>
          {/* Top 3 Podium Cards */}
          {top3.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5 pt-2">
              {top3.map((entry, idx) => {
                const rank = idx + 1;
                const isMe = entry.id === currentUser?.id;
                const firstName = entry.fullName ? entry.fullName.split(' ')[0] : 'Student';

                return (
                  <Card
                    key={entry.id}
                    className={cn(
                      'p-5 text-center relative flex flex-col items-center justify-between transition-transform duration-200 hover:-translate-y-0.5',
                      rank === 1 &&
                        'border-2 border-amber-400/80 bg-gradient-to-b from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 shadow-md sm:order-2',
                      rank === 2 && 'sm:order-1 border-slate-300 dark:border-slate-700',
                      rank === 3 && 'sm:order-3 border-amber-800/40'
                    )}
                  >
                    <div className="absolute top-3 right-3">{getRankBadge(rank)}</div>

                    <div className="relative mb-3 mt-2">
                      <div className="w-16 h-16 rounded-full bg-slate-100 dark:bg-slate-800 border-2 border-white dark:border-slate-700 shadow-sm flex items-center justify-center font-black text-lg text-slate-700 dark:text-slate-200">
                        {entry.avatarUrl ? (
                          <img
                            src={entry.avatarUrl}
                            alt={firstName}
                            className="w-full h-full rounded-full object-cover"
                          />
                        ) : (
                          firstName.charAt(0).toUpperCase()
                        )}
                      </div>
                    </div>

                    <div>
                      <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center justify-center gap-1">
                        {firstName}
                        {isMe && (
                          <span className="text-[10px] font-semibold text-brand-600 bg-brand-50 dark:bg-brand-950 px-1.5 py-0.2 rounded-full">
                            You
                          </span>
                        )}
                      </h3>
                      <p className="text-[11px] text-slate-500 truncate max-w-[140px] mx-auto">
                        {entry.department || 'Campus Student'}
                      </p>
                    </div>

                    <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 w-full">
                      <div className="text-xl font-black text-amber-600 dark:text-amber-400">
                        {entry.karmaPoints}{' '}
                        <span className="text-xs font-semibold text-slate-400">pts</span>
                      </div>
                      <span className="text-[10px] text-slate-400 block mt-0.5">
                        {entry.returnsCount} items returned
                      </span>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}

          {/* Ranks 4 to 20 List */}
          {rest.length > 0 && (
            <Card className="divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden">
              {rest.map((entry, idx) => {
                const rank = idx + 4;
                const isMe = entry.id === currentUser?.id;
                const firstName = entry.fullName ? entry.fullName.split(' ')[0] : 'Student';

                return (
                  <div
                    key={entry.id}
                    className={cn(
                      'p-4 flex items-center justify-between gap-4 transition-colors',
                      isMe
                        ? 'bg-brand-50/60 dark:bg-brand-950/40 font-semibold'
                        : 'hover:bg-slate-50 dark:hover:bg-slate-850'
                    )}
                  >
                    <div className="flex items-center gap-3.5">
                      <div className="w-7 text-center font-bold text-xs text-slate-400">
                        #{rank}
                      </div>

                      <div className="w-10 h-10 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center font-bold text-xs text-slate-700 dark:text-slate-300">
                        {firstName.charAt(0).toUpperCase()}
                      </div>

                      <div>
                        <div className="text-sm font-semibold text-slate-900 dark:text-white flex items-center gap-1.5">
                          <span>{firstName}</span>
                          {isMe && (
                            <span className="text-[10px] font-bold text-brand-600 bg-brand-100 dark:bg-brand-950 px-1.5 py-0.2 rounded-full">
                              You
                            </span>
                          )}
                        </div>
                        <span className="text-xs text-slate-500">
                          {entry.department || 'Campus Student'}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-4">
                      {/* Badges list */}
                      <div className="hidden sm:flex items-center gap-1">
                        {entry.badges?.map((b) => (
                          <span
                            key={b}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-[10px] font-medium text-slate-600 dark:text-slate-300"
                          >
                            {getBadgeIcon(b)}
                            {b}
                          </span>
                        ))}
                      </div>

                      <div className="text-right min-w-[70px]">
                        <span className="text-sm font-black text-brand-600 dark:text-brand-400">
                          {entry.karmaPoints}
                        </span>
                        <span className="text-[10px] text-slate-400 block">
                          {entry.returnsCount} returns
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </Card>
          )}
        </>
      )}
    </div>
  );
};

export default LeaderboardPage;
