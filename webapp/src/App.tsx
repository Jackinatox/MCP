import { BrowserRouter, NavLink, Navigate, Route, Routes } from "react-router"
import { Server } from "lucide-react"
import { ServersPage } from "@/pages/ServersPage"

const TOOLS = [
  { path: "/servers", label: "Servers", icon: Server, component: ServersPage },
]

export function App() {
  return (
    <BrowserRouter basename="/v1/webApp">
      <div className="flex min-h-svh">
        <aside className="w-52 shrink-0 border-r bg-sidebar py-6">
          <p className="mb-4 px-4 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Tools
          </p>
          <nav className="flex flex-col gap-0.5 px-2">
            {TOOLS.map((tool) => (
              <NavLink
                key={tool.path}
                to={tool.path}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors ${
                    isActive
                      ? "bg-sidebar-accent text-sidebar-accent-foreground font-medium"
                      : "text-sidebar-foreground hover:bg-sidebar-accent/60"
                  }`
                }
              >
                <tool.icon className="h-4 w-4 shrink-0" />
                {tool.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="flex-1 overflow-auto p-6">
          <div className="mx-auto max-w-2xl">
            <Routes>
              <Route index element={<Navigate to="/servers" replace />} />
              {TOOLS.map((tool) => (
                <Route key={tool.path} path={tool.path} element={<tool.component />} />
              ))}
            </Routes>
          </div>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
