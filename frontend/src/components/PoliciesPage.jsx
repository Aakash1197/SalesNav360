import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, LoaderCircle, Plus, Search, Shield, X } from 'lucide-react';
import { crmAPI } from '../services/api';

const STATUSES = ['PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'];
const EMPTY_FORM = {
  policyNumber: '',
  policyType: '',
  clientId: '',
  provider: '',
  premiumAmount: '',
  coverageAmount: '',
  startDate: '',
  expiryDate: '',
  renewalDate: '',
  status: 'PENDING',
  assignedTo: '',
};

function label(value) {
  return value?.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) || '—';
}

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(value || 0);
}

export default function PoliciesPage() {
  const [pageData, setPageData] = useState(null);
  const [portfolio, setPortfolio] = useState(null);
  const [clients, setClients] = useState([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const loadPolicies = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [policyResponse, clientResponse, portfolioResponse] = await Promise.all([
        crmAPI.policies({
          query: query.trim() || undefined,
          status: status || undefined,
          page,
          size: 10,
          sort: 'renewalDate',
          direction: 'ASC',
        }),
        crmAPI.clients({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
        crmAPI.portfolioSummary(),
      ]);
      setPageData(policyResponse.data);
      setPortfolio(portfolioResponse.data);
      setClients(clientResponse.data.content.filter((client) => client.status !== 'ARCHIVED'));
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Policies could not be loaded. Check access and API connectivity.');
    } finally {
      setLoading(false);
    }
  }, [page, query, status]);

  useEffect(() => { loadPolicies(); }, [loadPolicies]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  }

  async function openEdit(policy) {
    try {
      const response = await crmAPI.policy(policy.id);
      setEditing(response.data);
      setForm({
        ...EMPTY_FORM,
        ...response.data,
        clientId: response.data.clientId.toString(),
        premiumAmount: response.data.premiumAmount.toString(),
        coverageAmount: response.data.coverageAmount.toString(),
        startDate: response.data.startDate,
        expiryDate: response.data.expiryDate,
        renewalDate: response.data.renewalDate || '',
      });
      setDialogOpen(true);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Policy details could not be loaded.');
    }
  }

  async function savePolicy(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = {
      ...form,
      clientId: Number(form.clientId),
      premiumAmount: form.premiumAmount,
      coverageAmount: form.coverageAmount,
      renewalDate: form.renewalDate || null,
      assignedTo: form.assignedTo || null,
    };
    try {
      if (editing) await crmAPI.updatePolicy(editing.id, payload);
      else await crmAPI.createPolicy(payload);
      setDialogOpen(false);
      setNotice(editing ? 'Policy updated.' : 'Policy created.');
      await loadPolicies();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The policy could not be saved. Check its dates and client.');
    } finally {
      setSaving(false);
    }
  }

  const policies = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">CLIENT PORTFOLIOS</span><h1>Policies &amp; portfolios</h1><p>Keep client policies, premiums, coverage, and renewal dates connected.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> Add policy</button>
      </section>

      <section className="portfolio-summary-grid">
        <article className="card lead-summary"><small>TOTAL POLICIES</small><strong>{portfolio?.totalPolicies ?? '—'}</strong></article>
        <article className="card lead-summary"><small>ACTIVE POLICIES</small><strong>{portfolio?.activePolicies ?? '—'}</strong></article>
        <article className="card lead-summary"><small>RENEWALS NEXT 30 DAYS</small><strong>{portfolio?.renewalsNext30Days ?? '—'}</strong></article>
        <article className="card lead-summary"><small>ACTIVE PREMIUM VALUE</small><strong>{portfolio ? currency(portfolio.activePremiumValue) : '—'}</strong></article>
      </section>

      <section className="table-card">
        <div className="filters-row">
          <label className="search-field"><Search size={15} /><input aria-label="Search policies" placeholder="Search number, type, provider, or client" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} /></label>
          <select className="select-field" aria-label="Filter policies by status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">All statuses</option>{STATUSES.map((item) => <option value={item} key={item}>{label(item)}</option>)}</select>
        </div>
        {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
        {notice && <div className="notification-followups" role="status">{notice}</div>}
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={18} /> Loading policies…</div>
          : !policies.length ? <div className="empty-state"><span className="empty-icon"><Shield size={20} /></span><strong>{query || status ? 'No matching policies' : 'No policies yet'}</strong><p>Policies are linked to active client or investor records.</p><button className="button primary" type="button" onClick={openCreate}><Plus size={15} /> Add policy</button></div>
            : <div className="table-scroll"><table className="data-table">
              <thead><tr><th>Policy</th><th>Client</th><th>Provider</th><th>Premium</th><th>Coverage</th><th>Renewal</th><th>Status</th><th>Owner</th></tr></thead>
              <tbody>{policies.map((policy) => <tr key={policy.id}>
                <td><button className="table-link" type="button" onClick={() => openEdit(policy)}><strong>{policy.policyNumber}</strong><small>{label(policy.policyType)}</small></button></td>
                <td>{policy.clientName}</td><td>{policy.provider}</td><td>{currency(policy.premiumAmount)}</td><td>{currency(policy.coverageAmount)}</td><td>{policy.renewalDate || policy.expiryDate}</td>
                <td><span className={`status-badge ${policy.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}`}>{label(policy.status)}</span></td><td>{policy.assignedTo || 'Unassigned'}</td>
              </tr>)}</tbody>
            </table></div>}
        {pageData?.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {pageData.totalPages}</span><div><button className="button secondary" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="button secondary" type="button" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage(page + 1)}>Next</button></div></div>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="policy-dialog-title">
          <header className="modal-header"><div><h2 id="policy-dialog-title">{editing ? 'Update policy' : 'Create a policy'}</h2><p>Policy records require an existing active client or investor.</p></div><button className="icon-button" type="button" aria-label="Close policy dialog" onClick={() => setDialogOpen(false)}><X size={18} /></button></header>
          <form onSubmit={savePolicy}>
            <div className="form-grid">
              <label className="field"><span>Policy number *</span><input required maxLength="80" value={form.policyNumber} onChange={(event) => setForm({ ...form, policyNumber: event.target.value })} /></label>
              <label className="field"><span>Policy type *</span><input required maxLength="80" value={form.policyType} onChange={(event) => setForm({ ...form, policyType: event.target.value })} /></label>
              <label className="field full-field"><span>Client *</span><select required value={form.clientId} onChange={(event) => setForm({ ...form, clientId: event.target.value })}><option value="">Select a client or investor</option>{clients.map((client) => <option value={client.id} key={client.id}>{client.fullName}{client.companyName ? ` — ${client.companyName}` : ''}</option>)}</select></label>
              <label className="field"><span>Provider / insurer *</span><input required maxLength="160" value={form.provider} onChange={(event) => setForm({ ...form, provider: event.target.value })} /></label>
              <label className="field"><span>Status *</span><select required value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>{STATUSES.map((item) => <option value={item} key={item}>{label(item)}</option>)}</select></label>
              <label className="field"><span>Premium amount *</span><input required type="number" min="0" step="0.01" value={form.premiumAmount} onChange={(event) => setForm({ ...form, premiumAmount: event.target.value })} /></label>
              <label className="field"><span>Coverage amount *</span><input required type="number" min="0" step="0.01" value={form.coverageAmount} onChange={(event) => setForm({ ...form, coverageAmount: event.target.value })} /></label>
              <label className="field"><span>Start date *</span><input required type="date" value={form.startDate} onChange={(event) => setForm({ ...form, startDate: event.target.value })} /></label>
              <label className="field"><span>Expiry date *</span><input required type="date" value={form.expiryDate} onChange={(event) => setForm({ ...form, expiryDate: event.target.value })} /></label>
              <label className="field"><span>Renewal date</span><input type="date" value={form.renewalDate} onChange={(event) => setForm({ ...form, renewalDate: event.target.value })} /></label>
              <label className="field"><span>Assigned representative</span><input maxLength="80" value={form.assignedTo || ''} onChange={(event) => setForm({ ...form, assignedTo: event.target.value })} /></label>
            </div>
            <footer className="modal-footer"><button className="button secondary" type="button" disabled={saving} onClick={() => setDialogOpen(false)}>Cancel</button><button className="button primary" type="submit" disabled={saving || !clients.length}>{saving ? 'Saving…' : editing ? 'Save changes' : 'Create policy'}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}
