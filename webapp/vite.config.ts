import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig } from "vite"

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: "/v1/webApp/",
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    // HMR WebSocket must connect directly to Vite since Spring's servlet filter
    // cannot proxy WebSocket upgrades.
    hmr: {
      port: 5173,
      clientPort: 5173,
    },
  },
})
