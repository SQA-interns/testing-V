// The only module that calls the backend, and only under /api (AR-01).
// Shapes follow docs/02_contracts/registration-api.openapi.yaml.

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
  netFee: number | string;
  vat: number | string;
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

function isFieldError(value: unknown): value is FieldError {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.field === "string" && typeof candidate.message === "string"
  );
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
        Accept: "application/json, application/problem+json",
      },
      body: JSON.stringify(request),
    });
  } catch {
    return { kind: "failed" };
  }
  if (response.status === 429) {
    return { kind: "rateLimited" };
  }
  try {
    if (response.status === 201) {
      return {
        kind: "created",
        registration: (await response.json()) as Registration,
      };
    }
    if (response.status === 422) {
      const body = (await response.json()) as { errors?: unknown };
      const errors = Array.isArray(body.errors)
        ? body.errors.filter(isFieldError)
        : [];
      return { kind: "invalid", errors };
    }
  } catch {
    return { kind: "failed" };
  }
  return { kind: "failed" };
}
