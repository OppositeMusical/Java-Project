import { useState } from 'react';
import { STATUS_LABELS, TASK_STATUSES, type Project, type Task, type TaskRequest, type TaskStatus } from '../api/types';
import TaskForm from './TaskForm';

interface Props {
  tasks: Task[];
  projects: Project[];
  showProject: boolean;
  onStatusChange: (task: Task, status: TaskStatus) => void;
  onUpdate: (task: Task, request: TaskRequest) => Promise<void>;
  onDelete: (task: Task) => void;
}

export default function TaskList({ tasks, projects, showProject, onStatusChange, onUpdate, onDelete }: Props) {
  const [editingId, setEditingId] = useState<number | null>(null);

  if (tasks.length === 0) {
    return <p className="muted">No tasks here yet.</p>;
  }

  return (
    <ul className="task-list">
      {tasks.map((task) =>
        editingId === task.id ? (
          <li key={task.id} className="task-card editing">
            <TaskForm
              projects={projects}
              initial={task}
              defaultProjectId={task.projectId}
              onSubmit={async (request) => {
                await onUpdate(task, request);
                setEditingId(null);
              }}
              onCancel={() => setEditingId(null)}
            />
          </li>
        ) : (
          <li key={task.id} className={`task-card status-${task.status.toLowerCase()}`}>
            <div className="task-main">
              <div className="task-title">
                <span className={`priority priority-${task.priority.toLowerCase()}`}>{task.priority}</span>
                <strong>{task.title}</strong>
              </div>
              {task.description && <p className="task-description">{task.description}</p>}
              <div className="task-meta">
                {showProject && <span>{task.projectName}</span>}
                {task.dueDate && <span>Due {new Date(`${task.dueDate}T00:00`).toLocaleDateString()}</span>}
              </div>
            </div>
            <div className="task-actions">
              <select
                value={task.status}
                aria-label="Status"
                onChange={(e) => onStatusChange(task, e.target.value as TaskStatus)}
              >
                {TASK_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
              <button className="secondary" onClick={() => setEditingId(task.id)}>
                Edit
              </button>
              <button className="danger" onClick={() => onDelete(task)}>
                Delete
              </button>
            </div>
          </li>
        ),
      )}
    </ul>
  );
}
