import React from 'react';
import { Link } from 'react-router-dom';
import { Compass, ArrowLeft } from 'lucide-react';
import { Button } from '../components/ui/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
      <div className="w-16 h-16 rounded-2xl bg-brand-50 dark:bg-brand-950/60 text-brand-600 dark:text-brand-400 flex items-center justify-center mb-6 shadow-soft">
        <Compass className="w-8 h-8 animate-pulse" />
      </div>
      <h1 className="text-4xl font-extrabold text-slate-900 dark:text-white mb-2 tracking-tight">404</h1>
      <h2 className="text-xl font-semibold text-slate-800 dark:text-slate-200 mb-3">Page not found</h2>
      <p className="text-sm text-slate-500 dark:text-slate-400 max-w-md mb-8">
        The item or page you are looking for may have been moved, resolved, or does not exist.
      </p>
      <Link to="/">
        <Button variant="primary" className="inline-flex items-center gap-2">
          <ArrowLeft className="w-4 h-4" /> Back to Home Feed
        </Button>
      </Link>
    </div>
  );
};
