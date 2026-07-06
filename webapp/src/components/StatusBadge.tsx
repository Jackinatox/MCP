export function StatusBadge({
  status,
  colorClass,
}: {
  status: string
  colorClass?: string
}) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium">
      <span className={`h-2 w-2 rounded-full ${colorClass ?? "bg-gray-400"}`} />
      {status}
    </span>
  )
}
