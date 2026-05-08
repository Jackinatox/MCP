import { useEffect, useState } from "react"
import { useNavigate } from "react-router"
import { Code2, ChevronRight } from "lucide-react"
import type { GlyphSummary } from "@/types/glyph"

export function GlyphsPage() {
  const [glyphs, setGlyphs] = useState<GlyphSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    fetch("/v1/glyph")
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<GlyphSummary[]>
      })
      .then(setGlyphs)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        Loading…
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
        {error}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold">Glyphs</h2>
        <p className="text-sm text-muted-foreground">
          {glyphs.length} glyph{glyphs.length !== 1 ? "s" : ""} registered
        </p>
      </div>

      {glyphs.length === 0 && (
        <div className="py-8 text-center text-sm text-muted-foreground">
          No glyphs found.
        </div>
      )}

      <div className="flex flex-col gap-2">
        {glyphs.map((g) => (
          <button
            key={g.id}
            onClick={() => navigate(`/admin/glyphs/${g.id}`)}
            className="flex items-center gap-3 rounded-lg border bg-card px-4 py-3 text-left transition-colors hover:bg-accent"
          >
            <Code2 className="h-4 w-4 shrink-0 text-muted-foreground" />
            <div className="min-w-0 flex-1">
              <p className="font-medium leading-none">{g.name}</p>
              <p className="mt-1 truncate font-mono text-xs text-muted-foreground">
                {g.startup}
              </p>
            </div>
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span>{Object.keys(g.dockerImages).length} image{Object.keys(g.dockerImages).length !== 1 ? "s" : ""}</span>
              <span>{g.envVars.length} var{g.envVars.length !== 1 ? "s" : ""}</span>
              <ChevronRight className="h-4 w-4" />
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
