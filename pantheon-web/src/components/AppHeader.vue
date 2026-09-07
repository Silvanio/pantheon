<template>
  <header class="app-header">
    <div class="app-container flex h-16 items-center justify-between gap-4">
      <div class="flex min-w-0 items-center gap-3">
        <slot name="left" />
      </div>
      <div class="flex shrink-0 items-center gap-3">
        <slot name="right" />
      </div>
    </div>
  </header>
</template>
