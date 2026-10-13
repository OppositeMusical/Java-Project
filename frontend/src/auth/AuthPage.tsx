import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from './AuthProvider';

type Mode = 'signIn' | 'register';

export default function AuthPage() {
  const { signIn, register } = useAuth();
  const [mode, setMode] = useState<Mode>('signIn');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const isRegister = mode === 'register';

  function switchMode(next: Mode) {
    setMode(next);
    setErrors({});
    setPassword('');
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setErrors({});
    try {
      if (isRegister) {
        await register({ email: email.trim(), password, displayName: displayName.trim() });
      } else {
        await signIn({ email: email.trim(), password });
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 429) {
        const minutes = Math.ceil((error.retryAfterSeconds ?? 60) / 60);
        setErrors({
          form: `Too many failed sign-in attempts. Try again in ${minutes} minute${minutes === 1 ? '' : 's'}.`,
        });
      } else if (error instanceof ApiError) {
        setErrors({ ...error.fieldErrors, form: error.message });
      } else {
        setErrors({ form: 'Could not reach the server. Is the backend running?' });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit} noValidate>
        <h1>{isRegister ? 'Create your account' : 'Sign in'}</h1>
        <p className="muted">Task Manager</p>

        {isRegister && (
          <div className="field">
            <label htmlFor="displayName">Name</label>
            <input
              id="displayName"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              autoComplete="name"
              maxLength={100}
              required
            />
            {errors.displayName && <span className="error">{errors.displayName}</span>}
          </div>
        )}

        <div className="field">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
          {errors.email && <span className="error">{errors.email}</span>}
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            minLength={isRegister ? 8 : undefined}
            maxLength={72}
            required
          />
          {isRegister && !errors.password && <span className="hint">At least 8 characters.</span>}
          {errors.password && <span className="error">{errors.password}</span>}
        </div>

        {errors.form && (
          <p className="error" role="alert">
            {errors.form}
          </p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}
        </button>

        <p className="switch-mode">
          {isRegister ? 'Already have an account?' : 'New here?'}{' '}
          <button type="button" className="link" onClick={() => switchMode(isRegister ? 'signIn' : 'register')}>
            {isRegister ? 'Sign in' : 'Create an account'}
          </button>
        </p>
      </form>
    </div>
  );
}
