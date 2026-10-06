import { describe, expect, it } from "vitest";
import { checkRequired, type FormValues } from "./validation";

const filled: FormValues = {
  firstName: "Špela",
  lastName: "Žagar",
  email: "spela@example.com",
  payerType: "private",
  companyName: "",
  companyAddress: "",
  companyVatId: "",
  workshop: "",
};

describe("checkRequired", () => {
  it("accepts a complete private registration", () => {
    expect(checkRequired(filled)).toEqual([]);
  });

  it("reports empty names and a malformed e-mail", () => {
    expect(
      checkRequired({
        ...filled,
        firstName: " ",
        lastName: "",
        email: "not-an-address",
      }),
    ).toEqual(["firstName", "lastName", "email"]);
  });

  it("requires company fields only for a company payer", () => {
    expect(checkRequired({ ...filled, payerType: "company" })).toEqual([
      "companyName",
      "companyAddress",
      "companyVatId",
    ]);
    expect(
      checkRequired({
        ...filled,
        payerType: "company",
        companyName: "X d.o.o.",
        companyAddress: "Ljubljana",
        companyVatId: "SI1",
      }),
    ).toEqual([]);
  });
});
