import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8082/api/v1/salesnav',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

api.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase();
  if (!['post', 'put', 'patch', 'delete'].includes(method)) {
    return config;
  }

  const response = await api.get('/auth/csrf');
  config.headers['X-XSRF-TOKEN'] = response.data.token;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.endsWith('/auth/login')) {
      window.dispatchEvent(new Event('salesnav360:unauthorized'));
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  logout: () => api.post('/auth/logout'),
  currentUser: () => api.get('/auth/me'),
};

export const crmAPI = {
  dashboard: () => api.get('/dashboard'),
  clients: (params) => api.get('/clients', { params }),
  client: (id) => api.get(`/clients/${id}`),
  createClient: (client) => api.post('/clients', client),
  updateClient: (id, client) => api.put(`/clients/${id}`, client),
  archiveClient: (id) => api.delete(`/clients/${id}`),
  clientDuplicates: (params) => api.get('/clients/duplicates', { params }),
  leads: (params) => api.get('/leads', { params }),
  createLead: (lead) => api.post('/leads', lead),
  updateLead: (id, lead) => api.put(`/leads/${id}`, lead),
  moveLead: (id, stage) => api.patch(`/leads/${id}/stage`, { stage }),
  convertLead: (id) => api.post(`/leads/${id}/convert`),
  tasks: (params) => api.get('/tasks', { params }),
  task: (id) => api.get(`/tasks/${id}`),
  createTask: (task) => api.post('/tasks', task),
  updateTask: (id, task) => api.put(`/tasks/${id}`, task),
  changeTaskStatus: (id, status) => api.patch(`/tasks/${id}/status`, { status }),
  policies: (params) => api.get('/policies', { params }),
  policy: (id) => api.get(`/policies/${id}`),
  createPolicy: (policy) => api.post('/policies', policy),
  updatePolicy: (id, policy) => api.put(`/policies/${id}`, policy),
  quotations: (params) => api.get('/quotations', { params }),
  quotation: (id) => api.get(`/quotations/${id}`),
  quotationPdf: (id) => api.get(`/quotations/${id}/pdf`, { responseType: 'blob' }),
  createQuotation: (quotation) => api.post('/quotations', quotation),
  updateQuotation: (id, quotation) => api.put(`/quotations/${id}`, quotation),
  changeQuotationStatus: (id, status) => api.patch(`/quotations/${id}/status`, { status }),
};

export const adminAPI = {
  users: (params) => api.get('/admin/users', { params }),
  createUser: (user) => api.post('/admin/users', user),
  updateUser: (id, user) => api.patch(`/admin/users/${id}`, user),
  roles: () => api.get('/admin/roles'),
  createRole: (name) => api.post('/admin/roles', { name }),
  auditEvents: (params) => api.get('/admin/audit-events', { params }),
};

export default api;
