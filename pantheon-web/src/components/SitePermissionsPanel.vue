<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useSitePermissions,
  type AccessLevel,
  type PermissionCapability,
  type SitePermissionOverride,
} from '../composables/useSitePermissions'
import type { ConstructionFunction } from '../composables/useSiteMembers'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listOverrides, setFunctionOverride } = useSitePermissions()

const functions: ConstructionFunction[] = ['CLIENT', 'ARCHITECT', 'ENGINEER', 'SITE_FOREMAN', 'SERVICE_PROVIDER']
const capabilities: PermissionCapability[] = [
  'DOCUMENT_PROJECTS',
  'DAILY_REPORT',
  'EQUIPMENT_MATERIAL',
  'MATERIAL_REQUEST',
  'MATERIAL_APPROVAL',
]

const overrides = ref<SitePermissionOverride[]>([])
const loading = ref(false)
const savingKey = ref<string | null>(null)
const errorMessage = ref('')

function overrideFor(fn: ConstructionFunction, capability: PermissionCapability): AccessLevel | null {
  return overrides.value.find((o) => o.function === fn && o.capability === capability)?.accessLevel ?? null
}

async function load() {
  loading.value = true
  try {
    overrides.value = await listOverrides(props.siteId)
  } finally {
    loading.value = false
  }
}

async function onChange(fn: ConstructionFunction, capability: PermissionCapability, event: Event) {
  const accessLevel = (event.target as HTMLSelectElement).value as AccessLevel
  const key = `${fn}-${capability}`
  savingKey.value = key
  errorMessage.value = ''
  try {
    await setFunctionOverride(props.siteId, fn, capability, accessLevel)
    await load()
  } catch {
    errorMessage.value = t('sitePermissions.error')
  } finally {
    savingKey.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
    <h2 class="mb-1 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('sitePermissions.title') }}</h2>
    <p class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('sitePermissions.subtitle') }}</p>

    <p v-if="errorMessage" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <div v-if="!loading" class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-steel-200 dark:border-steel-700">
            <th class="py-2 text-left font-medium text-steel-600 dark:text-steel-300">{{ t('sitePermissions.function') }}</th>
            <th v-for="capability in capabilities" :key="capability" class="py-2 text-left font-medium text-steel-600 dark:text-steel-300">
              {{ t(`sitePermissions.capability.${capability}`) }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="fn in functions" :key="fn" class="border-b border-steel-100 dark:border-steel-700">
            <td class="py-2 font-medium text-steel-800 dark:text-steel-50">{{ t(`siteTeam.function.${fn}`) }}</td>
            <td v-for="capability in capabilities" :key="capability" class="py-2">
              <select
                :value="overrideFor(fn, capability) ?? ''"
                :disabled="savingKey === `${fn}-${capability}`"
                class="rounded-md border border-steel-300 bg-white px-2 py-1 text-xs text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
                @change="onChange(fn, capability, $event)"
              >
                <option value="" disabled>{{ t('sitePermissions.default') }}</option>
                <option value="VIEW">{{ t('sitePermissions.accessLevel.VIEW') }}</option>
                <option value="MANAGE">{{ t('sitePermissions.accessLevel.MANAGE') }}</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
