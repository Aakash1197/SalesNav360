import { useEffect, useState } from 'react';
import { Activity, CircleAlert, LoaderCircle, RefreshCw, TrendingUp } from 'lucide-react';
import { crmAPI } from '../services/api';

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);
}

function Metric({ label, value, detail }) {
  return <article className="card analytics-metric"><small>{label}</small><strong>{value}</strong><span>{detail}</span></article>;
}

export default function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadSummary() {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.analyticsSummary();
      setData(response.data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Analytics could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadSummary(); }, []);

  const stageEntries = Object.entries(data?.leadStages || {});
  const maxStageCount = Math.max(1, ...stageEntries.map(([, count]) => count));

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div><span className="eyebrow">WORKSPACE INSIGHTS</span><h1>Analytics</h1><p>Live pipeline, team workload, policy renewals, and quotation performance.</p></div>
        <button className="button secondary" type="button" onClick={loadSummary} disabled={loading}><RefreshCw size={15} /> Refresh</button>
      </section>

      {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
      {loading && !data ? <div className="table-state" role="status"><LoaderCircle className="spin" size={18} /> Loading analytics…</div> : data && <>
        <section className="analytics-metric-grid" aria-label="Sales analytics">
          <Metric label="TOTAL CLIENTS" value={data.totalClients} detail="Active and archived records" />
          <Metric label="LEAD CONVERSION" value={`${data.leadConversionRate}%`} detail={`${data.wonDeals} won of ${data.totalLeads} leads`} />
          <Metric label="ACTIVE PIPELINE" value={currency(data.activePipelineValue)} detail={`${data.openLeads} open leads`} />
          <Metric label="CLOSED-WON REVENUE" value={currency(data.closedWonRevenue)} detail="Expected values of won leads" />
          <Metric label="TASK COMPLETION" value={`${data.taskCompletionRate}%`} detail={`${data.completedTasks} completed · ${data.activeTasks} active`} />
          <Metric label="ACTIVE POLICIES" value={data.activePolicies} detail={`${data.renewalsNext30Days} renewals in the next 30 days`} />
          <Metric label="QUOTATION CONVERSION" value={`${data.quotationConversionRate}%`} detail={`${data.acceptedQuotations} accepted of ${data.totalQuotations}`} />
          <Metric label="ACCEPTED QUOTE VALUE" value={currency(data.acceptedQuotationValue)} detail="Accepted proposals only" />
        </section>

        <section className="card analytics-chart-card">
          <div className="card-heading"><div><h2><Activity size={17} /> Lead pipeline</h2><p>Distribution of leads by current stage.</p></div></div>
          {stageEntries.length ? <div className="analytics-stage-list">{stageEntries.map(([stage, count]) => (
            <div className="analytics-stage-row" key={stage}>
              <span>{stage.toLowerCase().replaceAll('_', ' ')}</span>
              <div className="analytics-stage-track"><span style={{ width: `${(count / maxStageCount) * 100}%` }} /></div>
              <strong>{count}</strong>
            </div>
          ))}</div> : <div className="empty-state compact"><span className="empty-icon"><TrendingUp size={19} /></span><strong>No lead data yet</strong><p>Lead stage insights will appear when your team adds leads.</p></div>}
        </section>
      </>}
    </div>
  );
}
