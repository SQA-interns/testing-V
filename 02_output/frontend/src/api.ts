// Client for POST /api/registrations (docs/02_contracts/registration-api.openapi.yaml).
// The frontend talks to the backend only through this call (AR-01).

export type PayerType = "private" | "company";

export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType;
  companyName?: string;
  companyAddress?: string;
  companyVatId?: string;
  workshops: string[];
}

export interface Registration {
  registrationNumber: string;
  email: string;
  grossFee: number | string;
}

export interface FieldError {
  field: string;
  message: string;
}

export type SubmitResult =
  | { kind: "created"; registration: Registration }
  | { kind: "invalid"; errors: FieldError[] }
  | { kind: "rateLimited" }
  | { kind: "failed" };

function fieldErrors(body: unknown): FieldError[] {
  if (typeof body !== "object" || body === null) {
    return [];
  }
  const errors = (body as Record<string, unknown>).errors;
  if (!Array.isArray(errors)) {
    return [];
  }
  return errors.filter(
    (error): error is FieldError =>
      typeof error === "object" &&
      error !== null &&
      typeof (error as FieldError).field === "string" &&
      typeof (error as FieldError).message === "string",
  );
}

export async function submitRegistration(
  request: RegistrationRequest,
  fetchImpl: typeof fetch = fetch,
): Promise<SubmitResult> {
  let response: Response;
  try {
    response = await fetchImpl("/api/registrations", {
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
  if (response.status === 201 || response.status === 200) {
    return {
      kind: "created",
      registration: (await response.json()) as Registration,
    };
  }
  if (response.status === 400) {
    const body: unknown = await response.json().catch(() => null);
    return { kind: "invalid", errors: fieldErrors(body) };
  }
  if (response.status === 429) {
    return { kind: "rateLimited" };
  }
  return { kind: "failed" };
}

export function formatAmount(value: number | string): string {
  return Number(value).toFixed(2);
}
