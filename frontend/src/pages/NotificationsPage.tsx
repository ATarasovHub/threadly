import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import type { CursorPage, Notification, NotificationType } from '../api/types';
import { Avatar } from '../components/Avatar';
import { relativeTime } from '../components/relativeTime';

const WORDING: Record<NotificationType, string> = {
  FOLLOW: 'followed you',
  LIKE: 'liked your post',
  REPLY: 'replied to your post',
  REPOST: 'reposted your post',
  QUOTE: 'quoted your post',
};

export function NotificationsPage() {
  const queryClient = useQueryClient();

  const query = useInfiniteQuery({
    queryKey: ['notifications', 'inbox'],
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }) =>
      api.get<CursorPage<Notification>>(
        `/api/v1/notifications${pageParam ? `?cursor=${encodeURIComponent(pageParam)}` : ''}`,
      ),
    getNextPageParam: (lastPage) => lastPage.nextCursor,
  });

  const markRead = useMutation({
    mutationFn: () => api.post<{ marked: number }>('/api/v1/notifications/read'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  // Opening the inbox is what "reading" means here, so clear the badge on arrival rather than
  // making the user press a button to dismiss something they have already seen.
  const hasUnread = query.data?.pages.some((page) => page.items.some((item) => !item.read));
  useEffect(() => {
    if (hasUnread && !markRead.isPending) {
      markRead.mutate();
    }
    // Deliberately keyed on hasUnread alone: re-running on every mutation state change would loop.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasUnread]);

  return (
    <div>
      <header className="sticky top-0 z-10 border-b border-line bg-canvas/80 px-4 py-4 backdrop-blur">
        <h1 className="text-xl font-bold">Notifications</h1>
      </header>

      {query.isPending && <p className="px-4 py-8 text-center text-ink-muted">Loading…</p>}
      {query.isError && (
        <p className="px-4 py-8 text-center text-like">Could not load notifications.</p>
      )}

      {query.data?.pages.flatMap((page) => page.items).length === 0 && (
        <p className="px-4 py-8 text-center text-ink-muted">Nothing yet.</p>
      )}

      {query.data?.pages
        .flatMap((page) => page.items)
        .map((item) => (
          <article
            key={item.id}
            className={`flex gap-3 border-b border-line px-4 py-3 ${
              item.read ? '' : 'bg-surface/50'
            }`}
          >
            <Avatar user={item.actor} size="sm" />
            <div className="min-w-0 flex-1 text-sm">
              <p>
                <Link to={`/${item.actor.username}`} className="font-semibold hover:underline">
                  {item.actor.displayName}
                </Link>{' '}
                <span className="text-ink-muted">{WORDING[item.type]}</span>{' '}
                <time dateTime={item.createdAt} className="text-ink-muted">
                  · {relativeTime(item.createdAt)}
                </time>
              </p>
              {item.post && (
                <Link
                  to={`/posts/${item.post.id}`}
                  className="mt-1 block truncate text-ink-muted hover:underline"
                >
                  {item.post.excerpt}
                </Link>
              )}
            </div>
          </article>
        ))}

      {query.hasNextPage && (
        <button
          type="button"
          onClick={() => void query.fetchNextPage()}
          disabled={query.isFetchingNextPage}
          className="w-full px-4 py-4 text-brand transition hover:bg-surface-hover disabled:opacity-50"
        >
          {query.isFetchingNextPage ? 'Loading…' : 'Show more'}
        </button>
      )}
    </div>
  );
}
