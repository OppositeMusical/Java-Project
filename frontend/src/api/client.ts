import type {
  LoginRequest,
  Project,
  ProjectRequest,
  RegisterRequest,
  Task,
  TaskRequest,
  TaskStatus,
  User,
} from './types';

/** Base URL of the backend. Empty in development, where Vite proxies /api to Spring Boot. */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;
  /** Set on 429 responses: how long the client must wait before retrying. */
  readonly retryAfterSeconds?: number;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}, retryAfterSeconds?: number) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

let onUnauthorized: () => void = () => {};

/** Called when the session has expired or the user was signed out elsewhere. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

/** Spring Security puts the CSRF token in a readable cookie; it must be echoed back in a header. */
function csrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length);
}

// Send the session cookie; "include" is needed only when the API is on another origin.
const credentials: RequestCredentials = API_BASE ? 'include' : 'same-origin';

/**
 * Returns the CSRF token, fetching a fresh one if there is none yet. Signing out deletes the
 * token cookie, and any GET to the API issues a new one.
 */
async function ensureCsrfToken(): Promise<string | undefined> {
  if (!csrfToken()) {
    await fetch(`${API_BASE}/api/auth/me`, { credentials }).catch(() => undefined);
  }
  return csrfToken();
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method ?? 'GET';
  const token = method === 'GET' ? undefined : await ensureCsrfToken();
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    credentials,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {}),
      ...init.headers,
    },
  });

  if (response.status === 401 && !path.startsWith('/api/auth/')) {
    onUnauthorized();
  }
  if (!response.ok) {
    // Spring returns RFC 9457 problem details: { detail, errors?, retryAfterSeconds? }
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(response.status, problem.detail ?? response.statusText, problem.errors, problem.retryAfterSeconds);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const authApi = {
  me: () => request<User>('/api/auth/me'),
  login: (body: LoginRequest) =>
    request<User>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body: RegisterRequest) =>
    request<User>('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
};

export const projectsApi = {
  list: () => request<Project[]>('/api/projects'),
  create: (body: ProjectRequest) =>
    request<Project>('/api/projects', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: number, body: ProjectRequest) =>
    request<Project>(`/api/projects/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove: (id: number) => request<void>(`/api/projects/${id}`, { method: 'DELETE' }),
};

export const tasksApi = {
  list: (filters: { projectId?: number; status?: TaskStatus } = {}) => {
    const params = new URLSearchParams();
    if (filters.projectId != null) params.set('projectId', String(filters.projectId));
    if (filters.status) params.set('status', filters.status);
    const query = params.toString();
    return request<Task[]>(`/api/tasks${query ? `?${query}` : ''}`);
  },
  create: (body: TaskRequest) =>
    request<Task>('/api/tasks', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: number, body: TaskRequest) =>
    request<Task>(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  updateStatus: (id: number, status: TaskStatus) =>
    request<Task>(`/api/tasks/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  remove: (id: number) => request<void>(`/api/tasks/${id}`, { method: 'DELETE' }),
};
