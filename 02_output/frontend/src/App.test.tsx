import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("shows the form with workshops from the configuration", async () => {
    render(
      <App load={async () => ({ workshops: [{ id: "K1", name: "Kappa" }] })} />,
    );

    expect(
      screen.getByRole("heading", { name: "Conference registration" }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("option", { name: "K1 - Kappa" }),
    ).toBeInTheDocument();
  });

  it("shows an error when the configuration cannot be loaded", async () => {
    render(
      <App
        load={async () => {
          throw new Error("down");
        }}
      />,
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The registration form is not available.",
    );
    expect(
      screen.queryByRole("form", { name: "Registration" }),
    ).not.toBeInTheDocument();
  });
});
