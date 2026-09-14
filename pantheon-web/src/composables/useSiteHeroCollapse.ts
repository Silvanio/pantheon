import { ref, watchEffect } from 'vue'

const STORAGE_KEY = 'pantheon-site-hero-collapsed'

function readInitial(): boolean {
  return localStorage.getItem(STORAGE_KEY) === 'true'
}

const collapsed = ref<boolean>(readInitial())

watchEffect(() => {
  localStorage.setItem(STORAGE_KEY, String(collapsed.value))
})

/**
 * Whether the obra cover photo/header is shown collapsed or expanded. A single, global
 * preference (not per-obra) shared across every SiteDetailView instance, persisted in
 * localStorage the same way useTheme persists the theme choice.
 */
export function useSiteHeroCollapse() {
  function toggle() {
    collapsed.value = !collapsed.value
  }

  return { collapsed, toggle }
}
