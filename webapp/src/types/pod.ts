export interface PodEntity {
  id: string
  cpuPercent: number
  memoryMb: number
  diskMb: number
  backups: number
}

export interface PodResponse {
  count: number
  pods: PodEntity[]
}
