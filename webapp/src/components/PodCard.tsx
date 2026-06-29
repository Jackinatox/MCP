import { useCallback, useEffect, useState } from "react"
import { Box, ChevronRight, Cpu, HardDrive, MemoryStick, Archive, Plus } from "lucide-react"
import { ServerCard } from "@/components/ServerCard"
import { CreateServerForm } from "@/components/CreateServerForm"
import { Button } from "@/components/ui/button"
import type { PodEntity } from "@/types/pod"
import type { ServerResponse } from "@/types/server"

export function PodCard({ pod }: { pod: PodEntity }) {
  const [open, setOpen] = useState(false)
  const [data, setData] = useState<ServerResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  const fetchServers = useCallback(() => {
    setLoading(true)
    setError(null)
    fetch(`/v1/pod/${pod.id}`)
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<ServerResponse>
      })
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [pod.id])

  useEffect(() => {
    if (open && !data && !loading) fetchServers()
  }, [open, data, loading, fetchServers])

  return (
    <div className="rounded-lg border bg-card text-card-foreground">
      <button
        className="flex w-full items-center justify-between p-4 text-left transition-colors hover:bg-accent/40"
        onClick={() => setOpen((o) => !o)}
      >
        <div className="flex items-center gap-3">
          <Box className="h-4 w-4 text-muted-foreground" />
          <div>
            <p className="font-mono text-sm leading-none font-medium">{pod.id}</p>
            <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Cpu className="h-3 w-3" /> {pod.cpuPercent}%
              </span>
              <span className="flex items-center gap-1">
                <MemoryStick className="h-3 w-3" /> {pod.memoryMb} MB
              </span>
              <span className="flex items-center gap-1">
                <HardDrive className="h-3 w-3" /> {pod.diskMb} MB
              </span>
              <span className="flex items-center gap-1">
                <Archive className="h-3 w-3" /> {pod.backups} backups
              </span>
            </div>
          </div>
        </div>
        <ChevronRight
          className={`h-4 w-4 shrink-0 text-muted-foreground transition-transform ${open ? "rotate-90" : ""}`}
        />
      </button>

      {open && (
        <div className="flex flex-col gap-2 border-t p-3">
          <div className="flex justify-end">
            <Button size="sm" onClick={() => setCreating(true)} disabled={creating}>
              <Plus className="mr-1 h-3.5 w-3.5" /> New server
            </Button>
          </div>

          {creating && (
            <CreateServerForm
              podId={pod.id}
              onCreated={() => {
                setCreating(false)
                fetchServers()
              }}
              onCancel={() => setCreating(false)}
            />
          )}

          {error && (
            <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {error}
            </div>
          )}

          {loading && !data && (
            <div className="py-4 text-center text-sm text-muted-foreground">Loading…</div>
          )}

          {data && data.servers.length === 0 && !creating && (
            <div className="py-4 text-center text-sm text-muted-foreground">
              No servers in this pod.
            </div>
          )}

          {data?.servers.map((s) => (
            <ServerCard key={s.id} server={s} onDeleted={fetchServers} />
          ))}
        </div>
      )}
    </div>
  )
}
