import { useEffect, useRef, useState } from 'react';

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

/** Lets callers hide the surrounding "or" divider when there is no button to divide from. */
export const googleSignInAvailable = CLIENT_ID.length > 0;
const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';

/** The slice of Google Identity Services this component uses. */
interface GoogleIdentityServices {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string;
        callback: (response: { credential: string }) => void;
      }) => void;
      renderButton: (parent: HTMLElement, options: Record<string, string | number>) => void;
    };
  };
}

declare global {
  interface Window {
    google?: GoogleIdentityServices;
  }
}

function loadScript(): Promise<void> {
  // The page may already carry the script from a previous mount; loading it twice would render
  // two buttons into the same container.
  const existing = document.querySelector<HTMLScriptElement>(`script[src="${SCRIPT_SRC}"]`);
  if (existing) {
    return existing.dataset.loaded === 'true'
      ? Promise.resolve()
      : new Promise((resolve, reject) => {
          existing.addEventListener('load', () => resolve());
          existing.addEventListener('error', () => reject(new Error('Google script failed')));
        });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = SCRIPT_SRC;
    script.async = true;
    script.onload = () => {
      script.dataset.loaded = 'true';
      resolve();
    };
    script.onerror = () => reject(new Error('Google script failed'));
    document.head.appendChild(script);
  });
}

interface GoogleButtonProps {
  onCredential: (idToken: string) => void;
}

/**
 * Renders Google's own sign-in button.
 *
 * <p>The button hands back an ID token, which this app forwards to the backend rather than
 * trusting: nothing here reads the token's claims, because anything decoded in the browser is
 * decoration. The backend checks the signature, audience and issuer before it means anything.
 *
 * <p>Renders nothing when no client id is configured, so a checkout without Google credentials
 * still shows a working sign-in page instead of a broken button.
 */
export function GoogleButton({ onCredential }: GoogleButtonProps) {
  const container = useRef<HTMLDivElement>(null);
  const [failed, setFailed] = useState(false);
  // Keep the latest callback without re-initialising Google on every render.
  const callback = useRef(onCredential);
  callback.current = onCredential;

  useEffect(() => {
    if (!CLIENT_ID) {
      return;
    }

    let cancelled = false;
    void loadScript()
      .then(() => {
        if (cancelled || !container.current || !window.google) {
          return;
        }
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => callback.current(response.credential),
        });
        window.google.accounts.id.renderButton(container.current, {
          theme: 'filled_black',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          width: 320,
        });
      })
      .catch(() => {
        if (!cancelled) {
          setFailed(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (!CLIENT_ID) {
    return null;
  }

  if (failed) {
    return <p className="text-sm text-ink-muted">Google sign-in is unavailable right now.</p>;
  }

  return <div ref={container} className="flex justify-center" />;
}
