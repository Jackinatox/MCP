export type PodStatus = "ACTIVE" | "DELETING" | "DELETED"

export interface PodEntity {
  id: string
  cpuPercent: number
  memoryMb: number
  diskMb: number
  backups: number
  status: PodStatus
}

export interface PodResponse {
  count: number
  pods: PodEntity[]
}

export const POD_STATUS_COLORS: Record<PodStatus, string> = {
  ACTIVE: "bg-green-500",
  DELETING: "bg-orange-400",
  DELETED: "bg-gray-400",
}
