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
  <div class="flex min-h-screen">
    <div
      class="relative hidden w-[600px] shrink-0 flex-col justify-between overflow-hidden p-14 text-white lg:flex"
      style="background: radial-gradient(120% 120% at 15% 0%, #22306b 0%, var(--color-ink-950) 55%)"
    >
      <div
        class="pointer-events-none absolute -right-28 -top-28 h-[420px] w-[420px] rounded-full"
        style="background: radial-gradient(circle, rgba(61, 99, 255, 0.35), transparent 70%)"
      ></div>
      <div class="relative">
        <BrandMark light />
      </div>
      <div class="relative max-w-md">
        <slot name="brand">
          <h2 class="text-3xl font-extrabold leading-tight xl:text-4xl">
            {{ heading ?? 'Gestão de obras, do orçamento à entrega.' }}
          </h2>
          <p class="mt-4 text-steel-300">
            {{ description ?? 'Centralize equipe, diário de obra, materiais e orçamentos de cada obra em um só lugar.' }}
          </p>
        </slot>
      </div>
      <p class="relative text-xs text-steel-400">© {{ new Date().getFullYear() }} Pantheon · Gestão de obras</p>
    </div>

    <div class="flex flex-1 items-center justify-center bg-white px-4 py-10 dark:bg-white">
      <div class="w-full" :class="cardClass">
        <div class="mb-8 flex justify-center lg:hidden">
          <BrandMark />
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>
