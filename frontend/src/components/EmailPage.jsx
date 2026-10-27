import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, Mail, MessageSquareText, Pencil, Plus, X } from 'lucide-react';
import { crmAPI } from '../services/api';

const EMPTY_TEMPLATE = { name: '', subject: '', body: '' };
const EMPTY_LOG = { recipientEmail: '', subject: '', body: '', templateId: '', clientId: '', leadId: '' };

export default function EmailPage() {
  const [templates, setTemplates] = useState([]);
  const [history, setHistory] = useState([]);
  const [clients, setClients] = useState([]);
  const [leads, setLeads] = useState([]);
  const [activeTab, setActiveTab] = useState('history');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [dialog, setDialog] = useState('');
  const [editing, setEditing] = useState(null);
  const [templateForm, setTemplateForm] = useState(EMPTY_TEMPLATE);
  const [logForm, setLogForm] = useState(EMPTY_LOG);

  const loadEmailData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [templateResponse, historyResponse, clientResponse, leadResponse] = await Promise.all([
        crmAPI.emailTemplates({ page: 0, size: 100 }),
        crmAPI.communications({ page: 0, size: 100 }),
        crmAPI.clients({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
        crmAPI.leads({ page: 0, size: 100, sort: 'fullName', direction: 'ASC' }),
      ]);
      setTemplates(templateResponse.data.content || []);
      setHistory(historyResponse.data.content || []);
      setClients((clientResponse.data.content || []).filter((client) => client.status !== 'ARCHIVED'));
      setLeads(leadResponse.data.content || []);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Email workspace data could not be loaded.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadEmailData(); }, [loadEmailData]);

  function openTemplate(template = null) {
    setEditing(template);
    setTemplateForm(template ? { name: template.name, subject: template.subject, body: template.body } : EMPTY_TEMPLATE);
    setDialog('template');
  }

  function chooseTemplate(templateId) {
    const template = templates.find((item) => String(item.id) === templateId);
    setLogForm({
      ...logForm,
      templateId,
      subject: template?.subject || logForm.subject,
      body: template?.body || logForm.body,
    });
  }

  async function saveTemplate(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    try {
      if (editing) await crmAPI.updateEmailTemplate(editing.id, templateForm);
      else await crmAPI.createEmailTemplate(templateForm);
      setDialog('');
      setNotice(editing ? 'Template updated.' : 'Template created.');
      await loadEmailData();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The email template could not be saved.');
    }
  }

  async function logCommunication(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    try {
      await crmAPI.logCommunication({
        ...logForm,
        templateId: logForm.templateId ? Number(logForm.templateId) : null,
        clientId: logForm.clientId ? Number(logForm.clientId) : null,
        leadId: logForm.leadId ? Number(logForm.leadId) : null,
      });
      setDialog('');
      setLogForm(EMPTY_LOG);
      setNotice('Communication recorded in history. No email was sent.');
      await loadEmailData();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'The communication could not be recorded.');
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">CLIENT COMMUNICATIONS</span><h1>Email</h1><p>Manage reusable templates and keep a client communication history.</p></div>
        <button className="button primary" type="button" onClick={() => { setError(''); setLogForm(EMPTY_LOG); setDialog('log'); }}><Plus size={16} /> Log communication</button>
      </section>

      <div className="email-provider-notice" role="note"><Mail size={17} /><span><strong>Email delivery is not configured.</strong> This area manages templates and records communications; it does not send emails.</span></div>
      {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
      {notice && <div className="notification-followups" role="status">{notice}</div>}

      <section className="card email-workspace">
        <div className="email-tabs" role="tablist" aria-label="Email workspace">
          <button type="button" role="tab" aria-selected={activeTab === 'history'} className={activeTab === 'history' ? 'email-tab active' : 'email-tab'} onClick={() => setActiveTab('history')}><MessageSquareText size={15} /> Communication history</button>
          <button type="button" role="tab" aria-selected={activeTab === 'templates'} className={activeTab === 'templates' ? 'email-tab active' : 'email-tab'} onClick={() => setActiveTab('templates')}><Mail size={15} /> Email templates</button>
        </div>

        {activeTab === 'history' ? <div className="email-content">
          <div className="card-heading"><div><h2>Recent communication</h2><p>Records are marked not sent until a delivery integration is configured.</p></div></div>
          {loading ? <div className="table-state">Loading communication history…</div> : history.length ? <div className="table-scroll"><table className="data-table">
            <thead><tr><th>Subject</th><th>Recipient</th><th>Client / Lead</th><th>Status</th><th>Recorded</th><th>Owner</th></tr></thead>
            <tbody>{history.map((item) => <tr key={item.id}><td><strong>{item.subject}</strong></td><td>{item.recipientEmail}</td><td>{item.clientName || item.leadName || '—'}</td><td><span className="status-badge status-inactive">Not sent</span></td><td>{new Date(item.createdAt).toLocaleString()}</td><td>{item.createdBy}</td></tr>)}</tbody>
          </table></div> : <div className="empty-state compact"><span className="empty-icon"><MessageSquareText size={20} /></span><strong>No communications recorded</strong><p>Log a follow-up to keep the customer history complete.</p></div>}
        </div> : <div className="email-content">
          <div className="card-heading"><div><h2>Email templates</h2><p>Reusable draft content for client and lead follow-ups.</p></div><button className="button primary" type="button" onClick={() => openTemplate()}><Plus size={15} /> New template</button></div>
          {loading ? <div className="table-state">Loading templates…</div> : templates.length ? <div className="template-grid">{templates.map((template) => <article className="card email-template-card" key={template.id}>
            <div><span className="status-badge status-prospect">Template</span><button className="icon-button" type="button" aria-label={`Edit ${template.name}`} onClick={() => openTemplate(template)}><Pencil size={15} /></button></div>
            <h3>{template.name}</h3><strong>{template.subject}</strong><p>{template.body}</p><small>Updated {new Date(template.updatedAt).toLocaleDateString()}</small>
          </article>)}</div> : <div className="empty-state compact"><span className="empty-icon"><Mail size={20} /></span><strong>No templates yet</strong><p>Create a template to reuse consistent follow-up messages.</p><button className="button primary" type="button" onClick={() => openTemplate()}><Plus size={15} /> New template</button></div>}
        </div>}
      </section>

      {dialog && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDialog(''); }}>
        {dialog === 'template' ? <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="email-template-title">
          <header className="modal-header"><div><h2 id="email-template-title">{editing ? 'Edit email template' : 'New email template'}</h2><p>Templates are drafts only; they are not sent automatically.</p></div><button className="icon-button" type="button" aria-label="Close dialog" onClick={() => setDialog('')}><X size={17} /></button></header>
          <form onSubmit={saveTemplate}><label className="field"><span>Name *</span><input required maxLength="100" value={templateForm.name} onChange={(event) => setTemplateForm({ ...templateForm, name: event.target.value })} /></label><label className="field"><span>Subject *</span><input required maxLength="255" value={templateForm.subject} onChange={(event) => setTemplateForm({ ...templateForm, subject: event.target.value })} /></label><label className="field"><span>Message *</span><textarea required maxLength="10000" value={templateForm.body} onChange={(event) => setTemplateForm({ ...templateForm, body: event.target.value })} /></label><footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDialog('')}>Cancel</button><button className="button primary" type="submit">Save template</button></footer></form>
        </section> : <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="communication-dialog-title">
          <header className="modal-header"><div><h2 id="communication-dialog-title">Log communication</h2><p>This creates a history record only. It will not send an email.</p></div><button className="icon-button" type="button" aria-label="Close dialog" onClick={() => setDialog('')}><X size={17} /></button></header>
          <form onSubmit={logCommunication}><div className="form-grid">
            <label className="field full-field"><span>Template</span><select value={logForm.templateId} onChange={(event) => chooseTemplate(event.target.value)}><option value="">Compose without a template</option>{templates.map((template) => <option value={template.id} key={template.id}>{template.name}</option>)}</select></label>
            <label className="field"><span>Client / investor</span><select value={logForm.clientId} onChange={(event) => setLogForm({ ...logForm, clientId: event.target.value, leadId: '' })}><option value="">Select client</option>{clients.map((client) => <option key={client.id} value={client.id}>{client.fullName}</option>)}</select></label>
            <label className="field"><span>Lead</span><select value={logForm.leadId} onChange={(event) => setLogForm({ ...logForm, leadId: event.target.value, clientId: '' })}><option value="">Select lead</option>{leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.fullName}</option>)}</select></label>
            <label className="field full-field"><span>Recipient email *</span><input required type="email" maxLength="255" value={logForm.recipientEmail} onChange={(event) => setLogForm({ ...logForm, recipientEmail: event.target.value })} /></label>
            <label className="field full-field"><span>Subject *</span><input required maxLength="255" value={logForm.subject} onChange={(event) => setLogForm({ ...logForm, subject: event.target.value })} /></label>
            <label className="field full-field"><span>Message *</span><textarea required maxLength="10000" value={logForm.body} onChange={(event) => setLogForm({ ...logForm, body: event.target.value })} /></label>
          </div><footer className="modal-footer"><button className="button secondary" type="button" onClick={() => setDialog('')}>Cancel</button><button className="button primary" type="submit">Record communication</button></footer></form>
        </section>}
      </div>}
    </div>
  );
}
