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
  <img v-if="objectUrl" :src="objectUrl" class="h-9 w-9 rounded-lg object-cover" alt="" />
  <div v-else class="h-9 w-9 rounded-lg bg-gradient-to-br from-blueprint-500 to-blueprint-700"></div>
</template>
