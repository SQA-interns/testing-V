import { defineConfig, devices } from "@playwright/test";

// End-to-end tests run against the local stack (`docker compose up` in 02_output/):
// frontend on 127.0.0.1:8000, Mailpit on 127.0.0.1:8025. Override with E2E_BASE_URL and
// E2E_MAILPIT_URL.
export default defineConfig({
  testDir: "./tests/e2e",
  use: { baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:8000" },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
      metadata: {
        mailpitUrl: process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025",
      },
    },
  ],
});
