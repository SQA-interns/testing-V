import { afterEach, describe, expect, it, vi } from "vitest";
import {
  fetchWorkshops,
  submitRegistration,
  type RegistrationRequest,
} from "./api";

const request: RegistrationRequest = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.com",
  payerType: "private",
  companyName: null,
  companyAddress: null,
  companyVatId: null,
  workshops: [],
};

function respond(status: number, body: unknown) {
  return vi.fn().mockResolvedValue(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("submitRegistration", () => {
  it("posts JSON to /api/registrations and returns the registration on 201", async () => {
    const fetchMock = respond(201, {
      registrationNumber: "REG-000001",
      netFee: 240,
    });
    vi.stubGlobal("fetch", fetchMock);

    const result = await submitRegistration(request);

    expect(result).toEqual({
      kind: "registered",
      registration: { registrationNumber: "REG-000001", netFee: 240 },
    });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual(request);
  });

  it("returns the invalid fields on 400", async () => {
    vi.stubGlobal(
      "fetch",
      respond(400, { error: "validation_failed", fields: ["email"] }),
    );
    expect(await submitRegistration(request)).toEqual({
      kind: "invalid",
      fields: ["email"],
    });
  });

  it("treats a 400 without fields as invalid with no field names", async () => {
    vi.stubGlobal("fetch", respond(400, { error: "malformed_request" }));
    expect(await submitRegistration(request)).toEqual({
      kind: "invalid",
      fields: [],
    });
  });

  it.each([429, 500, 503])("reports failure on %i", async (status) => {
    vi.stubGlobal("fetch", respond(status, { error: "x" }));
    expect(await submitRegistration(request)).toEqual({ kind: "failed" });
  });

  it("reports failure when the network fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    expect(await submitRegistration(request)).toEqual({ kind: "failed" });
  });
});

describe("fetchWorkshops", () => {
  it("returns the list from /api/workshops", async () => {
    vi.stubGlobal("fetch", respond(200, [{ id: "W1", title: "One" }]));
    expect(await fetchWorkshops()).toEqual([{ id: "W1", title: "One" }]);
  });

  it("returns an empty list on error", async () => {
    vi.stubGlobal("fetch", respond(429, { error: "rate_limited" }));
    expect(await fetchWorkshops()).toEqual([]);
  });
});
