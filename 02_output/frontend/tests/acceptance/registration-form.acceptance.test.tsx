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

// Black-box tests of the registration page (docs/02_contracts/registration-form.json).
// The backend is replaced by answers that follow docs/02_contracts/registration-api.openapi.json.

type Call = { url: string; method: string; body: unknown };

const options = (student: boolean) => ({
  workshops: [
    { id: "WA", title: "Alpha workshop" },
    { id: "WB", title: "Beta delavnica čšž" },
  ],
  conferenceTimeZone: "Europe/Ljubljana",
  earlyBirdDeadline: "2026-05-15",
  price: student
    ? { tier: "student", netFee: 0.0, vat: 0.0, grossFee: 0.0 }
    : { tier: "early", netFee: 111.11, vat: 27.78, grossFee: 138.89 },
});

const json = (status: number, body: unknown, type = "application/json") =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": type },
  });

let calls: Call[];
let answerPost: (body: Record<string, unknown>) => Response | Promise<Response>;

beforeEach(() => {
  calls = [];
  answerPost = () => json(500, { title: "unset" }, "application/problem+json");
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input.toString();
      const method = (init?.method ?? "GET").toUpperCase();
      const body = init?.body ? JSON.parse(String(init.body)) : undefined;
      calls.push({ url, method, body });
      if (method === "GET" && url.includes("/api/registration-options")) {
        return json(200, options(url.includes("student=true")));
      }
      if (method === "POST" && url.endsWith("/api/registrations")) {
        return answerPost(body as Record<string, unknown>);
      }
      return json(404, { title: "Not found" }, "application/problem+json");
    }),
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const stored = (body: Record<string, unknown>) => ({
  registrationNumber: "CR-000042",
  firstName: body.firstName,
  lastName: body.lastName,
  email: body.email,
  payerType: body.payerType,
  companyName: body.companyName ?? null,
  companyAddress: body.companyAddress ?? null,
  companyVatId: body.companyVatId ?? null,
  workshop:
    Array.isArray(body.workshops) && body.workshops.length
      ? body.workshops[0]
      : null,
  netFee: 111.11,
  vat: 27.78,
  grossFee: 138.89,
  student: false,
  registeredAt: "2026-05-01T10:00:00Z",
});

async function openPage() {
  render(<App />);
  await screen.findByRole("radio", { name: /Alpha workshop/ });
}

function fillPerson() {
  fireEvent.change(screen.getByLabelText("First name"), {
    target: { value: "Žiga" },
  });
  fireEvent.change(screen.getByLabelText("Last name"), {
    target: { value: "Čebašek" },
  });
  fireEvent.change(screen.getByLabelText("E-mail"), {
    target: { value: "ziga@example.org" },
  });
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

const posts = () => calls.filter((c) => c.method === "POST");

describe("US-001 registration form", () => {
  it("AC-001-20 shows the form fields, an unchecked student box and the configured workshops", async () => {
    await openPage();

    expect(calls[0].url).toContain("/api/registration-options");
    expect(screen.getByLabelText("First name")).toBeInTheDocument();
    expect(screen.getByLabelText("Last name")).toBeInTheDocument();
    expect(screen.getByLabelText("E-mail")).toBeInTheDocument();
    expect(
      screen.getByRole("checkbox", { name: "I am a student" }),
    ).not.toBeChecked();
    expect(screen.getByRole("radio", { name: "Private person" })).toBeChecked();
    expect(screen.getByRole("radio", { name: "Company" })).not.toBeChecked();
    expect(
      screen.getByRole("radio", { name: /Alpha workshop/ }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("radio", { name: /Beta delavnica čšž/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "No workshop" })).toBeChecked();
  });

  it("AC-001-21 shows company fields only for a company payer", async () => {
    await openPage();

    expect(screen.queryByLabelText("Company name")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Company address")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Company VAT ID")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("radio", { name: "Company" }));

    expect(screen.getByLabelText("Company name")).toBeInTheDocument();
    expect(screen.getByLabelText("Company address")).toBeInTheDocument();
    expect(screen.getByLabelText("Company VAT ID")).toBeInTheDocument();
  });

  it("AC-001-21 does not send company fields for a private payer", async () => {
    answerPost = (body) => json(201, stored(body));
    await openPage();
    fillPerson();
    fireEvent.click(screen.getByRole("radio", { name: "Company" }));
    fireEvent.change(screen.getByLabelText("Company name"), {
      target: { value: "Primer d.o.o." },
    });
    fireEvent.click(screen.getByRole("radio", { name: "Private person" }));

    submit();

    await waitFor(() => expect(posts()).toHaveLength(1));
    const body = posts()[0].body as Record<string, unknown>;
    expect(body.payerType).toBe("private");
    expect(body).not.toHaveProperty("companyName");
    expect(body).not.toHaveProperty("companyAddress");
    expect(body).not.toHaveProperty("companyVatId");
  });

  it("AC-001-22 shows the current price and updates it when the student box changes", async () => {
    await openPage();

    const summary = await screen.findByTestId("price-summary");
    await waitFor(() => expect(summary).toHaveTextContent("138.89"));
    expect(summary).toHaveTextContent("111.11");
    expect(summary).toHaveTextContent("27.78");

    fireEvent.click(screen.getByRole("checkbox", { name: "I am a student" }));

    await waitFor(() =>
      expect(screen.getByTestId("price-summary")).toHaveTextContent("0.00"),
    );
    expect(screen.getByTestId("price-summary")).not.toHaveTextContent("138.89");
    expect(calls.some((c) => c.url.includes("student=true"))).toBe(true);
  });

  it("AC-001-01 submits the registration and shows the registration number and amounts", async () => {
    answerPost = (body) => json(201, stored(body));
    await openPage();
    fillPerson();
    fireEvent.click(screen.getByRole("radio", { name: "Company" }));
    fireEvent.change(screen.getByLabelText("Company name"), {
      target: { value: "Primer d.o.o." },
    });
    fireEvent.change(screen.getByLabelText("Company address"), {
      target: { value: "Slovenska cesta 1" },
    });
    fireEvent.change(screen.getByLabelText("Company VAT ID"), {
      target: { value: "SI12345678" },
    });
    fireEvent.click(screen.getByRole("radio", { name: /Beta delavnica/ }));

    submit();

    const confirmation = await screen.findByTestId("confirmation");
    expect(confirmation).toHaveTextContent("CR-000042");
    expect(confirmation).toHaveTextContent("138.89");
    expect(posts()[0].body).toEqual({
      firstName: "Žiga",
      lastName: "Čebašek",
      email: "ziga@example.org",
      payerType: "company",
      companyName: "Primer d.o.o.",
      companyAddress: "Slovenska cesta 1",
      companyVatId: "SI12345678",
      workshops: ["WB"],
      student: false,
    });
  });

  it("AC-001-01 sends an empty workshop list and the student flag", async () => {
    answerPost = (body) =>
      json(201, {
        ...stored(body),
        student: true,
        netFee: 0,
        vat: 0,
        grossFee: 0,
      });
    await openPage();
    fillPerson();
    fireEvent.click(screen.getByRole("checkbox", { name: "I am a student" }));

    submit();

    await screen.findByTestId("confirmation");
    const body = posts()[0].body as Record<string, unknown>;
    expect(body.workshops).toEqual([]);
    expect(body.student).toBe(true);
  });

  it("AC-001-23 shows each server error next to its field and keeps the values", async () => {
    answerPost = () =>
      json(
        400,
        {
          title: "Invalid registration",
          status: 400,
          errors: [
            {
              field: "email",
              code: "invalid",
              message: "Enter a valid e-mail address.",
            },
            {
              field: "lastName",
              code: "required",
              message: "Last name is required.",
            },
          ],
        },
        "application/problem+json",
      );
    await openPage();
    fillPerson();

    submit();

    await waitFor(() =>
      expect(screen.getByLabelText("E-mail")).toHaveAccessibleDescription(
        /Enter a valid e-mail address\./,
      ),
    );
    expect(screen.getByLabelText("Last name")).toHaveAccessibleDescription(
      /Last name is required\./,
    );
    expect(screen.getByLabelText("First name")).toHaveValue("Žiga");
    expect(screen.getByLabelText("E-mail")).toHaveValue("ziga@example.org");
    expect(screen.queryByTestId("confirmation")).not.toBeInTheDocument();
  });

  it("AC-001-24 explains a duplicate e-mail address and keeps the values", async () => {
    answerPost = () =>
      json(
        409,
        {
          title: "Already registered",
          status: 409,
          detail: "A registration with this e-mail address already exists.",
        },
        "application/problem+json",
      );
    await openPage();
    fillPerson();

    submit();

    const error = await screen.findByTestId("form-error");
    expect(error).toHaveTextContent(/already exists/i);
    expect(screen.getByLabelText("E-mail")).toHaveValue("ziga@example.org");
  });

  it("AC-001-24 shows a generic message without internal details when the backend fails or is unreachable", async () => {
    answerPost = () =>
      json(500, {
        title: "Internal",
        detail: "java.lang.NullPointerException at Foo.java:42",
      });
    await openPage();
    fillPerson();

    submit();

    const error = await screen.findByTestId("form-error");
    expect(error).toHaveTextContent(/try again later/i);
    expect(document.body).not.toHaveTextContent(
      /NullPointerException|Foo\.java/,
    );
    expect(screen.getByLabelText("Last name")).toHaveValue("Čebašek");

    answerPost = () => Promise.reject(new TypeError("Failed to fetch"));
    submit();

    await waitFor(() => expect(posts()).toHaveLength(2));
    expect(
      within(await screen.findByTestId("form-error")).getByText(
        /try again later/i,
      ),
    ).toBeVisible();
    expect(screen.getByLabelText("First name")).toHaveValue("Žiga");
  });
});
