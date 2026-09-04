<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjects, type Plan } from '../composables/useProjects'
import { useProjectOnboarding } from '../composables/useProjectOnboarding'

const router = useRouter()
const { confirmPlan } = useProjects()
const { status, invalidate } = useProjectOnboarding()

const plans: { value: Plan; title: string; description: string }[] = [
  { value: 'BASIC', title: 'Basic', description: 'Até 2 projetos' },
  { value: 'PRO', title: 'Pro', description: 'Até 10 projetos' },
  { value: 'UNLIMITED', title: 'Ilimitado', description: 'Projetos ilimitados' },
]

const selectedPlan = ref<Plan | null>(null)
const errorMessage = ref('')
const submitting = ref(false)

async function onConfirm() {
  const projectId = status.value?.expiredProjectId
  if (!selectedPlan.value || !projectId) {
    return
  }

  errorMessage.value = ''
  submitting.value = true
  try {
    await confirmPlan(projectId, selectedPlan.value)
    invalidate()
    router.push('/')
  } catch {
    errorMessage.value = 'Não foi possível confirmar o plano. Tente novamente.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 py-8 dark:bg-steel-900">
    <div class="w-full max-w-2xl rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <template v-if="status?.expiredProjectId">
        <h1 class="mb-1 text-xl font-semibold text-steel-800 dark:text-steel-50">Escolha um plano</h1>
        <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">
          O período trial deste projeto terminou. Selecione um plano para continuar.
        </p>

        <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <button
            v-for="plan in plans"
            :key="plan.value"
            type="button"
            class="rounded-lg border p-4 text-left transition"
            :class="
              selectedPlan === plan.value
                ? 'border-blueprint-500 bg-blueprint-50 dark:bg-blueprint-900/30'
                : 'border-steel-200 hover:bg-steel-50 dark:border-steel-600 dark:hover:bg-steel-700'
            "
            @click="selectedPlan = plan.value"
          >
            <div class="font-semibold text-steel-800 dark:text-steel-50">{{ plan.title }}</div>
            <div class="mt-1 text-sm text-steel-500 dark:text-steel-400">{{ plan.description }}</div>
          </button>
        </div>

        <p v-if="errorMessage" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

        <button
          type="button"
          :disabled="!selectedPlan || submitting"
          class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
          @click="onConfirm"
        >
          Concluído
        </button>
      </template>

      <template v-else>
        <h1 class="mb-2 text-xl font-semibold text-steel-800 dark:text-steel-50">Plano expirado</h1>
        <p class="text-steel-500 dark:text-steel-400">
          O plano deste projeto expirou. Peça para o administrador do projeto confirmar um novo plano.
        </p>
      </template>
    </div>
  </div>
</template>
