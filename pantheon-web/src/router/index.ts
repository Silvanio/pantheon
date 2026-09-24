import { createRouter, createWebHistory } from 'vue-router'
import { HttpError, useAuth } from '../composables/useAuth'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import LoginView from '../views/LoginView.vue'
import OAuthCallbackView from '../views/OAuthCallbackView.vue'
import DashboardView from '../views/DashboardView.vue'
import CompanyCreationView from '../views/CompanyCreationView.vue'
import PlanSelectionView from '../views/PlanSelectionView.vue'
import CompanyProfileView from '../views/CompanyProfileView.vue'
import CompanySettingsView from '../views/CompanySettingsView.vue'
import SiteDetailView from '../views/SiteDetailView.vue'
import InvitationView from '../views/InvitationView.vue'
import DailyReportDetailView from '../views/DailyReportDetailView.vue'
import OrcamentoDetailView from '../views/OrcamentoDetailView.vue'
import PurchaseRequestDetailView from '../views/PurchaseRequestDetailView.vue'
import GlobalTasksBoardView from '../views/GlobalTasksBoardView.vue'
import UserProfileView from '../views/UserProfileView.vue'
import AdminCompaniesView from '../views/AdminCompaniesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/oauth2/callback', name: 'oauth2-callback', component: OAuthCallbackView },
    { path: '/invitations/:token', name: 'invitation', component: InvitationView },
    { path: '/companies/new', name: 'company-new', component: CompanyCreationView, meta: { requiresAuth: true } },
    {
      path: '/companies/:companyId/plan',
      name: 'company-plan',
      component: PlanSelectionView,
      meta: { requiresAuth: true },
    },
    {
      path: '/companies/:companyId/profile',
      name: 'company-profile',
      component: CompanyProfileView,
      meta: { requiresAuth: true },
    },
    {
      path: '/companies/:companyId/settings',
      name: 'company-settings',
      component: CompanySettingsView,
      meta: { requiresAuth: true },
    },
    { path: '/sites/:siteId', name: 'site-detail', component: SiteDetailView, meta: { requiresAuth: true } },
    {
      path: '/daily-reports/:id',
      name: 'daily-report-detail',
      component: DailyReportDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/orcamentos/:id',
      name: 'orcamento-detail',
      component: OrcamentoDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/purchase-requests/:id',
      name: 'purchase-request-detail',
      component: PurchaseRequestDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/companies/:companyId/tasks-board',
      name: 'global-tasks-board',
      component: GlobalTasksBoardView,
      meta: { requiresAuth: true },
    },
    { path: '/profile', name: 'user-profile', component: UserProfileView, meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin-companies', component: AdminCompaniesView, meta: { requiresAuth: true } },
  ],
})

const onboardingRouteNames = new Set(['company-new', 'company-plan', 'company-profile'])

/**
 * Post-login sequencing: account -> mandatory plan selection -> company profile -> dashboard.
 * See company-onboarding's "Post-login routing guard order". `useCompanyOnboarding`'s status is
 * cached for the session and only refetched after company creation/plan/profile invalidates it.
 */
router.beforeEach(async (to) => {
  const { isAuthenticated, isSuperAdmin, logout } = useAuth()

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { name: 'login' }
  }
  if (!isAuthenticated.value) {
    return true
  }
  if (to.name === 'login') {
    return { name: isSuperAdmin.value ? 'admin-companies' : 'dashboard' }
  }
  if (to.name === 'oauth2-callback') {
    return true
  }
  // The invitation page is reachable from an emailed link in any auth/onboarding state.
  if (to.name === 'invitation') {
    return true
  }

  // A superadmin belongs to no company of their own (see PlatformAdminService's bypass —
  // every company/site membership gate is skipped for them server-side too), so none of the
  // onboarding-status logic below applies. Their home is the cross-tenant company picker.
  if (isSuperAdmin.value) {
    if (to.name === 'dashboard') {
      return { name: 'admin-companies' }
    }
    return true
  }

  const { status, refresh, pendingCompany } = useCompanyOnboarding()
  let current
  try {
    current = status.value ?? (await refresh())
  } catch (error) {
    if (error instanceof HttpError && error.status === 401) {
      logout()
      return { name: 'login' }
    }
    throw error
  }

  if (!current.hasCompany) {
    // A user with only site membership(s) — e.g. a client or an outside architect/engineer —
    // has no company of their own and shouldn't be forced through company onboarding. With
    // exactly one obra, send them straight into it; with more than one (possibly spanning
    // different companies), send them to the dashboard's site-only view instead of an
    // arbitrary first obra. `siteIds` is a reliable "how many obras can this user reach" count
    // ONLY for this branch — it comes from their own SiteMembership rows, which is exactly how
    // a site-only member's access works.
    if (current.siteIds.length === 0) {
      return to.name === 'company-new' ? true : { name: 'company-new' }
    }
    if (to.name === 'dashboard' && current.siteIds.length === 1) {
      return { name: 'site-detail', params: { siteId: current.siteIds[0] } }
    }
    return true
  }

  // A company-staff user (admin or member) always lands on the dashboard, regardless of how
  // many obras the company has. Unlike a site-only member, their access isn't defined by
  // SiteMembership rows — company staff can reach every obra of their company whether or not
  // they hold an explicit SiteMembership on each one — so `siteIds.length` does not represent
  // "how many obras this user has" for them, and must never drive an auto-redirect here.
  const pending = pendingCompany(current)
  if (pending) {
    const targetName = pending.onboardingStatus === 'PLAN_PENDING' ? 'company-plan' : 'company-profile'
    if (to.name === targetName && to.params.companyId === pending.companyId) {
      return true
    }
    return { name: targetName, params: { companyId: pending.companyId } }
  }

  if (onboardingRouteNames.has(to.name as string)) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
