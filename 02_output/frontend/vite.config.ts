import { defineConfig, type Plugin } from "vitest/config";
import react from "@vitejs/plugin-react";

// Serves /config.js in development from APP_WORKSHOPS ("id=title;id=title"), like the container
// does at start (D-14). Values are JSON-encoded and "<" escaped so they cannot leave the script.
function runtimeConfig(): Plugin {
  return {
    name: "runtime-config",
    configureServer(server) {
      server.middlewares.use("/config.js", (_request, response) => {
        const workshops = (process.env.APP_WORKSHOPS ?? "")
          .split(";")
          .map((entry) => entry.trim())
          .filter((entry) => entry !== "")
          .map((entry) => {
            const separator = entry.indexOf("=");
            const id = (
              separator < 0 ? entry : entry.slice(0, separator)
            ).trim();
            const title =
              separator < 0 ? id : entry.slice(separator + 1).trim();
            return { id, title };
          })
          .filter((workshop) => workshop.id !== "");
        const json = JSON.stringify({ workshops }).replace(/</g, "\\u003c");
        response.setHeader("Content-Type", "text/javascript; charset=utf-8");
        response.end(`window.APP_CONFIG = ${json};\n`);
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), runtimeConfig()],
  server: {
    host: "127.0.0.1",
    proxy: { "/api": "http://127.0.0.1:8080" },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/setupTests.ts"],
    include: ["src/**/*.test.{ts,tsx}", "tests/acceptance/**/*.test.{ts,tsx}"],
    passWithNoTests: true,
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      reporter: ["text", "html", "json-summary"],
    },
  },
});
