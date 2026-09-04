import { ref } from 'vue'
import { useProjects, type OnboardingStatus } from './useProjects'

const status = ref<OnboardingStatus | null>(null)
const loading = ref(false)

export function useProjectOnboarding() {
  const { getOnboardingStatus } = useProjects()

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

  return { status, loading, refresh, invalidate }
}
