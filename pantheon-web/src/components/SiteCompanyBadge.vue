<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMySites } from '../composables/useMySites'

const props = defineProps<{ siteId: string; companyName: string | null }>()

const { getSiteCompanyLogoBlob } = useMySites()
const objectUrl = ref<string | null>(null)

onMounted(async () => {
  try {
    const blob = await getSiteCompanyLogoBlob(props.siteId)
    objectUrl.value = URL.createObjectURL(blob)
  } catch {
    objectUrl.value = null
  }
})
</script>

<template>
  <span class="inline-flex max-w-full items-center gap-1.5 rounded-full bg-white/90 px-2 py-1 text-xs font-medium text-steel-700 shadow-sm backdrop-blur dark:bg-steel-800/90 dark:text-steel-200">
    <img v-if="objectUrl" :src="objectUrl" class="h-4 w-4 shrink-0 rounded object-cover" alt="" />
    <span v-else class="h-4 w-4 shrink-0 rounded bg-gradient-to-br from-blueprint-500 to-blueprint-700"></span>
    <span class="truncate">{{ companyName }}</span>
  </span>
</template>
