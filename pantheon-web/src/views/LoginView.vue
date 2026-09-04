<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HttpError, useAuth } from '../composables/useAuth'

const router = useRouter()
const route = useRoute()
const { login, register, googleLoginUrl } = useAuth()

function redirectTarget(): string {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/'
}

const mode = ref<'login' | 'register'>('login')
const email = ref('')
const password = ref('')
const displayName = ref('')
const errorMessage = ref('')
const submitting = ref(false)

function messageForError(error: unknown): string {
  if (error instanceof HttpError) {
    if (error.status === 409) {
      return 'Este e-mail já está cadastrado. Use "Entrar" para acessar sua conta.'
    }
    if (error.status === 401) {
      return 'E-mail ou senha incorretos.'
    }
    if (error.status === 400) {
      return 'Dados inválidos. A senha precisa ter ao menos 8 caracteres.'
    }
  }
  return 'Não foi possível autenticar. Verifique os dados e tente novamente.'
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await login(email.value, password.value)
    } else {
      await register(email.value, password.value, displayName.value)
    }
    router.push(redirectTarget())
  } catch (error) {
    errorMessage.value = messageForError(error)
    if (error instanceof HttpError && error.status === 409) {
      mode.value = 'login'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 dark:bg-steel-900">
    <div class="w-full max-w-sm rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <h1 class="mb-6 text-xl font-semibold text-steel-800 dark:text-steel-50">
        {{ mode === 'login' ? 'Entrar' : 'Criar conta' }}
      </h1>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <div v-if="mode === 'register'">
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="displayName">Nome</label>
          <input
            id="displayName"
            v-model="displayName"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="email">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="password">Senha</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            minlength="8"
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        >
          {{ mode === 'login' ? 'Entrar' : 'Criar conta' }}
        </button>
      </form>

      <div class="my-4 flex items-center gap-3 text-xs text-steel-400">
        <div class="h-px flex-1 bg-steel-200 dark:bg-steel-700"></div>
        ou
        <div class="h-px flex-1 bg-steel-200 dark:bg-steel-700"></div>
      </div>

      <a
        :href="googleLoginUrl()"
        class="flex w-full items-center justify-center gap-2 rounded-md border border-steel-300 px-4 py-2 font-medium text-steel-700 transition hover:bg-steel-50 dark:border-steel-600 dark:text-steel-100 dark:hover:bg-steel-700"
      >
        Continuar com Google
      </a>

      <button
        type="button"
        class="mt-4 w-full text-center text-sm text-blueprint-600 hover:underline dark:text-blueprint-400"
        @click="mode = mode === 'login' ? 'register' : 'login'"
      >
        {{ mode === 'login' ? 'Não tem conta? Criar uma' : 'Já tem conta? Entrar' }}
      </button>
    </div>
  </div>
</template>
