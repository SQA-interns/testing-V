// Acceptance tests for the registration form (US-001: AC-001-14, AC-001-15).
// Black-box through the rendered page and the contract `docs/02_contracts/registration-form.yaml`;
// the API is stubbed with responses shaped by `registration-api.openapi.yaml`.
// Workshops and amounts are synthetic test data supplied as configuration, not business values.
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../../src/App";

type AppWindow = Window & { APP_CONFIG?: unknown };

const testWorkshops = [
  { id: "TW-A", title: "Test workshop Alpha" },
  { id: "TW-B", title: "Test workshop Beta" },
];

const storedRegistration = {
  registrationNumber: "CR-TEST000001",
  firstName: "Ana",
  lastName: "Novak",
  email: "ana.novak@example.org",
  payerType: "private",
  companyName: null,
  companyAddress: null,
  companyVatId: null,
  workshop: null,
  netFee: 111.1,
  vat: 24.44,
  grossFee: 135.54,
};

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type":
        status >= 400 ? "application/problem+json" : "application/json",
    },
  });
}

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  (window as AppWindow).APP_CONFIG = { workshops: testWorkshops };
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  delete (window as AppWindow).APP_CONFIG;
});

function fillParticipant(email = "ana.novak@example.org") {
  fireEvent.change(screen.getByRole("textbox", { name: "First name" }), {
    target: { value: "Ana" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Last name" }), {
    target: { value: "Novak" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "E-mail" }), {
    target: { value: email },
  });
}

function chooseCompanyPayer() {
  const payer = screen.getByRole("radiogroup", { name: "Payer" });
  fireEvent.click(within(payer).getByRole("radio", { name: "Company" }));
  fireEvent.change(screen.getByRole("textbox", { name: "Company name" }), {
    target: { value: "Primer d.o.o." },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "Company address" }), {
    target: { value: "Koroška cesta 1, 2000 Maribor" },
  });
  fireEvent.change(screen.getByRole("textbox", { name: "VAT ID" }), {
    target: { value: "SI00000001" },
  });
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

function sentRequest(): {
  url: string;
  init: RequestInit;
  body: Record<string, unknown>;
} {
  expect(fetchMock).toHaveBeenCalledTimes(1);
  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
  return { url: String(url), init, body: JSON.parse(String(init.body)) };
}

describe("US-001 registration form", () => {
  it("AC-001-14 offers the configured workshops plus No workshop, selected by default", () => {
    render(<App />);

    const group = screen.getByRole("radiogroup", { name: "Workshop" });
    for (const workshop of testWorkshops) {
      expect(
        within(group).getByRole("radio", { name: workshop.title }),
      ).not.toBeChecked();
    }
    expect(within(group).getAllByRole("radio")).toHaveLength(
      testWorkshops.length + 1,
    );
    expect(
      within(group).getByRole("radio", { name: "No workshop" }),
    ).toBeChecked();
  });

  it("AC-001-14 asks for company data only for a company payer", () => {
    render(<App />);

    const payer = screen.getByRole("radiogroup", { name: "Payer" });
    expect(
      within(payer).getByRole("radio", { name: "Private person" }),
    ).toBeChecked();
    expect(
      screen.queryByRole("textbox", { name: "Company name" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "Company address" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("textbox", { name: "VAT ID" }),
    ).not.toBeInTheDocument();

    fireEvent.click(within(payer).getByRole("radio", { name: "Company" }));

    expect(
      screen.getByRole("textbox", { name: "Company name" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("textbox", { name: "Company address" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "VAT ID" })).toBeInTheDocument();
  });

  it("AC-001-14 private payer without workshop: submits and shows number and fees", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(201, storedRegistration));
    render(<App />);

    fillParticipant();
    submit();

    const confirmation = await screen.findByRole("status", {
      name: "Registration confirmed",
    });
    const { url, init, body } = sentRequest();
    expect(url).toBe("/api/registrations");
    expect(init.method).toBe("POST");
    expect(new Headers(init.headers).get("Content-Type")).toContain(
      "application/json",
    );
    expect(body).toEqual({
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      payerType: "private",
      workshops: [],
    });
    expect(confirmation).toHaveTextContent("CR-TEST000001");
    expect(confirmation).toHaveTextContent("111.10 EUR");
    expect(confirmation).toHaveTextContent("24.44 EUR");
    expect(confirmation).toHaveTextContent("135.54 EUR");
  });

  it("AC-001-14 company payer with a workshop: sends company data and the workshop id", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(201, {
        ...storedRegistration,
        payerType: "company",
        companyName: "Primer d.o.o.",
        companyAddress: "Koroška cesta 1, 2000 Maribor",
        companyVatId: "SI00000001",
        workshop: "TW-B",
      }),
    );
    render(<App />);

    fillParticipant();
    chooseCompanyPayer();
    const workshop = screen.getByRole("radiogroup", { name: "Workshop" });
    fireEvent.click(
      within(workshop).getByRole("radio", { name: "Test workshop Beta" }),
    );
    submit();

    const confirmation = await screen.findByRole("status", {
      name: "Registration confirmed",
    });
    expect(sentRequest().body).toEqual({
      firstName: "Ana",
      lastName: "Novak",
      email: "ana.novak@example.org",
      payerType: "company",
      companyName: "Primer d.o.o.",
      companyAddress: "Koroška cesta 1, 2000 Maribor",
      companyVatId: "SI00000001",
      workshops: ["TW-B"],
    });
    expect(confirmation).toHaveTextContent("CR-TEST000001");
    expect(confirmation).toHaveTextContent("135.54 EUR");
  });

  it("AC-001-15 shows each rejected field's error next to it and keeps the values", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(422, {
        status: 422,
        title: "Validation failed",
        errors: [
          { field: "email", message: "must be a valid e-mail address" },
          { field: "companyVatId", message: "is not accepted" },
        ],
      }),
    );
    render(<App />);

    fillParticipant("ana.novak@example");
    chooseCompanyPayer();
    submit();

    const email = screen.getByRole("textbox", { name: "E-mail" });
    await waitFor(() => expect(email).toHaveAttribute("aria-invalid", "true"));
    expect(email).toHaveAccessibleDescription(/must be a valid e-mail address/);
    const vatId = screen.getByRole("textbox", { name: "VAT ID" });
    expect(vatId).toHaveAttribute("aria-invalid", "true");
    expect(vatId).toHaveAccessibleDescription(/is not accepted/);
    expect(vatId).toHaveValue("SI00000001");

    expect(
      screen.getByRole("textbox", { name: "First name" }),
    ).not.toHaveAttribute("aria-invalid", "true");
    expect(email).toHaveValue("ana.novak@example");
    expect(screen.getByRole("textbox", { name: "Company name" })).toHaveValue(
      "Primer d.o.o.",
    );
    expect(
      screen.queryByRole("status", { name: "Registration confirmed" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/CR-/)).not.toBeInTheDocument();
  });

  it("AC-001-15 a single rejected participant field is marked and no number is shown", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(422, {
        status: 422,
        title: "Validation failed",
        errors: [{ field: "firstName", message: "is required" }],
      }),
    );
    render(<App />);

    fillParticipant();
    submit();

    const firstName = screen.getByRole("textbox", { name: "First name" });
    await waitFor(() =>
      expect(firstName).toHaveAttribute("aria-invalid", "true"),
    );
    expect(firstName).toHaveAccessibleDescription(/is required/);
    expect(screen.getByRole("textbox", { name: "Last name" })).toHaveValue(
      "Novak",
    );
    expect(
      screen.queryByRole("status", { name: "Registration confirmed" }),
    ).not.toBeInTheDocument();
  });
});
