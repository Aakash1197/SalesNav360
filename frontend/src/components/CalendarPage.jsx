import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, CircleAlert, Clock3, LoaderCircle, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import { crmAPI } from '../services/api';

const EMPTY_FORM = {
  title: '',
  type: 'MEETING',
  eventDate: new Date().toISOString().slice(0, 10),
  startTime: '',
  endTime: '',
  description: '',
  clientId: '',
};
const EVENT_TYPES = ['MEETING', 'CLIENT_CALL', 'FOLLOW_UP', 'POLICY_RENEWAL', 'TASK_DEADLINE', 'APPOINTMENT'];

function monthRange(dateValue) {
  const date = new Date(`${dateValue}T12:00:00`);
  const year = date.getFullYear();
  const month = date.getMonth();
  const dateString = (value) => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  return {
    fromDate: dateString(new Date(year, month, 1, 12)),
    toDate: dateString(new Date(year, month + 1, 0, 12)),
  };
}

function readable(value) {
  return value?.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) || 'Event';
}

export default function CalendarPage() {
  const [events, setEvents] = useState([]);
  const [clients, setClients] = useState([]);
  const [month, setMonth] = useState(EMPTY_FORM.eventDate);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const loadCalendar = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [eventResponse, clientResponse] = await Promise.all([
        crmAPI.calendarEvents({ ...monthRange(month), page: 0, size: 100, sort: 'eventDate', direction: 'ASC' }),
        crmAPI.clients({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
      ]);
      setEvents(eventResponse.data.content || []);
      setClients((clientResponse.data.content || []).filter((client) => client.status !== 'ARCHIVED'));
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Calendar events could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [month]);

  useEffect(() => { loadCalendar(); }, [loadCalendar]);

  function openCreate() {
    setEditing(null);
    setForm({ ...EMPTY_FORM, eventDate: `${month.slice(0, 7)}-01` });
    setDialogOpen(true);
  }

  function openEdit(event) {
    setEditing(event);
    setForm({
      title: event.title,
      type: event.type,
      eventDate: event.eventDate,
      startTime: event.startTime || '',
      endTime: event.endTime || '',
      description: event.description || '',
      clientId: event.clientId ? String(event.clientId) : '',
    });
    setDialogOpen(true);
  }

  async function saveEvent(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = { ...form, clientId: form.clientId ? Number(form.clientId) : null, startTime: form.startTime || null, endTime: form.endTime || null };
    try {
      if (editing) await crmAPI.updateCalendarEvent(editing.id, payload);
      else await crmAPI.createCalendarEvent(payload);
      setDialogOpen(false);
      setNotice(editing ? 'Calendar event updated.' : 'Calendar event added.');
      await loadCalendar();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The calendar event could not be saved.');
    } finally {
      setSaving(false);
    }
  }

  async function deleteEvent(event) {
    if (!window.confirm(`Delete "${event.title}" from the calendar?`)) return;
    setError('');
    setNotice('');
    try {
      await crmAPI.deleteCalendarEvent(event.id);
      setNotice('Calendar event deleted.');
      await loadCalendar();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The calendar event could not be deleted.');
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">SCHEDULE &amp; FOLLOW-UPS</span><h1>Calendar</h1><p>Coordinate meetings, client calls, policy renewals, and task deadlines.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> Add event</button>
      </section>

      <section className="card calendar-toolbar">
        <label className="field"><span>Choose month</span><input aria-label="Calendar month" type="month" value={month.slice(0, 7)} onChange={(event) => setMonth(`${event.target.value}-01`)} /></label>
        <button className="button secondary" type="button" onClick={loadCalendar} disabled={loading}><RefreshCw size={15} /> Refresh</button>
      </section>
      {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
      {notice && <div className="notification-followups" role="status">{notice}</div>}

      <section className="card calendar-events">
        <div className="card-heading"><div><h2><CalendarDays size={18} /> Events this month</h2><p>{new Date(`${month.slice(0, 7)}-15T12:00:00`).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</p></div></div>
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={18} /> Loading calendar…</div>
          : events.length ? <div className="calendar-event-list">{events.map((event) => (
            <article className="calendar-event-row" key={event.id}>
              <div className="calendar-date-block"><strong>{new Date(`${event.eventDate}T12:00:00`).getDate()}</strong><small>{new Date(`${event.eventDate}T12:00:00`).toLocaleDateString(undefined, { month: 'short' })}</small></div>
              <div className="calendar-event-copy"><span className="status-badge status-prospect">{readable(event.type)}</span><strong>{event.title}</strong><p>{event.clientName || 'No client linked'}{event.description ? ` · ${event.description}` : ''}</p></div>
              <div className="calendar-event-time"><Clock3 size={14} />{event.startTime ? event.startTime.slice(0, 5) : 'All day'}{event.endTime ? ` – ${event.endTime.slice(0, 5)}` : ''}</div>
              <div className="row-actions"><button className="icon-button" type="button" aria-label={`Edit ${event.title}`} onClick={() => openEdit(event)}><Pencil size={15} /></button><button className="icon-button danger-icon" type="button" aria-label={`Delete ${event.title}`} onClick={() => deleteEvent(event)}><Trash2 size={15} /></button></div>
            </article>
          ))}</div> : <div className="empty-state"><span className="empty-icon"><CalendarDays size={20} /></span><strong>No events this month</strong><p>Add an appointment or follow-up to keep your schedule up to date.</p><button className="button primary" type="button" onClick={openCreate}><Plus size={15} /> Add event</button></div>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="calendar-dialog-title">
          <header className="modal-header"><div><h2 id="calendar-dialog-title">{editing ? 'Edit calendar event' : 'Add calendar event'}</h2><p>Event details are saved to your CRM calendar.</p></div><button className="icon-button" type="button" aria-label="Close dialog" onClick={() => setDialogOpen(false)}><X size={17} /></button></header>
          <form onSubmit={saveEvent}>
            <div className="form-grid">
              <label className="field full-field"><span>Title *</span><input required maxLength="140" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
              <label className="field"><span>Type *</span><select required value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>{EVENT_TYPES.map((type) => <option value={type} key={type}>{readable(type)}</option>)}</select></label>
              <label className="field"><span>Date *</span><input required type="date" value={form.eventDate} onChange={(event) => setForm({ ...form, eventDate: event.target.value })} /></label>
              <label className="field"><span>Start time</span><input type="time" value={form.startTime} onChange={(event) => setForm({ ...form, startTime: event.target.value })} /></label>
              <label className="field"><span>End time</span><input type="time" value={form.endTime} onChange={(event) => setForm({ ...form, endTime: event.target.value })} /></label>
              <label className="field full-field"><span>Client / investor</span><select value={form.clientId} onChange={(event) => setForm({ ...form, clientId: event.target.value })}><option value="">No client linked</option>{clients.map((client) => <option key={client.id} value={client.id}>{client.fullName}</option>)}</select></label>
              <label className="field full-field"><span>Notes</span><textarea maxLength="2000" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
            </div>
            {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
            <footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDialogOpen(false)} disabled={saving}>Cancel</button><button className="button primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Add event'}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}
