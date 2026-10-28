import { useCallback, useEffect, useState } from 'react';
import { CircleAlert, LoaderCircle, Plus, ShieldCheck, UserPlus } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { adminAPI } from '../services/api';

const EMPTY_USER = { username: '', email: '', password: '', roles: [] };

function messageFrom(error, fallback) {
  return error.response?.data?.message || error.response?.data?.detail || fallback;
}

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState(null);
  const [roles, setRoles] = useState([]);
  const [events, setEvents] = useState([]);
  const [query, setQuery] = useState('');
  const [userForm, setUserForm] = useState(EMPTY_USER);
  const [editingUser, setEditingUser] = useState(null);
  const [editingRoles, setEditingRoles] = useState([]);
  const [newRole, setNewRole] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadAdminData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [usersResponse, rolesResponse, auditResponse] = await Promise.all([
        adminAPI.users({ query: query.trim() || undefined, page: 0, size: 50 }),
        adminAPI.roles(),
        adminAPI.auditEvents({ page: 0, size: 10 }),
      ]);
      setUsers(usersResponse.data);
      setRoles(rolesResponse.data);
      setEvents(auditResponse.data.content);
    } catch (requestError) {
      setError(messageFrom(requestError, 'Administration data could not be loaded.'));
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => { loadAdminData(); }, [loadAdminData]);

  function toggleRole(role) {
    setUserForm((form) => ({
      ...form,
      roles: form.roles.includes(role) ? form.roles.filter((item) => item !== role) : [...form.roles, role],
    }));
  }

  async function createRole(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await adminAPI.createRole(newRole);
      setNewRole('');
      setNotice('Role created.');
      await loadAdminData();
    } catch (requestError) {
      setError(messageFrom(requestError, 'Role could not be created.'));
    } finally {
      setSaving(false);
    }
  }

  async function createUser(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await adminAPI.createUser(userForm);
      setUserForm(EMPTY_USER);
      setNotice('User created.');
      await loadAdminData();
    } catch (requestError) {
      setError(messageFrom(requestError, 'User could not be created.'));
    } finally {
      setSaving(false);
    }
  }

  async function toggleUserEnabled(account) {
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await adminAPI.updateUser(account.id, {
        enabled: !account.enabled,
        roles: account.roles,
      });
      setNotice(`${account.username} ${account.enabled ? 'deactivated' : 'activated'}.`);
      await loadAdminData();
    } catch (requestError) {
      setError(messageFrom(requestError, 'User access could not be updated.'));
    } finally {
      setSaving(false);
    }
  }

  function openRoleEditor(account) {
    setEditingUser(account);
    setEditingRoles(account.roles);
  }

  async function saveRoles(event) {
    event.preventDefault();
    if (!editingUser || !editingRoles.length) return;
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await adminAPI.updateUser(editingUser.id, {
        enabled: editingUser.enabled,
        roles: editingRoles,
      });
      setNotice(`Roles updated for ${editingUser.username}.`);
      setEditingUser(null);
      await loadAdminData();
    } catch (requestError) {
      setError(messageFrom(requestError, 'User roles could not be updated.'));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page-stack">
      <section className="page-heading">
        <div>
          <span className="eyebrow">ADMINISTRATION</span>
          <h1>User management</h1>
          <p>Manage Apex Finodeal LLP accounts, permitted roles, and administrative activity.</p>
        </div>
        <ShieldCheck size={23} />
      </section>

      {error && <div className="inline-error" role="alert"><CircleAlert size={16} />{error}</div>}
      {notice && <div className="notification-followups" role="status">{notice}</div>}

      <div className="filters-row">
        <label className="search-field">
          <span className="sr-only">Search users</span>
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search username or email" />
        </label>
      </div>

      <section className="table-card">
        <div className="table-toolbar">
          <div className="table-title"><h2>Users</h2><span>{users?.totalElements ?? 0}</span></div>
          {loading && <LoaderCircle className="spin" size={17} aria-label="Loading users" />}
        </div>
        {!users?.content?.length && !loading ? (
          <div className="empty-state compact"><strong>No users found</strong><p>Create a user account or adjust the search.</p></div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>User</th><th>Roles</th><th>Status</th><th>Created</th><th>Action</th></tr></thead>
              <tbody>
                {(users?.content || []).map((account) => {
                  const isSelf = account.username.toLowerCase() === currentUser?.username?.toLowerCase();
                  return <tr key={account.id}>
                    <td><div className="person-cell"><span className="person-avatar">{account.username.slice(0, 1).toUpperCase()}</span><span><strong>{account.username}</strong><small>{account.email}</small></span></div></td>
                    <td>{account.roles.join(', ')}</td>
                    <td><span className={`status-badge ${account.enabled ? 'status-active' : 'status-inactive'}`}>{account.enabled ? 'Active' : 'Inactive'}</span></td>
                    <td>{new Date(account.createdAt).toLocaleDateString()}</td>
                    <td><div className="row-actions">
                      <button className="button secondary" type="button" disabled={saving || isSelf} title={isSelf ? 'You cannot deactivate or edit your own account here.' : ''} onClick={() => openRoleEditor(account)}>Roles</button>
                      <button className="button secondary" type="button" disabled={saving || isSelf} title={isSelf ? 'You cannot deactivate or edit your own account here.' : ''} onClick={() => toggleUserEnabled(account)}>{account.enabled ? 'Deactivate' : 'Activate'}</button>
                    </div></td>
                  </tr>;
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="dashboard-bottom-grid">
        <section className="table-card">
          <div className="table-toolbar"><div className="table-title"><UserPlus size={17} /><h2>Create user</h2></div></div>
          <form onSubmit={createUser}>
            <div className="form-grid">
              <label className="field"><span>Username</span><input required maxLength="80" pattern="[A-Za-z0-9._-]+" value={userForm.username} onChange={(event) => setUserForm({ ...userForm, username: event.target.value })} /></label>
              <label className="field"><span>Email</span><input required type="email" maxLength="255" value={userForm.email} onChange={(event) => setUserForm({ ...userForm, email: event.target.value })} /></label>
              <label className="field full-field"><span>Temporary password</span><input required type="password" minLength="12" maxLength="72" value={userForm.password} onChange={(event) => setUserForm({ ...userForm, password: event.target.value })} /><small>At least 12 characters. Share it securely with the user.</small></label>
            </div>
            <fieldset className="field">
              <legend>Assign roles</legend>
              {roles.length ? roles.map((role) => <label key={role} className="role-choice"><input type="checkbox" checked={userForm.roles.includes(role)} onChange={() => toggleRole(role)} />{role}</label>) : <small>Create a role before adding a user.</small>}
            </fieldset>
            <button className="button primary" type="submit" disabled={saving || !roles.length || !userForm.roles.length}><Plus size={15} /> Create user</button>
          </form>
        </section>

        <section className="table-card">
          <div className="table-toolbar"><div className="table-title"><ShieldCheck size={17} /><h2>Permitted roles</h2></div></div>
          <p className="muted">The bootstrap ADMIN role is preserved. New role names are restricted to the supported role catalog.</p>
          <form onSubmit={createRole}>
            <label className="field"><span>Role name</span>
              <select required value={newRole} onChange={(event) => setNewRole(event.target.value)}>
                <option value="">Select a supported role</option>
                {['SALES_REPRESENTATIVE', 'SALES_MANAGER', 'OPERATIONS', 'READ_ONLY'].filter((role) => !roles.includes(role)).map((role) => <option key={role} value={role}>{role}</option>)}
              </select>
            </label>
            <button className="button secondary" type="submit" disabled={saving || !newRole}><Plus size={15} /> Add role</button>
          </form>
          <div className="role-list">{roles.map((role) => <span className="stage-pill" key={role}>{role}</span>)}</div>
        </section>
      </div>

      <section className="table-card">
        <div className="table-toolbar"><div className="table-title"><h2>Recent audit activity</h2><span>Latest 10</span></div></div>
        {events.length ? <div className="table-scroll"><table className="data-table">
          <thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Record</th><th>Summary</th></tr></thead>
          <tbody>{events.map((event) => <tr key={event.id}><td>{new Date(event.createdAt).toLocaleString()}</td><td>{event.actor}</td><td>{event.action}</td><td>{event.entityType} #{event.entityId}</td><td>{event.summary}</td></tr>)}</tbody>
        </table></div> : <div className="empty-state compact"><strong>No audit activity</strong><p>Administrative and CRM actions will appear here.</p></div>}
      </section>

      {editingUser && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) setEditingUser(null);
      }}>
        <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="role-editor-title">
          <div className="modal-header">
            <div><h2 id="role-editor-title">Manage roles</h2><p>{editingUser.username}</p></div>
            <button className="icon-button" type="button" aria-label="Close role editor" onClick={() => setEditingUser(null)}>×</button>
          </div>
          <form onSubmit={saveRoles}>
            <fieldset className="field">
              <legend>Assigned roles</legend>
              {roles.map((role) => <label key={role} className="role-choice">
                <input type="checkbox" checked={editingRoles.includes(role)} onChange={() => setEditingRoles((current) => current.includes(role)
                  ? current.filter((value) => value !== role)
                  : [...current, role])} />
                {role}
              </label>)}
            </fieldset>
            <div className="modal-footer">
              <button className="button secondary" type="button" disabled={saving} onClick={() => setEditingUser(null)}>Cancel</button>
              <button className="button primary" type="submit" disabled={saving || !editingRoles.length}>{saving ? 'Saving…' : 'Save roles'}</button>
            </div>
          </form>
        </section>
      </div>}
    </div>
  );
}
