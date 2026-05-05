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
    proxy: {
      "/v1": {
        target: "http://localhost:8080",
        changeOrigin: true,
        ws: true,
        // Let Vite serve the React app itself; proxy everything else to Spring.
        bypass: (req) => {
          if (req.url?.startsWith("/v1/webApp")) return req.url
          return null
        },
      },
    },
  },
})
