// Client of the registration API (docs/02_contracts/registration-api.openapi.yaml); /api only (AR-01).

export type PayerType = "private" | "company";

export interface Workshop {
  id: string;
  title: string;
}

export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType;
  companyName: string | null;
  companyAddress: string | null;
  companyVatId: string | null;
  workshops: string[];
}

export interface Registration {
  registrationNumber: string;
  netFee: number | string;
  vat: number | string;
  grossFee: number | string;
}

export type SubmitResult =
  | { kind: "registered"; registration: Registration }
  | { kind: "invalid"; fields: string[] }
  | { kind: "failed" };

export async function fetchWorkshops(): Promise<Workshop[]> {
  const response = await fetch("/api/workshops", {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    return [];
  }
  return (await response.json()) as Workshop[];
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
      kind: "registered",
      registration: (await response.json()) as Registration,
    };
  }
  if (response.status === 400) {
    const body = (await response.json().catch(() => ({}))) as {
      fields?: unknown;
    };
    const fields = Array.isArray(body.fields) ? body.fields.map(String) : [];
    return { kind: "invalid", fields };
  }
  return { kind: "failed" };
}
