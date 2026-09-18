<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * Shared status pill for the Pedido de Compra (4-state), Orçamento (2-state) and approval-step
 * (3-state) statuses, so coloring stays consistent across cards and detail views. See
 * `redesign-purchase-request-approval-and-comparison`'s design.md decision 9.
 */
const props = defineProps<{
  kind: 'purchaseRequest' | 'purchaseRequestItem' | 'orcamento' | 'approval'
  status: string
}>()

const { t } = useI18n()

const CLASS_MAP: Record<string, Record<string, string>> = {
  purchaseRequest: {
    INICIADO: 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200',
    ORCADO: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
    CONFERIDO: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
    CONCLUIDO: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
  },
  orcamento: {
    DRAFT: 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200',
    LOCKED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  },
  purchaseRequestItem: {
    PENDING: 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200',
    CONVERTED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  },
  approval: {
    PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
    APPROVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
    REJECTED: 'bg-safety-100 text-safety-700 dark:bg-safety-900/50 dark:text-safety-300',
  },
}

const LABEL_KEY: Record<string, string> = {
  purchaseRequest: 'purchaseRequests.status',
  purchaseRequestItem: 'purchaseRequests.status',
  orcamento: 'orcamento.status',
  approval: 'purchaseRequests.approvalStatus',
}

const badgeClass = computed(() => CLASS_MAP[props.kind]?.[props.status] ?? 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200')
const label = computed(() => t(`${LABEL_KEY[props.kind]}.${props.status}`))
</script>

<template>
  <span class="badge" :class="badgeClass">{{ label }}</span>
</template>
