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
    <template #brand>
      <h2 class="text-3xl font-extrabold leading-tight xl:text-4xl">Todas as suas obras, sem perder o fio da meada.</h2>
      <div class="mt-7 flex flex-col gap-3.5">
        <div class="flex items-start gap-3">
          <div class="mt-0.5 flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-emerald-400/20">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#4ADE80" stroke-width="3" class="h-3 w-3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p class="text-sm leading-relaxed text-steel-200">Pedidos de compra e orçamentos em um só lugar</p>
        </div>
        <div class="flex items-start gap-3">
          <div class="mt-0.5 flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-emerald-400/20">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#4ADE80" stroke-width="3" class="h-3 w-3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p class="text-sm leading-relaxed text-steel-200">Aprovações por etapa, com o histórico de cada decisão</p>
        </div>
        <div class="flex items-start gap-3">
          <div class="mt-0.5 flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-emerald-400/20">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#4ADE80" stroke-width="3" class="h-3 w-3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p class="text-sm leading-relaxed text-steel-200">Diário de obra, tarefas e equipe sempre à mão</p>
        </div>
      </div>
    </template>

    <span class="text-xs font-extrabold uppercase tracking-widest text-blueprint-600">Acesso</span>
    <h1 class="mb-1.5 mt-2.5 text-[26px] font-extrabold text-steel-800">
      {{ mode === 'login' ? 'Bem-vindo de volta' : 'Criar conta' }}
    </h1>
    <p class="mb-8 text-sm text-steel-500">
      {{ mode === 'login' ? 'Entre para acessar suas obras.' : 'Leve alguns segundos para começar.' }}
    </p>

    <form class="space-y-[18px]" @submit.prevent="onSubmit">
      <div v-if="mode === 'register'">
        <label class="mb-[7px] block text-[13px] font-bold text-steel-700" for="displayName">Nome</label>
        <input
          id="displayName"
          v-model="displayName"
          type="text"
          required
          class="w-full rounded-[11px] border-[1.5px] border-steel-200 bg-white px-[15px] py-[13px] text-[14.5px] text-steel-900 focus:border-blueprint-500 focus:outline-none focus:ring-2 focus:ring-blueprint-500/20"
        />
      </div>

      <div>
        <label class="mb-[7px] block text-[13px] font-bold text-steel-700" for="email">Email</label>
        <input
          id="email"
          v-model="email"
          type="email"
          required
          class="w-full rounded-[11px] border-[1.5px] border-steel-200 bg-white px-[15px] py-[13px] text-[14.5px] text-steel-900 focus:border-blueprint-500 focus:outline-none focus:ring-2 focus:ring-blueprint-500/20"
        />
      </div>

      <div>
        <label class="mb-[7px] block text-[13px] font-bold text-steel-700" for="password">Senha</label>
        <input
          id="password"
          v-model="password"
          type="password"
          required
          minlength="8"
          class="w-full rounded-[11px] border-[1.5px] border-steel-200 bg-white px-[15px] py-[13px] text-[14.5px] text-steel-900 focus:border-blueprint-500 focus:outline-none focus:ring-2 focus:ring-blueprint-500/20"
        />
      </div>

      <p v-if="errorMessage" class="text-sm text-safety-600">{{ errorMessage }}</p>

      <button
        type="submit"
        :disabled="submitting"
        class="w-full rounded-[11px] bg-blueprint-600 px-5 py-[14px] text-[15px] font-extrabold text-white transition hover:bg-blueprint-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {{ mode === 'login' ? 'Entrar' : 'Criar conta' }}
      </button>
    </form>

    <div class="my-[22px] flex items-center gap-3 text-xs font-bold text-steel-500">
      <div class="h-px flex-1 bg-steel-200"></div>
      ou
      <div class="h-px flex-1 bg-steel-200"></div>
    </div>

    <a
      :href="googleLoginUrl()"
      class="flex w-full items-center justify-center gap-2.5 rounded-[11px] border-[1.5px] border-steel-200 bg-white px-5 py-[13px] text-sm font-bold text-steel-700 transition hover:bg-steel-50"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="h-4 w-4">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" />
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.99.66-2.25 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.85A11 11 0 0 0 12 23z" />
        <path fill="#FBBC05" d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.05H2.18a11 11 0 0 0 0 9.9z" />
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1a11 11 0 0 0-9.82 6.05l3.66 2.85C6.71 7.31 9.14 5.38 12 5.38z" />
      </svg>
      Continuar com Google
    </a>

    <button type="button" class="mt-[26px] w-full text-center text-[13px] text-steel-500" @click="mode = mode === 'login' ? 'register' : 'login'">
      {{ mode === 'login' ? 'Não tem conta? ' : 'Já tem conta? ' }}<span class="font-bold text-blueprint-600 hover:underline">{{ mode === 'login' ? 'Criar uma' : 'Entrar' }}</span>
    </button>
  </AuthShell>
</template>
