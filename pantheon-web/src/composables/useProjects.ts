import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type Plan = 'BASIC' | 'PRO' | 'UNLIMITED'
export type ProjectRole = 'ADMIN' | 'MEMBER'

export interface ActiveProject {
  id: string
  role: ProjectRole
}

export interface OnboardingStatus {
  hasProject: boolean
  activeProject: ActiveProject | null
  needsPlanSelection: boolean
  expiredProjectId: string | null
}

export interface ProjectMembership {
  projectId: string
  projectName: string
  role: ProjectRole
  active: boolean
}

export interface Project {
  id: string
  name: string
  plan: Plan | null
  trialExpiresAt: string
  planValidUntil: string | null
}

export interface ProjectRegistrationData {
  projectName: string
  cnpjCpf: string
  legalName: string
  address: string
  postalCode: string
}

export type SiteStatus = 'PLANNING' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED'
export type ConstructionFunction = 'CLIENT' | 'ENGINEER' | 'ARCHITECT' | 'SITE_FOREMAN' | 'SERVICE_PROVIDER' | 'OTHER'

export interface ConstructionSite {
  id: string
  projectId: string
  name: string
  address: string
  status: SiteStatus
  startDate: string
  expectedEndDate: string | null
}

export interface ConstructionSiteRegistrationData {
  name: string
  address: string
  startDate: string
  expectedEndDate: string | null
}

export interface ArchitecturalProject {
  id: string
  projectId: string
  constructionSiteId: string | null
  name: string
  description: string | null
}

export interface ArchitecturalProjectRegistrationData {
  name: string
  description: string | null
  constructionSiteId: string | null
}

export type EquipmentStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'UNAVAILABLE'

export interface Equipment {
  id: string
  constructionSiteId: string
  name: string
  type: string | null
  status: EquipmentStatus
}

export interface EquipmentRegistrationData {
  name: string
  type: string | null
  status: EquipmentStatus
}

export interface MaterialItem {
  id: string
  constructionSiteId: string
  name: string
  unit: string
}

export interface MaterialRegistrationData {
  name: string
  unit: string
}

export type MembershipStatus = 'INVITED' | 'ACTIVE'

export interface ProjectMember {
  membershipId: string
  userId: string
  email: string
  displayName: string
  role: ProjectRole
  function: ConstructionFunction | null
  specialty: string | null
  status: MembershipStatus
  invited: boolean
}

export interface AddMemberData {
  email: string
  function: ConstructionFunction | null
  specialty: string | null
}

export interface MemberInvitation {
  invitationId: string
  membershipId: string
  email: string
  requiresRegistration: boolean
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

export function useProjects() {
  function getOnboardingStatus(): Promise<OnboardingStatus> {
    return authFetch('/api/onboarding/status')
  }

  function listMyProjects(): Promise<ProjectMembership[]> {
    return authFetch('/api/projects/me')
  }

  function createProject(data: ProjectRegistrationData): Promise<Project> {
    return authFetch('/api/projects', { method: 'POST', body: JSON.stringify(data) })
  }

  function confirmPlan(projectId: string, plan: Plan): Promise<Project> {
    return authFetch(`/api/projects/${projectId}/plan`, { method: 'POST', body: JSON.stringify({ plan }) })
  }

  function addMember(projectId: string, data: AddMemberData): Promise<MemberInvitation> {
    return authFetch(`/api/projects/${projectId}/members`, { method: 'POST', body: JSON.stringify(data) })
  }

  function listMembers(projectId: string): Promise<ProjectMember[]> {
    return authFetch(`/api/projects/${projectId}/members`)
  }

  function listConstructionSites(projectId: string): Promise<ConstructionSite[]> {
    return authFetch(`/api/projects/${projectId}/construction-sites`)
  }

  function createConstructionSite(
    projectId: string,
    data: ConstructionSiteRegistrationData,
  ): Promise<ConstructionSite> {
    return authFetch(`/api/projects/${projectId}/construction-sites`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function updateConstructionSiteStatus(siteId: string, status: SiteStatus): Promise<ConstructionSite> {
    return authFetch(`/api/construction-sites/${siteId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    })
  }

  function listArchitecturalProjects(projectId: string): Promise<ArchitecturalProject[]> {
    return authFetch(`/api/projects/${projectId}/architectural-projects`)
  }

  function createArchitecturalProject(
    projectId: string,
    data: ArchitecturalProjectRegistrationData,
  ): Promise<ArchitecturalProject> {
    return authFetch(`/api/projects/${projectId}/architectural-projects`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function listEquipment(siteId: string): Promise<Equipment[]> {
    return authFetch(`/api/construction-sites/${siteId}/equipment`)
  }

  function createEquipment(siteId: string, data: EquipmentRegistrationData): Promise<Equipment> {
    return authFetch(`/api/construction-sites/${siteId}/equipment`, { method: 'POST', body: JSON.stringify(data) })
  }

  function updateEquipmentStatus(equipmentId: string, status: EquipmentStatus): Promise<Equipment> {
    return authFetch(`/api/equipment/${equipmentId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
  }

  function listMaterials(siteId: string): Promise<MaterialItem[]> {
    return authFetch(`/api/construction-sites/${siteId}/materials`)
  }

  function createMaterial(siteId: string, data: MaterialRegistrationData): Promise<MaterialItem> {
    return authFetch(`/api/construction-sites/${siteId}/materials`, { method: 'POST', body: JSON.stringify(data) })
  }

  return {
    getOnboardingStatus,
    listMyProjects,
    createProject,
    confirmPlan,
    addMember,
    listMembers,
    listConstructionSites,
    createConstructionSite,
    updateConstructionSiteStatus,
    listArchitecturalProjects,
    createArchitecturalProject,
    listEquipment,
    createEquipment,
    updateEquipmentStatus,
    listMaterials,
    createMaterial,
  }
}
