import { useState } from 'react';
import { useCursorPages } from '../api/posts';
import { Composer } from '../components/Composer';
import { PostList } from '../components/PostList';

type Tab = 'for-you' | 'following';

export function HomePage() {
  const [tab, setTab] = useState<Tab>('for-you');

  // Both feeds stay cached under their own key, so switching tabs back and forth is instant.
  const query = useCursorPages(['posts', 'feed', tab], `/api/v1/feed/${tab}`);

  return (
    <div>
      <div className="sticky top-0 z-10 flex border-b border-line bg-canvas/80 backdrop-blur">
        <TabButton label="For you" active={tab === 'for-you'} onClick={() => setTab('for-you')} />
        <TabButton
          label="Following"
          active={tab === 'following'}
          onClick={() => setTab('following')}
        />
      </div>

      <Composer />

      <PostList
        query={query}
        emptyMessage={
          tab === 'following'
            ? 'Follow some accounts and their posts will show up here.'
            : 'Nothing here yet. Be the first to post.'
        }
      />
    </div>
  );
}

function TabButton({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? 'page' : undefined}
      className="flex-1 py-4 transition hover:bg-surface-hover"
    >
      <span
        className={`pb-3 ${
          active ? 'border-b-4 border-brand font-semibold text-ink' : 'text-ink-muted'
        }`}
      >
        {label}
      </span>
    </button>
  );
}
