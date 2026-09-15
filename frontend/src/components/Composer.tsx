import { useState } from 'react';
import { usePublish } from '../api/posts';
import { useAuth } from '../auth/AuthProvider';
import { Avatar } from './Avatar';

const MAX_LENGTH = 500;

interface ComposerProps {
  /** Set to publish a reply instead of a root post. */
  replyTo?: number;
  placeholder?: string;
}

export function Composer({ replyTo, placeholder = "What's happening?" }: ComposerProps) {
  const { user } = useAuth();
  const publish = usePublish();
  const [content, setContent] = useState('');

  const remaining = MAX_LENGTH - content.length;
  const canSubmit = content.trim().length > 0 && remaining >= 0 && !publish.isPending;

  function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }
    publish.mutate(
      { content, replyTo },
      // Clear only once the post exists, so a failure does not lose what was typed.
      { onSuccess: () => setContent('') },
    );
  }

  return (
    <form onSubmit={onSubmit} className="flex gap-3 border-b border-line px-4 py-3">
      {user && <Avatar user={user} />}

      <div className="min-w-0 flex-1">
        <textarea
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder={placeholder}
          rows={content.length > 80 ? 4 : 2}
          className="w-full resize-none bg-transparent text-lg outline-none placeholder:text-ink-muted"
        />

        <div className="flex items-center justify-end gap-3">
          <span className={remaining < 0 ? 'text-sm text-like' : 'text-sm text-ink-muted'}>
            {remaining}
          </span>
          <button
            type="submit"
            disabled={!canSubmit}
            className="rounded-full bg-brand px-4 py-1.5 font-semibold text-canvas transition hover:bg-brand-strong disabled:opacity-40"
          >
            {replyTo ? 'Reply' : 'Post'}
          </button>
        </div>

        {publish.isError && (
          <p role="alert" className="text-sm text-like">
            Could not publish. Try again.
          </p>
        )}
      </div>
    </form>
  );
}
