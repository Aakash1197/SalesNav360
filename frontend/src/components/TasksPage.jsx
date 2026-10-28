import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CalendarClock, CircleAlert, ClipboardCheck, LoaderCircle, Plus, Search, X } from 'lucide-react';
import { crmAPI } from '../services/api';

const STATUSES = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
const TRANSITIONS = {
  OPEN: ['IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
  IN_PROGRESS: ['OPEN', 'COMPLETED', 'CANCELLED'],
  COMPLETED: ['OPEN'],
  CANCELLED: ['OPEN'],
};
const EMPTY_FORM = {
  title: '',
  description: '',
  dueDate: '',
  priority: 'MEDIUM',
  assignedTo: '',
  relatedEntityType: '',
  relatedEntityId: '',
};

function label(value) {
  return value?.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) || '—';
}

export default function TasksPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [pageData, setPageData] = useState(null);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.tasks({
        query: query.trim() || undefined,
        status: status || undefined,
        assignedTo: assignedTo.trim() || undefined,
        page,
        size: 10,
        sort: 'dueDate',
        direction: 'ASC',
      });
      setPageData(response.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Tasks could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [assignedTo, page, query, status]);

  useEffect(() => { loadTasks(); }, [loadTasks]);
  useEffect(() => {
    if (searchParams.get('new') === '1') {
      openCreate();
      setSearchParams((current) => {
        current.delete('new');
        return current;
      }, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  }

  async function openEdit(task) {
    try {
      const response = await crmAPI.task(task.id);
      setEditing(response.data);
      setForm({
        ...EMPTY_FORM,
        ...response.data,
        dueDate: response.data.dueDate || '',
        relatedEntityId: response.data.relatedEntityId?.toString() || '',
      });
      setDialogOpen(true);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Task details could not be loaded.');
    }
  }

  async function saveTask(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = {
      ...form,
      description: form.description || null,
      dueDate: form.dueDate || null,
      assignedTo: form.assignedTo || null,
      relatedEntityType: form.relatedEntityType || null,
      relatedEntityId: form.relatedEntityId ? Number(form.relatedEntityId) : null,
    };
    try {
      if (editing) await crmAPI.updateTask(editing.id, payload);
      else await crmAPI.createTask(payload);
      setDialogOpen(false);
      setNotice(editing ? 'Task updated.' : 'Task created.');
      await loadTasks();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The task could not be saved. Check the fields and linked record.');
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(task, nextStatus) {
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await crmAPI.changeTaskStatus(task.id, nextStatus);
      setNotice(`Task marked ${label(nextStatus).toLowerCase()}.`);
      await loadTasks();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The requested task status change was not allowed.');
      await loadTasks();
    } finally {
      setSaving(false);
    }
  }

  const tasks = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">WORKSPACE</span><h1>Tasks &amp; follow-ups</h1><p>Prioritize assigned work, due dates, and client follow-ups.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> Add task</button>
      </section>

      <section className="task-summary-grid">
        <article className="card lead-summary"><small>TASKS IN VIEW</small><strong>{pageData?.totalElements ?? '—'}</strong></article>
        <article className="card lead-summary"><small>OPEN IN VIEW</small><strong>{tasks.filter((task) => ['OPEN', 'IN_PROGRESS'].includes(task.status)).length}</strong></article>
        <article className="card lead-summary"><small>OVERDUE IN VIEW</small><strong>{tasks.filter((task) => task.dueDate && new Date(`${task.dueDate}T00:00:00`) < new Date(new Date().toDateString()) && ['OPEN', 'IN_PROGRESS'].includes(task.status)).length}</strong></article>
      </section>

      <section className="table-card">
        <div className="filters-row">
          <label className="search-field"><Search size={15} /><input aria-label="Search tasks" placeholder="Search title or description" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} /></label>
          <select className="select-field" aria-label="Filter tasks by status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">All statuses</option>{STATUSES.map((item) => <option value={item} key={item}>{label(item)}</option>)}</select>
          <input className="select-field" aria-label="Filter tasks by assignee" placeholder="Representative username" value={assignedTo} onChange={(event) => { setAssignedTo(event.target.value); setPage(0); }} />
        </div>
        {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
        {notice && <div className="notification-followups" role="status">{notice}</div>}
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={19} /> Loading tasks…</div>
          : !tasks.length ? <div className="empty-state"><span className="empty-icon"><ClipboardCheck size={20} /></span><strong>{query || status ? 'No matching tasks' : 'No tasks yet'}</strong><p>Create a task and assign an owner or due date to start tracking follow-ups.</p><button className="button primary" type="button" onClick={openCreate}><Plus size={15} /> Add task</button></div>
            : <div className="table-scroll"><table className="data-table">
              <thead><tr><th>Task</th><th>Due</th><th>Priority</th><th>Owner</th><th>Related record</th><th>Status</th><th>Update status</th></tr></thead>
              <tbody>{tasks.map((task) => <tr key={task.id}>
                <td><button className="table-link" type="button" onClick={() => openEdit(task)}><strong>{task.title}</strong><small>{task.description || 'Open task details'}</small></button></td>
                <td><span className="person-cell"><CalendarClock size={13} />{task.dueDate || 'No due date'}</span></td>
                <td><span className={`priority-dot priority-${task.priority.toLowerCase()}`} />{label(task.priority)}</td>
                <td>{task.assignedTo || 'Unassigned'}</td>
                <td>{task.relatedEntityType ? `${label(task.relatedEntityType)} #${task.relatedEntityId}` : '—'}</td>
                <td><span className={`status-badge ${task.status === 'COMPLETED' ? 'status-active' : 'status-inactive'}`}>{label(task.status)}</span></td>
                <td><select className="stage-select" aria-label={`Update status for ${task.title}`} value={task.status} disabled={saving} onChange={(event) => changeStatus(task, event.target.value)}><option value={task.status}>{label(task.status)}</option>{(TRANSITIONS[task.status] || []).map((next) => <option value={next} key={next}>{label(next)}</option>)}</select></td>
              </tr>)}</tbody>
            </table></div>}
        {pageData?.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {pageData.totalPages}</span><div><button className="button secondary" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="button secondary" type="button" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage(page + 1)}>Next</button></div></div>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="task-dialog-title">
          <header className="modal-header"><div><h2 id="task-dialog-title">{editing ? 'Update task' : 'Create a task'}</h2><p>Reminders are not sent; use the due date to track work in Apex Finodeal LLP.</p></div><button className="icon-button" type="button" aria-label="Close task dialog" onClick={() => setDialogOpen(false)}><X size={18} /></button></header>
          <form onSubmit={saveTask}>
            <div className="form-grid">
              <label className="field full-field"><span>Title *</span><input required maxLength="140" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
              <label className="field"><span>Due date</span><input type="date" value={form.dueDate} onChange={(event) => setForm({ ...form, dueDate: event.target.value })} /></label>
              <label className="field"><span>Priority *</span><select required value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value })}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
              <label className="field"><span>Assigned representative</span><input maxLength="80" value={form.assignedTo || ''} onChange={(event) => setForm({ ...form, assignedTo: event.target.value })} /></label>
              <label className="field"><span>Linked record type</span><select value={form.relatedEntityType || ''} onChange={(event) => setForm({ ...form, relatedEntityType: event.target.value, relatedEntityId: '' })}><option value="">None</option><option value="CLIENT">Client</option><option value="LEAD">Lead</option></select></label>
              <label className="field"><span>Linked record ID</span><input type="number" min="1" disabled={!form.relatedEntityType} value={form.relatedEntityId || ''} onChange={(event) => setForm({ ...form, relatedEntityId: event.target.value })} /></label>
              <label className="field full-field"><span>Description</span><textarea rows="3" maxLength="2000" value={form.description || ''} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
            </div>
            <footer className="modal-footer"><button className="button secondary" type="button" disabled={saving} onClick={() => setDialogOpen(false)}>Cancel</button><button className="button primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Create task'}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}
