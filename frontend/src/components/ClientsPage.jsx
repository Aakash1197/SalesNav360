import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Archive,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Download,
  Eye,
  LoaderCircle,
  Pencil,
  Plus,
  Search,
  UsersRound,
  X,
} from 'lucide-react';
import { crmAPI } from '../services/api';

const EMPTY_FORM = {
  fullName: '',
  email: '',
  phone: '',
  companyName: '',
  type: 'CLIENT',
  status: 'ACTIVE',
  leadSource: '',
  assignedTo: '',
  notes: '',
};

function ErrorBanner({ message }) {
  return message ? <div className="inline-error" role="alert"><CircleAlert size={16} />{message}</div> : null;
}

function displayDate(value) {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value)) : '—';
}

function downloadCsv(rows) {
  const columns = ['id', 'fullName', 'email', 'phone', 'companyName', 'type', 'status', 'assignedTo'];
  const quote = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`;
  const csv = [columns.join(','), ...rows.map((row) => columns.map((column) => quote(row[column])).join(','))].join('\r\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'apex-finodeal-clients.csv';
  anchor.click();
  URL.revokeObjectURL(url);
}

export default function ClientsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState(searchParams.get('query') || '');
  const [status, setStatus] = useState('');
  const [assignedTo, setAssignedTo] = useState('');
  const [sort, setSort] = useState('updatedAt');
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [duplicates, setDuplicates] = useState([]);
  const [checkingDuplicates, setCheckingDuplicates] = useState(false);
  const [detail, setDetail] = useState(null);

  const loadClients = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.clients({
        query: query.trim() || undefined,
        status: status || undefined,
        assignedTo: assignedTo.trim() || undefined,
        page,
        size: 10,
        sort,
        direction: 'DESC',
      });
      setPageData(response.data);
    } catch (requestError) {
      setError(requestError.response?.status === 401
        ? 'Your session has expired. Sign in again to view clients.'
        : 'Clients could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }, [assignedTo, page, query, sort, status]);

  useEffect(() => { loadClients(); }, [loadClients]);
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
    setDuplicates([]);
    setDialogOpen(true);
  }

  async function openEdit(client) {
    setFormError('');
    setDuplicates([]);
    try {
      const response = await crmAPI.client(client.id);
      setEditing(response.data);
      setForm({ ...EMPTY_FORM, ...response.data });
      setDialogOpen(true);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Client details could not be loaded.');
    }
  }

  async function openDetails(client) {
    try {
      const response = await crmAPI.client(client.id);
      setDetail(response.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Client details could not be loaded.');
    }
  }

  async function saveClient(event) {
    event.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = {
      ...form,
      email: form.email || null,
      phone: form.phone || null,
      companyName: form.companyName || null,
      leadSource: form.leadSource || null,
      assignedTo: form.assignedTo || null,
      notes: form.notes || null,
    };
    try {
      if (editing) await crmAPI.updateClient(editing.id, payload);
      else await crmAPI.createClient(payload);
      setDialogOpen(false);
      await loadClients();
    } catch (requestError) {
      setFormError(requestError.response?.data?.detail || requestError.response?.data?.message || 'The client could not be saved. Check the fields and try again.');
    } finally {
      setSaving(false);
    }
  }

  async function archiveClient(client) {
    if (!window.confirm(`Archive ${client.fullName}? It will be removed from the active client list.`)) return;
    try {
      await crmAPI.archiveClient(client.id);
      await loadClients();
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'The client could not be archived.');
    }
  }

  async function checkDuplicates() {
    if (!form.email && !form.phone) {
      setFormError('Enter an email address or phone number to check for possible duplicates.');
      return;
    }
    setCheckingDuplicates(true);
    setFormError('');
    try {
      const response = await crmAPI.clientDuplicates({
        email: form.email || undefined,
        phone: form.phone || undefined,
      });
      setDuplicates(response.data.filter((client) => client.id !== editing?.id));
    } catch (requestError) {
      setFormError(requestError.response?.data?.detail || 'Duplicate check failed. Please try again.');
    } finally {
      setCheckingDuplicates(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">RELATIONSHIP MANAGEMENT</span><h1>Clients &amp; Investors</h1><p>Manage customer relationships and investor records in one place.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={17} /> Add client</button>
      </section>

      <section className="client-summary-grid">
        <article className="card summary-card"><span className="summary-icon blue"><UsersRound size={18} /></span><div><small>ACTIVE RECORDS</small><strong>{pageData?.totalElements ?? '—'}</strong></div></article>
        <article className="card summary-card"><span className="summary-icon green"><Eye size={18} /></span><div><small>PAGE</small><strong>{pageData ? `${pageData.totalPages === 0 ? 0 : page + 1} / ${pageData.totalPages}` : '—'}</strong></div></article>
      </section>

      <section className="card table-card">
        <div className="table-toolbar">
          <div className="table-title"><h2>Client directory</h2><span>{pageData?.totalElements ?? 0} records</span></div>
          <button className="button secondary compact-button" type="button" disabled={!pageData?.content?.length} onClick={() => downloadCsv(pageData.content)}><Download size={15} /> Export page</button>
        </div>
        <div className="filters-row">
          <label className="search-field"><Search size={16} /><input aria-label="Search clients" placeholder="Search name, email, company, or phone" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} /></label>
          <select aria-label="Filter clients by status" className="select-field" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
            <option value="">All statuses</option><option value="ACTIVE">Active</option><option value="PROSPECT">Prospect</option><option value="INACTIVE">Inactive</option>
          </select>
          <input className="select-field assigned-filter" aria-label="Filter clients by representative" placeholder="Representative username" value={assignedTo} onChange={(event) => { setAssignedTo(event.target.value); setPage(0); }} />
          <select aria-label="Sort clients" className="select-field" value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}>
            <option value="updatedAt">Recently updated</option><option value="createdAt">Recently created</option><option value="fullName">Name A–Z</option><option value="status">Status</option>
          </select>
        </div>
        <ErrorBanner message={error} />
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={20} /> Loading client records…</div>
          : pageData?.content?.length ? (
            <div className="table-scroll"><table className="data-table">
              <thead><tr><th>Client</th><th>Type</th><th>Status</th><th>Assigned to</th><th>Updated</th><th><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>{pageData.content.map((client) => (
                <tr key={client.id}>
                  <td><div className="person-cell"><span className="person-avatar">{client.fullName.slice(0, 1).toUpperCase()}</span><span><strong>{client.fullName}</strong><small>{client.companyName || client.email || client.phone || 'Contact details not provided'}</small></span></div></td>
                  <td>{client.type === 'INVESTOR' ? 'Investor' : 'Client'}</td>
                  <td><span className={`status-badge status-${client.status.toLowerCase()}`}>{client.status.toLowerCase()}</span></td>
                  <td>{client.assignedTo || 'Unassigned'}</td>
                  <td>{displayDate(client.updatedAt)}</td>
                  <td><div className="row-actions">
                    <button className="icon-button" type="button" aria-label={`View ${client.fullName}`} onClick={() => openDetails(client)}><Eye size={16} /></button>
                    <button className="icon-button" type="button" aria-label={`Edit ${client.fullName}`} onClick={() => openEdit(client)}><Pencil size={15} /></button>
                    <button className="icon-button danger-icon" type="button" aria-label={`Archive ${client.fullName}`} onClick={() => archiveClient(client)}><Archive size={15} /></button>
                  </div></td>
                </tr>
              ))}</tbody>
            </table></div>
          ) : <div className="empty-state"><span className="empty-icon"><UsersRound size={21} /></span><strong>{query || status ? 'No matching records' : 'Your client list is ready'}</strong><p>{query || status ? 'Try a different search or clear the status filter.' : 'Add your first client or investor to start building relationships.'}</p>{!query && !status && <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> Add a record</button>}</div>}
        {pageData?.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {pageData.totalPages}</span><div><button className="button secondary icon-text" type="button" aria-label="Previous page" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeft size={16} /> Previous</button><button className="button secondary icon-text" type="button" aria-label="Next page" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage(page + 1)}>Next <ChevronRight size={16} /></button></div></div>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="client-dialog-title">
          <header className="modal-header"><div><h2 id="client-dialog-title">{editing ? 'Edit client record' : 'Add client or investor'}</h2><p>Contact information is stored securely in your Apex Finodeal LLP workspace.</p></div><button className="icon-button" type="button" aria-label="Close dialog" onClick={() => setDialogOpen(false)}><X size={18} /></button></header>
          <form onSubmit={saveClient}>
            <div className="form-grid">
              <label className="field"><span>Full name *</span><input required maxLength="120" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></label>
              <label className="field"><span>Record type *</span><select required value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}><option value="CLIENT">Client</option><option value="INVESTOR">Investor</option></select></label>
              <label className="field"><span>Email</span><input type="email" maxLength="255" value={form.email || ''} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
              <label className="field"><span>Phone</span><input type="tel" maxLength="30" value={form.phone || ''} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></label>
              <label className="field"><span>Company / organization</span><input maxLength="160" value={form.companyName || ''} onChange={(event) => setForm({ ...form, companyName: event.target.value })} /></label>
              <label className="field"><span>Status *</span><select required value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="ACTIVE">Active</option><option value="PROSPECT">Prospect</option><option value="INACTIVE">Inactive</option></select></label>
              <label className="field"><span>Lead source</span><input maxLength="80" value={form.leadSource || ''} onChange={(event) => setForm({ ...form, leadSource: event.target.value })} /></label>
              <label className="field"><span>Assigned representative</span><input maxLength="80" value={form.assignedTo || ''} onChange={(event) => setForm({ ...form, assignedTo: event.target.value })} /></label>
              <label className="field full-field"><span>Notes</span><textarea rows="3" maxLength="2000" value={form.notes || ''} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></label>
            </div>
            <button className="button secondary duplicate-button" type="button" onClick={checkDuplicates} disabled={checkingDuplicates}>{checkingDuplicates ? <LoaderCircle className="spin" size={15} /> : <Search size={15} />} Check possible duplicates</button>
            {duplicates.length > 0 && <div className="duplicate-results"><strong>Possible matches</strong>{duplicates.map((duplicate) => <span key={duplicate.id}>{duplicate.fullName} · {duplicate.email || duplicate.phone}</span>)}</div>}
            {duplicates.length === 0 && (form.email || form.phone) && <span className="duplicate-clear">No matching email or phone records found.</span>}
            <ErrorBanner message={formError} />
            <footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDialogOpen(false)}>Cancel</button><button className="button primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Create record'}</button></footer>
          </form>
        </section>
      </div>}

      {detail && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDetail(null); }}>
        <section className="modal-panel detail-panel" role="dialog" aria-modal="true" aria-labelledby="client-detail-title">
          <header className="modal-header"><div><span className="eyebrow">CLIENT RECORD · #{detail.id}</span><h2 id="client-detail-title">{detail.fullName}</h2><p>{detail.companyName || 'Individual relationship'}</p></div><button className="icon-button" type="button" aria-label="Close details" onClick={() => setDetail(null)}><X size={18} /></button></header>
          <div className="detail-grid"><div><small>TYPE</small><strong>{detail.type}</strong></div><div><small>STATUS</small><strong>{detail.status}</strong></div><div><small>EMAIL</small><strong>{detail.email || 'Not provided'}</strong></div><div><small>PHONE</small><strong>{detail.phone || 'Not provided'}</strong></div><div><small>ASSIGNED TO</small><strong>{detail.assignedTo || 'Unassigned'}</strong></div><div><small>LEAD SOURCE</small><strong>{detail.leadSource || 'Not recorded'}</strong></div><div className="full-field"><small>NOTES</small><p>{detail.notes || 'No notes have been added.'}</p></div></div>
          <footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDetail(null)}>Close</button></footer>
        </section>
      </div>}
    </div>
  );
}
