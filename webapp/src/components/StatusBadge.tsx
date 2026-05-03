import { STATUS_COLORS, type ServerStatus } from "@/types/server"

export function StatusBadge({ status }: { status: ServerStatus }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium">
      <span className={`h-2 w-2 rounded-full ${STATUS_COLORS[status] ?? "bg-gray-400"}`} />
      {status}
    </span>
  )
}
