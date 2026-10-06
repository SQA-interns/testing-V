import { defineConfig, devices } from "@playwright/test";

// Runs against the local stack (`docker compose --env-file ../.env up --build` in 02_output/).
export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.e2e.spec.ts",
  timeout: 60_000,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:3000",
    trace: "off",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
