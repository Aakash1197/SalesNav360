import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowRight,
  CalendarClock,
  CircleAlert,
  Columns3,
  LoaderCircle,
  Plus,
  Search,
  Table2,
  UserRound,
  X,
} from 'lucide-react';
import { crmAPI } from '../services/api';

const STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST', 'UNQUALIFIED'];
const ALLOWED_TRANSITIONS = {
  NEW: ['CONTACTED', 'QUALIFIED', 'UNQUALIFIED'],
  CONTACTED: ['QUALIFIED', 'PROPOSAL', 'LOST', 'UNQUALIFIED'],
  QUALIFIED: ['CONTACTED', 'PROPOSAL', 'NEGOTIATION', 'LOST', 'UNQUALIFIED'],
  PROPOSAL: ['NEGOTIATION', 'WON', 'LOST'],
  NEGOTIATION: ['PROPOSAL', 'WON', 'LOST'],
  WON: [],
  LOST: [],
  UNQUALIFIED: [],
};
const EMPTY_FORM = {
  fullName: '',
  email: '',
  phone: '',
  companyName: '',
  expectedValue: '',
  stage: 'NEW',
  priority: 'MEDIUM',
  source: '',
  followUpDate: '',
  assignedTo: '',
  notes: '',
};

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);
}

function label(value) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function ErrorBanner({ message }) {
  return message ? <div className="inline-error" role="alert"><CircleAlert size={16} />{message}</div> : null;
}

export default function LeadsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [pageData, setPageData] = useState(null);
  const [query, setQuery] = useState('');
  const [stageFilter, setStageFilter] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [sort, setSort] = useState('updatedAt');
  const [view, setView] = useState('kanban');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [movingId, setMovingId] = useState(null);

  const loadLeads = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.leads({
        query: query.trim() || undefined,
        stage: stageFilter || undefined,
        assignedTo: assignedTo.trim() || undefined,
        page,
        size: view === 'kanban' ? 100 : 10,
        sort,
        direction: sort === 'followUpDate' ? 'ASC' : 'DESC',
      });
      setPageData(response.data);
    } catch (requestError) {
      setError(requestError.response?.status === 401
        ? 'Your session has expired. Sign in again to view leads.'
        : 'Leads could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [assignedTo, page, query, sort, stageFilter, view]);

  useEffect(() => { loadLeads(); }, [loadLeads]);
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
    setFormError('');
    setDialogOpen(true);
  }

  function openEdit(lead) {
    setEditing(lead);
    setForm({
      ...EMPTY_FORM,
      ...lead,
      expectedValue: lead.expectedValue ?? '',
      followUpDate: lead.followUpDate || '',
    });
    setFormError('');
    setDialogOpen(true);
  }

  async function saveLead(event) {
    event.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = {
      ...form,
      email: form.email || null,
      phone: form.phone || null,
      companyName: form.companyName || null,
      expectedValue: form.expectedValue === '' ? null : form.expectedValue,
      source: form.source || null,
      followUpDate: form.followUpDate || null,
      assignedTo: form.assignedTo || null,
      notes: form.notes || null,
    };
    try {
      if (editing) await crmAPI.updateLead(editing.id, payload);
      else await crmAPI.createLead(payload);
      setDialogOpen(false);
      await loadLeads();
    } catch (requestError) {
      setFormError(requestError.response?.data?.detail || requestError.response?.data?.message || 'The lead could not be saved. Check the fields and try again.');
    } finally {
      setSaving(false);
    }
  }

  async function moveLead(lead, nextStage) {
    setMovingId(lead.id);
    setError('');
    try {
      await crmAPI.moveLead(lead.id, nextStage);
      await loadLeads();
    } catch (requestError) {
      const message = requestError.response?.data?.detail || 'This lead cannot move to that stage.';
      await loadLeads();
      setError(message);
    } finally {
      setMovingId(null);
    }
  }

  async function convertLead(lead) {
    if (!window.confirm(`Convert ${lead.fullName} into a client prospect? This does not mark the lead as a sale.`)) return;
    setMovingId(lead.id);
    setError('');
    setNotice('');
    try {
      const response = await crmAPI.convertLead(lead.id);
      setNotice(`Lead converted to client prospect #${response.data.convertedClientId}.`);
      await loadLeads();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The lead could not be converted. Review duplicate matches and lead stage.');
    } finally {
      setMovingId(null);
    }
  }

  const leads = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">SALES WORKSPACE</span><h1>Leads &amp; Pipeline</h1><p>Qualify prospects and move opportunities through your sales process.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={17} /> Add lead</button>
      </section>

      <section className="lead-summary-grid">
        <article className="card lead-summary"><small>LEADS IN VIEW</small><strong>{pageData?.totalElements ?? '—'}</strong></article>
        <article className="card lead-summary"><small>EXPECTED VALUE IN VIEW</small><strong>{currency(leads.reduce((sum, lead) => sum + Number(lead.expectedValue || 0), 0))}</strong></article>
        <article className="card lead-summary"><small>FOLLOW-UPS IN VIEW</small><strong>{leads.filter((lead) => lead.followUpDate).length}</strong></article>
      </section>

      <section className="card pipeline-card">
        <div className="pipeline-toolbar">
          <label className="search-field"><Search size={16} /><input aria-label="Search leads" placeholder="Search name, email, company, or phone" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} /></label>
          <select className="select-field" aria-label="Filter leads by stage" value={stageFilter} onChange={(event) => { setStageFilter(event.target.value); setPage(0); }}>
            <option value="">All stages</option>{STAGES.map((stage) => <option value={stage} key={stage}>{label(stage)}</option>)}
          </select>
          <input className="select-field assigned-filter" aria-label="Filter leads by representative" placeholder="Representative username" value={assignedTo} onChange={(event) => { setAssignedTo(event.target.value); setPage(0); }} />
          <select className="select-field" aria-label="Sort leads" value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}>
            <option value="updatedAt">Recently updated</option><option value="followUpDate">Next follow-up</option><option value="expectedValue">Highest value</option><option value="fullName">Name A–Z</option>
          </select>
          <div className="view-switch" role="group" aria-label="Lead view">
            <button className={view === 'kanban' ? 'selected' : ''} type="button" onClick={() => { setView('kanban'); setPage(0); }}><Columns3 size={15} /> Pipeline</button>
            <button className={view === 'table' ? 'selected' : ''} type="button" onClick={() => { setView('table'); setPage(0); }}><Table2 size={15} /> Table</button>
          </div>
        </div>
        <ErrorBanner message={error} />
        {notice && <div className="notification-followups" role="status">{notice}</div>}
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={20} /> Loading pipeline…</div>
          : !leads.length ? <div className="empty-state"><span className="empty-icon"><UserRound size={21} /></span><strong>{query || stageFilter ? 'No matching leads' : 'No leads yet'}</strong><p>{query || stageFilter ? 'Adjust your search or stage filter.' : 'Create a lead to start tracking a sales opportunity.'}</p>{!query && !stageFilter && <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> Add first lead</button>}</div>
            : view === 'table' ? <div className="table-scroll"><table className="data-table leads-table">
              <thead><tr><th>Lead</th><th>Stage</th><th>Expected value</th><th>Priority</th><th>Follow-up</th><th>Owner</th><th>Move to</th><th>Client</th></tr></thead>
              <tbody>{leads.map((lead) => <tr key={lead.id}>
                <td><button className="table-link" type="button" onClick={() => openEdit(lead)}><strong>{lead.fullName}</strong><small>{lead.companyName || lead.email || 'Open lead details'}</small></button></td>
                <td><span className={`stage-pill stage-pill-${lead.stage.toLowerCase()}`}>{label(lead.stage)}</span></td>
                <td>{currency(lead.expectedValue)}</td><td><span className={`priority-dot priority-${lead.priority.toLowerCase()}`} />{label(lead.priority)}</td>
                <td>{lead.followUpDate || '—'}</td><td>{lead.assignedTo || 'Unassigned'}</td>
                <td><select className="stage-select" aria-label={`Move ${lead.fullName}`} value={lead.stage} disabled={movingId === lead.id || !ALLOWED_TRANSITIONS[lead.stage].length} onChange={(event) => moveLead(lead, event.target.value)}><option value={lead.stage}>{label(lead.stage)}</option>{ALLOWED_TRANSITIONS[lead.stage].map((next) => <option value={next} key={next}>{label(next)}</option>)}</select></td>
                <td>{lead.convertedClientId ? `#${lead.convertedClientId}` : ['QUALIFIED', 'PROPOSAL'].includes(lead.stage) && <button className="button secondary" type="button" disabled={movingId === lead.id} onClick={() => convertLead(lead)}>Convert</button>}</td>
              </tr>)}</tbody>
            </table></div>
              : <div className="kanban-scroll"><div className="kanban-board">{STAGES.map((stage) => {
                const stageLeads = leads.filter((lead) => lead.stage === stage);
                return <section className={`kanban-column column-${stage.toLowerCase()}`} key={stage}>
                  <header><span className="column-dot" /><strong>{label(stage)}</strong><span className="column-count">{stageLeads.length}</span></header>
                  <div className="kanban-cards">{stageLeads.map((lead) => <article className="lead-card" key={lead.id}>
                    <button className="lead-card-title" type="button" onClick={() => openEdit(lead)}>{lead.fullName}</button>
                    <p>{lead.companyName || lead.email || 'No company details'}</p>
                    <div className="lead-card-value">{currency(lead.expectedValue)}<span className={`priority-label priority-${lead.priority.toLowerCase()}`}>{label(lead.priority)}</span></div>
                    <footer><span><UserRound size={13} />{lead.assignedTo || 'Unassigned'}</span>{lead.followUpDate && <span><CalendarClock size={13} />{lead.followUpDate}</span>}</footer>
                    {ALLOWED_TRANSITIONS[stage].length > 0 && <label className="move-lead"><span>Move to</span><select aria-label={`Move ${lead.fullName} to another stage`} value="" disabled={movingId === lead.id} onChange={(event) => { if (event.target.value) moveLead(lead, event.target.value); }}><option value="">Choose stage</option>{ALLOWED_TRANSITIONS[stage].map((next) => <option value={next} key={next}>{label(next)}</option>)}</select><ArrowRight size={13} /></label>}
                    {lead.convertedClientId ? <p className="table-footnote">Client prospect #{lead.convertedClientId}</p> : ['QUALIFIED', 'PROPOSAL'].includes(stage) && <button className="button secondary" type="button" disabled={movingId === lead.id} onClick={() => convertLead(lead)}>Convert to client</button>}
                  </article>)}
                    {!stageLeads.length && <div className="column-empty">No leads in this stage</div>}
                  </div>
                </section>;
              })}</div></div>}
        {view === 'table' && pageData?.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {pageData.totalPages}</span><div><button className="button secondary icon-text" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="button secondary icon-text" type="button" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage(page + 1)}>Next</button></div></div>}
        {view === 'kanban' && pageData?.totalElements > 100 && <p className="table-footnote">Showing the first 100 leads in the pipeline view. Use table view to page through all results.</p>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="lead-dialog-title">
          <header className="modal-header"><div><h2 id="lead-dialog-title">{editing ? 'Update lead' : 'Create a lead'}</h2><p>Stage changes are checked by the backend against the pipeline rules.</p></div><button className="icon-button" type="button" aria-label="Close dialog" onClick={() => setDialogOpen(false)}><X size={18} /></button></header>
          <form onSubmit={saveLead}>
            <div className="form-grid">
              <label className="field"><span>Full name *</span><input required maxLength="120" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></label>
              <label className="field"><span>Company</span><input maxLength="160" value={form.companyName || ''} onChange={(event) => setForm({ ...form, companyName: event.target.value })} /></label>
              <label className="field"><span>Email</span><input type="email" maxLength="255" value={form.email || ''} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
              <label className="field"><span>Phone</span><input type="tel" maxLength="30" value={form.phone || ''} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
              <label className="field"><span>Expected value (INR)</span><input type="number" min="0" step="0.01" value={form.expectedValue} onChange={(event) => setForm({ ...form, expectedValue: event.target.value })} /></label>
              <label className="field"><span>Stage *</span><select required value={form.stage} onChange={(event) => setForm({ ...form, stage: event.target.value })}>{STAGES.map((stage) => <option value={stage} key={stage} disabled={(!editing && stage !== 'NEW') || (editing && stage !== form.stage && !ALLOWED_TRANSITIONS[form.stage].includes(stage))}>{label(stage)}</option>)}</select></label>
              <label className="field"><span>Priority *</span><select required value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value })}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
              <label className="field"><span>Lead source</span><input maxLength="80" value={form.source || ''} onChange={(event) => setForm({ ...form, source: event.target.value })} /></label>
              <label className="field"><span>Follow-up date</span><input type="date" value={form.followUpDate || ''} onChange={(event) => setForm({ ...form, followUpDate: event.target.value })} /></label>
              <label className="field"><span>Assigned representative</span><input maxLength="80" value={form.assignedTo || ''} onChange={(event) => setForm({ ...form, assignedTo: event.target.value })} /></label>
              <label className="field full-field"><span>Notes</span><textarea rows="3" maxLength="2000" value={form.notes || ''} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></label>
            </div>
            <ErrorBanner message={formError} />
            <footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDialogOpen(false)}>Cancel</button><button className="button primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Create lead'}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}
