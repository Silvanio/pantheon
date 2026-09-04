<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useCompanies } from '../composables/useCompanies'

const props = defineProps<{ companyId: string }>()

const { getLogoBlob } = useCompanies()
const objectUrl = ref<string | null>(null)

async function load() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = null
  }
  try {
    const blob = await getLogoBlob(props.companyId)
    objectUrl.value = URL.createObjectURL(blob)
  } catch {
    objectUrl.value = null
  }
}

onMounted(load)
watch(() => props.companyId, load)
onUnmounted(() => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
})
</script>

<template>
  <img v-if="objectUrl" :src="objectUrl" class="h-8 w-8 rounded-md object-cover" alt="" />
  <div v-else class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
</template>
