import { ref } from 'vue'
import { useCompanies, type CompanyMembership, type OnboardingStatus } from './useCompanies'

const status = ref<OnboardingStatus | null>(null)
const loading = ref(false)

export function useCompanyOnboarding() {
  const { getOnboardingStatus } = useCompanies()

  async function refresh(): Promise<OnboardingStatus> {
    loading.value = true
    try {
      const result = await getOnboardingStatus()
      status.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  function invalidate() {
    status.value = null
  }

  /** A company the user administers that still needs onboarding (plan or profile). */
  function pendingCompany(current: OnboardingStatus): CompanyMembership | null {
    return current.companies.find((c) => c.role === 'ADMIN' && c.onboardingStatus !== 'COMPLETE') ?? null
  }

  /** The first fully onboarded company — used as "the" active company for the dashboard. */
  function activeCompany(current: OnboardingStatus): CompanyMembership | null {
    return current.companies.find((c) => c.onboardingStatus === 'COMPLETE') ?? null
  }

  return { status, loading, refresh, invalidate, pendingCompany, activeCompany }
}
