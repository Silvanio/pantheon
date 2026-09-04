import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useProjectOnboarding } from '../composables/useProjectOnboarding'
import LoginView from '../views/LoginView.vue'
import OAuthCallbackView from '../views/OAuthCallbackView.vue'
import DashboardView from '../views/DashboardView.vue'
import OnboardingView from '../views/OnboardingView.vue'
import ProjectRegistrationView from '../views/ProjectRegistrationView.vue'
import PlanSelectionView from '../views/PlanSelectionView.vue'
import InvitationView from '../views/InvitationView.vue'
import DailyReportHistoryView from '../views/DailyReportHistoryView.vue'
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
    { path: '/onboarding', name: 'onboarding', component: OnboardingView, meta: { requiresAuth: true } },
    { path: '/projects/new', name: 'project-new', component: ProjectRegistrationView, meta: { requiresAuth: true } },
    { path: '/plans', name: 'plan-selection', component: PlanSelectionView, meta: { requiresAuth: true } },
    {
      path: '/construction-sites/:siteId/daily-reports',
      name: 'daily-report-history',
      component: DailyReportHistoryView,
      meta: { requiresAuth: true },
    },
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

/**
 * Post-login sequencing: once authenticated, the user must have an active project before
 * reaching the dashboard (or any other protected route) — see design.md "Router guard
 * placement". `useProjectOnboarding`'s status is cached for the session and only refetched
 * after project creation/plan confirmation invalidates it.
 */
router.beforeEach(async (to) => {
  const { isAuthenticated } = useAuth()

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
  // The invitation page is reachable from an emailed link in any auth/onboarding state:
  // an unregistered invitee, a logged-in user with no project yet, or an existing member.
  if (to.name === 'invitation') {
    return true
  }

  const { status, refresh } = useProjectOnboarding()
  const current = status.value ?? (await refresh())

  if (!current.hasProject) {
    return to.name === 'onboarding' || to.name === 'project-new' ? true : { name: 'onboarding' }
  }

  if (current.needsPlanSelection) {
    return to.name === 'plan-selection' ? true : { name: 'plan-selection' }
  }

  if (to.name === 'onboarding' || to.name === 'project-new' || to.name === 'plan-selection') {
    return { name: 'dashboard' }
  }

  return true
})

export default router
