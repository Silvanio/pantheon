<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useConstructionSites } from '../composables/useConstructionSites'

const props = defineProps<{ siteId: string; hasPhoto: boolean }>()

const { getSitePhotoBlob } = useConstructionSites()
const objectUrl = ref<string | null>(null)

async function load() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = null
  }
  if (!props.hasPhoto) return
  try {
    const blob = await getSitePhotoBlob(props.siteId)
    objectUrl.value = URL.createObjectURL(blob)
  } catch {
    objectUrl.value = null
  }
}

onMounted(load)
watch(() => [props.siteId, props.hasPhoto], load)
onUnmounted(() => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
})
</script>

<template>
  <img
    v-if="objectUrl"
    :src="objectUrl"
    class="h-32 w-full rounded-md object-cover"
    alt=""
  />
  <div
    v-else
    class="flex h-32 w-full items-center justify-center rounded-md bg-steel-100 text-steel-400 dark:bg-steel-700 dark:text-steel-500"
  >
    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7h4l2-3h6l2 3h4v13H3V7z" />
      <circle cx="12" cy="13" r="3.5" stroke-width="1.5" />
    </svg>
  </div>
</template>
