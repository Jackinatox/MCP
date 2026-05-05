import { useNavigate } from "react-router"
import { Server, ChevronRight } from "lucide-react"
import { StatusBadge } from "@/components/StatusBadge"
import type { ServerEntity } from "@/types/server"

export function ServerCard({ server }: { server: ServerEntity }) {
  const navigate = useNavigate()

  return (
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
          <ChevronRight className="h-4 w-4 text-muted-foreground" />
        </div>
      </div>
    </div>
  )
}
