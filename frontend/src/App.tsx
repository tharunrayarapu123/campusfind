import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import { AuthProvider } from './features/auth/AuthContext';
import { ProtectedRoute, AdminRoute, PublicOnlyRoute } from './components/layout/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorBoundary } from './components/layout/ErrorBoundary';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { PostItemPage } from './pages/PostItemPage';
import { ItemDetailPage } from './pages/ItemDetailPage';
import { MatchesPage } from './pages/MatchesPage';
import { MatchDetailPage } from './pages/MatchDetailPage';
import { ChatPage } from './pages/ChatPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { ProfilePage } from './pages/ProfilePage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { MapPage } from './pages/MapPage';
import { AdminPage } from './pages/AdminPage';
import { NotFoundPage } from './pages/NotFoundPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Dynamic Page Title Updater based on active route
const RouteTitleObserver: React.FC = () => {
  const location = useLocation();

  useEffect(() => {
    const path = location.pathname;
    let title = 'CampusFind - College Lost & Found';

    if (path === '/') title = 'CampusFind - Campus Lost & Found Feed';
    else if (path.startsWith('/items/')) title = 'Item Details - CampusFind';
    else if (path === '/post') title = 'Post an Item - CampusFind';
    else if (path === '/matches') title = 'My Matches - CampusFind';
    else if (path.startsWith('/matches/')) title = 'Match Details & Verification - CampusFind';
    else if (path.startsWith('/chat/')) title = 'Realtime Match Chat - CampusFind';
    else if (path === '/notifications') title = 'Notifications - CampusFind';
    else if (path === '/profile') title = 'My Profile & Karma - CampusFind';
    else if (path === '/leaderboard') title = 'Campus Karma Leaderboard - CampusFind';
    else if (path === '/map') title = 'Campus Map - CampusFind';
    else if (path === '/admin') title = 'Admin Console - CampusFind';
    else if (path === '/login') title = 'Log In - CampusFind';
    else if (path === '/register') title = 'Create Account - CampusFind';

    document.title = title;
  }, [location]);

  return null;
};

export const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <RouteTitleObserver />
            <Toaster position="top-right" richColors closeButton />
            <Routes>
              {/* App Layout wrapping student-accessible pages */}
              <Route element={<AppLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/items/:id" element={<ItemDetailPage />} />
                <Route path="/leaderboard" element={<LeaderboardPage />} />
                <Route path="/map" element={<MapPage />} />

                {/* Protected Student Routes */}
                <Route
                  path="/post"
                  element={
                    <ProtectedRoute>
                      <PostItemPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/matches"
                  element={
                    <ProtectedRoute>
                      <MatchesPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/matches/:id"
                  element={
                    <ProtectedRoute>
                      <MatchDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/chat/:matchId"
                  element={
                    <ProtectedRoute>
                      <ChatPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/notifications"
                  element={
                    <ProtectedRoute>
                      <NotificationsPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/profile"
                  element={
                    <ProtectedRoute>
                      <ProfilePage />
                    </ProtectedRoute>
                  }
                />

                {/* Protected Admin Route */}
                <Route
                  path="/admin"
                  element={
                    <AdminRoute>
                      <AdminPage />
                    </AdminRoute>
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
