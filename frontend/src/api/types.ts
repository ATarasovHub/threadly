/** Mirrors the backend DTOs. Kept hand-written and small rather than generated from OpenAPI,
 *  so the client only carries the fields it actually renders. */

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}

export interface UserSummary {
  id: number;
  username: string;
  displayName: string;
  bio?: string | null;
  avatarUrl?: string | null;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface Profile {
  id: number;
  username: string;
  displayName: string;
  bio?: string | null;
  location?: string | null;
  website?: string | null;
  avatarUrl?: string | null;
  bannerUrl?: string | null;
  joinedAt: string;
  stats: { followers: number; following: number; posts: number };
  /** Absent on one's own profile. */
  relationship?: { following: boolean; followedBy: boolean } | null;
}

export interface PostAuthor {
  id: number;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
}

export interface Post {
  id: number;
  /** Null for a plain repost, which carries no text of its own. */
  content: string | null;
  author: PostAuthor;
  edited: boolean;
  createdAt: string;
  metrics: { likes: number; replies: number; reposts: number };
  viewer: { liked: boolean; bookmarked: boolean; reposted: boolean };
  inReplyTo?: { id: number; authorUsername: string } | null;
  repostOf?: { id: number; content: string | null; author: PostAuthor; createdAt: string } | null;
}

export type NotificationType = 'FOLLOW' | 'LIKE' | 'REPLY' | 'REPOST' | 'QUOTE';

export interface Notification {
  id: number;
  type: NotificationType;
  actor: { id: number; username: string; displayName: string; avatarUrl?: string | null };
  post?: { id: number; excerpt: string | null } | null;
  read: boolean;
  createdAt: string;
}

export interface AuthenticationResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

/** RFC 7807 problem detail, as returned by every backend error. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  field?: string;
  errors?: Record<string, string>;
}
