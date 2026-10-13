import { useState, type FormEvent } from 'react';
import type { Project, User } from '../api/types';

interface Props {
  projects: Project[];
  selectedId: number | null;
  onSelect: (id: number | null) => void;
  onCreate: (name: string) => Promise<void>;
  onDelete: (project: Project) => void;
  user: User;
  onSignOut: () => void;
}

export default function ProjectSidebar({
  projects,
  selectedId,
  onSelect,
  onCreate,
  onDelete,
  user,
  onSignOut,
}: Props) {
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    try {
      await onCreate(name.trim());
      setName('');
    } finally {
      setSaving(false);
    }
  }

  return (
    <aside className="sidebar">
      <h2>Projects</h2>
      <ul className="project-list">
        <li>
          <button
            className={`project-item ${selectedId === null ? 'active' : ''}`}
            onClick={() => onSelect(null)}
          >
            <span>All tasks</span>
          </button>
        </li>
        {projects.map((project) => (
          <li key={project.id}>
            <button
              className={`project-item ${selectedId === project.id ? 'active' : ''}`}
              onClick={() => onSelect(project.id)}
            >
              <span>{project.name}</span>
              <span className="badge" title={`${project.openTaskCount} open of ${project.taskCount}`}>
                {project.openTaskCount}
              </span>
            </button>
            <button
              className="icon-button"
              aria-label={`Delete ${project.name}`}
              title="Delete project"
              onClick={() => onDelete(project)}
            >
              ×
            </button>
          </li>
        ))}
      </ul>

      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="New project name"
          maxLength={100}
          aria-label="New project name"
        />
        <button type="submit" disabled={saving || !name.trim()}>
          Add
        </button>
      </form>

      <div className="account">
        <div>
          <strong>{user.displayName}</strong>
          <span className="muted">{user.email}</span>
        </div>
        <button className="secondary" onClick={onSignOut}>
          Sign out
        </button>
      </div>
    </aside>
  );
}
