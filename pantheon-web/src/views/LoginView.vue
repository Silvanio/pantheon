<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HttpError, useAuth } from '../composables/useAuth'
import AuthShell from '../components/AuthShell.vue'

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
  <AuthShell>
    <h1 class="mb-1 text-2xl font-semibold text-steel-800 dark:text-steel-50">
      {{ mode === 'login' ? 'Bem-vindo de volta' : 'Criar conta' }}
    </h1>
    <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">
      {{ mode === 'login' ? 'Entre para acessar suas obras.' : 'Leve alguns segundos para começar.' }}
    </p>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div v-if="mode === 'register'">
        <label class="field-label" for="displayName">Nome</label>
        <input id="displayName" v-model="displayName" type="text" required class="field-input" />
      </div>

      <div>
        <label class="field-label" for="email">Email</label>
        <input id="email" v-model="email" type="email" required class="field-input" />
      </div>

      <div>
        <label class="field-label" for="password">Senha</label>
        <input id="password" v-model="password" type="password" required minlength="8" class="field-input" />
      </div>

      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

      <button type="submit" :disabled="submitting" class="btn-primary w-full py-2.5">
        {{ mode === 'login' ? 'Entrar' : 'Criar conta' }}
      </button>
    </form>

    <div class="my-5 flex items-center gap-3 text-xs text-steel-400">
      <div class="h-px flex-1 bg-steel-200 dark:bg-steel-700"></div>
      ou
      <div class="h-px flex-1 bg-steel-200 dark:bg-steel-700"></div>
    </div>

    <a :href="googleLoginUrl()" class="btn-secondary w-full py-2.5">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="h-4 w-4">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" />
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.99.66-2.25 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.85A11 11 0 0 0 12 23z" />
        <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.05H2.18a11 11 0 0 0 0 9.9z" />
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1a11 11 0 0 0-9.82 6.05l3.66 2.85C6.71 7.31 9.14 5.38 12 5.38z" />
      </svg>
      Continuar com Google
    </a>

    <button type="button" class="mt-5 w-full text-center text-sm font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="mode = mode === 'login' ? 'register' : 'login'">
      {{ mode === 'login' ? 'Não tem conta? Criar uma' : 'Já tem conta? Entrar' }}
    </button>
  </AuthShell>
</template>
