import { NavLink, Outlet } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthProvider';

const NAV_ITEMS = [
  { to: '/', label: 'Home', end: true },
  { to: '/notifications', label: 'Notifications', end: false },
  { to: '/bookmarks', label: 'Bookmarks', end: false },
  { to: '/settings', label: 'Settings', end: false },
] as const;

export function AppLayout() {
  const { user, signOut } = useAuth();

  const { data: unread } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => api.get<{ unread: number }>('/api/v1/notifications/unread-count'),
    // The badge is the one thing worth polling: nothing else tells the tab that something
    // happened while the user was reading.
    refetchInterval: 30_000,
  });

  return (
    <div className="mx-auto flex min-h-full w-full max-w-5xl gap-8 px-4">
      <aside className="sticky top-0 hidden h-screen w-56 shrink-0 flex-col py-6 sm:flex">
        <span className="px-3 text-xl font-bold">Threadly</span>

        <nav className="mt-6 flex flex-col gap-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center justify-between rounded-full px-4 py-2 transition hover:bg-surface-hover ${
                  isActive ? 'font-semibold text-ink' : 'text-ink-muted'
                }`
              }
            >
              {item.label}
              {item.to === '/notifications' && unread && unread.unread > 0 ? (
                <span className="ml-2 rounded-full bg-brand px-2 py-0.5 text-xs font-semibold text-canvas">
                  {unread.unread > 99 ? '99+' : unread.unread}
                </span>
              ) : null}
            </NavLink>
          ))}

          {user && (
            <NavLink
              to={`/${user.username}`}
              className={({ isActive }) =>
                `rounded-full px-4 py-2 transition hover:bg-surface-hover ${
                  isActive ? 'font-semibold text-ink' : 'text-ink-muted'
                }`
              }
            >
              Profile
            </NavLink>
          )}
        </nav>

        <div className="mt-auto px-3">
          <p className="truncate text-sm font-semibold">{user?.displayName}</p>
          <p className="truncate text-sm text-ink-muted">@{user?.username}</p>
          <button
            type="button"
            onClick={() => void signOut()}
            className="mt-2 text-sm text-ink-muted transition hover:text-like"
          >
            Sign out
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 border-line sm:border-x">
        <Outlet />
      </main>
    </div>
  );
}
