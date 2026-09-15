interface AvatarProps {
  user: { username: string; displayName: string; avatarUrl?: string | null };
  size?: 'sm' | 'md' | 'lg';
}

const SIZES = { sm: 'size-8 text-sm', md: 'size-10', lg: 'size-20 text-2xl' } as const;

/** Falls back to an initial: avatars are optional, and a broken image is worse than a letter. */
export function Avatar({ user, size = 'md' }: AvatarProps) {
  if (user.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt=""
        className={`${SIZES[size]} shrink-0 rounded-full object-cover`}
      />
    );
  }

  return (
    <div
      aria-hidden
      className={`${SIZES[size]} flex shrink-0 items-center justify-center rounded-full bg-surface-hover font-semibold text-ink-muted`}
    >
      {user.displayName.charAt(0).toUpperCase()}
    </div>
  );
}
