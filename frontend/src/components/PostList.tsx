import type { UseInfiniteQueryResult } from '@tanstack/react-query';
import type { CursorPage, Post } from '../api/types';
import { PostCard } from './PostCard';

interface PostListProps {
  query: UseInfiniteQueryResult<{ pages: CursorPage<Post>[] }, Error>;
  emptyMessage: string;
}

/** Renders a cursor-paginated list with an explicit "load more", not an infinite scroller:
 *  a button keeps the footer reachable and the behaviour predictable. */
export function PostList({ query, emptyMessage }: PostListProps) {
  if (query.isPending) {
    return <p className="px-4 py-8 text-center text-ink-muted">Loading…</p>;
  }
  if (query.isError) {
    return <p className="px-4 py-8 text-center text-like">Could not load posts.</p>;
  }

  const posts = query.data.pages.flatMap((page) => page.items);
  if (posts.length === 0) {
    return <p className="px-4 py-8 text-center text-ink-muted">{emptyMessage}</p>;
  }

  return (
    <div>
      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
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
