import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthProvider';
import { GoogleButton, googleSignInAvailable } from '../auth/GoogleButton';
import { Field } from '../components/Field';

export function SignInPage() {
  const { user, signIn, signInWithGoogle } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onGoogleCredential(idToken: string) {
    setError(null);
    try {
      await signInWithGoogle(idToken);
    } catch {
      setError('Could not sign in with Google.');
    }
  }

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signIn(identifier, password);
    } catch (cause) {
      // The API answers every failed sign-in identically, on purpose; show what it said.
      setError(cause instanceof ApiError ? cause.message : 'Something went wrong. Try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-full w-full max-w-sm flex-col justify-center px-4 py-12">
      <h1 className="text-3xl font-bold">Sign in to Threadly</h1>
      <p className="mt-2 text-sm text-ink-muted">Use your handle or email address.</p>

      <form className="mt-8 space-y-4" onSubmit={onSubmit}>
        <Field
          label="Handle or email"
          value={identifier}
          onChange={setIdentifier}
          autoComplete="username"
          required
        />
        <Field
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
          required
        />

        {error && (
          <p role="alert" className="rounded-lg bg-like/10 px-3 py-2 text-sm text-like">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-full bg-brand px-4 py-2.5 font-semibold text-canvas transition hover:bg-brand-strong disabled:opacity-50"
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>


      {googleSignInAvailable && (
        <>
          <div className="my-6 flex items-center gap-3 text-sm text-ink-muted">
            <span className="h-px flex-1 bg-line" />
            or
            <span className="h-px flex-1 bg-line" />
          </div>

          <GoogleButton onCredential={onGoogleCredential} />
        </>
      )}

      <p className="mt-6 text-sm text-ink-muted">
        No account?{' '}
        <Link to="/sign-up" className="text-brand hover:underline">
          Create one
        </Link>
      </p>
    </div>
  );
}
