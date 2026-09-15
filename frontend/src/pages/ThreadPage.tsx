import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useCursorPages } from '../api/posts';
import type { Post } from '../api/types';
import { Composer } from '../components/Composer';
import { PostCard } from '../components/PostCard';
import { PostList } from '../components/PostList';

export function ThreadPage() {
  const { postId = '' } = useParams();

  const post = useQuery({
    queryKey: ['post', postId],
    queryFn: () => api.get<Post>(`/api/v1/posts/${postId}`),
    retry: false,
  });

  // Replies come back oldest first — a conversation is read from its start.
  const replies = useCursorPages(
    ['posts', 'replies', postId],
    `/api/v1/posts/${postId}/replies`,
    post.isSuccess,
  );

  if (post.isPending) {
    return <p className="px-4 py-8 text-center text-ink-muted">Loading…</p>;
  }

  if (post.isError) {
    const notFound = post.error instanceof ApiError && post.error.status === 404;
    return (
      <div className="px-4 py-12 text-center">
        <p className="text-lg font-semibold">
          {notFound ? 'This post is no longer available' : 'Could not load this post'}
        </p>
        <Link to="/" className="mt-2 inline-block text-brand hover:underline">
          Back home
        </Link>
      </div>
    );
  }

  return (
    <div>
      <header className="sticky top-0 z-10 border-b border-line bg-canvas/80 px-4 py-4 backdrop-blur">
        <h1 className="text-xl font-bold">Thread</h1>
      </header>

      <PostCard post={post.data} standalone />
      <Composer replyTo={post.data.id} placeholder="Post your reply" />
      <PostList query={replies} emptyMessage="No replies yet." />
    </div>
  );
}
