<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjects } from '../composables/useProjects'
import { useProjectOnboarding } from '../composables/useProjectOnboarding'

const router = useRouter()
const { createProject } = useProjects()
const { invalidate } = useProjectOnboarding()

const projectName = ref('')
const cnpjCpf = ref('')
const legalName = ref('')
const address = ref('')
const postalCode = ref('')
const errorMessage = ref('')
const submitting = ref(false)

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createProject({
      projectName: projectName.value,
      cnpjCpf: cnpjCpf.value,
      legalName: legalName.value,
      address: address.value,
      postalCode: postalCode.value,
    })
    invalidate()
    router.push('/')
  } catch {
    errorMessage.value = 'Não foi possível criar o projeto. Verifique os dados e tente novamente.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 py-8 dark:bg-steel-900">
    <div class="w-full max-w-md rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <h1 class="mb-1 text-xl font-semibold text-steel-800 dark:text-steel-50">Criar novo projeto</h1>
      <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">
        Este cadastro inicia em modo trial por 3 dias.
      </p>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="projectName">
            Nome do projeto
          </label>
          <input
            id="projectName"
            v-model="projectName"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="cnpjCpf">
            CNPJ/CPF
          </label>
          <input
            id="cnpjCpf"
            v-model="cnpjCpf"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="legalName">
            Nome/Razão Social
          </label>
          <input
            id="legalName"
            v-model="legalName"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="address">
            Endereço
          </label>
          <input
            id="address"
            v-model="address"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="postalCode">
            CEP
          </label>
          <input
            id="postalCode"
            v-model="postalCode"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        >
          Criar projeto
        </button>
      </form>
    </div>
  </div>
</template>
