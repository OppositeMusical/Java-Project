import type { Project, ProjectRequest, Task, TaskRequest, TaskStatus } from './types';

/** Base URL of the backend. Empty in development, where Vite proxies /api to Spring Boot. */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  });

  if (!response.ok) {
    // Spring returns RFC 9457 problem details: { detail, errors? }
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(response.status, problem.detail ?? response.statusText, problem.errors);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

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
