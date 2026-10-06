import { defineConfig, devices } from "@playwright/test";

// End-to-end tests run against the frontend at E2E_BASE_URL (default: the Vite dev
// server, which proxies /api to the backend on 127.0.0.1:8080) and read e-mail from
// Mailpit at MAILPIT_URL. Backend, PostgreSQL and Mailpit must be running, for
// example with `docker compose up` in 02_output (docs/03_test-strategy.md).
const baseURL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report" }],
  ],
  use: {
    baseURL,
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: "npm run dev -- --port 5173 --strictPort",
        url: baseURL,
        reuseExistingServer: true,
        timeout: 60_000,
      },
});
