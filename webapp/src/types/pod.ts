export interface PodEntity {
  id: string
  cpuPercent: number
  memoryMb: number
  diskMb: number
  backups: number
  status: "ACTIVE" | "DELETING" | "DELETED"
}

export interface PodResponse {
  count: number
  pods: PodEntity[]
}
