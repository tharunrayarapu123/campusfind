import React from 'react';
import { Navbar } from './components/Navbar';
import { HomePage } from './pages/HomePage';

function App() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-900 text-slate-800 dark:text-slate-100">
      <Navbar />
      <main className="flex-1">
        <HomePage />
      </main>
      <footer className="border-t border-slate-200 dark:border-slate-800 py-6 text-center text-xs text-slate-500">
        CampusFind &copy; {new Date().getFullYear()} — College Lost & Found Platform
      </footer>
    </div>
  );
}

export default App;
