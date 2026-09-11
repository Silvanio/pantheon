<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrcamentos, type OrcamentoApproverFunction, type OrcamentoApprovalLevelInput } from '../composables/useOrcamentos'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { getApprovalLevels, setApprovalLevels } = useOrcamentos()

const functions: OrcamentoApproverFunction[] = ['CLIENT', 'ARCHITECT', 'ENGINEER', 'SITE_FOREMAN', 'SERVICE_PROVIDER', 'OTHER']

const levels = ref<OrcamentoApprovalLevelInput[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const savedMessage = ref('')

async function load() {
  loading.value = true
  try {
    const effective = await getApprovalLevels(props.siteId)
    levels.value = effective
      .slice()
      .sort((a, b) => a.stepOrder - b.stepOrder)
      .map((l) => ({ stepOrder: l.stepOrder, approverFunction: l.approverFunction }))
  } finally {
    loading.value = false
  }
}

function addLevel() {
  levels.value.push({ stepOrder: levels.value.length + 1, approverFunction: 'ENGINEER' })
}

function removeLevel(index: number) {
  levels.value.splice(index, 1)
  levels.value.forEach((level, i) => {
    level.stepOrder = i + 1
  })
}

async function onSave() {
  errorMessage.value = ''
  savedMessage.value = ''
  saving.value = true
  try {
    await setApprovalLevels(props.siteId, levels.value)
    savedMessage.value = t('orcamento.approvalLevels.saved')
  } catch {
    errorMessage.value = t('orcamento.approvalLevels.error')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="card card-pad">
    <h2 class="mb-1 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.approvalLevels.title') }}</h2>
    <p class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.approvalLevels.subtitle') }}</p>

    <div v-if="!loading" class="space-y-2">
      <div v-for="(level, index) in levels" :key="index" class="flex items-center gap-2">
        <span class="w-20 shrink-0 text-sm text-steel-600 dark:text-steel-300">{{ t('orcamento.approvalLevels.stepLabel') }} {{ level.stepOrder }}</span>
        <select v-model="level.approverFunction" class="field-input">
          <option v-for="fn in functions" :key="fn" :value="fn">{{ t(`orcamento.approverFunction.${fn}`) }}</option>
        </select>
        <button type="button" class="shrink-0 text-sm font-medium text-safety-600 hover:underline dark:text-safety-500" @click="removeLevel(index)">
          {{ t('orcamento.approvalLevels.removeLevelButton') }}
        </button>
      </div>
    </div>

    <button type="button" class="mt-3 text-sm font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="addLevel">
      + {{ t('orcamento.approvalLevels.addLevelButton') }}
    </button>

    <p v-if="errorMessage" class="mt-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
    <p v-if="savedMessage" class="mt-3 text-sm text-blueprint-600 dark:text-blueprint-400">{{ savedMessage }}</p>

    <div class="mt-4">
      <button type="button" :disabled="saving || levels.length === 0" class="btn-primary" @click="onSave">
        {{ t('orcamento.approvalLevels.saveButton') }}
      </button>
    </div>
  </section>
</template>
