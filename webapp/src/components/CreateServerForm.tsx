import { useEffect, useRef, useState } from "react"
import { Plus, X, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"

interface CreateServerFields {
  name: string
  imageName: string
  description: string
  cpuPercent: string
  memoryMb: string
  glyphId: string
  startCommand: string
  env: { key: string; value: string }[]
}

const EMPTY_FORM: CreateServerFields = {
  name: "",
  imageName: "",
  description: "",
  cpuPercent: "",
  memoryMb: "",
  glyphId: "",
  startCommand: "",
  env: [],
}

function Field({
  label,
  required,
  children,
}: {
  label: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-muted-foreground">
        {label}
        {required && <span className="ml-0.5 text-destructive">*</span>}
      </span>
      {children}
    </label>
  )
}

const inputCls =
  "rounded-md border bg-background px-3 py-1.5 text-sm outline-none ring-ring/50 focus:ring-2 disabled:opacity-50"

export function CreateServerForm({
  onCreated,
  onCancel,
}: {
  onCreated: () => void
  onCancel: () => void
}) {
  const [fields, setFields] = useState<CreateServerFields>(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const firstRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    firstRef.current?.focus()
  }, [])

  const set = (key: keyof Omit<CreateServerFields, "env">, value: string) =>
    setFields((f) => ({ ...f, [key]: value }))

  const addEnv = () => setFields((f) => ({ ...f, env: [...f.env, { key: "", value: "" }] }))
  const removeEnv = (i: number) =>
    setFields((f) => ({ ...f, env: f.env.filter((_, idx) => idx !== i) }))
  const setEnv = (i: number, part: "key" | "value", value: string) =>
    setFields((f) => {
      const env = [...f.env]
      env[i] = { ...env[i], [part]: value }
      return { ...f, env }
    })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    const body = {
      name: fields.name,
      imageName: fields.imageName,
      description: fields.description || undefined,
      cpuPercent: Number(fields.cpuPercent),
      memoryMb: Number(fields.memoryMb),
      glyphId: Number(fields.glyphId),
      startCommand: fields.startCommand || undefined,
      env: Object.fromEntries(fields.env.filter((e) => e.key).map((e) => [e.key, e.value])),
    }

    fetch("/v1/server", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    })
      .then((r) => {
        if (!r.ok) return r.text().then((t) => Promise.reject(new Error(t || `${r.status} ${r.statusText}`)))
        return r.json()
      })
      .then(() => onCreated())
      .catch((e: Error) => setError(e.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <form onSubmit={submit} className="rounded-lg border bg-card text-card-foreground">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <p className="font-medium">New server</p>
        <button type="button" onClick={onCancel} className="text-muted-foreground hover:text-foreground">
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="flex flex-col gap-3 p-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Name" required>
            <input
              ref={firstRef}
              className={inputCls}
              value={fields.name}
              onChange={(e) => set("name", e.target.value)}
              required
            />
          </Field>
          <Field label="Image" required>
            <input
              className={inputCls}
              value={fields.imageName}
              onChange={(e) => set("imageName", e.target.value)}
              required
            />
          </Field>
        </div>

        <Field label="Description">
          <input
            className={inputCls}
            value={fields.description}
            onChange={(e) => set("description", e.target.value)}
          />
        </Field>

        <div className="grid grid-cols-3 gap-3">
          <Field label="CPU %" required>
            <input
              className={inputCls}
              type="number"
              min={1}
              value={fields.cpuPercent}
              onChange={(e) => set("cpuPercent", e.target.value)}
              required
            />
          </Field>
          <Field label="Memory (MB)" required>
            <input
              className={inputCls}
              type="number"
              min={1}
              value={fields.memoryMb}
              onChange={(e) => set("memoryMb", e.target.value)}
              required
            />
          </Field>
          <Field label="Glyph ID" required>
            <input
              className={inputCls}
              type="number"
              min={1}
              value={fields.glyphId}
              onChange={(e) => set("glyphId", e.target.value)}
              required
            />
          </Field>
        </div>

        <Field label="Start command">
          <input
            className={inputCls}
            value={fields.startCommand}
            onChange={(e) => set("startCommand", e.target.value)}
          />
        </Field>

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">Environment variables</span>
            <button
              type="button"
              onClick={addEnv}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
            >
              <Plus className="h-3 w-3" /> Add
            </button>
          </div>
          {fields.env.map((entry, i) => (
            <div key={i} className="flex items-center gap-2">
              <input
                className={`${inputCls} flex-1 font-mono`}
                placeholder="KEY"
                value={entry.key}
                onChange={(e) => setEnv(i, "key", e.target.value)}
              />
              <span className="text-muted-foreground">=</span>
              <input
                className={`${inputCls} flex-1 font-mono`}
                placeholder="value"
                value={entry.value}
                onChange={(e) => setEnv(i, "value", e.target.value)}
              />
              <button
                type="button"
                onClick={() => removeEnv(i)}
                className="text-muted-foreground hover:text-destructive"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>

        {error && (
          <div className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
            {error}
          </div>
        )}

        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" size="sm" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
          <Button type="submit" size="sm" disabled={submitting}>
            {submitting ? "Creating…" : "Create server"}
          </Button>
        </div>
      </div>
    </form>
  )
}
