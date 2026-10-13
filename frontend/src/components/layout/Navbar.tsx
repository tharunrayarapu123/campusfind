import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Compass,
  Moon,
  Sun,
  Bell,
  PlusCircle,
  LogOut,
  MapPin,
  Trophy,
  CheckCircle2,
  ShieldAlert,
} from 'lucide-react';
import { useAuth } from '../../features/auth/AuthContext';
import { useDarkMode } from '../../hooks/useDarkMode';
import { useUnreadNotificationsCount } from '../../hooks/useUnreadNotificationsCount';
import { Button } from '../ui/Button';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout, role } = useAuth();
  const { theme, toggleTheme } = useDarkMode();
  const { data: unreadCount = 0 } = useUnreadNotificationsCount();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const firstName = user?.fullName ? user.fullName.split(' ')[0] : 'Student';

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200/80 dark:border-slate-800 bg-white/85 dark:bg-slate-900/85 backdrop-blur-md transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-10 h-10 rounded-xl bg-brand-600 text-white flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform">
            <Compass className="w-5 h-5" />
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-lg leading-tight tracking-tight text-slate-900 dark:text-white">
              Campus<span className="text-brand-600 dark:text-brand-400">Find</span>
            </span>
            <span className="text-[10px] font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider">
              Lost & Found
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-6">
          <Link
            to="/"
            className="text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
          >
            Home Feed
          </Link>
          <Link
            to="/map"
            className="inline-flex items-center gap-1 text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
          >
            <MapPin className="w-4 h-4 text-rose-500" />
            Campus Map
          </Link>
          <Link
            to="/leaderboard"
            className="inline-flex items-center gap-1 text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
          >
            <Trophy className="w-4 h-4 text-amber-500" />
            Leaderboard
          </Link>

          {isAuthenticated && (
            <>
              <Link
                to="/matches"
                className="inline-flex items-center gap-1 text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
              >
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                Matches
              </Link>
              <Link
                to="/post"
                className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-600 dark:text-slate-300 hover:text-brand-600 dark:hover:text-brand-400 transition-colors"
              >
                <PlusCircle className="w-4 h-4 text-brand-600" />
                Post an Item
              </Link>
            </>
          )}

          {role === 'ADMIN' && (
            <Link
              to="/admin"
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-rose-50 dark:bg-rose-950/60 text-xs font-bold text-rose-700 dark:text-rose-300 hover:bg-rose-100 dark:hover:bg-rose-900 transition-colors"
            >
              <ShieldAlert className="w-3.5 h-3.5" />
              Admin
            </Link>
          )}
        </nav>

        {/* Right Actions: Theme, Notifications, User */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Dark Mode Toggle */}
          <button
            onClick={toggleTheme}
            aria-label="Toggle dark mode"
            className="p-2.5 rounded-full text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-touch min-w-touch flex items-center justify-center"
          >
            {theme === 'dark' ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5" />}
          </button>

          {/* Notifications Bell (Logged in) */}
          {isAuthenticated && (
            <Link
              to="/notifications"
              aria-label="Notifications"
              className="relative p-2.5 rounded-full text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-touch min-w-touch flex items-center justify-center"
            >
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>
          )}

          {/* Auth State */}
          {isAuthenticated ? (
            <div className="flex items-center gap-2.5 pl-2 sm:pl-3 border-l border-slate-200 dark:border-slate-800">
              <Link to="/profile" className="flex items-center gap-2.5 group">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-sm font-semibold text-slate-900 dark:text-white group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors leading-tight">
                    {firstName}
                  </span>
                  <span className="text-xs text-brand-600 dark:text-brand-400 font-medium">
                    {user?.karmaPoints ?? 0} pts
                  </span>
                </div>
                <div className="w-9 h-9 rounded-full bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-bold flex items-center justify-center text-sm border border-brand-200 dark:border-brand-800 shrink-0 group-hover:scale-105 transition-transform overflow-hidden">
                  {user?.avatarUrl ? (
                    <img src={user.avatarUrl} alt={firstName} className="w-full h-full object-cover" />
                  ) : (
                    firstName.charAt(0).toUpperCase()
                  )}
                </div>
              </Link>
              <button
                onClick={handleLogout}
                title="Log out"
                aria-label="Log out"
                className="hidden md:flex p-2 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 transition-colors"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Log in
                </Button>
              </Link>
              <Link to="/register" className="hidden sm:block">
                <Button variant="primary" size="sm">
                  Register
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
