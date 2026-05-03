import { useEffect, useState } from "react"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ServerCard } from "@/components/ServerCard"
import { CreateServerForm } from "@/components/CreateServerForm"
import type { ServerResponse } from "@/types/server"

export function ServersPage() {
  const [data, setData] = useState<ServerResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  const fetchServers = () => {
    setLoading(true)
    setError(null)
    fetch("/v1/server")
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<ServerResponse>
      })
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchServers()
  }, [])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Servers</h2>
          {data && (
            <p className="text-sm text-muted-foreground">
              {data.count} server{data.count !== 1 ? "s" : ""}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={fetchServers} disabled={loading}>
            {loading ? "Loading…" : "Refresh"}
          </Button>
          <Button size="sm" onClick={() => setCreating(true)} disabled={creating}>
            <Plus className="mr-1 h-3.5 w-3.5" /> New server
          </Button>
        </div>
      </div>

      {creating && (
        <CreateServerForm
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
        <div className="py-8 text-center text-sm text-muted-foreground">Loading…</div>
      )}

      {data && data.servers.length === 0 && !creating && (
        <div className="py-8 text-center text-sm text-muted-foreground">No servers found.</div>
      )}

      {data && data.servers.map((s) => <ServerCard key={s.id} server={s} />)}
    </div>
  )
}
