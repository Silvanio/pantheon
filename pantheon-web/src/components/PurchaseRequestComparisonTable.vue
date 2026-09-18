<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { PurchaseRequestComparison, ComparisonRow } from '../composables/usePurchaseRequests'

const props = defineProps<{
  comparison: PurchaseRequestComparison
  /** Selection can only change while the header is `ORCADO`; otherwise cells are read-only. */
  readonly: boolean
  printingOrcamentoId: string | null
}>()

const emit = defineEmits<{
  select: [itemId: string, orcamentoLineItemId: string | null]
  print: [orcamentoId: string]
}>()

const { t } = useI18n()

function cellFor(row: ComparisonRow, orcamentoId: string) {
  return row.cells.find((cell) => cell.orcamentoId === orcamentoId) ?? null
}

function onCellClick(row: ComparisonRow, orcamentoId: string) {
  if (props.readonly) return
  const cell = cellFor(row, orcamentoId)
  if (!cell) return
  emit('select', row.itemId, cell.selected ? null : cell.lineItemId)
}

function formatMoney(value: string | null): string {
  if (value === null) return '—'
  const parsed = parseFloat(value)
  return Number.isNaN(parsed) ? value : parsed.toFixed(2)
}
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-steel-200 text-left text-xs uppercase tracking-wide text-steel-500 dark:border-steel-700 dark:text-steel-400">
          <th class="py-2 pr-3 font-medium">{{ t('purchaseRequests.comparison.itemColumn') }}</th>
          <th v-for="column in comparison.columns" :key="column.orcamentoId" class="py-2 px-3 font-medium">
            {{ column.supplierName }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in comparison.rows" :key="row.itemId" class="border-b border-steel-100 last:border-0 dark:border-steel-800">
          <td class="py-2.5 pr-3">
            <p class="font-medium text-steel-800 dark:text-steel-50">{{ row.itemName }}</p>
            <p class="text-xs text-steel-500 dark:text-steel-400">{{ row.quantity }}{{ row.unit ? ` ${row.unit}` : '' }}</p>
          </td>
          <td v-for="column in comparison.columns" :key="column.orcamentoId" class="py-2.5 px-3">
            <button
              v-if="cellFor(row, column.orcamentoId)"
              type="button"
              :disabled="readonly"
              class="flex w-full items-center gap-2 rounded-lg border px-2.5 py-1.5 text-left transition disabled:cursor-default"
              :class="
                cellFor(row, column.orcamentoId)!.selected
                  ? 'border-blueprint-500 bg-blueprint-50 dark:bg-blueprint-900/30'
                  : 'border-steel-200 hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700/60'
              "
              @click="onCellClick(row, column.orcamentoId)"
            >
              <span
                class="flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full border-2"
                :class="
                  cellFor(row, column.orcamentoId)!.selected
                    ? 'border-blueprint-600 dark:border-blueprint-400'
                    : 'border-steel-300 dark:border-steel-600'
                "
              >
                <span v-if="cellFor(row, column.orcamentoId)!.selected" class="h-1.5 w-1.5 rounded-full bg-blueprint-600 dark:bg-blueprint-400" />
              </span>
              <span class="font-medium text-steel-800 dark:text-steel-50">{{ formatMoney(cellFor(row, column.orcamentoId)!.unitPrice) }}</span>
            </button>
            <span v-else class="text-steel-400 dark:text-steel-600">—</span>
          </td>
        </tr>
      </tbody>
      <tfoot v-if="comparison.columns.length > 0">
        <tr class="border-t border-steel-200 dark:border-steel-700">
          <td class="py-3 pr-3 text-xs font-medium text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.comparison.printLabel') }}</td>
          <td v-for="column in comparison.columns" :key="column.orcamentoId" class="py-3 px-3">
            <button
              type="button"
              :disabled="printingOrcamentoId === column.orcamentoId"
              class="btn-secondary px-3 py-1.5 text-xs"
              @click="emit('print', column.orcamentoId)"
            >
              {{ printingOrcamentoId === column.orcamentoId ? t('purchaseRequests.comparison.printing') : t('purchaseRequests.comparison.printButton') }}
            </button>
          </td>
        </tr>
      </tfoot>
    </table>
  </div>
</template>
