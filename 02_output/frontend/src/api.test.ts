import { afterEach, describe, expect, it, vi } from "vitest";
import {
  fetchWorkshops,
  formatAmount,
  submitRegistration,
  type RegistrationInput,
} from "./api";

const INPUT: RegistrationInput = {
  firstName: "A",
  lastName: "B",
  email: "a@b.co",
  payerType: "private",
  workshops: [],
};

function respond(status: number, body: unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async () =>
        new Response(typeof body === "string" ? body : JSON.stringify(body), {
          status,
        }),
    ),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("formatAmount", () => {
  it("writes two decimals and the currency", () => {
    expect(formatAmount(52.8)).toBe("52.80 EUR");
    expect(formatAmount("292.80")).toBe("292.80 EUR");
    expect(formatAmount(0)).toBe("0.00 EUR");
  });

  it("keeps a value that is not a number", () => {
    expect(formatAmount("n/a")).toBe("n/a EUR");
  });
});

describe("fetchWorkshops", () => {
  it("returns well-formed workshops only", async () => {
    respond(200, [
      { id: "A", title: "Alpha" },
      { id: 1, title: "x" },
      null,
      { id: "B" },
    ]);

    await expect(fetchWorkshops()).resolves.toEqual([
      { id: "A", title: "Alpha" },
    ]);
  });

  it("fails on an HTTP error or an unexpected body", async () => {
    respond(500, {});
    await expect(fetchWorkshops()).rejects.toThrow("HTTP 500");
    respond(200, { not: "a list" });
    await expect(fetchWorkshops()).rejects.toThrow("unexpected body");
  });
});

describe("submitRegistration", () => {
  it("posts JSON to the registration endpoint", async () => {
    const fetchMock = vi.fn(
      async () =>
        new Response(JSON.stringify({ registrationNumber: "R" }), {
          status: 201,
        }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await submitRegistration(INPUT);

    expect(result).toEqual({
      kind: "created",
      registration: { registrationNumber: "R" },
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/registrations",
      expect.objectContaining({ method: "POST", body: JSON.stringify(INPUT) }),
    );
  });

  it("returns field errors for 422 and drops non-string messages", async () => {
    respond(422, { errors: { email: "bad", other: 5 } });

    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "invalid",
      errors: { email: "bad" },
    });
  });

  it("treats 422 without errors as no field errors", async () => {
    respond(422, {});

    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "invalid",
      errors: {},
    });
  });

  it("fails for other statuses, unreadable bodies and network errors", async () => {
    respond(429, {});
    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "failed",
    });
    respond(500, "oops");
    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "failed",
    });
    respond(201, "not json");
    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "failed",
    });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw new TypeError("network");
      }),
    );
    await expect(submitRegistration(INPUT)).resolves.toEqual({
      kind: "failed",
    });
  });
});
