// Minimal typing for the environment variables the Playwright harness reads, so the
// type check needs no @types/node (not listed in tech-stack.md).
declare const process: { env: Record<string, string | undefined> };
