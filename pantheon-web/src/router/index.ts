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
import MaterialRequestListView from '../views/MaterialRequestListView.vue'
import MaterialRequestDetailView from '../views/MaterialRequestDetailView.vue'

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
      path: '/construction-sites/:siteId/material-requests',
      name: 'material-request-list',
      component: MaterialRequestListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/material-requests/:id',
      name: 'material-request-detail',
      component: MaterialRequestDetailView,
      meta: { requiresAuth: true },
    },
  ],
})

const onboardingRouteNames = new Set(['company-new', 'company-plan', 'company-profile'])

/**
 * Post-login sequencing: account -> mandatory plan selection -> company profile -> dashboard.
 * See company-onboarding's "Post-login routing guard order". `useCompanyOnboarding`'s status is
 * cached for the session and only refetched after company creation/plan/profile invalidates it.
 */
router.beforeEach(async (to) => {
  const { isAuthenticated, logout } = useAuth()

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { name: 'login' }
  }
  if (!isAuthenticated.value) {
    return true
  }
  if (to.name === 'login') {
    return { name: 'dashboard' }
  }
  if (to.name === 'oauth2-callback') {
    return true
  }
  // The invitation page is reachable from an emailed link in any auth/onboarding state.
  if (to.name === 'invitation') {
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
    // has no company of their own and shouldn't be forced through company onboarding; send
    // them straight to one of their obras instead of the (company-scoped) dashboard.
    if (current.siteIds.length > 0) {
      if (to.name === 'dashboard') {
        return { name: 'site-detail', params: { siteId: current.siteIds[0] } }
      }
      return true
    }
    return to.name === 'company-new' ? true : { name: 'company-new' }
  }

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
