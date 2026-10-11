import React, { useEffect, useState } from 'react';
import { checkBackendHealth } from '../services/api';
import { HealthCheckResponse } from '../types';
import {
  CheckCircle2,
  Database,
  Server,
  Layers,
  ArrowRight,
  ShieldCheck,
  Cpu,
  MessageSquare,
  Award,
  RefreshCw,
  Clock
} from 'lucide-react';

export const HomePage: React.FC = () => {
  const [health, setHealth] = useState<HealthCheckResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await checkBackendHealth();
      if (res.success && res.data) {
        setHealth(res.data);
      } else {
        setError(res.message || 'Failed to get healthy response');
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to connect to backend server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const steps = [
    { num: 1, title: 'Setup & Monorepo Architecture', status: 'In Progress', active: true },
    { num: 2, title: 'Auth & College Domain JWT', status: 'Upcoming', active: false },
    { num: 3, title: 'Lost & Found Postings + AI Auto-tag', status: 'Upcoming', active: false },
    { num: 4, title: 'Smart 100-Point Matching Engine', status: 'Upcoming', active: false },
    { num: 5, title: 'Real-time Chat & Claim Verification', status: 'Upcoming', active: false },
    { num: 6, title: 'Karma, Map Drop Points & Admin', status: 'Upcoming', active: false },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10">
      {/* Hero Banner */}
      <section className="bg-gradient-to-br from-indigo-900 via-indigo-800 to-slate-900 rounded-2xl p-8 sm:p-10 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-3xl space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-500/30 text-indigo-200 text-xs font-semibold backdrop-blur">
            <span className="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span>
            <span>CampusFind Foundation • Step 1 of 6</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
            Connecting Lost Items to Owners with Privacy-First Matching.
          </h1>
          <p className="text-indigo-200 text-sm sm:text-base leading-relaxed">
            Eliminating scattered group chats and public privacy risks. Designed specifically for college campuses
            with room-level spatial matching, smart AI tagging, and verified claim handoffs.
          </p>
        </div>
      </section>

      {/* Backend & MySQL Connectivity Card */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/70 text-indigo-600 dark:text-indigo-400">
                <Server className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm">Spring Boot 3 API</h3>
                <p className="text-xs text-slate-500">Java 21 Backend</p>
              </div>
            </div>
            <button
              onClick={fetchHealth}
              disabled={loading}
              title="Refresh Health"
              className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-500 transition-colors"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="pt-2 border-t border-slate-100 dark:border-slate-700/60">
            {loading ? (
              <p className="text-xs text-slate-400 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-ping"></span>
                Checking backend health...
              </p>
            ) : error ? (
              <div className="text-xs text-rose-500 bg-rose-50 dark:bg-rose-950/40 p-2.5 rounded-lg">
                Backend offline or starting up ({error})
              </div>
            ) : (
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">Status:</span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" /> {health?.status}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Active Profile:</span>
                  <span className="font-mono text-slate-700 dark:text-slate-300">{health?.activeProfile}</span>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-emerald-50 dark:bg-emerald-950/70 text-emerald-600 dark:text-emerald-400">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-semibold text-sm">MySQL 8 Database</h3>
              <p className="text-xs text-slate-500">Dockerized Container</p>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-100 dark:border-slate-700/60">
            {loading ? (
              <p className="text-xs text-slate-400">Checking database connection...</p>
            ) : error ? (
              <p className="text-xs text-slate-400">Database check pending backend restart</p>
            ) : (
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">Database:</span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                    {health?.databaseStatus.includes('CONNECTED') ? 'CONNECTED' : 'DISCONNECTED'}
                  </span>
                </div>
                <p className="text-[11px] font-mono text-slate-500 truncate" title={health?.databaseStatus}>
                  {health?.databaseStatus}
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-violet-50 dark:bg-violet-950/70 text-violet-600 dark:text-violet-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-semibold text-sm">Frontend Client</h3>
              <p className="text-xs text-slate-500">Vite + React + Tailwind</p>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-100 dark:border-slate-700/60 space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-slate-500">Vite Dev Server:</span>
              <span className="font-semibold text-emerald-600 dark:text-emerald-400">ONLINE</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">API Proxy Target:</span>
              <span className="font-mono text-slate-700 dark:text-slate-300">http://localhost:8080</span>
            </div>
          </div>
        </div>
      </section>

      {/* Six Step Implementation Roadmap */}
      <section className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-6 shadow-sm space-y-6">
        <div>
          <h2 className="text-lg font-bold">CampusFind Architecture Roadmap</h2>
          <p className="text-xs text-slate-500">Step-by-step full-stack engineering plan</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {steps.map((step) => (
            <div
              key={step.num}
              className={`p-4 rounded-xl border transition-all ${
                step.active
                  ? 'border-indigo-500 bg-indigo-50/40 dark:bg-indigo-950/30'
                  : 'border-slate-200 dark:border-slate-700 opacity-70'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                  Step {step.num}
                </span>
                <span className={`text-xs font-medium ${step.active ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400'}`}>
                  {step.status}
                </span>
              </div>
              <h4 className="font-medium text-sm text-slate-800 dark:text-slate-200">{step.title}</h4>
            </div>
          ))}
        </div>
      </section>

      {/* Feature Architecture Preview */}
      <section className="space-y-4">
        <h2 className="text-lg font-bold">Planned Capabilities</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-4 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2">
            <Cpu className="w-5 h-5 text-indigo-500" />
            <h4 className="font-semibold text-sm">Smart Match (100 pts)</h4>
            <p className="text-xs text-slate-500">Classroom block, room, category, timestamps & photo similarity score.</p>
          </div>
          <div className="p-4 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2">
            <MessageSquare className="w-5 h-5 text-emerald-500" />
            <h4 className="font-semibold text-sm">Private In-App Chat</h4>
            <p className="text-xs text-slate-500">Only matched students can talk. Contact numbers and emails stay confidential.</p>
          </div>
          <div className="p-4 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2">
            <ShieldCheck className="w-5 h-5 text-violet-500" />
            <h4 className="font-semibold text-sm">Claim Verification</h4>
            <p className="text-xs text-slate-500">Finder asks security question before handing over item to rightful owner.</p>
          </div>
          <div className="p-4 rounded-xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 space-y-2">
            <Award className="w-5 h-5 text-amber-500" />
            <h4 className="font-semibold text-sm">Karma & Leaderboard</h4>
            <p className="text-xs text-slate-500">Rewarded badges and reputation for honest finders returning items.</p>
          </div>
        </div>
      </section>
    </div>
  );
};
