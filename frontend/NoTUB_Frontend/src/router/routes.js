const routes = [
  {
    path: '/',
    redirect: '/signin'
  },
  {
    path: '/signin',
    component: () => import('layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'signin', component: () => import('pages/SignInPage.vue') }
    ]
  },
  {
    path: '/signup',
    component: () => import('layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'signup', component: () => import('pages/SignUpPage.vue') }
    ]
  },
  {
    path: '/forgot-password',
    component: () => import('layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'forgot-password', component: () => import('pages/ForgotPasswordPage.vue') }
    ]
  },
  {
    path: '/reset-password',
    component: () => import('layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'reset-password', component: () => import('pages/ResetPasswordPage.vue') }
    ]
  },
  {
    path: '/home',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('pages/HomePage.vue'), meta: { activeTab: 'home' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/trips',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'trips', component: () => import('pages/TripsPage.vue'), meta: { activeTab: 'trips' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/scan',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'scan', component: () => import('pages/QRScannerPage.vue'), meta: { activeTab: 'scan' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/tickets',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'tickets', component: () => import('pages/TicketsPage.vue'), meta: { activeTab: 'tickets' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/account',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'account', component: () => import('pages/EditAccountPage.vue'), meta: { activeTab: 'account' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/traveling',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'traveling', component: () => import('pages/TravelingPage.vue'), meta: { activeTab: 'scan' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/terms',
    name: 'terms',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/TermsPage.vue'), meta: { activeTab: 'account' } }
    ],
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('pages/admin/AdminDashboardPage.vue') },
      { path: 'users', name: 'admin-users', component: () => import('pages/admin/AdminUsersPage.vue') },
      { path: 'vehicles', name: 'admin-vehicles', component: () => import('pages/admin/AdminVehiclesPage.vue') },
      { path: 'tariffs', name: 'admin-tariffs', component: () => import('pages/admin/AdminTariffsPage.vue') },
      { path: 'zones', name: 'admin-zones', component: () => import('pages/admin/AdminZonesPage.vue') },
      { path: 'trips', name: 'admin-trips', component: () => import('pages/admin/AdminTripsPage.vue') },
      { path: 'network', name: 'admin-network', component: () => import('pages/admin/AdminNetworkPage.vue') }
    ]
  },
  {
    path: '/oauth2/redirect',
    name: 'oauth2-redirect',
    component: () => import('pages/OAuth2Redirect.vue')
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
