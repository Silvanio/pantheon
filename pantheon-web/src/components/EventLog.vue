<script setup lang="ts">
import { onMounted } from 'vue'
import { useSse } from '../composables/useSse'

const { connected, entries, connect } = useSse(['user-registered'])

onMounted(connect)
</script>

<template>
  <div class="rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="text-sm font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
        Eventos em tempo real
      </h2>
      <span class="flex items-center gap-2 text-xs font-medium" :class="connected ? 'text-emerald-600 dark:text-emerald-400' : 'text-safety-600 dark:text-safety-500'">
        <span class="h-2 w-2 rounded-full" :class="connected ? 'bg-emerald-500' : 'bg-safety-500'"></span>
        {{ connected ? 'Conectado' : 'Reconectando...' }}
      </span>
    </div>

    <ul v-if="entries.length > 0" class="max-h-72 space-y-2 overflow-y-auto">
      <li
        v-for="entry in entries"
        :key="entry.id"
        class="rounded-md border border-steel-100 bg-steel-50 p-3 text-sm dark:border-steel-700 dark:bg-steel-900"
      >
        <div class="mb-1 flex items-center justify-between text-xs text-steel-400">
          <span class="font-mono text-blueprint-600 dark:text-blueprint-400">{{ entry.eventName }}</span>
          <span>{{ entry.receivedAt.toLocaleTimeString() }}</span>
        </div>
        <pre class="overflow-x-auto whitespace-pre-wrap break-all text-steel-700 dark:text-steel-200">{{ entry.data }}</pre>
      </li>
    </ul>
    <p v-else class="text-sm text-steel-400">Nenhum evento recebido ainda.</p>
  </div>
</template>
