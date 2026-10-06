/// <reference types="vitest/config" />
import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

// Dev server only: serves /config.json from APP_WORKSHOPS ("id=name;id=name"), as the
// frontend container does at start (docs/02_specification.md 3.6, AR-04).
function devConfigJson(): Plugin {
  return {
    name: "dev-config-json",
    configureServer(server) {
      server.middlewares.use("/config.json", (_request, response) => {
        const workshops = (process.env.APP_WORKSHOPS ?? "")
          .split(";")
          .map((entry) => entry.split("="))
          .filter((parts) => parts.length >= 2 && parts[0].trim() !== "")
          .map(([id, ...name]) => ({
            id: id.trim(),
            name: name.join("=").trim(),
          }));
        response.setHeader("Content-Type", "application/json");
        response.end(JSON.stringify({ workshops }));
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), devConfigJson()],
  server: {
    host: "127.0.0.1",
    proxy: {
      "/api": "http://127.0.0.1:8080",
    },
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}", "tests/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      include: ["src/**"],
    },
  },
});
