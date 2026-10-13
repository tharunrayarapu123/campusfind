import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { AuthProvider } from './features/auth/AuthContext';
import { ProtectedRoute, PublicOnlyRoute } from './components/layout/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorBoundary } from './components/layout/ErrorBoundary';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { PostItemPage } from './pages/PostItemPage';
import { ItemDetailPage } from './pages/ItemDetailPage';
import { NotFoundPage } from './pages/NotFoundPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <Toaster position="top-right" richColors closeButton />
            <Routes>
              {/* App Layout wrapping standard pages */}
              <Route element={<AppLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/items/:id" element={<ItemDetailPage />} />
                <Route
                  path="/post"
                  element={
                    <ProtectedRoute>
                      <PostItemPage />
                    </ProtectedRoute>
                  }
                />
                {/* Placeholders for Step 5B pages */}
                <Route
                  path="/matches/:id"
                  element={
                    <ProtectedRoute>
                      <div className="max-w-xl mx-auto py-16 px-4 text-center">
                        <h2 className="text-xl font-bold mb-2">Claim Verification & Chat</h2>
                        <p className="text-sm text-slate-500 mb-6">
                          Match resolution and realtime chat will be connected in Step 5B.
                        </p>
                      </div>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/matches"
                  element={
                    <ProtectedRoute>
                      <div className="max-w-xl mx-auto py-16 px-4 text-center">
                        <h2 className="text-xl font-bold mb-2">My Matches</h2>
                        <p className="text-sm text-slate-500 mb-6">
                          Matches list will be built in Step 5B.
                        </p>
                      </div>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/notifications"
                  element={
                    <ProtectedRoute>
                      <div className="max-w-xl mx-auto py-16 px-4 text-center">
                        <h2 className="text-xl font-bold mb-2">Notifications</h2>
                        <p className="text-sm text-slate-500 mb-6">
                          Alerts center will be built in Step 5B.
                        </p>
                      </div>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/profile"
                  element={
                    <ProtectedRoute>
                      <div className="max-w-xl mx-auto py-16 px-4 text-center">
                        <h2 className="text-xl font-bold mb-2">Student Profile</h2>
                        <p className="text-sm text-slate-500 mb-6">
                          Profile settings and karma achievements will be built in Step 5B.
                        </p>
                      </div>
                    </ProtectedRoute>
                  }
                />
              </Route>

              {/* Public-only Auth Routes */}
              <Route
                path="/login"
                element={
                  <PublicOnlyRoute>
                    <LoginPage />
                  </PublicOnlyRoute>
                }
              />
              <Route
                path="/register"
                element={
                  <PublicOnlyRoute>
                    <RegisterPage />
                  </PublicOnlyRoute>
                }
              />

              {/* 404 Catch All */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

export default App;
