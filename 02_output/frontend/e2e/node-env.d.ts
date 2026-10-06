// The e2e harness runs in Node.js; only the environment lookup is needed, so @types/node
// (not in tech-stack.md) is not added.
declare const process: { env: Record<string, string | undefined> };
