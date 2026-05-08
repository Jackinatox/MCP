import { useEffect, useState } from "react"
import { useParams, useNavigate } from "react-router"
import { ArrowLeft, Code2 } from "lucide-react"
import Editor from "@monaco-editor/react"
import { Button } from "@/components/ui/button"
import type { GlyphDetail } from "@/types/glyph"

export function GlyphDetailPage() {
  const { glyphId } = useParams<{ glyphId: string }>()
  const navigate = useNavigate()
  const [glyph, setGlyph] = useState<GlyphDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!glyphId) return
    fetch(`/v1/glyph/${glyphId}`)
      .then((r) => {
        if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
        return r.json() as Promise<GlyphDetail>
      })
      .then(setGlyph)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [glyphId])

  if (loading) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        Loading…
      </div>
    )
  }

  if (error || !glyph) {
    return (
      <div className="flex flex-col gap-4">
        <Button variant="ghost" size="sm" className="w-fit" onClick={() => navigate("/admin/glyphs")}>
          <ArrowLeft className="mr-1 h-4 w-4" /> Back
        </Button>
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error ?? "Glyph not found"}
        </div>
      </div>
    )
  }

  const dockerEntries = Object.entries(glyph.dockerImages)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Button variant="ghost" size="sm" className="-ml-2 w-fit" onClick={() => navigate("/admin/glyphs")}>
          <ArrowLeft className="mr-1 h-4 w-4" /> Back
        </Button>
        <div className="flex items-center gap-3">
          <Code2 className="h-5 w-5 text-muted-foreground" />
          <div>
            <h2 className="text-lg leading-none font-semibold">{glyph.name}</h2>
            <p className="mt-1 text-sm text-muted-foreground">{glyph.description}</p>
          </div>
        </div>
      </div>

      {/* Meta */}
      <div className="rounded-lg border bg-card p-4 text-xs">
        <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1.5">
          <dt className="text-muted-foreground">Author</dt>
          <dd>{glyph.author}</dd>

          <dt className="text-muted-foreground">Schema</dt>
          <dd className="font-mono">{glyph.metaVersion}</dd>

          <dt className="text-muted-foreground">Exported</dt>
          <dd>{new Date(glyph.exportedAt).toLocaleString()}</dd>

          <dt className="text-muted-foreground">Startup</dt>
          <dd className="font-mono break-all">{glyph.startup}</dd>

          <dt className="text-muted-foreground">Stop cmd</dt>
          <dd className="font-mono">{glyph.configStop}</dd>

          {glyph.features.length > 0 && (
            <>
              <dt className="text-muted-foreground">Features</dt>
              <dd className="flex flex-wrap gap-1">
                {glyph.features.map((f) => (
                  <span key={f} className="rounded bg-muted px-1.5 py-0.5 font-mono">
                    {f}
                  </span>
                ))}
              </dd>
            </>
          )}
        </dl>
      </div>

      {/* Docker images */}
      {dockerEntries.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Docker Images</p>
          <div className="rounded-lg border bg-card divide-y text-xs">
            {dockerEntries.map(([label, image]) => (
              <div key={label} className="flex items-center gap-4 px-4 py-2.5">
                <span className="w-28 shrink-0 font-medium">{label}</span>
                <span className="font-mono text-muted-foreground break-all">{image}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Variables */}
      {glyph.envVars.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Variables</p>
          <div className="rounded-lg border bg-card divide-y text-xs">
            {glyph.envVars.map((v) => (
              <div key={v.env_variable} className="flex flex-col gap-1 px-4 py-3">
                <div className="flex items-center gap-2">
                  <span className="font-medium">{v.name}</span>
                  <span className="font-mono text-muted-foreground">{v.env_variable}</span>
                  {v.required && (
                    <span className="rounded bg-destructive/15 px-1.5 py-0.5 text-destructive font-medium">
                      required
                    </span>
                  )}
                  {!v.user_editable && (
                    <span className="rounded bg-muted px-1.5 py-0.5 text-muted-foreground">
                      locked
                    </span>
                  )}
                </div>
                {v.description && (
                  <p className="text-muted-foreground">{v.description}</p>
                )}
                {v.default_value !== "" && (
                  <p className="font-mono text-muted-foreground">
                    default: <span className="text-foreground">{v.default_value}</span>
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Install script */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium">Install Script</p>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="font-mono">{glyph.installContainer}</span>
            <span>·</span>
            <span className="font-mono">{glyph.installEntrypoint}</span>
          </div>
        </div>
        <div className="overflow-hidden rounded-lg border">
          <Editor
            height="400px"
            defaultLanguage="shell"
            value={glyph.installScript}
            theme="vs-dark"
            options={{
              readOnly: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              fontSize: 12,
              lineNumbers: "on",
              wordWrap: "on",
              renderLineHighlight: "none",
            }}
          />
        </div>
      </div>
    </div>
  )
}
