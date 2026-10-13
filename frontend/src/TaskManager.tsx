import { useCallback, useEffect, useState } from 'react';
import { projectsApi, tasksApi } from './api/client';
import { STATUS_LABELS, TASK_STATUSES, type Project, type Task, type TaskStatus } from './api/types';
import { useAuth } from './auth/AuthProvider';
import ProjectSidebar from './components/ProjectSidebar';
import TaskForm from './components/TaskForm';
import TaskList from './components/TaskList';

export default function TaskManager() {
  const { user, signOut } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const loadProjects = useCallback(async () => {
    setProjects(await projectsApi.list());
  }, []);

  const loadTasks = useCallback(async () => {
    setTasks(
      await tasksApi.list({
        projectId: selectedProjectId ?? undefined,
        status: statusFilter || undefined,
      }),
    );
  }, [selectedProjectId, statusFilter]);

  /** Runs a mutation, then refreshes both lists so counts stay in sync. */
  const mutate = useCallback(
    async (action: () => Promise<unknown>) => {
      setError(null);
      try {
        await action();
      } finally {
        await Promise.all([loadProjects(), loadTasks()]).catch((e: Error) => setError(e.message));
      }
    },
    [loadProjects, loadTasks],
  );

  useEffect(() => {
    loadProjects().catch((e: Error) => setError(`Could not reach the API: ${e.message}`));
  }, [loadProjects]);

  useEffect(() => {
    setLoading(true);
    loadTasks()
      .catch((e: Error) => setError(`Could not load tasks: ${e.message}`))
      .finally(() => setLoading(false));
  }, [loadTasks]);

  const selectedProject = projects.find((p) => p.id === selectedProjectId);

  return (
    <div className="layout">
      <ProjectSidebar
        projects={projects}
        selectedId={selectedProjectId}
        onSelect={setSelectedProjectId}
        onCreate={(name) =>
          mutate(async () => {
            const created = await projectsApi.create({ name });
            setSelectedProjectId(created.id);
          })
        }
        user={user!}
        onSignOut={() => void signOut()}
        onDelete={(project) => {
          if (!confirm(`Delete "${project.name}" and all of its tasks?`)) return;
          void mutate(async () => {
            await projectsApi.remove(project.id);
            if (selectedProjectId === project.id) setSelectedProjectId(null);
          }).catch((e: Error) => setError(e.message));
        }}
      />

      <main className="content">
        <header className="content-header">
          <div>
            <h1>{selectedProject?.name ?? 'All tasks'}</h1>
            {selectedProject?.description && <p className="muted">{selectedProject.description}</p>}
          </div>
          <label className="filter">
            Status
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as TaskStatus | '')}>
              <option value="">All</option>
              {TASK_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </label>
        </header>

        {error && (
          <div className="alert" role="alert">
            {error}
            <button className="icon-button" aria-label="Dismiss" onClick={() => setError(null)}>
              ×
            </button>
          </div>
        )}

        <section className="panel">
          <h2>New task</h2>
          <TaskForm
            key={selectedProjectId ?? 'all'}
            projects={projects}
            defaultProjectId={selectedProjectId}
            onSubmit={(request) => mutate(() => tasksApi.create(request))}
          />
        </section>

        <section>
          {loading ? (
            <p className="muted">Loading…</p>
          ) : (
            <TaskList
              tasks={tasks}
              projects={projects}
              showProject={selectedProjectId === null}
              onStatusChange={(task, status) =>
                void mutate(() => tasksApi.updateStatus(task.id, status)).catch((e: Error) => setError(e.message))
              }
              onUpdate={(task, request) => mutate(() => tasksApi.update(task.id, request))}
              onDelete={(task) => {
                if (!confirm(`Delete "${task.title}"?`)) return;
                void mutate(() => tasksApi.remove(task.id)).catch((e: Error) => setError(e.message));
              }}
            />
          )}
        </section>
      </main>
    </div>
  );
}
