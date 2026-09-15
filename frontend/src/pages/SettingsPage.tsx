import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import type { CurrentUser } from '../api/types';
import { useAuth } from '../auth/AuthProvider';
import { Field } from '../components/Field';

export function SettingsPage() {
  const { user, refreshCurrentUser } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const [username, setUsername] = useState(user?.username ?? '');
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setSubmitting(true);
    try {
      const updated = await api.patch<CurrentUser>('/api/v1/me/username', { username });
      await refreshCurrentUser();
      // Profiles are cached under the old handle; that cache is now about a URL nobody will use.
      queryClient.removeQueries({ queryKey: ['profile'] });
      setSaved(true);
      navigate(`/${updated.username}`);
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? (cause.fieldErrors.username ?? cause.message)
          : 'Something went wrong. Try again.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <header className="sticky top-0 z-10 border-b border-line bg-canvas/80 px-4 py-4 backdrop-blur">
        <h1 className="text-xl font-bold">Settings</h1>
      </header>

      <form onSubmit={onSubmit} className="max-w-md space-y-4 px-4 py-6">
        <Field
          label="Handle"
          value={username}
          onChange={setUsername}
          error={error ?? undefined}
          hint="This is your profile URL. Changing it breaks existing links to your profile."
          required
        />

        {saved && <p className="text-sm text-repost">Saved.</p>}

        <button
          type="submit"
          disabled={submitting || username === user?.username}
          className="rounded-full bg-brand px-4 py-2 font-semibold text-canvas transition hover:bg-brand-strong disabled:opacity-40"
        >
          {submitting ? 'Saving…' : 'Save'}
        </button>
      </form>
    </div>
  );
}
