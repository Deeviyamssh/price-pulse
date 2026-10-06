import { FormEvent, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      await login(email, password);
      const from = (location.state as { from?: string } | null)?.from ?? '/';
      navigate(from, { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 429) {
        setError('Too many failed attempts. Please try again later.');
      } else if (requestError instanceof ApiError) {
        // Login failed due to invalid credentials
        setError('Invalid email or password.');
      } else {
        setError(requestError instanceof Error ? requestError.message : 'Unable to log in.');
      }
    }
  }

  return (
    <main>
      <h1>PricePulse</h1>
      <h2>Log in</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Email
          <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        </label>
        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={isLoading}>{isLoading ? 'Logging in…' : 'Log in'}</button>
      </form>
      <p>New to PricePulse? <Link to="/register">Create an account</Link></p>
    </main>
  );
}