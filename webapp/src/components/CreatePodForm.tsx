import { useEffect, useRef, useState } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"

interface CreatePodFields {
  cpuPercent: string
  memoryMb: string
  diskMb: string
  backups: string
}

const EMPTY_FORM: CreatePodFields = {
  cpuPercent: "100",
  memoryMb: "1024",
  diskMb: "10240",
  backups: "0",
}

function Field({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-muted-foreground">
        {label}
        <span className="ml-0.5 text-destructive">*</span>
      </span>
      {children}
    </label>
  )
}

const inputCls =
  "rounded-md border bg-background px-3 py-1.5 text-sm outline-none ring-ring/50 focus:ring-2 disabled:opacity-50"

export function CreatePodForm({
  onCreated,
  onCancel,
}: {
  onCreated: () => void
  onCancel: () => void
}) {
  const [fields, setFields] = useState<CreatePodFields>(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const firstRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    firstRef.current?.focus()
  }, [])

  const set = (key: keyof CreatePodFields, value: string) =>
    setFields((f) => ({ ...f, [key]: value }))

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    const body = {
      cpuPercent: Number(fields.cpuPercent),
      memoryMb: Number(fields.memoryMb),
      diskMb: Number(fields.diskMb),
      backups: Number(fields.backups),
    }

    fetch("/v1/pod", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    })
      .then((r) => {
        if (!r.ok)
          return r
            .text()
            .then((t) =>
              Promise.reject(new Error(t || `${r.status} ${r.statusText}`))
            )
        return r.json()
      })
      .then(() => onCreated())
      .catch((e: Error) => setError(e.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <form
      onSubmit={submit}
      className="rounded-lg border bg-card text-card-foreground"
    >
      <div className="flex items-center justify-between border-b px-4 py-3">
        <p className="font-medium">New pod</p>
        <button
          type="button"
          onClick={onCancel}
          className="text-muted-foreground hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="flex flex-col gap-3 p-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label="CPU %">
            <input
              ref={firstRef}
              className={inputCls}
              type="number"
              min={1}
              value={fields.cpuPercent}
              onChange={(e) => set("cpuPercent", e.target.value)}
              required
            />
          </Field>
          <Field label="Memory (MB)">
            <input
              className={inputCls}
              type="number"
              min={1}
              value={fields.memoryMb}
              onChange={(e) => set("memoryMb", e.target.value)}
              required
            />
          </Field>
          <Field label="Disk (MB)">
            <input
              className={inputCls}
              type="number"
              min={1}
              value={fields.diskMb}
              onChange={(e) => set("diskMb", e.target.value)}
              required
            />
          </Field>
          <Field label="Backups">
            <input
              className={inputCls}
              type="number"
              min={0}
              value={fields.backups}
              onChange={(e) => set("backups", e.target.value)}
              required
            />
          </Field>
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
            {submitting ? "Creating…" : "Create pod"}
          </Button>
        </div>
      </div>
    </form>
  )
}
