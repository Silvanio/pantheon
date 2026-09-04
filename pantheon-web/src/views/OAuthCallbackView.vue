<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()
const { setToken } = useAuth()

onMounted(() => {
  const token = route.query.token
  if (typeof token === 'string' && token.length > 0) {
    setToken(token)
    router.replace('/')
  } else {
    router.replace('/login')
  }
})
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 dark:bg-steel-900">
    <p class="text-steel-500 dark:text-steel-400">Concluindo login...</p>
  </div>
</template>
