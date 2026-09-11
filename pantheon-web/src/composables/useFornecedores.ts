import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export interface FornecedorSuggestion {
  id: string
  cnpj: string
  name: string
  address: string | null
  contactName: string | null
  contactPhone: string | null
}

export interface FornecedorInput {
  cnpj: string
  name: string
  address: string | null
  contactName: string | null
  contactPhone: string | null
}

async function authFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token.value}`,
      ...options.headers,
    },
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export function useFornecedores() {
  function searchByCnpjPrefix(siteId: string, cnpjPrefix: string): Promise<FornecedorSuggestion[]> {
    return authFetch(`/api/construction-sites/${siteId}/fornecedores?cnpjPrefix=${encodeURIComponent(cnpjPrefix)}`)
  }

  return { searchByCnpjPrefix }
}
