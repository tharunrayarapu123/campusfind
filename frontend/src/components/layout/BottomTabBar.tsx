import React from 'react';
import { NavLink } from 'react-router-dom';
import { Home, PlusCircle, CheckCircle2, Bell, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../features/auth/AuthContext';
import { useUnreadNotificationsCount } from '../../hooks/useUnreadNotificationsCount';
import { cn } from '../../lib/utils';

export const BottomTabBar: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const { data: unreadCount = 0 } = useUnreadNotificationsCount();

  const navItems = [
    { label: 'Home', path: '/', icon: Home },
    { label: 'Post', path: '/post', icon: PlusCircle, isPrimary: true },
    { label: 'Matches', path: '/matches', icon: CheckCircle2, requiresAuth: true },
    {
      label: 'Alerts',
      path: '/notifications',
      icon: Bell,
      badge: unreadCount > 0 ? unreadCount : undefined,
      requiresAuth: true,
    },
    { label: 'Profile', path: isAuthenticated ? '/profile' : '/login', icon: UserIcon },
  ];

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-lg border-t border-slate-200 dark:border-slate-800 pb-safe transition-colors">
      <div className="grid grid-cols-5 h-16 items-center px-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.label}
              to={item.path}
              className={({ isActive }) =>
                cn(
                  'flex flex-col items-center justify-center min-h-touch py-1 px-1 rounded-xl transition-all relative text-xs font-medium',
                  isActive
                    ? 'text-brand-600 dark:text-brand-400 font-semibold'
                    : 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200'
                )
              }
            >
              {({ isActive }) => (
                <>
                  <div className="relative">
                    <Icon
                      className={cn(
                        'w-5 h-5 transition-transform duration-150',
                        item.isPrimary && 'text-brand-600 dark:text-brand-400 w-6 h-6',
                        isActive && !item.isPrimary && 'scale-110'
                      )}
                    />
                    {item.badge !== undefined && item.badge > 0 && (
                      <span className="absolute -top-1 -right-2 min-w-[16px] h-4 px-1 rounded-full bg-rose-500 text-white text-[9px] font-bold flex items-center justify-center">
                        {item.badge > 9 ? '9+' : item.badge}
                      </span>
                    )}
                  </div>
                  <span className="text-[11px] mt-0.5 tracking-tight">{item.label}</span>
                </>
              )}
            </NavLink>
          );
        })}
      </div>
    </nav>
  );
};
