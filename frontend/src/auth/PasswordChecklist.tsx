import { PASSWORD_RULES } from './passwordRules';

/**
 * Shows the password requirements and ticks them off as they are met.
 *
 * Listed up front rather than revealed by rejection: a form that only says what is wrong after
 * you submit makes people guess. Nothing here blocks submission — the server decides.
 */
export function PasswordChecklist({ password }: { password: string }) {
  if (password.length === 0) {
    return (
      <ul className="mt-1 space-y-0.5 text-sm text-ink-muted">
        {PASSWORD_RULES.map((rule) => (
          <li key={rule.label}>· {rule.label}</li>
        ))}
      </ul>
    );
  }

  return (
    <ul className="mt-1 space-y-0.5 text-sm">
      {PASSWORD_RULES.map((rule) => {
        const met = rule.satisfied(password);
        return (
          <li key={rule.label} className={met ? 'text-repost' : 'text-ink-muted'}>
            <span aria-hidden>{met ? '✓' : '·'}</span>{' '}
            <span className={met ? 'line-through decoration-repost/50' : undefined}>
              {rule.label}
            </span>
          </li>
        );
      })}
    </ul>
  );
}
