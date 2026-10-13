import AuthPage from './auth/AuthPage';
import { useAuth } from './auth/AuthProvider';
import TaskManager from './TaskManager';

export default function App() {
  const { user } = useAuth();

  if (user === undefined) {
    return <p className="muted centered">Loading…</p>;
  }
  // Keyed by user so nothing from a previous account survives a sign-out / sign-in.
  return user ? <TaskManager key={user.id} /> : <AuthPage />;
}
