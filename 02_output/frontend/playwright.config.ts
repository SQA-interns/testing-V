import { defineConfig, devices } from "@playwright/test";

// E2E_BASE_URL unset: Playwright starts the Vite dev server, which proxies /api to the
// backend of the local stack on 127.0.0.1:8080. Set E2E_BASE_URL=http://127.0.0.1:3000 to
// test the frontend container of `docker compose up` instead.
const externalBaseUrl = process.env.E2E_BASE_URL;
const devServerUrl = "http://127.0.0.1:5173";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: { timeout: 5_000 },
  retries: 0,
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: externalBaseUrl ?? devServerUrl,
    actionTimeout: 5_000,
    trace: "off",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: externalBaseUrl
    ? undefined
    : {
        command: "npm run dev -- --port 5173 --strictPort",
        url: devServerUrl,
        reuseExistingServer: true,
        timeout: 60_000,
      },
});
