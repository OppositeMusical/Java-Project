import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import {
  STATUS_LABELS,
  TASK_PRIORITIES,
  TASK_STATUSES,
  type Project,
  type Task,
  type TaskRequest,
} from '../api/types';

interface Props {
  projects: Project[];
  initial?: Task;
  defaultProjectId: number | null;
  onSubmit: (request: TaskRequest) => Promise<void>;
  onCancel?: () => void;
}

export default function TaskForm({ projects, initial, defaultProjectId, onSubmit, onCancel }: Props) {
  const [form, setForm] = useState<TaskRequest>(() => ({
    title: initial?.title ?? '',
    description: initial?.description ?? '',
    status: initial?.status ?? 'TODO',
    priority: initial?.priority ?? 'MEDIUM',
    dueDate: initial?.dueDate ?? '',
    projectId: initial?.projectId ?? defaultProjectId ?? projects[0]?.id ?? 0,
  }));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  function update<K extends keyof TaskRequest>(key: K, value: TaskRequest[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setErrors({});
    try {
      await onSubmit({
        ...form,
        description: form.description?.trim() || null,
        dueDate: form.dueDate || null,
      });
      if (!initial) {
        setForm((current) => ({ ...current, title: '', description: '', dueDate: '' }));
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors({ ...error.fieldErrors, form: error.message });
      } else {
        throw error;
      }
    } finally {
      setSaving(false);
    }
  }

  if (projects.length === 0) {
    return <p className="muted">Create a project to start adding tasks.</p>;
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="field grow">
        <label htmlFor={`title-${initial?.id ?? 'new'}`}>Title</label>
        <input
          id={`title-${initial?.id ?? 'new'}`}
          value={form.title}
          onChange={(e) => update('title', e.target.value)}
          maxLength={200}
          required
        />
        {errors.title && <span className="error">{errors.title}</span>}
      </div>

      <div className="field">
        <label>Project</label>
        <select value={form.projectId} onChange={(e) => update('projectId', Number(e.target.value))}>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label>Priority</label>
        <select
          value={form.priority}
          onChange={(e) => update('priority', e.target.value as TaskRequest['priority'])}
        >
          {TASK_PRIORITIES.map((p) => (
            <option key={p} value={p}>
              {p.charAt(0) + p.slice(1).toLowerCase()}
            </option>
          ))}
        </select>
      </div>

      {initial && (
        <div className="field">
          <label>Status</label>
          <select
            value={form.status}
            onChange={(e) => update('status', e.target.value as TaskRequest['status'])}
          >
            {TASK_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="field">
        <label>Due date</label>
        <input
          type="date"
          value={form.dueDate ?? ''}
          onChange={(e) => update('dueDate', e.target.value)}
        />
      </div>

      <div className="field full">
        <label>Description</label>
        <textarea
          value={form.description ?? ''}
          onChange={(e) => update('description', e.target.value)}
          maxLength={2000}
          rows={2}
        />
      </div>

      {errors.form && <p className="error full">{errors.form}</p>}

      <div className="actions full">
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Cancel
          </button>
        )}
        <button type="submit" disabled={saving}>
          {initial ? 'Save' : 'Add task'}
        </button>
      </div>
    </form>
  );
}
