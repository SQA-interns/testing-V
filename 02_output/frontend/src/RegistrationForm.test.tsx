// Unit tests for the form's client-side behaviour (phase 5).
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "./RegistrationForm";

const fetchMock = vi.fn<typeof fetch>();
const onRegistered = vi.fn();

beforeEach(() => {
  (window as Window & { APP_CONFIG?: unknown }).APP_CONFIG = {
    workshops: [{ id: "A", title: "Alpha" }],
  };
  fetchMock.mockReset();
  onRegistered.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function fill(name: string, value: string) {
  fireEvent.change(screen.getByRole("textbox", { name }), {
    target: { value },
  });
}

function fillParticipant() {
  fill("First name", "Ana");
  fill("Last name", "Novak");
  fill("E-mail", "ana@example.org");
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Register" }));
}

describe("RegistrationForm", () => {
  it("does not submit while required fields are blank and marks them", () => {
    render(<RegistrationForm onRegistered={onRegistered} />);
    fill("First name", "  ");
    submit();

    expect(fetchMock).not.toHaveBeenCalled();
    for (const name of ["First name", "Last name", "E-mail"]) {
      expect(screen.getByRole("textbox", { name })).toHaveAttribute(
        "aria-invalid",
        "true",
      );
    }
  });

  it("requires company fields only for a company payer", () => {
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    fireEvent.click(screen.getByRole("radio", { name: "Company" }));
    submit();

    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.getByRole("textbox", { name: "VAT ID" })).toHaveAttribute(
      "aria-invalid",
      "true",
    );
  });

  it("does not send company fields after switching back to a private payer", async () => {
    fetchMock.mockResolvedValue(new Response("{}", { status: 201 }));
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    fireEvent.click(screen.getByRole("radio", { name: "Company" }));
    fill("Company name", "Primer d.o.o.");
    fireEvent.click(screen.getByRole("radio", { name: "Private person" }));
    submit();

    await waitFor(() => expect(onRegistered).toHaveBeenCalled());
    const body = JSON.parse(
      String((fetchMock.mock.calls[0][1] as RequestInit).body),
    );
    expect(body).not.toHaveProperty("companyName");
    expect(body.payerType).toBe("private");
  });

  it("shows the rate-limit message on 429", async () => {
    fetchMock.mockResolvedValue(new Response("", { status: 429 }));
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    submit();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Too many registrations from your network. Please try again later.",
    );
    expect(onRegistered).not.toHaveBeenCalled();
  });

  it("shows a generic message on other failures", async () => {
    fetchMock.mockResolvedValue(new Response("boom", { status: 500 }));
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    submit();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(
      "Registration failed. Please try again later.",
    );
    expect(alert).not.toHaveTextContent("boom");
  });

  it("shows errors for fields without an input and for groups", async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          errors: [
            { field: "workshops", message: "unknown workshop" },
            { field: "payerType", message: "must be private or company" },
            { field: "somethingElse", message: "is odd" },
          ],
        }),
        { status: 422 },
      ),
    );
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    submit();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "somethingElse is odd",
    );
    expect(
      screen.getByRole("radiogroup", { name: "Workshop" }),
    ).toHaveAccessibleDescription("unknown workshop");
    expect(
      screen.getByRole("radiogroup", { name: "Payer" }),
    ).toHaveAccessibleDescription("must be private or company");
  });

  it("disables the button while the request is pending", async () => {
    let resolve: (response: Response) => void = () => {};
    fetchMock.mockReturnValue(new Promise<Response>((r) => (resolve = r)));
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    submit();

    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();
    resolve(new Response("{}", { status: 201 }));
    await waitFor(() => expect(onRegistered).toHaveBeenCalled());
  });

  it("sends the selected workshop id", async () => {
    fetchMock.mockResolvedValue(new Response("{}", { status: 201 }));
    render(<RegistrationForm onRegistered={onRegistered} />);
    fillParticipant();
    const group = screen.getByRole("radiogroup", { name: "Workshop" });
    fireEvent.click(within(group).getByRole("radio", { name: "Alpha" }));
    submit();

    await waitFor(() => expect(onRegistered).toHaveBeenCalled());
    const body = JSON.parse(
      String((fetchMock.mock.calls[0][1] as RequestInit).body),
    );
    expect(body.workshops).toEqual(["A"]);
  });
});
