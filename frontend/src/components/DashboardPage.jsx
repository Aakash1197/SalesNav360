import { useEffect, useState } from 'react';
import {
  ArrowDownRight,
  ArrowRight,
  ArrowUpRight,
  BriefcaseBusiness,
  CalendarClock,
  CircleDollarSign,
  ContactRound,
  CheckSquare,
  Plus,
  RefreshCw,
  Target,
  UsersRound,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { crmAPI } from '../services/api';

const PIPELINE_STAGES = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];
const STAGE_LABELS = {
  NEW: 'New',
  CONTACTED: 'Contacted',
  QUALIFIED: 'Qualified',
  PROPOSAL: 'Proposal',
  NEGOTIATION: 'Negotiation',
  WON: 'Won',
  LOST: 'Lost',
};

function currency(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value || 0);
}

function MetricCard({ icon: Icon, label, value, detail, tone }) {
  return (
    <article className="metric-card">
      <span className={`metric-icon ${tone}`}><Icon size={19} /></span>
      <div><p>{label}</p><strong>{value}</strong>{detail && <small>{detail}</small>}</div>
    </article>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const loadDashboard = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await crmAPI.dashboard();
      setData(response.data);
    } catch (requestError) {
      setError(requestError.response?.status === 401
        ? 'Your session has expired. Please sign in again.'
        : 'Dashboard data could not be loaded. Check the API connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  if (loading) return <div className="page-loading" role="status">Loading your dashboard…</div>;
  if (error) {
    return (
      <section className="state-panel">
        <h1>Dashboard unavailable</h1><p>{error}</p>
        <button className="button primary" type="button" onClick={loadDashboard}><RefreshCw size={16} /> Try again</button>
      </section>
    );
  }

  const leads = data.totalLeads;
  const won = data.wonDeals;
  const barMaximum = Math.max(1, ...PIPELINE_STAGES.map((stage) => data.leadStages?.[stage] || 0));
  const roleNames = user?.roles?.join(', ').replaceAll('_', ' ') || 'Sales workspace';

  return (
    <div className="page-stack">
      <section className="page-heading dashboard-heading">
        <div><span className="eyebrow">YOUR WORKSPACE · {roleNames}</span><h1>Welcome, {user?.username || 'there'}</h1><p>Here’s a live snapshot of your client relationships and sales pipeline.</p></div>
        <span className="updated-pill"><span className="status-dot" /> Data from your workspace</span>
      </section>

      <section className="quick-actions card">
        <div className="quick-search-copy"><span className="quick-icon"><Target size={18} /></span><div><strong>Keep your pipeline moving</strong><small>Choose a next step to update your workspace.</small></div></div>
        <div className="quick-action-buttons">
          <Link className="button primary" to="/clients?new=1"><Plus size={16} /> Add client</Link>
          <Link className="button secondary" to="/leads?new=1"><Plus size={16} /> Add lead</Link>
          <Link className="button secondary" to="/tasks?new=1"><Plus size={16} /> Add task</Link>
        </div>
      </section>

      <section className="metric-grid" aria-label="CRM metrics">
        <MetricCard icon={UsersRound} label="Clients & investors" value={data.totalClients + data.totalInvestors} detail={`${data.totalClients} clients · ${data.totalInvestors} investors`} tone="blue" />
        <MetricCard icon={ContactRound} label="Total leads" value={leads} detail={`${data.openLeads} in active pipeline`} tone="green" />
        <MetricCard icon={CalendarClock} label="Follow-ups due today" value={data.followUpsDueToday} detail="Open leads with a follow-up today" tone="violet" />
        <MetricCard icon={Target} label="Win conversion" value={`${data.conversionRate}%`} detail={`${won} won of eligible leads`} tone="amber" />
        <MetricCard icon={CheckSquare} label="Active tasks" value={data.activeTasks} detail={`${data.overdueTasks} overdue`} tone="blue" />
        <MetricCard icon={CalendarClock} label="Completed tasks" value={data.completedTasks} detail="Recorded completions" tone="green" />
      </section>

      <section className="revenue-card card">
        <div className="section-title-row">
          <div className="section-title-icon"><CircleDollarSign size={20} /></div>
          <div><h2>Sales pipeline</h2><p>Lead values are based on expected value entered by your team.</p></div>
          <Link className="text-link" to="/leads">View pipeline <ArrowRight size={15} /></Link>
        </div>
        <div className="revenue-summary">
          <div><span>CLOSED-WON VALUE</span><strong>{currency(data.closedWonRevenue)}</strong><small>{won} won {won === 1 ? 'deal' : 'deals'}</small></div>
          <div className="pipeline-value"><span>ACTIVE PIPELINE</span><strong>{currency(data.activePipelineValue)}</strong><small>Open lead expected value</small></div>
          <div className="pipeline-value"><span>AVERAGE WON DEAL</span><strong>{currency(data.averageDealSize)}</strong><small>{won ? 'Average expected value of won leads' : 'Available when a lead is marked won'}</small></div>
        </div>
        <div className="stage-bars" aria-label="Leads by pipeline stage">
          {PIPELINE_STAGES.map((stage) => {
            const count = data.leadStages?.[stage] || 0;
            return <div className="stage-bar-row" key={stage}>
              <span>{STAGE_LABELS[stage]}</span><div className="stage-track"><span className={`stage-fill stage-${stage.toLowerCase()}`} style={{ width: `${Math.max(count ? 5 : 0, (count / barMaximum) * 100)}%` }} /></div><strong>{count}</strong>
            </div>;
          })}
        </div>
      </section>

      <section className="dashboard-bottom-grid">
        <article className="card activity-card">
          <div className="card-heading"><div><h2>Recent activity</h2><p>Recent CRM changes recorded for this workspace.</p></div><button className="icon-button" type="button" aria-label="Refresh activity" onClick={loadDashboard}><RefreshCw size={16} /></button></div>
          {data.recentActivity.length ? (
            <ul className="activity-list">{data.recentActivity.map((activity) => (
              <li key={activity.id}><span className="activity-avatar">{activity.actor.slice(0, 1).toUpperCase()}</span><div><strong>{activity.actor}</strong><p>{activity.summary}</p><time>{new Date(activity.createdAt).toLocaleString()}</time></div></li>
            ))}</ul>
          ) : <div className="empty-state compact"><span className="empty-icon"><BriefcaseBusiness size={20} /></span><strong>No activity recorded yet</strong><p>Client and lead updates will appear here.</p></div>}
        </article>
        <article className="card focus-card">
          <div className="card-heading"><div><h2>Today at a glance</h2><p>Current CRM workload</p></div></div>
          {data.upcomingTasks?.slice(0, 3).map((task) => <div className="focus-row" key={task.id}><span className="focus-icon violet"><CalendarClock size={17} /></span><div><strong>{task.title}</strong><small>{task.dueDate || 'No due date'} · {task.status.replaceAll('_', ' ').toLowerCase()}</small></div></div>)}
          <div className="focus-row"><span className="focus-icon blue"><ContactRound size={17} /></span><div><strong>{leads} leads tracked</strong><small>{data.openLeads} still active</small></div><Link to="/leads" aria-label="View leads"><ArrowUpRight size={16} /></Link></div>
          <div className="focus-row"><span className="focus-icon green"><CalendarClock size={17} /></span><div><strong>{data.followUpsDueToday} follow-ups today</strong><small>Open leads scheduled for today</small></div><Link to="/leads" aria-label="View follow-ups"><ArrowUpRight size={16} /></Link></div>
          <div className="focus-note"><ArrowDownRight size={15} /> Sales quota is not shown until a target is configured.</div>
          <Link className="text-link" to="/tasks">View task list <ArrowRight size={15} /></Link>
        </article>
      </section>
    </div>
  );
}
