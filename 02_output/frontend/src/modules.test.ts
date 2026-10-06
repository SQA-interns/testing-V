// Unit tests for format.ts, config.ts and api.ts (phase 5).
import { afterEach, describe, expect, it, vi } from "vitest";
import { submitRegistration, type RegistrationRequest } from "./api";
import { configuredWorkshops } from "./config";
import { formatAmount } from "./format";

type AppWindow = Window & { APP_CONFIG?: unknown };

const request: RegistrationRequest = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana.novak@example.org",
  payerType: "private",
  workshops: [],
};

afterEach(() => {
  vi.unstubAllGlobals();
  delete (window as AppWindow).APP_CONFIG;
});

describe("formatAmount", () => {
  it("formats numbers and decimal strings with two decimals", () => {
    expect(formatAmount(240)).toBe("240.00");
    expect(formatAmount(52.8)).toBe("52.80");
    expect(formatAmount("292.80")).toBe("292.80");
    expect(formatAmount("0.5")).toBe("0.50");
  });

  it("returns non-numeric input unchanged", () => {
    expect(formatAmount("n/a")).toBe("n/a");
  });
});

describe("configuredWorkshops", () => {
  it("returns the configured workshops", () => {
    (window as AppWindow).APP_CONFIG = {
      workshops: [{ id: "A", title: "Alpha" }],
    };
    expect(configuredWorkshops()).toEqual([{ id: "A", title: "Alpha" }]);
  });

  it("returns no workshops without configuration", () => {
    expect(configuredWorkshops()).toEqual([]);
  });

  it("drops malformed entries", () => {
    (window as AppWindow).APP_CONFIG = {
      workshops: [{ id: "A", title: "Alpha" }, { id: 1 }, null, "B"],
    };
    expect(configuredWorkshops()).toEqual([{ id: "A", title: "Alpha" }]);
  });

  it("ignores a non-list value", () => {
    (window as AppWindow).APP_CONFIG = { workshops: "A=Alpha" };
    expect(configuredWorkshops()).toEqual([]);
  });
});

function respond(status: number, body: string) {
  vi.stubGlobal(
    "fetch",
    vi.fn<typeof fetch>().mockResolvedValue(new Response(body, { status })),
  );
}

describe("submitRegistration", () => {
  it("returns the created registration on 201", async () => {
    respond(201, JSON.stringify({ registrationNumber: "CR-1" }));
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "created",
      registration: { registrationNumber: "CR-1" },
    });
  });

  it("returns well-formed field errors on 422", async () => {
    respond(
      422,
      JSON.stringify({
        errors: [{ field: "email", message: "bad" }, { field: 1 }, "x"],
      }),
    );
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "invalid",
      errors: [{ field: "email", message: "bad" }],
    });
  });

  it("treats a 422 without errors as invalid with no field errors", async () => {
    respond(422, JSON.stringify({ title: "Validation failed" }));
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "invalid",
      errors: [],
    });
  });

  it("returns rateLimited on 429", async () => {
    respond(429, "");
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "rateLimited",
    });
  });

  it("returns failed on other statuses, bad JSON and network errors", async () => {
    respond(500, "{}");
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
    respond(201, "not json");
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn<typeof fetch>().mockRejectedValue(new TypeError("offline")),
    );
    await expect(submitRegistration(request)).resolves.toEqual({
      kind: "failed",
    });
  });

  it("posts JSON to /api/registrations", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValue(new Response("{}", { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    await submitRegistration(request);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual(request);
  });
});
