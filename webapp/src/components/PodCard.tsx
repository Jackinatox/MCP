import { useCallback, useEffect, useState } from "react"
import { Box, ChevronRight, Cpu, HardDrive, MemoryStick, Archive, Plus, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { ServerCard } from "@/components/ServerCard"
import { CreateServerForm } from "@/components/CreateServerForm"
import { Button } from "@/components/ui/button"
import type { PodEntity } from "@/types/pod"
import type { ServerResponse } from "@/types/server"

interface DeleteError {
  status: number
  body: string
}

export function PodCard({
  pod,
  onDeleted,
}: {
  pod: PodEntity
  onDeleted?: () => void
}) {
  const [open, setOpen] = useState(false)
  const [data, setData] = useState<ServerResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<DeleteError | null>(null)

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

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation()
    setDeleting(true)
    try {
      const res = await fetch(`/v1/pod/${pod.id}`, { method: "DELETE" })
      if (!res.ok) {
        const text = await res.text()
        setDeleteError({ status: res.status, body: text })
      } else {
        toast.success(`Pod ${pod.id} deleted`)
        onDeleted?.()
      }
    } catch (err) {
      setDeleteError({ status: 0, body: String(err) })
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="rounded-lg border bg-card text-card-foreground">
      <div
        role="button"
        tabIndex={0}
        className="flex w-full cursor-pointer items-center justify-between p-4 text-left transition-colors hover:bg-accent/40"
        onClick={() => setOpen((o) => !o)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault()
            setOpen((o) => !o)
          }
        }}
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
        <div className="flex items-center gap-3">
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="rounded p-1 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-50"
          >
            <Trash2 className="h-4 w-4" />
          </button>
          <ChevronRight
            className={`h-4 w-4 shrink-0 text-muted-foreground transition-transform ${open ? "rotate-90" : ""}`}
          />
        </div>
      </div>

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

      {deleteError && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          onClick={() => setDeleteError(null)}
        >
          <div
            className="max-h-[80vh] w-full max-w-md overflow-auto rounded-lg border bg-card p-5 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <p className="mb-2 font-semibold text-destructive">
              Error {deleteError.status || ""}
            </p>
            <pre className="whitespace-pre-wrap break-all rounded bg-muted p-3 text-xs text-muted-foreground">
              {(() => {
                try {
                  return JSON.stringify(JSON.parse(deleteError.body), null, 2)
                } catch {
                  return deleteError.body
                }
              })()}
            </pre>
            <button
              onClick={() => setDeleteError(null)}
              className="mt-4 w-full rounded border px-3 py-1.5 text-sm hover:bg-accent"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
