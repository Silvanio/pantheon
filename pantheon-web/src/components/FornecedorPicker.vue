<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFornecedores, type FornecedorInput, type FornecedorSuggestion } from '../composables/useFornecedores'

const props = defineProps<{ siteId: string }>()
const emit = defineEmits<{ confirm: [FornecedorInput]; cancel: [] }>()

const { t } = useI18n()
const { searchByCnpjPrefix } = useFornecedores()

const cnpj = ref('')
const name = ref('')
const address = ref('')
const contactName = ref('')
const contactPhone = ref('')

const suggestions = ref<FornecedorSuggestion[]>([])
const searching = ref(false)
const searchedOnce = ref(false)
const errorMessage = ref('')

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function onCnpjInput() {
  suggestions.value = []
  searchedOnce.value = false
  if (debounceTimer) clearTimeout(debounceTimer)
  const value = cnpj.value.trim()
  if (value.length < 5) return
  debounceTimer = setTimeout(async () => {
    searching.value = true
    try {
      suggestions.value = await searchByCnpjPrefix(props.siteId, value)
    } catch {
      suggestions.value = []
    } finally {
      searching.value = false
      searchedOnce.value = true
    }
  }, 300)
}

function selectSuggestion(suggestion: FornecedorSuggestion) {
  cnpj.value = suggestion.cnpj
  name.value = suggestion.name
  address.value = suggestion.address ?? ''
  contactName.value = suggestion.contactName ?? ''
  contactPhone.value = suggestion.contactPhone ?? ''
  suggestions.value = []
  searchedOnce.value = false
}

function onContinue() {
  errorMessage.value = ''
  if (!cnpj.value.trim() || !name.value.trim()) {
    errorMessage.value = t('fornecedor.error')
    return
  }
  emit('confirm', {
    cnpj: cnpj.value.trim(),
    name: name.value.trim(),
    address: address.value.trim() || null,
    contactName: contactName.value.trim() || null,
    contactPhone: contactPhone.value.trim() || null,
  })
}

onBeforeUnmount(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<template>
  <div class="rounded-lg border border-blueprint-200 bg-blueprint-50 p-4 dark:border-blueprint-800 dark:bg-blueprint-900/20">
    <h3 class="mb-1 text-sm font-semibold text-steel-800 dark:text-steel-50">{{ t('fornecedor.title') }}</h3>
    <p class="mb-3 text-sm text-steel-500 dark:text-steel-400">{{ t('fornecedor.subtitle') }}</p>

    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
      <div class="relative sm:col-span-2">
        <label class="field-label">{{ t('fornecedor.cnpjLabel') }}</label>
        <input
          v-model="cnpj"
          type="text"
          :placeholder="t('fornecedor.cnpjPlaceholder')"
          class="field-input"
          @input="onCnpjInput"
        />
        <div
          v-if="searching || suggestions.length > 0"
          class="absolute z-10 mt-1 w-full rounded-lg border border-steel-200 bg-white shadow-lg dark:border-steel-700 dark:bg-steel-800"
        >
          <p v-if="searching" class="px-3 py-2 text-xs text-steel-500 dark:text-steel-400">{{ t('fornecedor.searching') }}</p>
          <ul v-else>
            <li v-for="suggestion in suggestions" :key="suggestion.id">
              <button
                type="button"
                class="block w-full px-3 py-2 text-left text-sm hover:bg-steel-50 dark:hover:bg-steel-700"
                @click="selectSuggestion(suggestion)"
              >
                <span class="font-medium text-steel-800 dark:text-steel-50">{{ suggestion.name }}</span>
                <span class="ml-2 text-steel-500 dark:text-steel-400">{{ suggestion.cnpj }}</span>
              </button>
            </li>
          </ul>
        </div>
        <p v-if="searchedOnce && !searching && suggestions.length === 0" class="mt-1.5 text-xs text-steel-500 dark:text-steel-400">
          {{ t('fornecedor.noMatchHint') }}
        </p>
      </div>

      <div>
        <label class="field-label">{{ t('fornecedor.nameLabel') }}</label>
        <input v-model="name" type="text" class="field-input" />
      </div>
      <div>
        <label class="field-label">{{ t('fornecedor.addressLabel') }}</label>
        <input v-model="address" type="text" class="field-input" />
      </div>
      <div>
        <label class="field-label">{{ t('fornecedor.contactNameLabel') }}</label>
        <input v-model="contactName" type="text" class="field-input" />
      </div>
      <div>
        <label class="field-label">{{ t('fornecedor.contactPhoneLabel') }}</label>
        <input v-model="contactPhone" type="text" class="field-input" />
      </div>
    </div>

    <p v-if="errorMessage" class="mt-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <div class="mt-4 flex gap-2">
      <button type="button" class="btn-primary" @click="onContinue">{{ t('fornecedor.continueButton') }}</button>
      <button type="button" class="btn-secondary" @click="emit('cancel')">{{ t('fornecedor.cancelButton') }}</button>
    </div>
  </div>
</template>
