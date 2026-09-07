<script setup lang="ts">
withDefaults(defineProps<{ light?: boolean }>(), { light: false })
</script>

<template>
  <div class="flex items-center gap-2.5">
    <div
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blueprint-500 to-blueprint-700 text-white shadow-sm"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-5 w-5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4 21V9l8-6 8 6v12" />
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 21v-7h6v7" />
      </svg>
    </div>
    <span class="text-lg font-semibold tracking-tight" :class="light ? 'text-white' : 'text-steel-800 dark:text-steel-50'">
      Pantheon
    </span>
  </div>
</template>
