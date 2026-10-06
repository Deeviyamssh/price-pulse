const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function humanizeError(status: number, message: string | undefined): string {
  if (message) return message;
  if (status === 401) return 'Invalid credentials';
  if (status === 429) return 'Too many attempts. Please try again later.';
  if (status >= 500) return 'The server encountered a problem. Please try again later.';
  return 'The request could not be completed.';
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 10_000);

  try {
    const headers = new Headers(options.headers);
    if (options.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: 'include',
      headers,
      signal: controller.signal,
    });

    if (response.status === 401) {
      if (window.location.pathname !== '/login') {
        window.history.replaceState({}, '', '/login');
      }
      throw new ApiError(401, 'Your session has expired. Please log in again.');
    }

    if (!response.ok) {
      const body = (await response.json().catch(() => ({}))) as { message?: string };
      throw new ApiError(response.status, humanizeError(response.status, body.message));
    }

    if (response.status === 204) return undefined as T;
    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('Unable to reach the server. Please check your connection.');
    }
    throw new Error('Unable to reach the server. Please check your connection.');
  } finally {
    window.clearTimeout(timeout);
  }
}
