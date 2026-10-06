import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

const options = (gross: number) => ({
  workshops: [{ id: "W1", title: "One" }],
  conferenceTimeZone: "Europe/Ljubljana",
  earlyBirdDeadline: "2026-07-31",
  price: { tier: "early", netFee: gross, vat: 0, grossFee: gross },
});

const ok = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("shows a generic message when the options cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ok({}, 503)),
    );
    render(<App />);

    expect(await screen.findByTestId("form-error")).toHaveTextContent(
      /try again later/i,
    );
  });

  it("ignores a stale price answer that arrives after a newer one", async () => {
    let releaseFirst: (r: Response) => void = () => undefined;
    const first = new Promise<Response>((resolve) => {
      releaseFirst = resolve;
    });
    vi.stubGlobal(
      "fetch",
      vi.fn((url: string) =>
        url.includes("student=true") ? Promise.resolve(ok(options(0))) : first,
      ),
    );
    render(<App />);

    fireEvent.click(screen.getByRole("checkbox", { name: "I am a student" }));
    await waitFor(() =>
      expect(screen.getByTestId("price-summary")).toHaveTextContent("0.00"),
    );
    releaseFirst(ok(options(999)));

    await new Promise((r) => setTimeout(r, 20));
    expect(screen.getByTestId("price-summary")).not.toHaveTextContent("999.00");
  });

  it("disables the button while sending and shows a workshop error next to the workshops", async () => {
    let releasePost: (r: Response) => void = () => undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn((_url: string, init?: RequestInit) => {
        if (init?.method === "POST") {
          return new Promise<Response>((resolve) => {
            releasePost = resolve;
          });
        }
        return Promise.resolve(ok(options(10)));
      }),
    );
    render(<App />);
    await screen.findByRole("radio", { name: "One" });

    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getByRole("button", { name: "Register" })).toBeDisabled();
    releasePost(
      ok(
        {
          errors: [
            {
              field: "workshops",
              code: "invalid",
              message: "Unknown workshop.",
            },
          ],
        },
        400,
      ),
    );

    expect(await screen.findByText("Unknown workshop.")).toBeInTheDocument();
    expect(
      screen.getByRole("group", { name: "Workshop" }),
    ).toHaveAccessibleDescription("Unknown workshop.");
    expect(screen.getByRole("button", { name: "Register" })).toBeEnabled();
  });

  it("states 'none' in the confirmation when no workshop was chosen", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (_url: string, init?: RequestInit) =>
        init?.method === "POST"
          ? ok(
              {
                registrationNumber: "CR-000009",
                workshop: null,
                netFee: 1,
                vat: 0,
                grossFee: 1,
              },
              201,
            )
          : ok(options(1)),
      ),
    );
    render(<App />);
    await screen.findByRole("radio", { name: "One" });

    fireEvent.click(screen.getByRole("button", { name: "Register" }));

    expect(await screen.findByTestId("confirmation")).toHaveTextContent(
      "Workshop: none",
    );
  });
});
