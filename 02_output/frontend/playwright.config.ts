import { defineConfig, devices } from "@playwright/test";

// End-to-end tests of the registration form (docs/02_contracts/registration-form.yaml).
// The form runs on the Vite dev server, which proxies /api to the backend of the local stack
// (`docker compose up` in 02_output/). E2E_API_URL and E2E_MAILPIT_URL point at that stack;
// ORGANIZER_USERNAME and ORGANIZER_PASSWORD are passed from .env by the command that runs the tests.
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
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: "npm run dev -- --host 127.0.0.1 --port 5173 --strictPort",
        url: "http://127.0.0.1:5173",
        reuseExistingServer: true,
        timeout: 60_000,
      },
});
