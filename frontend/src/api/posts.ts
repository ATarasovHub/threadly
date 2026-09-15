import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
  type InfiniteData,
  type QueryKey,
} from '@tanstack/react-query';
import { api } from './client';
import type { CursorPage, Post } from './types';

type PostPages = InfiniteData<CursorPage<Post>>;

/**
 * Every paginated list in the app has the same shape, so one hook serves feeds, timelines,
 * threads and bookmarks. The cursor comes straight from the previous page: the client never
 * constructs one.
 */
export function useCursorPages(key: QueryKey, path: string, enabled = true) {
  return useInfiniteQuery({
    queryKey: key,
    enabled,
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }) => {
      const separator = path.includes('?') ? '&' : '?';
      const cursor = pageParam ? `${separator}cursor=${encodeURIComponent(pageParam)}` : '';
      return api.get<CursorPage<Post>>(`${path}${cursor}`);
    },
    getNextPageParam: (lastPage) => lastPage.nextCursor,
  });
}

/** Applies a change to one post wherever it appears across every cached list. */
function patchPostEverywhere(
  queryClient: ReturnType<typeof useQueryClient>,
  postId: number,
  update: (post: Post) => Post,
): void {
  queryClient.setQueriesData<PostPages>({ queryKey: ['posts'] }, (data) => {
    if (!data) {
      return data;
    }
    return {
      ...data,
      pages: data.pages.map((page) => ({
        ...page,
        items: page.items.map((post) => (post.id === postId ? update(post) : post)),
      })),
    };
  });
}

interface ToggleOptions {
  postId: number;
  /** The state the button is moving to. */
  next: boolean;
}

/**
 * Builds a toggle mutation that updates the cache before the request is sent.
 *
 * A like has to feel instant, and the backend endpoints are idempotent, so applying the change
 * optimistically is safe: replaying it changes nothing. On failure the previous cache is put
 * back, which is why the snapshot is taken in onMutate.
 */
function useToggle(
  path: (postId: number) => string,
  apply: (post: Post, next: boolean) => Post,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ postId, next }: ToggleOptions) =>
      next ? api.post<void>(path(postId)) : api.delete<void>(path(postId)),

    onMutate: async ({ postId, next }) => {
      // Stop in-flight refetches from landing on top of the optimistic state.
      await queryClient.cancelQueries({ queryKey: ['posts'] });
      const snapshot = queryClient.getQueriesData<PostPages>({ queryKey: ['posts'] });
      patchPostEverywhere(queryClient, postId, (post) => apply(post, next));
      return { snapshot };
    },

    onError: (_error, _variables, context) => {
      context?.snapshot.forEach(([key, data]) => queryClient.setQueryData(key, data));
    },

    // Reconcile with the server once the dust settles: counts may have moved for other reasons.
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['posts'] }),
  });
}

export function useLike() {
  return useToggle(
    (postId) => `/api/v1/posts/${postId}/like`,
    (post, next) => ({
      ...post,
      viewer: { ...post.viewer, liked: next },
      metrics: { ...post.metrics, likes: post.metrics.likes + (next ? 1 : -1) },
    }),
  );
}

export function useRepost() {
  return useToggle(
    (postId) => `/api/v1/posts/${postId}/repost`,
    (post, next) => ({
      ...post,
      viewer: { ...post.viewer, reposted: next },
      metrics: { ...post.metrics, reposts: post.metrics.reposts + (next ? 1 : -1) },
    }),
  );
}

export function useBookmark() {
  return useToggle(
    (postId) => `/api/v1/posts/${postId}/bookmark`,
    (post, next) => ({ ...post, viewer: { ...post.viewer, bookmarked: next } }),
  );
}

export function usePublish() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ content, replyTo }: { content: string; replyTo?: number }) =>
      api.post<Post>(
        replyTo ? `/api/v1/posts/${replyTo}/replies` : '/api/v1/posts',
        { content },
      ),
    // A new post changes ordering and counts across several lists; refetching is simpler and
    // more honest than trying to splice it into each one.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['posts'] }),
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (postId: number) => api.delete<void>(`/api/v1/posts/${postId}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['posts'] }),
  });
}
