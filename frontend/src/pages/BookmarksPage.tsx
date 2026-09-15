import { useCursorPages } from '../api/posts';
import { PostList } from '../components/PostList';

export function BookmarksPage() {
  const query = useCursorPages(['posts', 'bookmarks'], '/api/v1/me/bookmarks');

  return (
    <div>
      <header className="sticky top-0 z-10 border-b border-line bg-canvas/80 px-4 py-4 backdrop-blur">
        <h1 className="text-xl font-bold">Bookmarks</h1>
        <p className="text-sm text-ink-muted">Only you can see these.</p>
      </header>

      <PostList query={query} emptyMessage="Saved posts will appear here." />
    </div>
  );
}
