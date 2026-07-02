export type ServerStatus =
  | "PROVISIONING"
  | "INSTALLING"
  | "STARTED"
  | "STARTING"
  | "STOPPED"
  | "STOPPING"
  | "ERROR"
  | "TRANSFERRING_LOCKED"

export interface ServerEntity {
  id: string
  name: string
  description: string | null
  containerId: string | null
  image: string
  status: ServerStatus
  skip_scripts: boolean
  memoryMb: number
  cpuPercent: number
  diskMb: number
  env: Record<string, string>
  startCommand: string
  createdAt: string
  updatedAt: string
}

export interface ServerResponse {
  count: number
  servers: ServerEntity[]
}

export const STATUS_COLORS: Record<ServerStatus, string> = {
  STARTED: "bg-green-500",
  STARTING: "bg-yellow-400",
  STOPPED: "bg-gray-400",
  STOPPING: "bg-orange-400",
  ERROR: "bg-red-500",
  PROVISIONING: "bg-yellow-400",
  INSTALLING: "bg-orange-400",
  TRANSFERRING_LOCKED: "bg-purple-400",
}
