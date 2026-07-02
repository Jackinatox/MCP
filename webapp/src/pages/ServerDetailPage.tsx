import { useEffect, useRef, useState, useCallback } from "react"
import { useParams, useNavigate } from "react-router"
import { ArrowLeft, Server, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ButtonGroup } from "@/components/ui/button-group"
import { StatusBadge } from "@/components/StatusBadge"
import type { ServerEntity, ServerResponse } from "@/types/server"
import { toast } from "sonner"

export function ServerDetailPage() {
  const { serverId } = useParams<{ serverId: string }>()
  const navigate = useNavigate()

  const [server, setServer] = useState<ServerEntity | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [consoleLines, setConsoleLines] = useState<string[]>([])
  const [eventLines, setEventLines] = useState<string[]>([])

  const consoleEndRef = useRef<HTMLDivElement>(null)
  const eventEndRef = useRef<HTMLDivElement>(null)

  const fetchServer = useCallback(() => {
    if (!serverId) return
    setLoading(true)
    setError(null)
    fetch("/v1/server")
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<ServerResponse>
      })
      .then((data) => {
        const found = data.servers.find((s) => s.id === serverId)
        if (!found) throw new Error("Server not found")
        setServer(found)
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [serverId])

  useEffect(() => {
    fetchServer()
  }, [fetchServer])

  useEffect(() => {
    consoleEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [consoleLines])

  useEffect(() => {
    eventEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [eventLines])

  useEffect(() => {
    if (!serverId) return

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
    const ws = new WebSocket(
      `${protocol}//${window.location.host}/v1/ws/console/${serverId}`
    )

    ws.onopen = () => {
      setEventLines((prev) => [...prev, "[connected] WebSocket connected"])
    }

    ws.onmessage = (event: MessageEvent<string>) => {
      try {
        const data = JSON.parse(event.data) as Record<string, unknown>
        if (data.type === "console_output") {
          const line = typeof data.message === "string" ? data.message : event.data
          setConsoleLines((prev) => [...prev, line])
        } else {
          setEventLines((prev) => [...prev, JSON.stringify(data, null, 2)])
        }
      } catch {
        setConsoleLines((prev) => [...prev, event.data])
      }
    }

    ws.onclose = (e) => {
      setEventLines((prev) => [
        ...prev,
        `[disconnected] WebSocket closed (code ${e.code})`,
      ])
    }

    ws.onerror = () => {
      setEventLines((prev) => [...prev, "[error] WebSocket error"])
    }

    return () => {
      ws.close()
    }
  }, [serverId])

  const powerAction = async (action: string) => {
    if (!serverId) return
    const response = await fetch(
      `/v1/server/${serverId}/power?action=${action}`,
      {
        method: "POST",
      }
    )
    if (!response.ok) {
      toast.error(
        `Failed to ${action.toLowerCase()}: ${response.status} ${response.statusText}`
      )
    } else {
      toast.success(`Server ${action.toLowerCase()}ing`)
      setTimeout(fetchServer, 1000)
    }
  }

  const handleReinstall = async () => {
    if (!serverId || !server) return
    const response = await fetch(`/v1/server/${serverId}/reinstall`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ image: server.image }),
    })
    if (!response.ok) {
      toast.error(
        `Failed to reinstall: ${response.status} ${response.statusText}`
      )
    } else {
      toast.success("Server reinstalling")
      setTimeout(fetchServer, 1000)
    }
  }

  if (loading && !server) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        Loading…
      </div>
    )
  }

  if (error || !server) {
    return (
      <div className="flex flex-col gap-4">
        <Button
          variant="ghost"
          size="sm"
          className="w-fit"
          onClick={() => navigate("/servers")}
        >
          <ArrowLeft className="mr-1 h-4 w-4" /> Back
        </Button>
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error ?? "Server not found"}
        </div>
      </div>
    )
  }

  const envEntries = Object.entries(server.env)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Button
          variant="ghost"
          size="sm"
          className="-ml-2 w-fit"
          onClick={() => navigate("/servers")}
        >
          <ArrowLeft className="mr-1 h-4 w-4" /> Back
        </Button>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Server className="h-5 w-5 text-muted-foreground" />
            <div>
              <h2 className="text-lg leading-none font-semibold">
                {server.name}
              </h2>
              {server.description && (
                <p className="mt-1 text-sm text-muted-foreground">
                  {server.description}
                </p>
              )}
            </div>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={server.status} />
            <Button
              variant="ghost"
              size="sm"
              onClick={fetchServer}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
              />
            </Button>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <ButtonGroup>
          <Button onClick={() => powerAction("START")}>Start</Button>
          <Button variant="outline" onClick={() => powerAction("STOP")}>
            Stop
          </Button>
          <Button variant="outline" onClick={() => powerAction("RESTART")}>
            Restart
          </Button>
        </ButtonGroup>
        <Button variant="outline" onClick={handleReinstall}>
          Reinstall
        </Button>
      </div>

      <div className="rounded-lg border bg-card p-4 text-xs">
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

          <dt className="text-muted-foreground">Disk</dt>
          <dd>{server.diskMb > 0 ? `${server.diskMb} MB` : "Unlimited (pod limit)"}</dd>

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

      <div className="grid grid-cols-2 gap-4">
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Console Output</p>
          <div className="h-80 overflow-auto rounded-lg border bg-black p-3 font-mono text-xs text-green-400">
            {consoleLines.length === 0 ? (
              <span className="text-zinc-600">No output yet…</span>
            ) : (
              consoleLines.map((line, i) => (
                <div key={i} className="break-all whitespace-pre-wrap">
                  {line}
                </div>
              ))
            )}
            <div ref={consoleEndRef} />
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Events</p>
          <div className="h-80 overflow-auto rounded-lg border bg-black p-3 font-mono text-xs text-blue-300">
            {eventLines.length === 0 ? (
              <span className="text-zinc-600">No events yet…</span>
            ) : (
              eventLines.map((line, i) => (
                <pre key={i} className="break-all whitespace-pre-wrap">
                  {line}
                </pre>
              ))
            )}
            <div ref={eventEndRef} />
          </div>
        </div>
      </div>
    </div>
  )
}
