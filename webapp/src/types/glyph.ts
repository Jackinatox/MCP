export interface EggVariable {
  name: string
  description: string
  env_variable: string
  default_value: string
  user_viewable: boolean
  user_editable: boolean
  required: boolean
  rules: string
  field_type: string | null
}

export interface GlyphSummary {
  id: number
  name: string
  envVars: EggVariable[]
  startup: string
  dockerImages: Record<string, string>
}

/** Full glyph as returned by GET /v1/glyph/:id (GlyphEntity serialization) */
export interface GlyphDetail {
  id: number
  comment: string | null
  metaVersion: string
  metaUpdateUrl: string | null
  exportedAt: string
  name: string
  author: string
  description: string
  features: string[]
  dockerImages: Record<string, string>
  fileDenylist: string[]
  startup: string
  configFiles: string
  configStartup: string
  configLogs: string
  configStop: string
  installScript: string
  installContainer: string
  installEntrypoint: string
  envVars: EggVariable[]
}
