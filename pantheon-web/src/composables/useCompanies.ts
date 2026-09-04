import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type PlanCode = 'BASIC' | 'PROFISSIONAL' | 'ILIMITADO'
export type CompanyRole = 'ADMIN' | 'MEMBER'
export type CompanyOnboardingStatus = 'PLAN_PENDING' | 'PROFILE_PENDING' | 'COMPLETE'
export type MembershipStatus = 'INVITED' | 'ACTIVE' | 'NONE'

export interface Plan {
  id: string
  code: PlanCode
  name: string
  activeSiteLimit: number | null
}

export interface Company {
  id: string
  name: string
  planId: string | null
  legalName: string | null
  tradeName: string | null
  cnpj: string | null
  address: string | null
  logoObjectKey: string | null
  onboardingStatus: CompanyOnboardingStatus
}

export interface CompanyMembership {
  companyId: string
  companyName: string
  role: CompanyRole
  onboardingStatus: CompanyOnboardingStatus
}

export interface OnboardingStatus {
  hasCompany: boolean
  companies: CompanyMembership[]
}

export interface CompanyMember {
  membershipId: string
  userId: string
  email: string
  displayName: string
  role: CompanyRole
  status: MembershipStatus
  invited: boolean
}

export interface MemberInvitation {
  invitationId: string
  membershipId: string
  email: string
  requiresRegistration: boolean
}

export interface CompanyProfileData {
  legalName: string
  tradeName: string
  cnpj: string
  address: string
  logo?: File | null
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
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function authUpload<T>(path: string, method: string, formData: FormData): Promise<T> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token.value}` },
    body: formData,
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export function useCompanies() {
  function getOnboardingStatus(): Promise<OnboardingStatus> {
    return authFetch('/api/onboarding/status')
  }

  function listMyCompanies(): Promise<CompanyMembership[]> {
    return authFetch('/api/companies/me')
  }

  function getCompany(companyId: string): Promise<Company> {
    return authFetch(`/api/companies/${companyId}`)
  }

  function createCompany(name: string): Promise<Company> {
    return authFetch('/api/companies', { method: 'POST', body: JSON.stringify({ companyName: name }) })
  }

  function listPlans(): Promise<Plan[]> {
    return authFetch('/api/plans')
  }

  function selectPlan(companyId: string, planCode: PlanCode): Promise<Company> {
    return authFetch(`/api/companies/${companyId}/plan`, { method: 'PUT', body: JSON.stringify({ planCode }) })
  }

  function completeProfile(companyId: string, data: CompanyProfileData): Promise<Company> {
    const formData = new FormData()
    formData.set('legalName', data.legalName)
    formData.set('tradeName', data.tradeName)
    formData.set('cnpj', data.cnpj)
    formData.set('address', data.address)
    if (data.logo) formData.set('logo', data.logo)
    return authUpload(`/api/companies/${companyId}/profile`, 'PUT', formData)
  }

  async function getLogoBlob(companyId: string): Promise<Blob> {
    const { token } = useAuth()
    const response = await fetch(`${SERVICE_BASE_URL}/api/companies/${companyId}/logo`, {
      headers: { Authorization: `Bearer ${token.value}` },
    })
    if (!response.ok) {
      throw new HttpError(response.status, `Request failed with status ${response.status}`)
    }
    return response.blob()
  }

  function addStaffMember(companyId: string, email: string): Promise<MemberInvitation> {
    return authFetch(`/api/companies/${companyId}/staff`, { method: 'POST', body: JSON.stringify({ email }) })
  }

  function listStaff(companyId: string): Promise<CompanyMember[]> {
    return authFetch(`/api/companies/${companyId}/staff`)
  }

  return {
    getOnboardingStatus,
    listMyCompanies,
    getCompany,
    createCompany,
    listPlans,
    selectPlan,
    completeProfile,
    getLogoBlob,
    addStaffMember,
    listStaff,
  }
}
