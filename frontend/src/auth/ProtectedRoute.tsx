import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export function ProtectedRoute() {
  const { currentUser, isInitializing, initializationError, retrySession } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return <p>Checking your session…</p>;
  }

  if (initializationError) {
    return (
      <main>
        <h1>Session unavailable</h1>
        <p>{initializationError}</p>
        <button type="button" onClick={retrySession}>Retry</button>
      </main>
    );
  }

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
