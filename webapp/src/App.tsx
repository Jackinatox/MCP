import { BrowserRouter, NavLink, Navigate, Route, Routes } from "react-router"
import { Code2, Server } from "lucide-react"
import { ServersPage } from "@/pages/ServersPage"
import { ServerDetailPage } from "@/pages/ServerDetailPage"
import { GlyphsPage } from "@/pages/GlyphsPage"
import { GlyphDetailPage } from "@/pages/GlyphDetailPage"

const TOOLS = [
  { path: "/servers", label: "Servers", icon: Server },
]

const ADMIN = [
  { path: "/admin/glyphs", label: "Glyphs", icon: Code2 },
]

function SidebarLink({ path, label, icon: Icon }: { path: string; label: string; icon: React.ElementType }) {
  return (
    <NavLink
      to={path}
      className={({ isActive }) =>
        `flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors ${
          isActive
            ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
            : "text-sidebar-foreground hover:bg-sidebar-accent/60"
        }`
      }
    >
      <Icon className="h-4 w-4 shrink-0" />
      {label}
    </NavLink>
  )
}

export function App() {
  return (
    <BrowserRouter basename="/v1/webApp">
      <div className="flex min-h-svh">
        <aside className="w-52 shrink-0 border-r bg-sidebar py-6">
          <p className="mb-4 px-4 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            Tools
          </p>
          <nav className="flex flex-col gap-0.5 px-2">
            {TOOLS.map((tool) => (
              <SidebarLink key={tool.path} {...tool} />
            ))}
          </nav>

          <p className="mb-4 mt-6 px-4 text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            Admin
          </p>
          <nav className="flex flex-col gap-0.5 px-2">
            {ADMIN.map((item) => (
              <SidebarLink key={item.path} {...item} />
            ))}
          </nav>
        </aside>

        <main className="flex-1 overflow-auto p-6">
          <Routes>
            <Route index element={<Navigate to="/servers" replace />} />
            <Route
              path="/servers"
              element={
                <div className="mx-auto max-w-2xl">
                  <ServersPage />
                </div>
              }
            />
            <Route path="/servers/:serverId" element={<ServerDetailPage />} />

            <Route
              path="/admin/glyphs"
              element={
                <div className="mx-auto max-w-2xl">
                  <GlyphsPage />
                </div>
              }
            />
            <Route path="/admin/glyphs/:glyphId" element={<GlyphDetailPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
