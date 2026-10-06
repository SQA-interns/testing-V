import { describe, expect, it, vi } from "vitest";
import { loadConfig, parseConfig } from "./config";

describe("parseConfig", () => {
  it("keeps only id and name of each workshop", () => {
    expect(
      parseConfig({ workshops: [{ id: "K1", name: "Kappa", extra: true }] }),
    ).toEqual({ workshops: [{ id: "K1", name: "Kappa" }] });
  });

  it.each([
    null,
    "text",
    {},
    { workshops: "K1" },
    { workshops: [{ id: "", name: "Kappa" }] },
    { workshops: [{ id: "K1" }] },
    { workshops: [null] },
  ])("rejects invalid configuration %j", (value) => {
    expect(() => parseConfig(value)).toThrow("Invalid configuration");
  });
});

describe("loadConfig", () => {
  it("fetches /config.json", async () => {
    const fetchImpl = vi.fn(
      async () =>
        new Response(JSON.stringify({ workshops: [] }), { status: 200 }),
    ) as unknown as typeof fetch;

    await expect(loadConfig(fetchImpl)).resolves.toEqual({ workshops: [] });
    expect(vi.mocked(fetchImpl).mock.calls[0][0]).toBe("/config.json");
  });

  it("fails when the configuration is not available", async () => {
    const fetchImpl = vi.fn(
      async () => new Response("", { status: 404 }),
    ) as unknown as typeof fetch;

    await expect(loadConfig(fetchImpl)).rejects.toThrow("not available");
  });
});
