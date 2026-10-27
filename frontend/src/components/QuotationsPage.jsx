import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, Download, FileText, LoaderCircle, Plus, Search, X } from 'lucide-react';
import { crmAPI } from '../services/api';

const STATUSES = ['DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED'];
const TRANSITIONS = {
  DRAFT: ['SENT'],
  SENT: ['ACCEPTED', 'REJECTED', 'EXPIRED'],
  ACCEPTED: [],
  REJECTED: [],
  EXPIRED: [],
};
const EMPTY_LINE = { description: '', quantity: '1', unitPrice: '0.00' };
const EMPTY_FORM = {
  clientId: '',
  leadId: '',
  validUntil: '',
  discountPercent: '0',
  taxPercent: '0',
  lineItems: [{ ...EMPTY_LINE }],
};

function label(value) {
  return value?.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) || '—';
}

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(value || 0);
}

function defaultValidity() {
  const date = new Date();
  date.setDate(date.getDate() + 30);
  return date.toISOString().slice(0, 10);
}

export default function QuotationsPage() {
  const [pageData, setPageData] = useState(null);
  const [clients, setClients] = useState([]);
  const [leads, setLeads] = useState([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ ...EMPTY_FORM, validUntil: defaultValidity() });

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [quotesResponse, clientsResponse, leadsResponse] = await Promise.all([
        crmAPI.quotations({ query: query.trim() || undefined, status: status || undefined, page, size: 10, sort: 'createdAt', direction: 'DESC' }),
        crmAPI.clients({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
        crmAPI.leads({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
      ]);
      setPageData(quotesResponse.data);
      setClients(clientsResponse.data.content.filter((client) => client.status !== 'ARCHIVED'));
      setLeads(leadsResponse.data.content);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Quotations could not be loaded. Check access and API connectivity.');
    } finally {
      setLoading(false);
    }
  }, [page, query, status]);

  useEffect(() => { loadData(); }, [loadData]);

  function openCreate() {
    setEditing(null);
    setForm({ ...EMPTY_FORM, validUntil: defaultValidity() });
    setDialogOpen(true);
  }

  async function openEdit(quotation) {
    if (quotation.status !== 'DRAFT') return;
    try {
      const response = await crmAPI.quotation(quotation.id);
      setEditing(response.data);
      setForm({
        clientId: response.data.clientId.toString(),
        leadId: response.data.leadId?.toString() || '',
        validUntil: response.data.validUntil,
        discountPercent: response.data.discountPercent.toString(),
        taxPercent: response.data.taxPercent.toString(),
        lineItems: response.data.lineItems.map((item) => ({
          description: item.description,
          quantity: item.quantity.toString(),
          unitPrice: item.unitPrice.toString(),
        })),
      });
      setDialogOpen(true);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Quotation details could not be loaded.');
    }
  }

  function updateLine(index, key, value) {
    setForm((current) => ({
      ...current,
      lineItems: current.lineItems.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item),
    }));
  }

  async function saveQuotation(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const payload = {
      ...form,
      clientId: Number(form.clientId),
      leadId: form.leadId ? Number(form.leadId) : null,
      lineItems: form.lineItems.map((item) => ({
        ...item,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
      })),
    };
    try {
      if (editing) await crmAPI.updateQuotation(editing.id, payload);
      else await crmAPI.createQuotation(payload);
      setDialogOpen(false);
      setNotice(editing ? 'Draft quotation updated.' : 'Draft quotation created.');
      await loadData();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The quotation could not be saved. Check its line items and client.');
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(quotation, nextStatus) {
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await crmAPI.changeQuotationStatus(quotation.id, nextStatus);
      setNotice(nextStatus === 'SENT'
        ? 'Quotation status updated to Sent. No email was sent.'
        : `Quotation marked ${label(nextStatus).toLowerCase()}.`);
      await loadData();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The quotation status change was not allowed.');
    } finally {
      setSaving(false);
    }
  }

  async function downloadQuotation(quotation) {
    setDownloadingId(quotation.id);
    setError('');
    setNotice('');
    try {
      const response = await crmAPI.quotationPdf(quotation.id);
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${quotation.quotationNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      setNotice(`Downloaded ${quotation.quotationNumber}.pdf.`);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The quotation PDF could not be downloaded.');
    } finally {
      setDownloadingId(null);
    }
  }

  const quotations = pageData?.content || [];

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">COMMERCIAL PROPOSALS</span><h1>Quotations</h1><p>Create decimal-calculated drafts linked to clients and opportunities.</p></div>
        <button className="button primary" type="button" onClick={openCreate}><Plus size={16} /> New quotation</button>
      </section>

      <section className="lead-summary-grid">
        <article className="card lead-summary"><small>QUOTATIONS IN VIEW</small><strong>{pageData?.totalElements ?? '—'}</strong></article>
        <article className="card lead-summary"><small>DRAFTS IN VIEW</small><strong>{quotations.filter((quote) => quote.status === 'DRAFT').length}</strong></article>
        <article className="card lead-summary"><small>ACCEPTED IN VIEW</small><strong>{quotations.filter((quote) => quote.status === 'ACCEPTED').length}</strong></article>
      </section>

      <section className="table-card">
        <div className="filters-row">
          <label className="search-field"><Search size={15} /><input aria-label="Search quotations" placeholder="Search quotation number or client" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} /></label>
          <select className="select-field" aria-label="Filter quotations by status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">All statuses</option>{STATUSES.map((item) => <option value={item} key={item}>{label(item)}</option>)}</select>
        </div>
        {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
        {notice && <div className="notification-followups" role="status">{notice}</div>}
        {loading ? <div className="table-state" role="status"><LoaderCircle className="spin" size={18} /> Loading quotations…</div>
          : !quotations.length ? <div className="empty-state"><span className="empty-icon"><FileText size={20} /></span><strong>{query || status ? 'No matching quotations' : 'No quotations yet'}</strong><p>A quotation can be created from an active client, with an optional lead association.</p><button className="button primary" type="button" onClick={openCreate}><Plus size={15} /> New quotation</button></div>
            : <div className="table-scroll"><table className="data-table">
              <thead><tr><th>Quotation</th><th>Client</th><th>Valid until</th><th>Subtotal</th><th>Discount</th><th>Tax</th><th>Total</th><th>Status</th><th>Update status</th><th>PDF</th></tr></thead>
              <tbody>{quotations.map((quote) => <tr key={quote.id}>
                <td><button className="table-link" type="button" disabled={quote.status !== 'DRAFT'} onClick={() => openEdit(quote)}><strong>{quote.quotationNumber}</strong><small>{quote.leadId ? `Lead #${quote.leadId}` : 'Client proposal'}</small></button></td>
                <td>{quote.clientName}</td><td>{quote.validUntil}</td><td>{currency(quote.subtotal)}</td><td>{currency(quote.discount)}</td><td>{currency(quote.tax)}</td><td>{currency(quote.grandTotal)}</td>
                <td><span className={`status-badge ${quote.status === 'ACCEPTED' ? 'status-active' : 'status-inactive'}`}>{label(quote.status)}</span></td>
                <td><select className="stage-select" aria-label={`Update status for ${quote.quotationNumber}`} value={quote.status} disabled={saving || !TRANSITIONS[quote.status]?.length} onChange={(event) => changeStatus(quote, event.target.value)}><option value={quote.status}>{label(quote.status)}</option>{(TRANSITIONS[quote.status] || []).map((next) => <option value={next} key={next}>{label(next)}</option>)}</select></td>
                <td><button className="button secondary" type="button" disabled={downloadingId === quote.id} onClick={() => downloadQuotation(quote)}><Download size={14} />{downloadingId === quote.id ? 'Preparing…' : 'Download'}</button></td>
              </tr>)}</tbody>
            </table></div>}
        {pageData?.totalPages > 1 && <div className="pagination"><span>Page {page + 1} of {pageData.totalPages}</span><div><button className="button secondary" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="button secondary" type="button" disabled={page + 1 >= pageData.totalPages} onClick={() => setPage(page + 1)}>Next</button></div></div>}
      </section>

      {dialogOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setDialogOpen(false); }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="quotation-dialog-title">
          <header className="modal-header"><div><h2 id="quotation-dialog-title">{editing ? 'Edit draft quotation' : 'Create quotation'}</h2><p>Totals are calculated and rounded by the backend using decimal-safe amounts.</p></div><button className="icon-button" type="button" aria-label="Close quotation dialog" onClick={() => setDialogOpen(false)}><X size={18} /></button></header>
          <form onSubmit={saveQuotation}>
            <div className="form-grid">
              <label className="field full-field"><span>Client *</span><select required value={form.clientId} onChange={(event) => setForm({ ...form, clientId: event.target.value })}><option value="">Select a client</option>{clients.map((client) => <option value={client.id} key={client.id}>{client.fullName}</option>)}</select></label>
              <label className="field"><span>Opportunity / lead</span><select value={form.leadId} onChange={(event) => setForm({ ...form, leadId: event.target.value })}><option value="">None</option>{leads.map((lead) => <option value={lead.id} key={lead.id}>{lead.fullName} · {label(lead.stage)}</option>)}</select></label>
              <label className="field"><span>Valid until *</span><input required type="date" min={new Date().toISOString().slice(0, 10)} value={form.validUntil} onChange={(event) => setForm({ ...form, validUntil: event.target.value })} /></label>
              <label className="field"><span>Discount (%)</span><input required type="number" min="0" max="100" step="0.01" value={form.discountPercent} onChange={(event) => setForm({ ...form, discountPercent: event.target.value })} /></label>
              <label className="field"><span>Tax (%)</span><input required type="number" min="0" max="100" step="0.01" value={form.taxPercent} onChange={(event) => setForm({ ...form, taxPercent: event.target.value })} /></label>
            </div>
            <div className="quotation-lines">
              <div className="table-toolbar"><div className="table-title"><h3>Line items</h3></div><button className="button secondary" type="button" onClick={() => setForm({ ...form, lineItems: [...form.lineItems, { ...EMPTY_LINE }] })}><Plus size={14} /> Add line</button></div>
              {form.lineItems.map((item, index) => <div className="quotation-line" key={index}>
                <label className="field"><span>Description *</span><input required maxLength="160" value={item.description} onChange={(event) => updateLine(index, 'description', event.target.value)} /></label>
                <label className="field"><span>Quantity *</span><input required type="number" min="0.01" step="0.01" value={item.quantity} onChange={(event) => updateLine(index, 'quantity', event.target.value)} /></label>
                <label className="field"><span>Unit price *</span><input required type="number" min="0" step="0.01" value={item.unitPrice} onChange={(event) => updateLine(index, 'unitPrice', event.target.value)} /></label>
                <button className="icon-button danger-icon" type="button" aria-label={`Remove line ${index + 1}`} disabled={form.lineItems.length <= 1} onClick={() => setForm({ ...form, lineItems: form.lineItems.filter((_, itemIndex) => itemIndex !== index) })}><X size={15} /></button>
              </div>)}
            </div>
            <footer className="modal-footer"><button className="button secondary" type="button" disabled={saving} onClick={() => setDialogOpen(false)}>Cancel</button><button className="button primary" type="submit" disabled={saving || !clients.length}>{saving ? 'Saving…' : editing ? 'Save draft' : 'Create draft'}</button></footer>
          </form>
        </section>
      </div>}
    </div>
  );
}
