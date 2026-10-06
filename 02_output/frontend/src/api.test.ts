import { describe, expect, it, vi } from "vitest";
import {
  formatAmount,
  submitRegistration,
  type RegistrationRequest,
} from "./api";

const request: RegistrationRequest = {
  firstName: "Ana",
  lastName: "Kovač",
  email: "ana@example.com",
  payerType: "private",
  workshops: [],
};

function respond(status: number, body?: unknown): typeof fetch {
  return vi.fn(async () =>
    body === undefined
      ? new Response(null, { status })
      : new Response(JSON.stringify(body), {
          status,
          headers: { "Content-Type": "application/json" },
        }),
  ) as unknown as typeof fetch;
}

describe("submitRegistration", () => {
  it("posts JSON to /api/registrations and returns the created registration", async () => {
    const fetchImpl = respond(201, {
      registrationNumber: "REG-000001",
      email: "ana@example.com",
      grossFee: 12.5,
    });

    const result = await submitRegistration(request, fetchImpl);

    expect(result).toEqual({
      kind: "created",
      registration: {
        registrationNumber: "REG-000001",
        email: "ana@example.com",
        grossFee: 12.5,
      },
    });
    const [url, init] = vi.mocked(fetchImpl).mock.calls[0];
    expect(url).toBe("/api/registrations");
    expect(init?.method).toBe("POST");
    expect(new Headers(init?.headers).get("Content-Type")).toBe(
      "application/json",
    );
    expect(JSON.parse(String(init?.body))).toEqual(request);
  });

  it("returns field errors of a 400 problem", async () => {
    const result = await submitRegistration(
      request,
      respond(400, {
        errors: [
          { field: "email", message: "must be a valid e-mail address" },
          { field: 3, message: "ignored" },
        ],
      }),
    );

    expect(result).toEqual({
      kind: "invalid",
      errors: [{ field: "email", message: "must be a valid e-mail address" }],
    });
  });

  it("treats a 400 without a JSON body as invalid without field errors", async () => {
    expect(await submitRegistration(request, respond(400))).toEqual({
      kind: "invalid",
      errors: [],
    });
    expect(
      await submitRegistration(request, respond(400, { errors: "x" })),
    ).toEqual({ kind: "invalid", errors: [] });
  });

  it("maps 429 to rate limited and other failures to failed", async () => {
    expect(await submitRegistration(request, respond(429))).toEqual({
      kind: "rateLimited",
    });
    expect(await submitRegistration(request, respond(503))).toEqual({
      kind: "failed",
    });
    const offline = vi.fn(async () => {
      throw new TypeError("offline");
    }) as unknown as typeof fetch;
    expect(await submitRegistration(request, offline)).toEqual({
      kind: "failed",
    });
  });
});

describe("formatAmount", () => {
  it("shows two decimals for numbers and decimal strings", () => {
    expect(formatAmount(12.5)).toBe("12.50");
    expect(formatAmount("7.00")).toBe("7.00");
  });
});
