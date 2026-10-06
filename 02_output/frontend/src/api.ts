// Client of the registration API (docs/02_contracts/registration-api.openapi.json, AR-01).

export type Amount = number | string;

export interface Workshop {
  id: string;
  title: string;
}

export interface Price {
  tier: "early" | "regular" | "student";
  netFee: Amount;
  vat: Amount;
  grossFee: Amount;
}

export interface RegistrationOptions {
  workshops: Workshop[];
  conferenceTimeZone: string;
  earlyBirdDeadline: string;
  price: Price;
}

export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  payerType: "private" | "company";
  companyName?: string;
  companyAddress?: string;
  companyVatId?: string;
  workshops: string[];
  student: boolean;
}

export interface Registration {
  registrationNumber: string;
  workshop: string | null;
  netFee: Amount;
  vat: Amount;
  grossFee: Amount;
}

export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export type SubmitResult =
  | { kind: "created"; registration: Registration }
  | { kind: "invalid"; errors: FieldError[] }
  | { kind: "duplicate" }
  | { kind: "failed" };

export async function fetchOptions(
  student: boolean,
): Promise<RegistrationOptions> {
  const response = await fetch(`/api/registration-options?student=${student}`, {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error("options unavailable");
  }
  return (await response.json()) as RegistrationOptions;
}

export async function submitRegistration(
  request: RegistrationRequest,
): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetch("/api/registrations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(request),
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 201) {
    return {
      kind: "created",
      registration: (await response.json()) as Registration,
    };
  }
  if (response.status === 409) {
    return { kind: "duplicate" };
  }
  if (response.status === 400) {
    try {
      const problem = (await response.json()) as { errors?: FieldError[] };
      if (Array.isArray(problem.errors) && problem.errors.length > 0) {
        return { kind: "invalid", errors: problem.errors };
      }
    } catch {
      // fall through to the generic message
    }
  }
  return { kind: "failed" };
}

export function formatAmount(value: Amount): string {
  return Number(value).toFixed(2);
}
