import type { AuthenticationResponse, ProblemDetail } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * The access token lives in a module variable, never in localStorage.
 *
 * Anything readable from JavaScript is readable by an XSS payload, and a token in localStorage
 * also survives the tab. Keeping it in memory means a stolen token needs an active page, and a
 * reload silently recovers the session from the refresh cookie instead.
 */
let accessToken: string | null = null;
let onSessionLost: (() => void) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/** Called when the session cannot be renewed, so the app can send the user back to sign-in. */
export function setSessionLostHandler(handler: (() => void) | null): void {
  onSessionLost = handler;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }

  /** Field-level validation messages, keyed by field name. */
  get fieldErrors(): Record<string, string> {
    return this.problem.errors ?? (this.problem.field ? { [this.problem.field]: this.message } : {});
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Set on the refresh call itself, so a failed refresh cannot trigger another refresh. */
  skipRefresh?: boolean;
}

/**
 * Only one refresh may be in flight.
 *
 * When a token expires, several queries usually fail at once. Without this, each would start its
 * own refresh, and since refresh tokens rotate, the first success would invalidate the token the
 * others are still holding — the backend would read that as token reuse and revoke the whole
 * session. Sharing one promise means the rotation happens exactly once.
 */
let refreshInFlight: Promise<boolean> | null = null;

async function refreshSession(): Promise<boolean> {
  refreshInFlight ??= (async () => {
    try {
      const response = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        // The refresh token is an HttpOnly cookie; it only travels if credentials are included.
        credentials: 'include',
      });
      if (!response.ok) {
        return false;
      }
      const session = (await response.json()) as AuthenticationResponse;
      accessToken = session.accessToken;
      return true;
    } catch {
      return false;
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
}

async function toProblem(response: Response): Promise<ProblemDetail> {
  try {
    return (await response.json()) as ProblemDetail;
  } catch {
    return { status: response.status, title: response.statusText };
  }
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  return fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await send(path, options);

  // A 401 means the access token is missing or expired. Renew once, then replay the request;
  // the caller never sees the interruption.
  if (response.status === 401 && !options.skipRefresh) {
    const renewed = await refreshSession();
    if (renewed) {
      response = await send(path, options);
    } else {
      accessToken = null;
      onSessionLost?.();
    }
  }

  if (!response.ok) {
    throw new ApiError(response.status, await toProblem(response));
  }

  // 204 responses have no body; the toggle endpoints all answer this way.
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  get: <T,>(path: string) => request<T>(path),
  post: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  patch: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  delete: <T,>(path: string) => request<T>(path, { method: 'DELETE' }),
};

/** Sign-in and sign-up deliberately bypass the refresh dance: there is no session yet. */
export async function authenticate(
  path: '/api/v1/auth/login' | '/api/v1/auth/refresh',
  body?: unknown,
): Promise<AuthenticationResponse> {
  const session = await request<AuthenticationResponse>(path, {
    method: 'POST',
    body,
    skipRefresh: true,
  });
  accessToken = session.accessToken;
  return session;
}

export { BASE_URL, refreshSession };
