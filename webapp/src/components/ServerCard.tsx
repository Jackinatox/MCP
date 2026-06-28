import { useState } from "react"
import { useNavigate } from "react-router"
import { Server, ChevronRight, Trash2 } from "lucide-react"
import { StatusBadge } from "@/components/StatusBadge"
import type { ServerEntity } from "@/types/server"

interface DeleteError {
  status: number
  body: string
}

export function ServerCard({
  server,
  onDeleted,
}: {
  server: ServerEntity
  onDeleted?: () => void
}) {
  const navigate = useNavigate()
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<DeleteError | null>(null)

  const handleDelete = async (e: React.MouseEvent) => {
    e.stopPropagation()
    setDeleting(true)
    try {
      const res = await fetch(`/v1/server/${server.id}`, { method: "DELETE" })
      if (!res.ok) {
        const text = await res.text()
        setDeleteError({ status: res.status, body: text })
      } else {
        onDeleted?.()
      }
    } catch (err) {
      setDeleteError({ status: 0, body: String(err) })
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <div
        className="cursor-pointer rounded-lg border bg-card text-card-foreground transition-colors hover:bg-accent/40"
        onClick={() => navigate(`/servers/${server.id}`)}
      >
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <Server className="h-4 w-4 text-muted-foreground" />
            <div>
              <p className="leading-none font-medium">{server.name}</p>
              {server.description && (
                <p className="mt-1 text-xs text-muted-foreground">
                  {server.description}
                </p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={server.status} />
            <button
              onClick={handleDelete}
              disabled={deleting}
              className="rounded p-1 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-50"
            >
              <Trash2 className="h-4 w-4" />
            </button>
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
          </div>
        </div>
      </div>

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
    </>
  )
}
