import { describe, expect, it } from "vitest";
import { EMPTY, toRequest } from "./formModel";

describe("toRequest", () => {
  it("omits company fields and sends no workshop for a private payer", () => {
    expect(
      toRequest({
        ...EMPTY,
        firstName: "Ana",
        lastName: "Kovač",
        email: "ana@example.com",
        payerType: "private",
        companyName: "left over",
      }),
    ).toEqual({
      firstName: "Ana",
      lastName: "Kovač",
      email: "ana@example.com",
      payerType: "private",
      workshops: [],
    });
  });

  it("sends company fields and the chosen workshop for a company payer", () => {
    expect(
      toRequest({
        ...EMPTY,
        firstName: "Žiga",
        lastName: "Šuštar",
        email: "z@example.com",
        payerType: "company",
        companyName: "Primer",
        companyAddress: "Čopova 1",
        companyVatId: "SI1",
        workshop: "K2",
      }),
    ).toEqual({
      firstName: "Žiga",
      lastName: "Šuštar",
      email: "z@example.com",
      payerType: "company",
      companyName: "Primer",
      companyAddress: "Čopova 1",
      companyVatId: "SI1",
      workshops: ["K2"],
    });
  });
});
