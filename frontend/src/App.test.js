import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import App from './App';

jest.mock('./services/api', () => ({
  authAPI: {
    currentUser: jest.fn(),
    login: jest.fn(),
    logout: jest.fn(),
  },
  crmAPI: {
    dashboard: jest.fn(),
    clients: jest.fn(),
    client: jest.fn(),
    createClient: jest.fn(),
    updateClient: jest.fn(),
    archiveClient: jest.fn(),
    clientDuplicates: jest.fn(),
    leads: jest.fn(),
    createLead: jest.fn(),
    updateLead: jest.fn(),
    moveLead: jest.fn(),
    convertLead: jest.fn(),
    tasks: jest.fn(),
    task: jest.fn(),
    createTask: jest.fn(),
    updateTask: jest.fn(),
    changeTaskStatus: jest.fn(),
    policies: jest.fn(),
    policy: jest.fn(),
    createPolicy: jest.fn(),
    updatePolicy: jest.fn(),
    portfolioSummary: jest.fn(),
    calendarEvents: jest.fn(),
    createCalendarEvent: jest.fn(),
    updateCalendarEvent: jest.fn(),
    deleteCalendarEvent: jest.fn(),
    emailTemplates: jest.fn(),
    createEmailTemplate: jest.fn(),
    updateEmailTemplate: jest.fn(),
    communications: jest.fn(),
    logCommunication: jest.fn(),
    analyticsSummary: jest.fn(),
    reportSummary: jest.fn(),
    quotations: jest.fn(),
    quotation: jest.fn(),
    createQuotation: jest.fn(),
    updateQuotation: jest.fn(),
    changeQuotationStatus: jest.fn(),
  },
  adminAPI: {
    users: jest.fn(),
    createUser: jest.fn(),
    updateUser: jest.fn(),
    roles: jest.fn(),
    createRole: jest.fn(),
    auditEvents: jest.fn(),
  },
}));

const clientRecord = {
  id: 7,
  fullName: 'Apex Finance',
  email: 'contact@apex.example',
  phone: '+91 55500 1100',
  companyName: 'Apex Financial Services',
  type: 'CLIENT',
  status: 'ACTIVE',
  leadSource: 'Referral',
  assignedTo: 'crm-admin',
  notes: '',
  createdAt: '2026-10-20T10:00:00Z',
  updatedAt: '2026-10-22T10:00:00Z',
};

function renderApp(path = '/') {
  return render(
    <MemoryRouter
      initialEntries={[path]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>
  );
}

test('shows the public landing page and login link when there is no active session', async () => {
  const { authAPI } = require('./services/api');
  authAPI.currentUser.mockRejectedValue(new Error('No active session'));

  renderApp();

  expect(await screen.findByRole('heading', { name: /loan rejected\? let's explore your options\./i })).toBeInTheDocument();
  const header = screen.getByRole('banner');
  expect(within(header).getByRole('link', { name: /login/i })).toHaveAttribute('href', '/login');
  expect(within(header).getByRole('link', { name: 'Home' })).toHaveAttribute('href', '#home');
  expect(within(header).getByRole('link', { name: 'About Us' })).toHaveAttribute('href', '#about');
  expect(within(header).getByRole('link', { name: 'Services' })).toHaveAttribute('href', '#services');
  expect(within(header).getByRole('link', { name: 'Contact Us' })).toHaveAttribute('href', '#contact');
  const contact = document.getElementById('contact');
  expect(screen.getByRole('heading', { name: /make the next move/i })).toBeInTheDocument();
  expect(screen.getByText(/A rejected loan application doesn't mean your options end here/i)).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /explore services/i })).toHaveAttribute('href', '#services');
  expect(screen.getByRole('heading', { name: 'Loan rejection assistance' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Credit profile guidance' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Loan eligibility assessment' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Alternative loan options' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Financial consultation' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Stay in control' })).toBeInTheDocument();
  expect(document.querySelectorAll('.public-steps article')).toHaveLength(4);
  expect(screen.getByRole('heading', { name: /questions after a loan decision/i })).toBeInTheDocument();
  const firstFaq = screen.getByText('Why might a loan application be rejected?');
  fireEvent.click(firstFaq);
  expect(firstFaq.closest('details')).toHaveAttribute('open');
  expect(screen.getByText(/Lenders may consider income/)).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Privacy policy' })).toHaveAttribute('href', '#privacy');
  expect(screen.getByRole('link', { name: 'Terms of use' })).toHaveAttribute('href', '#terms');
  expect(within(contact).getByRole('form')).toBeInTheDocument();
  expect(within(contact).getByRole('link', { name: /jayinfotech20@gmail\.com/i })).toHaveAttribute('href', 'mailto:jayinfotech20@gmail.com');
  expect(within(contact).getByRole('link', { name: /whatsapp/i })).toHaveAttribute('href', 'https://wa.me/918669178491');
  expect(within(contact).getByRole('link', { name: /instagram/i })).toHaveAttribute('href', 'https://www.instagram.com/jit_computer_laptop/');
  expect(within(contact).getByRole('link', { name: /\+91 8669178491/ })).toHaveAttribute('href', 'tel:+918669178491');
  expect(within(contact).getByTitle('Jay Info Tech location in Solapur')).toHaveAttribute('src', expect.stringContaining('google.com/maps/embed'));
  expect(within(contact).getByText(/Bhawani Path, Madal Wasti, Solapur - 413002/)).toBeInTheDocument();

  fireEvent.click(within(header).getByRole('link', { name: /login/i }));
  expect(await screen.findByRole('heading', { name: 'Sign in to continue' })).toBeInTheDocument();
});

test('keeps the selected tab when an API request returns unauthorized but the session is valid', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({
    data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
  });
  crmAPI.clients.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0 } });

  renderApp('/clients');
  expect(await screen.findByRole('heading', { name: 'Clients & Investors' })).toBeInTheDocument();

  fireEvent(window, new Event('salesnav360:unauthorized'));

  await waitFor(() => expect(authAPI.currentUser).toHaveBeenCalledTimes(2));
  expect(screen.getByRole('heading', { name: 'Clients & Investors' })).toBeInTheDocument();
});

test('redirects to login when an unauthorized API request confirms the session expired', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser
    .mockResolvedValueOnce({
      data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
    })
    .mockRejectedValueOnce({ response: { status: 401 } });
  crmAPI.clients.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0 } });

  renderApp('/clients');
  expect(await screen.findByRole('heading', { name: 'Clients & Investors' })).toBeInTheDocument();

  fireEvent(window, new Event('salesnav360:unauthorized'));

  expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument();
});

test('shows a clear message when the login credentials are rejected', async () => {
  const { authAPI } = require('./services/api');
  authAPI.currentUser.mockRejectedValueOnce(new Error('No active session'));
  authAPI.login.mockRejectedValue({ response: { status: 401 } });

  renderApp('/login');

  expect(await screen.findByRole('heading', { name: 'Sign in to continue' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /back to home/i })).toHaveAttribute('href', '/');
  expect(screen.getByRole('link', { name: /return to homepage/i })).toHaveAttribute('href', '/');
  expect(document.querySelector('.login-story-image')).toHaveAttribute('src', '/loan-advisor-background.png');
  fireEvent.change(await screen.findByLabelText('Username'), { target: { value: 'admin' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'incorrect-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Username or password is incorrect, or this account is disabled.'
  );
});

test('renders the dashboard using metrics returned by the CRM API', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] } });
  crmAPI.dashboard.mockResolvedValue({
    data: {
      totalClients: 2,
      totalInvestors: 1,
      totalLeads: 4,
      openLeads: 3,
      wonDeals: 1,
      followUpsDueToday: 2,
      conversionRate: 25,
      closedWonRevenue: 12000,
      averageDealSize: 12000,
      activePipelineValue: 34000,
      activeTasks: 2,
      completedTasks: 1,
      overdueTasks: 0,
      upcomingTasks: [],
      leadStages: { NEW: 1, CONTACTED: 1, QUALIFIED: 1, PROPOSAL: 0, NEGOTIATION: 0, WON: 1, LOST: 0 },
      recentActivity: [],
    },
  });

  renderApp('/dashboard');

  expect(await screen.findByRole('heading', { name: 'Welcome, crm-admin' })).toBeInTheDocument();
  expect(screen.getByText('Clients & investors')).toBeInTheDocument();
  expect(screen.getByText(/34,000/)).toBeInTheDocument();
  expect(crmAPI.dashboard).toHaveBeenCalledTimes(1);
});

test('renders analytics from the analytics API', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', roles: ['ADMIN'] } });
  crmAPI.analyticsSummary.mockResolvedValue({ data: {
    totalClients: 3,
    totalLeads: 5,
    openLeads: 2,
    wonDeals: 1,
    leadConversionRate: 20,
    activePipelineValue: 15000,
    closedWonRevenue: 5000,
    activeTasks: 2,
    completedTasks: 3,
    taskCompletionRate: 60,
    activePolicies: 2,
    renewalsNext30Days: 1,
    acceptedQuotations: 1,
    totalQuotations: 2,
    quotationConversionRate: 50,
    acceptedQuotationValue: 2500,
    leadStages: { NEW: 2, QUALIFIED: 2, WON: 1 },
  } });

  renderApp('/analytics');

  expect(await screen.findByRole('heading', { name: 'Analytics' })).toBeInTheDocument();
  expect(await screen.findByText('LEAD CONVERSION')).toBeInTheDocument();
  expect(await screen.findByText('new')).toBeInTheDocument();
});

test('renders the calendar screen and loads the selected month events', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', roles: ['ADMIN'] } });
  crmAPI.calendarEvents.mockResolvedValue({ data: { content: [] } });
  crmAPI.clients.mockResolvedValue({ data: { content: [] } });

  renderApp('/calendar');

  expect(await screen.findByRole('heading', { name: 'Calendar' })).toBeInTheDocument();
  expect(await screen.findByText('No events this month')).toBeInTheDocument();
  expect(crmAPI.calendarEvents).toHaveBeenCalledTimes(1);
});

test('shows email templates and history without implying email delivery', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', roles: ['ADMIN'] } });
  crmAPI.emailTemplates.mockResolvedValue({ data: { content: [] } });
  crmAPI.communications.mockResolvedValue({ data: { content: [] } });
  crmAPI.clients.mockResolvedValue({ data: { content: [] } });
  crmAPI.leads.mockResolvedValue({ data: { content: [] } });

  renderApp('/email');

  expect(await screen.findByRole('heading', { name: 'Email' })).toBeInTheDocument();
  expect(screen.getByText(/Email delivery is not configured/)).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'Email templates' })).toBeInTheDocument();
});

test('renders reports from the report API', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', roles: ['ADMIN'] } });
  crmAPI.reportSummary.mockResolvedValue({ data: {
    totalClients: 1, totalLeads: 0, openLeads: 0, wonDeals: 0, leadConversionRate: 0,
    activePipelineValue: 0, closedWonRevenue: 0, activeTasks: 0, completedTasks: 0,
    taskCompletionRate: 0, activePolicies: 0, renewalsNext30Days: 0,
    acceptedQuotations: 0, totalQuotations: 0, quotationConversionRate: 0,
    acceptedQuotationValue: 0, leadStages: {},
  } });

  renderApp('/reports');

  expect(await screen.findByRole('heading', { name: 'Reports' })).toBeInTheDocument();
  expect(await screen.findByText('Lead conversion')).toBeInTheDocument();
});

test('creates a client from the clients page and refreshes the list', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({ data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] } });
  crmAPI.clients.mockResolvedValue({
    data: { content: [clientRecord], totalElements: 1, totalPages: 1, number: 0 },
  });
  crmAPI.createClient.mockResolvedValue({ data: { ...clientRecord, id: 8 } });

  renderApp('/clients');
  expect(await screen.findByText('Apex Finance')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Add client' }));
  fireEvent.change(screen.getByLabelText('Full name *'), { target: { value: 'Northstar Investor' } });
  fireEvent.change(screen.getByLabelText('Record type *'), { target: { value: 'INVESTOR' } });
  fireEvent.click(screen.getByRole('button', { name: 'Create record' }));

  await waitFor(() => expect(crmAPI.createClient).toHaveBeenCalledTimes(1));
  expect(crmAPI.createClient).toHaveBeenCalledWith(expect.objectContaining({
    fullName: 'Northstar Investor',
    type: 'INVESTOR',
  }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  expect(crmAPI.clients).toHaveBeenCalledTimes(2);
});

test('shows administration data only on the admin route for an admin user', async () => {
  const { authAPI, adminAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({
    data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
  });
  adminAPI.users.mockResolvedValue({
    data: {
      content: [{
        id: 1,
        username: 'crm-admin',
        email: 'admin@example.test',
        roles: ['ADMIN'],
        enabled: true,
        createdAt: '2026-10-20T10:00:00Z',
      }, {
        id: 2,
        username: 'sales-rep',
        email: 'rep@example.test',
        roles: ['READ_ONLY'],
        enabled: true,
        createdAt: '2026-10-21T10:00:00Z',
      }],
      totalElements: 2,
    },
  });
  adminAPI.roles.mockResolvedValue({ data: ['ADMIN', 'READ_ONLY', 'SALES_REPRESENTATIVE'] });
  adminAPI.auditEvents.mockResolvedValue({ data: { content: [] } });

  renderApp('/admin/users');

  expect(await screen.findByRole('heading', { name: 'User management' })).toBeInTheDocument();
  expect((await screen.findAllByText('crm-admin')).length).toBeGreaterThan(0);
  expect(adminAPI.users).toHaveBeenCalledTimes(1);

  fireEvent.click(screen.getAllByRole('button', { name: 'Roles' })[1]);
  const roleDialog = await screen.findByRole('dialog', { name: 'Manage roles' });
  fireEvent.click(within(roleDialog).getByLabelText('SALES_REPRESENTATIVE'));
  fireEvent.click(within(roleDialog).getByRole('button', { name: 'Save roles' }));

  await waitFor(() => expect(adminAPI.updateUser).toHaveBeenCalledWith(2, {
    enabled: true,
    roles: ['READ_ONLY', 'SALES_REPRESENTATIVE'],
  }));
});

test('creates a task using the task API', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({
    data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
  });
  crmAPI.tasks.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0 } });
  crmAPI.createTask.mockResolvedValue({ data: { id: 21, status: 'OPEN' } });

  renderApp('/tasks');
  expect(await screen.findByRole('heading', { name: 'Tasks & follow-ups' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Add task' }));
  fireEvent.change(screen.getByLabelText('Title *'), { target: { value: 'Review policy renewal' } });
  fireEvent.click(screen.getByRole('button', { name: 'Create task' }));

  await waitFor(() => expect(crmAPI.createTask).toHaveBeenCalledWith(expect.objectContaining({
    title: 'Review policy renewal',
    priority: 'MEDIUM',
    relatedEntityType: null,
    relatedEntityId: null,
  })));
});

test('creates a policy linked to an existing client', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({
    data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
  });
  crmAPI.policies.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0 } });
  crmAPI.clients.mockResolvedValue({
    data: { content: [clientRecord], totalElements: 1, totalPages: 1 },
  });
  crmAPI.portfolioSummary.mockResolvedValue({ data: {
    totalPolicies: 0, activePolicies: 0, pendingPolicies: 0, expiredPolicies: 0,
    renewalsNext30Days: 0, activePremiumValue: 0,
  } });
  crmAPI.createPolicy.mockResolvedValue({ data: { id: 31 } });

  renderApp('/policies');
  expect(await screen.findByRole('heading', { name: 'Policies & portfolios' })).toBeInTheDocument();
  fireEvent.click(screen.getAllByRole('button', { name: 'Add policy' })[0]);
  fireEvent.change(screen.getByLabelText('Policy number *'), { target: { value: 'POL-TEST-1' } });
  fireEvent.change(screen.getByLabelText('Policy type *'), { target: { value: 'Life' } });
  fireEvent.change(screen.getByLabelText('Client *'), { target: { value: '7' } });
  fireEvent.change(screen.getByLabelText('Provider / insurer *'), { target: { value: 'Example Insurer' } });
  fireEvent.change(screen.getByLabelText('Premium amount *'), { target: { value: '100.25' } });
  fireEvent.change(screen.getByLabelText('Coverage amount *'), { target: { value: '10000' } });
  fireEvent.change(screen.getByLabelText('Start date *'), { target: { value: '2030-01-01' } });
  fireEvent.change(screen.getByLabelText('Expiry date *'), { target: { value: '2031-01-01' } });
  fireEvent.click(screen.getByRole('button', { name: 'Create policy' }));

  await waitFor(() => expect(crmAPI.createPolicy).toHaveBeenCalledWith(expect.objectContaining({
    policyNumber: 'POL-TEST-1',
    clientId: 7,
    premiumAmount: '100.25',
    coverageAmount: '10000',
  })));
});

test('creates a quotation draft with line items and decimal inputs', async () => {
  const { authAPI, crmAPI } = require('./services/api');
  authAPI.currentUser.mockResolvedValue({
    data: { username: 'crm-admin', email: 'admin@example.test', roles: ['ADMIN'] },
  });
  crmAPI.quotations.mockResolvedValue({ data: { content: [], totalElements: 0, totalPages: 0 } });
  crmAPI.clients.mockResolvedValue({ data: { content: [clientRecord], totalElements: 1 } });
  crmAPI.leads.mockResolvedValue({ data: { content: [], totalElements: 0 } });
  crmAPI.createQuotation.mockResolvedValue({ data: { id: 41, status: 'DRAFT' } });

  renderApp('/quotations');
  expect(await screen.findByRole('heading', { name: 'Quotations' })).toBeInTheDocument();
  fireEvent.click(screen.getAllByRole('button', { name: 'New quotation' })[0]);
  fireEvent.change(screen.getByLabelText('Client *'), { target: { value: '7' } });
  fireEvent.change(screen.getByLabelText('Tax (%)'), { target: { value: '18' } });
  fireEvent.change(screen.getByLabelText('Description *'), { target: { value: 'Annual service' } });
  fireEvent.change(screen.getByLabelText('Unit price *'), { target: { value: '10.10' } });
  fireEvent.change(screen.getByLabelText('Quantity *'), { target: { value: '2' } });
  fireEvent.click(screen.getByRole('button', { name: 'Create draft' }));

  await waitFor(() => expect(crmAPI.createQuotation).toHaveBeenCalledWith(expect.objectContaining({
    clientId: 7,
    discountPercent: '0',
    taxPercent: '18',
    lineItems: [{ description: 'Annual service', quantity: '2', unitPrice: '10.10' }],
  })));
});
