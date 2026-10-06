import { afterEach, describe, expect, it, vi } from "vitest";
import {
  fetchOptions,
  formatAmount,
  submitRegistration,
  type RegistrationRequest,
} from "./api";

const request: RegistrationRequest = {
  firstName: "Ana",
  lastName: "Novak",
  email: "ana@example.org",
  payerType: "private",
  workshops: [],
  student: false,
};

function answer(status: number, body: string) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => new Response(body, { status })),
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("submitRegistration", () => {
  it("returns the stored registration on 201", async () => {
    answer(201, JSON.stringify({ registrationNumber: "CR-000001" }));
    const result = await submitRegistration(request);
    expect(result).toEqual({
      kind: "created",
      registration: { registrationNumber: "CR-000001" },
    });
  });

  it("returns the field errors on 400", async () => {
    answer(
      400,
      JSON.stringify({
        errors: [{ field: "email", code: "invalid", message: "x" }],
      }),
    );
    expect(await submitRegistration(request)).toEqual({
      kind: "invalid",
      errors: [{ field: "email", code: "invalid", message: "x" }],
    });
  });

  it("treats a 400 without field errors or with a broken body as a failure", async () => {
    answer(400, JSON.stringify({ title: "Invalid request" }));
    expect(await submitRegistration(request)).toEqual({ kind: "failed" });
    answer(400, "not json");
    expect(await submitRegistration(request)).toEqual({ kind: "failed" });
  });

  it("returns duplicate on 409 and failed on other statuses", async () => {
    answer(409, "{}");
    expect(await submitRegistration(request)).toEqual({ kind: "duplicate" });
    for (const status of [413, 415, 429, 500, 503]) {
      answer(status, "{}");
      expect(await submitRegistration(request)).toEqual({ kind: "failed" });
    }
  });

  it("returns failed when the network fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("offline");
      }),
    );
    expect(await submitRegistration(request)).toEqual({ kind: "failed" });
  });

  it("posts JSON to the registration endpoint", async () => {
    const fetchMock = vi.fn(async () => new Response("{}", { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    await submitRegistration(request);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/registrations",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(request),
      }),
    );
  });
});

describe("fetchOptions", () => {
  it("requests the options for the student flag", async () => {
    const fetchMock = vi.fn(
      async () =>
        new Response(JSON.stringify({ workshops: [] }), { status: 200 }),
    );
    vi.stubGlobal("fetch", fetchMock);
    await fetchOptions(true);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/registration-options?student=true",
      expect.anything(),
    );
  });

  it("throws when the backend does not answer 2xx", async () => {
    answer(429, "{}");
    await expect(fetchOptions(false)).rejects.toThrow("options unavailable");
  });
});

describe("formatAmount", () => {
  it("shows two decimals for numbers and decimal strings", () => {
    expect(formatAmount(240)).toBe("240.00");
    expect(formatAmount("52.8")).toBe("52.80");
    expect(formatAmount(0)).toBe("0.00");
  });
});
