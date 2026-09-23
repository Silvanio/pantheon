import { SERVICE_BASE_URL } from '../lib/config'
import { useAuth } from './useAuth'

export interface ScheduleDependencyRef {
  id: string
  predecessorTaskId: string
}

export interface ScheduleTask {
  id: string
  stageId: string
  title: string
  startDate: string
  endDate: string
  responsibleSiteMembershipId: string | null
  percentComplete: number
  sortOrder: number
  dependsOn: ScheduleDependencyRef[]
  taskCardId: string | null
  taskCardTitle: string | null
}

export interface ScheduleStage {
  id: string
  constructionSiteId: string
  name: string
  color: string
  startDate: string
  endDate: string
  sortOrder: number
  percentComplete: number
  tasks: ScheduleTask[]
}

export interface ScheduleStageInput {
  name: string
  color: string
  startDate: string
  endDate: string
}

export interface ScheduleStageUpdate {
  name?: string
  color?: string
  startDate?: string
  endDate?: string
  sortOrder?: number
}

export interface ScheduleTaskInput {
  title: string
  startDate: string
  endDate: string
  responsibleSiteMembershipId: string | null
}

export interface ScheduleTaskUpdate {
  title?: string
  startDate?: string
  endDate?: string
  responsibleSiteMembershipId?: string | null
  clearResponsible?: boolean
  percentComplete?: number
  sortOrder?: number
}

export interface ScheduleTaskDependency {
  id: string
  predecessorTaskId: string
  successorTaskId: string
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
    throw new Error(`Request to ${path} failed with status ${response.status}`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function useConstructionSchedule() {
  function listStages(siteId: string): Promise<ScheduleStage[]> {
    return authFetch(`/api/construction-sites/${siteId}/schedule-stages`)
  }

  function createStage(siteId: string, data: ScheduleStageInput): Promise<ScheduleStage> {
    return authFetch(`/api/construction-sites/${siteId}/schedule-stages`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function updateStage(stageId: string, data: ScheduleStageUpdate): Promise<ScheduleStage> {
    return authFetch(`/api/schedule-stages/${stageId}`, { method: 'PATCH', body: JSON.stringify(data) })
  }

  function deleteStage(stageId: string): Promise<void> {
    return authFetch(`/api/schedule-stages/${stageId}`, { method: 'DELETE' })
  }

  function createTask(stageId: string, data: ScheduleTaskInput): Promise<ScheduleTask> {
    return authFetch(`/api/schedule-stages/${stageId}/tasks`, { method: 'POST', body: JSON.stringify(data) })
  }

  function updateTask(taskId: string, data: ScheduleTaskUpdate): Promise<ScheduleTask> {
    return authFetch(`/api/schedule-tasks/${taskId}`, { method: 'PATCH', body: JSON.stringify(data) })
  }

  function deleteTask(taskId: string): Promise<void> {
    return authFetch(`/api/schedule-tasks/${taskId}`, { method: 'DELETE' })
  }

  function linkDependency(successorTaskId: string, predecessorTaskId: string): Promise<ScheduleTaskDependency> {
    return authFetch(`/api/schedule-tasks/${successorTaskId}/dependencies`, {
      method: 'POST',
      body: JSON.stringify({ predecessorTaskId }),
    })
  }

  function unlinkDependency(successorTaskId: string, dependencyId: string): Promise<void> {
    return authFetch(`/api/schedule-tasks/${successorTaskId}/dependencies/${dependencyId}`, { method: 'DELETE' })
  }

  function createLinkedTaskCard(taskId: string): Promise<ScheduleTask> {
    return authFetch(`/api/schedule-tasks/${taskId}/task-card`, { method: 'POST' })
  }

  return {
    listStages,
    createStage,
    updateStage,
    deleteStage,
    createTask,
    updateTask,
    deleteTask,
    linkDependency,
    unlinkDependency,
    createLinkedTaskCard,
  }
}
