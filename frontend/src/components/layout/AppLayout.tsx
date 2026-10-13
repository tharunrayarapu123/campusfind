import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import { BottomTabBar } from './BottomTabBar';
import { OfflineBanner } from './OfflineBanner';

export const AppLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
      <Navbar />
      <OfflineBanner />
      <main className="flex-1 pb-20 md:pb-10">
        <Outlet />
      </main>
      <BottomTabBar />
    </div>
  );
};
