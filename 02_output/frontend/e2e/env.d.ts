// Minimal typing for environment variables read by Playwright files (no @types/node in tech-stack).
declare const process: { env: Record<string, string | undefined> };
