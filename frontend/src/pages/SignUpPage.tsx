import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthProvider';
import { Field } from '../components/Field';

export function SignUpPage() {
  const { user, signUp } = useAuth();
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      await signUp({ username, email, password, displayName });
    } catch (cause) {
      if (cause instanceof ApiError) {
        // The API reports every invalid field at once, and names the field on a conflict;
        // both shapes land in fieldErrors so they render next to the right input.
        setFieldErrors(cause.fieldErrors);
        if (Object.keys(cause.fieldErrors).length === 0) {
          setError(cause.message);
        }
      } else {
        setError('Something went wrong. Try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-full w-full max-w-sm flex-col justify-center px-4 py-12">
      <h1 className="text-3xl font-bold">Join Threadly</h1>

      <form className="mt-8 space-y-4" onSubmit={onSubmit}>
        <Field
          label="Handle"
          value={username}
          onChange={setUsername}
          autoComplete="username"
          error={fieldErrors.username}
          hint="Letters, digits and underscores. This is your URL: /yourhandle"
          required
        />
        <Field
          label="Display name"
          value={displayName}
          onChange={setDisplayName}
          autoComplete="name"
          error={fieldErrors.displayName}
          required
        />
        <Field
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="email"
          error={fieldErrors.email}
          required
        />
        <Field
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          error={fieldErrors.password}
          hint="At least 8 characters."
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
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="mt-6 text-sm text-ink-muted">
        Already here?{' '}
        <Link to="/sign-in" className="text-brand hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
