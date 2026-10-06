import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

type Handler = (url: string, init?: RequestInit) => Response;

let submitted: unknown[] = [];

function stubFetch(onSubmit: Handler) {
  submitted = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, init?: RequestInit) => {
      if (url === "/api/workshops") {
        return Promise.resolve(
          new Response(
            JSON.stringify([
              { id: "W1", title: "Workshop one" },
              { id: "W2", title: "Workshop two" },
            ]),
            { status: 200 },
          ),
        );
      }
      submitted.push(JSON.parse(init?.body as string));
      return Promise.resolve(onSubmit(url, init));
    }),
  );
}

function fill(testId: string, value: string) {
  fireEvent.change(screen.getByTestId(testId), { target: { value } });
}

function fillPerson() {
  fill("first-name", "Špela");
  fill("last-name", "Žagar");
  fill("email", "spela@example.com");
}

const created = () =>
  new Response(
    JSON.stringify({
      registrationNumber: "REG-000042",
      netFee: 240,
      vat: 52.8,
      grossFee: 292.8,
    }),
    { status: 201 },
  );

beforeEach(() => {
  stubFetch(created);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("registration form", () => {
  it("shows the fields with private payer preselected and company fields hidden", async () => {
    render(<App />);

    expect(screen.getByTestId("payer-type-private")).toBeChecked();
    expect(screen.queryByTestId("company-name")).toBeNull();
    await waitFor(() =>
      expect(screen.getByText("Workshop two")).toBeInTheDocument(),
    );
    expect(screen.getByText("No workshop")).toBeInTheDocument();
  });

  it("submits a private registration and shows the confirmation with two-decimal amounts", async () => {
    render(<App />);
    fillPerson();
    fireEvent.click(screen.getByTestId("submit"));

    const confirmation = await screen.findByTestId("confirmation");
    expect(confirmation).toHaveTextContent("REG-000042");
    expect(confirmation).toHaveTextContent(
      "240.00 EUR + VAT 52.80 EUR = 292.80 EUR",
    );
    expect(submitted).toEqual([
      {
        firstName: "Špela",
        lastName: "Žagar",
        email: "spela@example.com",
        payerType: "private",
        companyName: null,
        companyAddress: null,
        companyVatId: null,
        workshops: [],
      },
    ]);
  });

  it("sends company fields and the chosen workshop", async () => {
    render(<App />);
    await screen.findByText("Workshop one");
    fillPerson();
    fireEvent.click(screen.getByTestId("payer-type-company"));
    fill("company-name", " Podjetje d.o.o. ");
    fill("company-address", "Ljubljana");
    fill("company-vat-id", "SI12345678");
    fill("workshop", "W1");
    fireEvent.click(screen.getByTestId("submit"));

    await screen.findByTestId("confirmation");
    expect(submitted[0]).toMatchObject({
      payerType: "company",
      companyName: "Podjetje d.o.o.",
      companyAddress: "Ljubljana",
      companyVatId: "SI12345678",
      workshops: ["W1"],
    });
  });

  it("marks missing fields without calling the API", () => {
    render(<App />);
    fireEvent.click(screen.getByTestId("submit"));

    expect(screen.getByTestId("field-error-firstName")).toBeInTheDocument();
    expect(screen.getByTestId("field-error-lastName")).toBeInTheDocument();
    expect(screen.getByTestId("field-error-email")).toBeInTheDocument();
    expect(submitted).toEqual([]);
  });

  it("marks the fields the backend rejected and keeps the values", async () => {
    stubFetch(
      () =>
        new Response(
          JSON.stringify({ error: "validation_failed", fields: ["lastName"] }),
          {
            status: 400,
          },
        ),
    );
    render(<App />);
    fillPerson();
    fireEvent.click(screen.getByTestId("submit"));

    expect(
      await screen.findByTestId("field-error-lastName"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("last-name")).toHaveValue("Žagar");
    expect(screen.queryByTestId("confirmation")).toBeNull();
  });

  it("shows a generic message when registration is unavailable", async () => {
    stubFetch(
      () =>
        new Response(JSON.stringify({ error: "registration_unavailable" }), {
          status: 503,
        }),
    );
    render(<App />);
    fillPerson();
    fireEvent.click(screen.getByTestId("submit"));

    expect(await screen.findByTestId("error-message")).toHaveTextContent(
      "Registration is not possible right now",
    );
    expect(screen.getByTestId("first-name")).toHaveValue("Špela");
  });
});
