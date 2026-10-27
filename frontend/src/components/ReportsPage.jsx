import { useEffect, useState } from 'react';
import { ChartColumnIncreasing, CircleAlert, Download, LoaderCircle, RefreshCw } from 'lucide-react';
import { crmAPI } from '../services/api';

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);
}

const REPORTS = [
  { key: 'leadConversionRate', title: 'Lead conversion', description: 'Won leads compared with all tracked leads.', format: (data) => `${data.leadConversionRate}% · ${data.wonDeals}/${data.totalLeads}` },
  { key: 'activePipelineValue', title: 'Sales pipeline', description: 'Expected value across open pipeline stages.', format: (data) => currency(data.activePipelineValue) },
  { key: 'closedWonRevenue', title: 'Won revenue', description: 'Expected revenue on leads marked won.', format: (data) => currency(data.closedWonRevenue) },
  { key: 'taskCompletionRate', title: 'Task completion', description: 'Completed tasks compared with active tasks.', format: (data) => `${data.taskCompletionRate}% · ${data.completedTasks} completed` },
  { key: 'renewalsNext30Days', title: 'Policy renewals', description: 'Active policy renewals due in the next 30 days.', format: (data) => `${data.renewalsNext30Days} renewals` },
  { key: 'quotationConversionRate', title: 'Quotation conversion', description: 'Accepted quotations compared with all proposals.', format: (data) => `${data.quotationConversionRate}% · ${data.acceptedQuotations}/${data.totalQuotations}` },
];

export default function ReportsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadReport() {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.reportSummary();
      setData(response.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Reports could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadReport(); }, []);

  function downloadCsv() {
    if (!data) return;
    const rows = [
      ['Report', 'Result', 'Details'],
      ...REPORTS.map((report) => [report.title, report.format(data), report.description]),
    ];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(',')).join('\r\n');
    const file = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(file);
    link.download = 'salesnav360-report.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">PERFORMANCE REPORTS</span><h1>Reports</h1><p>Review lead, revenue, task, policy, and quotation performance.</p></div>
        <div className="quick-action-buttons"><button className="button secondary" type="button" onClick={loadReport} disabled={loading}><RefreshCw size={15} /> Refresh</button><button className="button primary" type="button" onClick={downloadCsv} disabled={!data}><Download size={15} /> Export CSV</button></div>
      </section>
      {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
      {loading && !data ? <div className="table-state" role="status"><LoaderCircle className="spin" size={18} /> Loading reports…</div> : data && <>
        <section className="card reports-summary">
          <span className="section-title-icon"><ChartColumnIncreasing size={20} /></span>
          <div><h2>Workspace performance</h2><p>Current aggregate results from recorded CRM activity. Policy renewal figures cover the next 30 days.</p></div>
          <span className="updated-pill">Live data</span>
        </section>
        <section className="report-grid">
          {REPORTS.map((report) => <article className="card report-card" key={report.key}><small>{report.title}</small><strong>{report.format(data)}</strong><p>{report.description}</p></article>)}
        </section>
        <section className="card report-note"><strong>Reporting note</strong><p>Revenue values use expected values on leads marked Won and accepted quotation totals. They are separate measures and are not added together.</p></section>
      </>}
    </div>
  );
}
