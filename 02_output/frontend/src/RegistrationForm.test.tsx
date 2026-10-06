import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { SubmitResult } from "./api";
import { RegistrationForm } from "./RegistrationForm";

const workshops = [
  { id: "K1", name: "Kappa" },
  { id: "K2", name: "Lambda" },
];

function fillParticipant() {
  fireEvent.change(screen.getByLabelText("First name"), {
    target: { value: "Ana" },
  });
  fireEvent.change(screen.getByLabelText("Last name"), {
    target: { value: "Kovač" },
  });
  fireEvent.change(screen.getByLabelText("E-mail"), {
    target: { value: "ana@example.com" },
  });
}

function renderForm(result: SubmitResult) {
  const submit = vi.fn(async () => result);
  render(<RegistrationForm workshops={workshops} submit={submit} />);
  return submit;
}

describe("RegistrationForm", () => {
  it("renders the contract fields with the configured workshops", () => {
    renderForm({ kind: "failed" });

    expect(
      screen.getByRole("form", { name: "Registration" }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("First name")).toBeRequired();
    expect(screen.getByLabelText("E-mail")).toHaveAttribute("type", "email");
    expect(screen.getByLabelText("Private person")).not.toBeChecked();
    expect(screen.getByLabelText("Company")).not.toBeChecked();
    expect(screen.queryByLabelText("Company name")).not.toBeInTheDocument();
    const options = screen
      .getAllByRole("option")
      .map((option) => option.textContent);
    expect(options).toEqual(["No workshop", "K1 - Kappa", "K2 - Lambda"]);
  });

  it("shows required company fields only for a company payer", () => {
    renderForm({ kind: "failed" });

    fireEvent.click(screen.getByLabelText("Company"));
    for (const label of ["Company name", "Company address", "VAT ID"]) {
      expect(screen.getByLabelText(label)).toBeRequired();
    }
    fireEvent.click(screen.getByLabelText("Private person"));
    expect(screen.queryByLabelText("Company name")).not.toBeInTheDocument();
  });

  it("submits the request and shows number and fee, then resets", async () => {
    const submit = renderForm({
      kind: "created",
      registration: {
        registrationNumber: "REG-000009",
        email: "ana@example.com",
        grossFee: 12.5,
      },
    });
    fillParticipant();
    fireEvent.click(screen.getByLabelText("Private person"));
    fireEvent.change(screen.getByLabelText("Workshop"), {
      target: { value: "K2" },
    });

    fireEvent.submit(screen.getByRole("form", { name: "Registration" }));

    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent(
        "Registration completed. Your registration number is REG-000009. Fee: 12.50 EUR (including VAT). A confirmation e-mail has been sent to ana@example.com.",
      ),
    );
    expect(submit).toHaveBeenCalledWith({
      firstName: "Ana",
      lastName: "Kovač",
      email: "ana@example.com",
      payerType: "private",
      workshops: ["K2"],
    });
    expect(screen.getByLabelText("First name")).toHaveValue("");
  });

  it("shows server field errors next to their fields and keeps the input", async () => {
    renderForm({
      kind: "invalid",
      errors: [
        { field: "email", message: "must be a valid e-mail address" },
        { field: "email", message: "must not be longer than 254 characters" },
        { field: "workshops", message: "must be a configured workshop id" },
      ],
    });
    fillParticipant();

    fireEvent.submit(screen.getByRole("form", { name: "Registration" }));

    const email = screen.getByLabelText("E-mail");
    await waitFor(() => expect(email).toHaveAttribute("aria-invalid", "true"));
    expect(email).toHaveAccessibleDescription(
      "must be a valid e-mail address; must not be longer than 254 characters",
    );
    expect(screen.getByLabelText("Workshop")).toHaveAccessibleDescription(
      "must be a configured workshop id",
    );
    expect(email).toHaveValue("ana@example.com");
  });

  it.each([
    [
      { kind: "rateLimited" } as SubmitResult,
      "Too many registration attempts. Please try again later.",
    ],
    [
      { kind: "failed" } as SubmitResult,
      "The registration could not be completed. Please try again later.",
    ],
    [
      { kind: "invalid", errors: [] } as SubmitResult,
      "The registration could not be completed. Please try again later.",
    ],
  ])("shows a general error for %j", async (result, text) => {
    renderForm(result);
    fillParticipant();

    fireEvent.submit(screen.getByRole("form", { name: "Registration" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(text);
    expect(screen.getByLabelText("Last name")).toHaveValue("Kovač");
  });

  it("renders user input as text, not markup", async () => {
    renderForm({
      kind: "created",
      registration: {
        registrationNumber: "<b>REG</b>",
        email: "<img src=x>",
        grossFee: "1.00",
      },
    });

    fireEvent.submit(screen.getByRole("form", { name: "Registration" }));

    const status = await screen.findByText(/Registration completed/);
    expect(status.innerHTML).toContain("&lt;b&gt;REG&lt;/b&gt;");
    expect(status.querySelector("img")).toBeNull();
  });
});
