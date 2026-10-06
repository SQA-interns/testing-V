/**
 * AC-001-11 (frontend acceptance): the registration form page, through its UI only.
 * Element ids come from docs/02_contracts/registration-form.json; the backend is replaced
 * by responses shaped as in docs/02_contracts/registration-api.openapi.yaml. The workshop
 * list and amounts below are mock API data, not configuration: the page must show whatever
 * the API returns.
 */
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";

type Call = { url: string; method: string; body: unknown };

const MOCK_WORKSHOPS = [
  { id: "WA", title: "Mock workshop alpha" },
  { id: "WB", title: "Mock workshop beta" },
];

const CREATED = {
  registrationNumber: "REG-MOCK000001",
  status: "registered",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana.novak@example.org",
  payerType: "private",
  companyName: null,
  companyAddress: null,
  companyVatId: null,
  workshop: null,
  netFee: 123.4,
  vat: 27.15,
  grossFee: 150.55,
  submittedAt: "2026-07-20T10:00:00Z",
};

let calls: Call[];
let registrationResponse: () => Promise<Response>;

function json(
  status: number,
  body: unknown,
  contentType = "application/json",
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": contentType },
  });
}

beforeEach(() => {
  calls = [];
  registrationResponse = async () => json(201, CREATED);
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url;
      const method = (init?.method ?? "GET").toUpperCase();
      const body =
        typeof init?.body === "string" ? JSON.parse(init.body) : undefined;
      calls.push({ url, method, body });
      if (url.endsWith("/api/workshops") && method === "GET") {
        return json(200, MOCK_WORKSHOPS);
      }
      if (url.endsWith("/api/registrations") && method === "POST") {
        return registrationResponse();
      }
      return json(404, {
        type: "about:blank",
        title: "Not Found",
        status: 404,
      });
    }),
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function field(id: string): HTMLInputElement {
  const element = document.getElementById(id);
  expect(element, `element #${id}`).not.toBeNull();
  return element as HTMLInputElement;
}

function type(id: string, value: string) {
  fireEvent.change(field(id), { target: { value } });
}

async function renderPage() {
  render(<App />);
  await screen.findByRole("heading", { name: "Conference registration" });
  await waitFor(() =>
    expect(document.getElementById("workshop-WA")).not.toBeNull(),
  );
}

function fillParticipant() {
  type("firstName", "Ana");
  type("lastName", "Novak");
  type("email", "ana.novak@example.org");
}

function posts(): Call[] {
  return calls.filter(
    (c) => c.method === "POST" && c.url.endsWith("/api/registrations"),
  );
}

async function submit() {
  await act(async () => {
    fireEvent.submit(field("registration-form"));
  });
}

describe("AC-001-11 registration form", () => {
  it("AC-001-11 shows the participant, payer and workshop fields of the contract", async () => {
    await renderPage();

    for (const id of [
      "firstName",
      "lastName",
      "email",
      "payerType-private",
      "payerType-company",
    ]) {
      field(id);
    }
    expect(field("payerType-private").checked).toBe(true);
    expect(field("workshop-none").checked).toBe(true);
    for (const workshop of MOCK_WORKSHOPS) {
      const option = field(`workshop-${workshop.id}`);
      expect(option.type).toBe("radio");
      expect(screen.getByLabelText(workshop.title)).toBe(option);
    }
    expect(field("submit").textContent).toContain("Register");
  });

  it("AC-001-11 shows company fields only for a company payer", async () => {
    await renderPage();

    for (const id of ["companyName", "companyAddress", "companyVatId"]) {
      expect(document.getElementById(id)).toBeNull();
    }
    fireEvent.click(field("payerType-company"));
    for (const id of ["companyName", "companyAddress", "companyVatId"]) {
      expect(field(id)).toBeVisible();
      expect(field(id).required).toBe(true);
    }
    fireEvent.click(field("payerType-private"));
    for (const id of ["companyName", "companyAddress", "companyVatId"]) {
      expect(document.getElementById(id)).toBeNull();
    }
  });

  it("AC-001-11 private payer without workshop: one POST without company fields", async () => {
    await renderPage();
    fillParticipant();

    await submit();

    await waitFor(() => expect(posts()).toHaveLength(1));
    expect(posts()[0].body).toEqual({
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      payerType: "private",
      workshops: [],
    });
  });

  it("AC-001-11 selected workshop is sent as a one-element list", async () => {
    await renderPage();
    fillParticipant();
    fireEvent.click(field("workshop-WB"));

    await submit();

    await waitFor(() => expect(posts()).toHaveLength(1));
    expect((posts()[0].body as { workshops: string[] }).workshops).toEqual([
      "WB",
    ]);
  });

  it("AC-001-11 company payer sends the company fields", async () => {
    await renderPage();
    fillParticipant();
    fireEvent.click(field("payerType-company"));
    type("companyName", "Primer d.o.o.");
    type("companyAddress", "Koroška cesta 1, 2000 Maribor");
    type("companyVatId", "SI00000001");

    await submit();

    await waitFor(() => expect(posts()).toHaveLength(1));
    expect(posts()[0].body).toMatchObject({
      payerType: "company",
      companyName: "Primer d.o.o.",
      companyAddress: "Koroška cesta 1, 2000 Maribor",
      companyVatId: "SI00000001",
    });
  });

  it("AC-001-11 on 201 shows registration number, net fee, VAT and gross fee", async () => {
    await renderPage();
    fillParticipant();

    await submit();

    const confirmation = await waitFor(() => field("confirmation"));
    expect(confirmation).toBeVisible();
    expect(field("confirmation-registration-number").textContent).toContain(
      CREATED.registrationNumber,
    );
    expect(field("confirmation-net-fee").textContent).toContain("123.40 EUR");
    expect(field("confirmation-vat").textContent).toContain("27.15 EUR");
    expect(field("confirmation-gross-fee").textContent).toContain("150.55 EUR");
    expect(document.getElementById("registration-form")).toBeNull();
  });

  it("AC-001-11 on 422 shows each field error next to its field and keeps the input", async () => {
    registrationResponse = async () =>
      json(
        422,
        {
          type: "about:blank",
          title: "Validation failed",
          status: 422,
          errors: {
            email: "Mock e-mail message",
            lastName: "Mock last name message",
          },
        },
        "application/problem+json",
      );
    await renderPage();
    fillParticipant();

    await submit();

    await waitFor(() =>
      expect(document.getElementById("email-error")).not.toBeNull(),
    );
    expect(field("email-error").textContent).toContain("Mock e-mail message");
    expect(field("lastName-error").textContent).toContain(
      "Mock last name message",
    );
    expect(field("email").getAttribute("aria-describedby") ?? "").toContain(
      "email-error",
    );
    expect(field("lastName").getAttribute("aria-describedby") ?? "").toContain(
      "lastName-error",
    );
    expect(document.getElementById("firstName-error")).toBeNull();
    expect(field("email").value).toBe("ana.novak@example.org");
    expect(field("firstName").value).toBe("Ana");
    expect(document.getElementById("confirmation")).toBeNull();
    expect(document.body.textContent).not.toContain(CREATED.registrationNumber);
  });

  it("AC-001-11 workshop error from the API is shown at the workshop field", async () => {
    registrationResponse = async () =>
      json(
        422,
        {
          type: "about:blank",
          title: "Validation failed",
          status: 422,
          errors: { workshops: "Mock workshop message" },
        },
        "application/problem+json",
      );
    await renderPage();
    fillParticipant();

    await submit();

    await waitFor(() =>
      expect(document.getElementById("workshop-error")).not.toBeNull(),
    );
    expect(field("workshop-error").textContent).toContain(
      "Mock workshop message",
    );
  });

  it("AC-001-11 on 429 shows one general error and keeps the input", async () => {
    registrationResponse = async () =>
      json(
        429,
        { type: "about:blank", title: "Too Many Requests", status: 429 },
        "application/problem+json",
      );
    await renderPage();
    fillParticipant();

    await submit();

    const alert = await waitFor(() => field("general-error"));
    expect(alert.getAttribute("role")).toBe("alert");
    expect(alert.textContent?.trim()).not.toBe("");
    expect(field("firstName").value).toBe("Ana");
    expect(document.getElementById("confirmation")).toBeNull();
  });

  it("AC-001-11 submit is disabled while the request is pending (no double submission)", async () => {
    let release: (r: Response) => void = () => undefined;
    registrationResponse = () =>
      new Promise<Response>((resolve) => (release = resolve));
    await renderPage();
    fillParticipant();

    await submit();

    await waitFor(() => expect(field("submit").disabled).toBe(true));
    await submit();
    expect(posts()).toHaveLength(1);
    await act(async () => release(json(201, CREATED)));
    await waitFor(() => field("confirmation"));
  });
});
