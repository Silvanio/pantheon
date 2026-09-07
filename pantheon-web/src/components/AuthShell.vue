<script setup lang="ts">
import BrandMark from './BrandMark.vue'

withDefaults(
  defineProps<{
    heading?: string
    description?: string
    cardClass?: string
  }>(),
  { cardClass: 'max-w-md' },
)
</script>

<template>
  <div class="grid min-h-screen lg:grid-cols-2">
    <div class="relative hidden overflow-hidden bg-gradient-to-br from-blueprint-600 via-blueprint-700 to-steel-900 lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
      <div class="blueprint-grid absolute inset-0 opacity-30"></div>
      <div class="relative">
        <BrandMark light />
      </div>
      <div class="relative max-w-md">
        <h2 class="text-3xl font-semibold leading-tight text-white xl:text-4xl">
          {{ heading ?? 'Gestão de obras, do orçamento à entrega.' }}
        </h2>
        <p class="mt-4 text-blueprint-100/90">
          {{ description ?? 'Centralize equipe, diário de obra, materiais e orçamentos de cada obra em um só lugar.' }}
        </p>
      </div>
      <p class="relative text-sm text-blueprint-200/70">© {{ new Date().getFullYear() }} Pantheon</p>
    </div>

    <div class="blueprint-grid flex items-center justify-center bg-steel-50 px-4 py-10 dark:bg-steel-900 lg:px-10">
      <div class="w-full" :class="cardClass">
        <div class="mb-8 flex justify-center lg:hidden">
          <BrandMark />
        </div>
        <div class="card card-pad shadow-lg sm:p-8">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>
