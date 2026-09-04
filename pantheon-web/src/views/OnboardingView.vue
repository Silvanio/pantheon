<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showNoProjectMessage = ref(false)

function chooseCreate() {
  router.push('/projects/new')
}

function chooseJoin() {
  showNoProjectMessage.value = true
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 dark:bg-steel-900">
    <div class="w-full max-w-md rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <template v-if="!showNoProjectMessage">
        <h1 class="mb-2 text-xl font-semibold text-steel-800 dark:text-steel-50">Bem-vindo!</h1>
        <p class="mb-6 text-steel-500 dark:text-steel-400">
          Você ainda não faz parte de nenhum projeto. O que você gostaria de fazer?
        </p>
        <div class="space-y-3">
          <button
            type="button"
            class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
            @click="chooseCreate"
          >
            Criar um novo projeto
          </button>
          <button
            type="button"
            class="w-full rounded-md border border-steel-300 px-4 py-2 font-medium text-steel-700 transition hover:bg-steel-50 dark:border-steel-600 dark:text-steel-100 dark:hover:bg-steel-700"
            @click="chooseJoin"
          >
            Participar de um projeto
          </button>
        </div>
      </template>

      <template v-else>
        <h1 class="mb-2 text-xl font-semibold text-steel-800 dark:text-steel-50">Nenhum projeto encontrado</h1>
        <p class="mb-6 text-steel-500 dark:text-steel-400">
          Você ainda não possui nenhum projeto. Peça para um administrador adicionar você a um projeto existente.
        </p>
        <button
          type="button"
          class="w-full rounded-md border border-steel-300 px-4 py-2 font-medium text-steel-700 transition hover:bg-steel-50 dark:border-steel-600 dark:text-steel-100 dark:hover:bg-steel-700"
          @click="showNoProjectMessage = false"
        >
          Voltar
        </button>
      </template>
    </div>
  </div>
</template>
