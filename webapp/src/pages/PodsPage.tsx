import { useEffect, useState } from "react"
import { Plus } from "lucide-react"
import { PodCard } from "@/components/PodCard"
import { CreatePodForm } from "@/components/CreatePodForm"
import { Button } from "@/components/ui/button"
import type { PodResponse } from "@/types/pod"

export function PodsPage() {
  const [data, setData] = useState<PodResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  const fetchPods = () => {
    setLoading(true)
    setError(null)
    fetch("/v1/pod")
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<PodResponse>
      })
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchPods()
  }, [])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Pods</h2>
          {data && (
            <p className="text-sm text-muted-foreground">
              {data.count} pod{data.count !== 1 ? "s" : ""}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={fetchPods} disabled={loading}>
            {loading ? "Loading…" : "Refresh"}
          </Button>
          <Button size="sm" onClick={() => setCreating(true)} disabled={creating}>
            <Plus className="mr-1 h-3.5 w-3.5" /> New pod
          </Button>
        </div>
      </div>

      {creating && (
        <CreatePodForm
          onCreated={() => {
            setCreating(false)
            fetchPods()
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

      {data && data.pods.length === 0 && (
        <div className="py-8 text-center text-sm text-muted-foreground">No pods found.</div>
      )}

      {data?.pods.map((p) => <PodCard key={p.id} pod={p} />)}
    </div>
  )
}
