<script setup lang="ts">
import { useI18n } from 'vue-i18n'

/**
 * `parent` inserts one extra, specific-record segment between the site and this page's own
 * label — e.g. an Orcamento reached through its originating Pedido de Compra shows
 * Obra / Lista de pedido de compra / Pedido X / Orçamento instead of the default
 * Obra / Lista de orçamentos / Orçamento, and the back arrow returns to that record
 * (`parent.to`) rather than to this page's own tab.
 */
const props = defineProps<{
  siteId: string
  siteName: string | null
  tab: string
  label: string
  parent?: { tab: string; label: string; name: string; to: string } | null
}>()

const { t } = useI18n()
</script>

<template>
  <nav class="flex min-w-0 items-center gap-1.5 text-sm">
    <RouterLink
      :to="parent ? parent.to : { path: `/sites/${siteId}`, query: { tab } }"
      class="-ml-1.5 flex shrink-0 items-center justify-center rounded-lg p-1.5 text-steel-500 transition hover:bg-steel-100 hover:text-steel-700 dark:text-steel-400 dark:hover:bg-steel-800 dark:hover:text-steel-200"
      :aria-label="label"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="h-4 w-4">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
      </svg>
    </RouterLink>
    <RouterLink
      :to="`/sites/${siteId}`"
      class="truncate font-medium text-steel-500 hover:text-blueprint-600 hover:underline dark:text-steel-400 dark:hover:text-blueprint-400"
    >
      {{ siteName ?? '…' }}
    </RouterLink>
    <span class="shrink-0 text-steel-400 dark:text-steel-600">/</span>
    <RouterLink
      :to="{ path: `/sites/${siteId}`, query: { tab: parent ? parent.tab : tab } }"
      class="truncate font-medium text-steel-500 hover:text-blueprint-600 hover:underline dark:text-steel-400 dark:hover:text-blueprint-400"
    >
      {{ t('common.breadcrumb.list', { label: parent ? parent.label : label }) }}
    </RouterLink>
    <template v-if="parent">
      <span class="shrink-0 text-steel-400 dark:text-steel-600">/</span>
      <RouterLink
        :to="parent.to"
        class="truncate font-medium text-steel-500 hover:text-blueprint-600 hover:underline dark:text-steel-400 dark:hover:text-blueprint-400"
      >
        {{ parent.name }}
      </RouterLink>
    </template>
    <span class="shrink-0 text-steel-400 dark:text-steel-600">/</span>
    <span class="truncate font-semibold text-steel-800 dark:text-steel-50">{{ label }}</span>
  </nav>
</template>
