import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("still offers registration without a workshop when the workshop list fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("", { status: 503 })),
    );

    render(<App />);

    await waitFor(() =>
      expect(
        screen.getByText(/workshops could not be loaded/i),
      ).toBeInTheDocument(),
    );
    expect(document.getElementById("workshop-none")).toBeChecked();
    expect(document.getElementById("submit")).toBeEnabled();
  });

  it("shows a payer type error from the API next to the payer field", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: string) =>
        url.endsWith("/api/workshops")
          ? new Response("[]", { status: 200 })
          : new Response(
              JSON.stringify({ errors: { payerType: "Mock payer message" } }),
              {
                status: 422,
              },
            ),
      ),
    );
    render(<App />);
    await screen.findByRole("button", { name: "Register" });

    await act(async () => {
      fireEvent.submit(
        document.getElementById("registration-form") as HTMLFormElement,
      );
    });

    await waitFor(() =>
      expect(document.getElementById("payerType-error")?.textContent).toBe(
        "Mock payer message",
      ),
    );
    expect(
      document.getElementById("payerType")?.getAttribute("aria-describedby"),
    ).toBe("payerType-error");
  });
});
