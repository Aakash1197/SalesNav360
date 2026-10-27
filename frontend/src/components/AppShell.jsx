import { useEffect, useRef, useState } from 'react';
import {
  ArrowUpRight,
  Bell,
  ChevronDown,
  CircleHelp,
  Command,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Search,
  Sun,
  UsersRound,
  UserCog,
  CheckSquare,
  Shield,
  FileText,
  X,
} from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { crmAPI } from '../services/api';

const THEME_KEY = 'salesnav360-theme';

export default function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [dark, setDark] = useState(() => window.localStorage.getItem(THEME_KEY) === 'dark');
  const [menuOpen, setMenuOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationData, setNotificationData] = useState(null);
  const [notificationError, setNotificationError] = useState('');
  const searchInputRef = useRef(null);

  useEffect(() => {
    const onUnauthorized = () => navigate('/login', { replace: true });
    const onShortcut = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    window.addEventListener('salesnav360:unauthorized', onUnauthorized);
    window.addEventListener('keydown', onShortcut);
    return () => {
      window.removeEventListener('salesnav360:unauthorized', onUnauthorized);
      window.removeEventListener('keydown', onShortcut);
    };
  }, [navigate]);

  const toggleTheme = () => {
    const nextTheme = dark ? 'light' : 'dark';
    setDark(!dark);
    window.localStorage.setItem(THEME_KEY, nextTheme);
  };

  const submitSearch = (event) => {
    event.preventDefault();
    const query = search.trim();
    if (query) navigate(`/clients?query=${encodeURIComponent(query)}`);
  };

  const toggleNotifications = async () => {
    const opening = !notificationsOpen;
    setNotificationsOpen(opening);
    if (!opening || notificationData) return;
    setNotificationError('');
    try {
      const response = await crmAPI.dashboard();
      setNotificationData(response.data);
    } catch (error) {
      setNotificationError(error.response?.status === 401
        ? 'Your session has expired.'
        : 'Recent updates are unavailable right now.');
    }
  };

  const signOut = async () => {
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch (error) {
      console.error('SalesNav360 logout request failed', error);
      navigate('/login', { replace: true });
    }
  };

  const initials = user?.username?.slice(0, 1).toUpperCase() || 'U';
  const role = user?.roles?.[0]?.replaceAll('_', ' ') || 'User';

  return (
    <div className={`application ${dark ? 'theme-dark' : ''}`}>
      <header className="topbar">
        <button className="icon-button mobile-menu" type="button" aria-label="Open navigation" onClick={() => setMobileNavOpen(!mobileNavOpen)}>
          {mobileNavOpen ? <X size={19} /> : <Menu size={19} />}
        </button>
        <button className="brand-lockup" type="button" onClick={() => navigate('/')} aria-label="SalesNav360 dashboard">
          <span className="brand-mark"><Command size={19} /></span>
          <span className="brand-name">SalesNav<span>360</span></span>
          <span className="brand-subtitle">Financial Services &amp; Insurance CRM</span>
        </button>

        <div className="topbar-right">
          <form className="global-search" onSubmit={submitSearch}>
            <Search size={15} />
            <input
              ref={searchInputRef}
              aria-label="Search clients"
              placeholder="Search clients and investors"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <kbd>Ctrl K</kbd>
          </form>
          <button className="icon-button notification-button" type="button" aria-label="Recent updates" aria-expanded={notificationsOpen} onClick={toggleNotifications}>
            <Bell size={18} />
          </button>
          {notificationsOpen && <div className="notification-popover">
            <div className="notification-heading"><strong>Recent updates</strong><button className="icon-button" type="button" aria-label="Close recent updates" onClick={() => setNotificationsOpen(false)}><X size={15} /></button></div>
            {notificationError ? <p className="notification-empty">{notificationError}</p> : !notificationData ? <p className="notification-empty">Loading recent updates…</p> : <>
              <div className="notification-followups"><span className="status-dot" />{notificationData.followUpsDueToday} open lead follow-ups due today</div>
              {notificationData.recentActivity?.length ? notificationData.recentActivity.slice(0, 5).map((item) => <div className="notification-item" key={item.id}><strong>{item.actor}</strong><span>{item.summary}</span><small>{new Date(item.createdAt).toLocaleString()}</small></div>) : <p className="notification-empty">No recent activity has been recorded.</p>}
            </>}
          </div>}
          <button className="icon-button" type="button" onClick={toggleTheme} aria-label={`Switch to ${dark ? 'light' : 'dark'} mode`}>
            {dark ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          <div className="profile-wrap">
            <button className="profile-trigger" type="button" onClick={() => setMenuOpen(!menuOpen)} aria-expanded={menuOpen}>
              <span className="avatar">{initials}</span>
              <span className="profile-copy"><strong>{user?.username}</strong><small>{role}</small></span>
              <ChevronDown size={15} />
            </button>
            {menuOpen && (
              <div className="profile-menu">
                <div className="profile-menu-user"><strong>{user?.username}</strong><small>{user?.email}</small></div>
                <button type="button" onClick={signOut}><LogOut size={15} /> Sign out</button>
              </div>
            )}
          </div>
        </div>
      </header>

      <div className="app-body">
        <aside className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}>
          <div className="sidebar-brand">
            <span className="brand-mark small"><Command size={17} /></span>
            <span><strong>SalesNav</strong><small>FINANCIAL SERVICES CRM</small></span>
          </div>
          <nav aria-label="Main navigation">
            <p className="nav-caption">WORKSPACE</p>
            <NavLink to="/" end onClick={() => setMobileNavOpen(false)}><LayoutDashboard size={17} /> Dashboard</NavLink>
            <NavLink to="/clients" onClick={() => setMobileNavOpen(false)}><UsersRound size={17} /> Clients &amp; Investors</NavLink>
            <NavLink to="/leads" onClick={() => setMobileNavOpen(false)}><ArrowUpRight size={17} /> Leads &amp; Pipeline</NavLink>
            <NavLink to="/tasks" onClick={() => setMobileNavOpen(false)}><CheckSquare size={17} /> Tasks &amp; follow-ups</NavLink>
            {user?.roles?.some((roleName) => ['ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS'].includes(roleName)) && <NavLink to="/policies" onClick={() => setMobileNavOpen(false)}><Shield size={17} /> Policies &amp; portfolios</NavLink>}
            {user?.roles?.some((roleName) => ['ADMIN', 'SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS'].includes(roleName)) && <NavLink to="/quotations" onClick={() => setMobileNavOpen(false)}><FileText size={17} /> Quotations</NavLink>}
            {user?.roles?.includes('ADMIN') && <NavLink to="/admin/users" onClick={() => setMobileNavOpen(false)}><UserCog size={17} /> Administration</NavLink>}
          </nav>
          <div className="sidebar-bottom">
            <div className="help-link"><CircleHelp size={16} /><span>Sales workspace</span></div>
            <div className="sidebar-user"><span className="avatar">{initials}</span><span><strong>{user?.username}</strong><small>{role}</small></span></div>
          </div>
        </aside>
        {mobileNavOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setMobileNavOpen(false)} />}
        <main className="main-content"><Outlet /></main>
      </div>
    </div>
  );
}
