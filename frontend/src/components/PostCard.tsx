import { Link, useNavigate } from 'react-router-dom';
import { useBookmark, useDeletePost, useLike, useRepost } from '../api/posts';
import type { Post } from '../api/types';
import { useAuth } from '../auth/AuthProvider';
import { Avatar } from './Avatar';
import { relativeTime } from './relativeTime';

interface PostCardProps {
  post: Post;
  /** Suppresses the click-through and shows the post at full size. */
  standalone?: boolean;
}

export function PostCard({ post, standalone = false }: PostCardProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const like = useLike();
  const repost = useRepost();
  const bookmark = useBookmark();
  const remove = useDeletePost();

  const isMine = user?.id === post.author.id;

  return (
    <article
      className={`border-b border-line px-4 py-3 transition ${
        standalone ? '' : 'cursor-pointer hover:bg-surface/60'
      }`}
      onClick={standalone ? undefined : () => navigate(`/posts/${post.id}`)}
    >
      {post.inReplyTo && (
        <p className="mb-1 pl-13 text-sm text-ink-muted">
          Replying to @{post.inReplyTo.authorUsername}
        </p>
      )}

      <div className="flex gap-3">
        <Avatar user={post.author} />

        <div className="min-w-0 flex-1">
          <header className="flex flex-wrap items-baseline gap-x-2 text-sm">
            <Link
              to={`/${post.author.username}`}
              onClick={(event) => event.stopPropagation()}
              className="font-semibold hover:underline"
            >
              {post.author.displayName}
            </Link>
            <span className="text-ink-muted">@{post.author.username}</span>
            <span className="text-ink-muted">·</span>
            <time dateTime={post.createdAt} className="text-ink-muted">
              {relativeTime(post.createdAt)}
            </time>
            {post.edited && <span className="text-ink-muted">· edited</span>}
          </header>

          {post.content && (
            <p className="mt-1 whitespace-pre-wrap break-words">{post.content}</p>
          )}

          {/* A plain repost has no words of its own, so the quoted post is the whole content. */}
          {post.repostOf && (
            <Link
              to={`/posts/${post.repostOf.id}`}
              onClick={(event) => event.stopPropagation()}
              className="mt-2 block rounded-xl border border-line p-3 transition hover:bg-surface-hover"
            >
              <p className="text-sm">
                <span className="font-semibold">{post.repostOf.author.displayName}</span>{' '}
                <span className="text-ink-muted">@{post.repostOf.author.username}</span>
              </p>
              <p className="mt-1 whitespace-pre-wrap break-words text-sm">
                {post.repostOf.content}
              </p>
            </Link>
          )}

          <footer className="mt-3 flex max-w-md items-center justify-between text-sm text-ink-muted">
            <Action
              label="Reply"
              count={post.metrics.replies}
              onClick={() => navigate(`/posts/${post.id}`)}
              symbol="↩"
            />
            <Action
              label={post.viewer.reposted ? 'Undo repost' : 'Repost'}
              count={post.metrics.reposts}
              active={post.viewer.reposted}
              activeClass="text-repost"
              symbol="⇄"
              onClick={() => repost.mutate({ postId: post.id, next: !post.viewer.reposted })}
            />
            <Action
              label={post.viewer.liked ? 'Unlike' : 'Like'}
              count={post.metrics.likes}
              active={post.viewer.liked}
              activeClass="text-like"
              symbol={post.viewer.liked ? '♥' : '♡'}
              onClick={() => like.mutate({ postId: post.id, next: !post.viewer.liked })}
            />
            <Action
              label={post.viewer.bookmarked ? 'Remove bookmark' : 'Bookmark'}
              active={post.viewer.bookmarked}
              activeClass="text-brand"
              symbol={post.viewer.bookmarked ? '★' : '☆'}
              onClick={() => bookmark.mutate({ postId: post.id, next: !post.viewer.bookmarked })}
            />
            {isMine && (
              <Action
                label="Delete"
                symbol="🗑"
                onClick={() => remove.mutate(post.id)}
                activeClass="text-like"
              />
            )}
          </footer>
        </div>
      </div>
    </article>
  );
}

interface ActionProps {
  label: string;
  symbol: string;
  count?: number;
  active?: boolean;
  activeClass?: string;
  onClick: () => void;
}

function Action({ label, symbol, count, active, activeClass, onClick }: ActionProps) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={(event) => {
        // The whole card is clickable; an action must not also open the thread.
        event.stopPropagation();
        onClick();
      }}
      className={`flex items-center gap-1.5 rounded-full px-2 py-1 transition hover:bg-surface-hover ${
        active && activeClass ? activeClass : ''
      }`}
    >
      <span aria-hidden>{symbol}</span>
      {count !== undefined && count > 0 && <span>{count}</span>}
    </button>
  );
}
