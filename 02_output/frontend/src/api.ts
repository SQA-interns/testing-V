/** Client of the registration API (docs/02_contracts/registration-api.openapi.yaml). */

export type Workshop = { id: string; title: string };

export type PayerType = "private" | "company";

export type RegistrationInput = {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType;
  companyName?: string;
  companyAddress?: string;
  companyVatId?: string;
  workshops: string[];
};

export type Registration = {
  registrationNumber: string;
  netFee: number | string;
  vat: number | string;
  grossFee: number | string;
};

export type SubmitResult =
  | { kind: "created"; registration: Registration }
  | { kind: "invalid"; errors: Record<string, string> }
  | { kind: "failed" };

export async function fetchWorkshops(): Promise<Workshop[]> {
  const response = await fetch("/api/workshops", {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error(`workshops: HTTP ${response.status}`);
  }
  const body: unknown = await response.json();
  if (!Array.isArray(body)) {
    throw new Error("workshops: unexpected body");
  }
  return body.filter(
    (w): w is Workshop =>
      typeof w === "object" &&
      w !== null &&
      typeof (w as Workshop).id === "string" &&
      typeof (w as Workshop).title === "string",
  );
}

export async function submitRegistration(
  input: RegistrationInput,
): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(input),
    });
  } catch {
    return { kind: "failed" };
  }
  try {
    if (response.status === 201) {
      return {
        kind: "created",
        registration: (await response.json()) as Registration,
      };
    }
    if (response.status === 422) {
      const body = (await response.json()) as {
        errors?: Record<string, unknown>;
      };
      const errors: Record<string, string> = {};
      for (const [field, message] of Object.entries(body.errors ?? {})) {
        if (typeof message === "string") {
          errors[field] = message;
        }
      }
      return { kind: "invalid", errors };
    }
  } catch {
    return { kind: "failed" };
  }
  return { kind: "failed" };
}

/** Amount with two decimals and the currency, for example "292.80 EUR". */
export function formatAmount(value: number | string): string {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? `${amount.toFixed(2)} EUR`
    : `${String(value)} EUR`;
}
