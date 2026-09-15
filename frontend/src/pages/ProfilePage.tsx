import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useCursorPages } from '../api/posts';
import type { Profile } from '../api/types';
import { useAuth } from '../auth/AuthProvider';
import { Avatar } from '../components/Avatar';
import { PostList } from '../components/PostList';

export function ProfilePage() {
  const { username = '' } = useParams();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const profile = useQuery({
    queryKey: ['profile', username.toLowerCase()],
    queryFn: () => api.get<Profile>(`/api/v1/users/${encodeURIComponent(username)}`),
    retry: false,
  });

  const timeline = useCursorPages(
    ['posts', 'timeline', username.toLowerCase()],
    `/api/v1/users/${encodeURIComponent(username)}/posts`,
    // Asking for a timeline the profile could not load only produces a second 404.
    profile.isSuccess,
  );

  const follow = useMutation({
    mutationFn: (next: boolean) =>
      next
        ? api.post<void>(`/api/v1/users/${encodeURIComponent(username)}/follow`)
        : api.delete<void>(`/api/v1/users/${encodeURIComponent(username)}/follow`),
    onSuccess: async () => {
      // Following changes the follower count and the Following feed, not just the button.
      await queryClient.invalidateQueries({ queryKey: ['profile', username.toLowerCase()] });
      await queryClient.invalidateQueries({ queryKey: ['posts', 'feed', 'following'] });
    },
  });

  if (profile.isPending) {
    return <p className="px-4 py-8 text-center text-ink-muted">Loading…</p>;
  }

  if (profile.isError) {
    const notFound = profile.error instanceof ApiError && profile.error.status === 404;
    return (
      <div className="px-4 py-12 text-center">
        <p className="text-lg font-semibold">
          {notFound ? 'This account does not exist' : 'Could not load this profile'}
        </p>
        <Link to="/" className="mt-2 inline-block text-brand hover:underline">
          Back home
        </Link>
      </div>
    );
  }

  const data = profile.data;
  const isMe = user?.id === data.id;

  return (
    <div>
      <div className="h-32 bg-surface-hover">
        {data.bannerUrl && (
          <img src={data.bannerUrl} alt="" className="size-full object-cover" />
        )}
      </div>

      <header className="border-b border-line px-4 pb-4">
        <div className="-mt-10 flex items-end justify-between">
          <Avatar user={data} size="lg" />

          {!isMe && data.relationship && (
            <button
              type="button"
              onClick={() => follow.mutate(!data.relationship?.following)}
              disabled={follow.isPending}
              className={`rounded-full px-4 py-1.5 font-semibold transition disabled:opacity-50 ${
                data.relationship.following
                  ? 'border border-line text-ink hover:border-like hover:text-like'
                  : 'bg-brand text-canvas hover:bg-brand-strong'
              }`}
            >
              {data.relationship.following ? 'Following' : 'Follow'}
            </button>
          )}
        </div>

        <h1 className="mt-3 text-xl font-bold">{data.displayName}</h1>
        <p className="text-ink-muted">
          @{data.username}
          {/* The backend reports this as followedBy; X calls it "follows you". */}
          {data.relationship?.followedBy && (
            <span className="ml-2 rounded bg-surface-hover px-1.5 py-0.5 text-xs">
              Follows you
            </span>
          )}
        </p>

        {data.bio && <p className="mt-3 whitespace-pre-wrap">{data.bio}</p>}

        <div className="mt-2 flex flex-wrap gap-x-4 text-sm text-ink-muted">
          {data.location && <span>{data.location}</span>}
          {data.website && (
            <a
              href={data.website}
              target="_blank"
              // noreferrer also covers noopener; both keep the target page away from this one.
              rel="noreferrer"
              className="text-brand hover:underline"
            >
              {data.website.replace(/^https?:\/\//, '')}
            </a>
          )}
          <span>Joined {new Date(data.joinedAt).toLocaleDateString()}</span>
        </div>

        <div className="mt-3 flex gap-4 text-sm">
          <span>
            <strong>{data.stats.following}</strong>{' '}
            <span className="text-ink-muted">Following</span>
          </span>
          <span>
            <strong>{data.stats.followers}</strong>{' '}
            <span className="text-ink-muted">Followers</span>
          </span>
          <span>
            <strong>{data.stats.posts}</strong> <span className="text-ink-muted">Posts</span>
          </span>
        </div>
      </header>

      <PostList query={timeline} emptyMessage="No posts yet." />
    </div>
  );
}
