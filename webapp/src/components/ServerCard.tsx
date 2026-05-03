import { useState } from "react"
import { Server, ChevronRight } from "lucide-react"
import { StatusBadge } from "@/components/StatusBadge"
import type { ServerEntity } from "@/types/server"

export function ServerCard({ server }: { server: ServerEntity }) {
  const [open, setOpen] = useState(false)
  const envEntries = Object.entries(server.env)

  return (
    <div className="rounded-lg border bg-card text-card-foreground">
      <div
        className="flex cursor-pointer items-center justify-between p-4"
        onClick={() => setOpen((v) => !v)}
      >
        <div className="flex items-center gap-3">
          <Server className="h-4 w-4 text-muted-foreground" />
          <div>
            <p className="font-medium leading-none">{server.name}</p>
            {server.description && (
              <p className="mt-1 text-xs text-muted-foreground">{server.description}</p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={server.status} />
          <ChevronRight
            className={`h-4 w-4 text-muted-foreground transition-transform ${open ? "rotate-90" : ""}`}
          />
        </div>
      </div>

      {open && (
        <div className="border-t px-4 pb-4 pt-3 text-xs">
          <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1.5">
            <dt className="text-muted-foreground">ID</dt>
            <dd className="font-mono break-all">{server.id}</dd>

            <dt className="text-muted-foreground">Image</dt>
            <dd className="font-mono">{server.image || "—"}</dd>

            <dt className="text-muted-foreground">Container</dt>
            <dd className="font-mono">{server.containerId ?? "—"}</dd>

            <dt className="text-muted-foreground">Memory</dt>
            <dd>{server.memoryMb} MB</dd>

            <dt className="text-muted-foreground">CPU</dt>
            <dd>{server.cpuPercent}%</dd>

            <dt className="text-muted-foreground">Start command</dt>
            <dd className="font-mono">{server.startCommand || "—"}</dd>

            <dt className="text-muted-foreground">Created</dt>
            <dd>{new Date(server.createdAt).toLocaleString()}</dd>

            <dt className="text-muted-foreground">Updated</dt>
            <dd>{new Date(server.updatedAt).toLocaleString()}</dd>
          </dl>

          {envEntries.length > 0 && (
            <div className="mt-3">
              <p className="mb-1.5 text-muted-foreground">Environment</p>
              <div className="rounded bg-muted px-3 py-2 font-mono">
                {envEntries.map(([k, v]) => (
                  <div key={k}>
                    <span className="text-primary">{k}</span>=
                    <span className="text-muted-foreground">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
