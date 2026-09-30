<script setup lang="ts">
withDefaults(defineProps<{ light?: boolean }>(), { light: false })
</script>

<template>
  <div class="flex items-center gap-2.5">
    <div
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blueprint-500 to-blueprint-700 text-white shadow-sm"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="h-5 w-5">
        <path d="M6 4H8.3V20H6z M8.3 4H15.2C16.99 4 18.4 5.46 18.4 7.3C18.4 9.14 16.99 10.6 15.2 10.6H8.3V4Z" />
      </svg>
    </div>
    <span class="text-lg font-semibold tracking-tight" :class="light ? 'text-white' : 'text-steel-800 dark:text-steel-50'">
      Pantheon
    </span>
  </div>
</template>
