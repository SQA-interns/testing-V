// Client-side required-field check for usability only; the backend is authoritative (SB-01).

import type { PayerType } from "./api";

export interface FormValues {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType;
  companyName: string;
  companyAddress: string;
  companyVatId: string;
  workshop: string;
}

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Names of fields that are required but empty, or an e-mail that is clearly malformed. */
export function checkRequired(values: FormValues): string[] {
  const missing: string[] = [];
  if (!values.firstName.trim()) {
    missing.push("firstName");
  }
  if (!values.lastName.trim()) {
    missing.push("lastName");
  }
  if (!EMAIL.test(values.email.trim())) {
    missing.push("email");
  }
  if (values.payerType === "company") {
    if (!values.companyName.trim()) {
      missing.push("companyName");
    }
    if (!values.companyAddress.trim()) {
      missing.push("companyAddress");
    }
    if (!values.companyVatId.trim()) {
      missing.push("companyVatId");
    }
  }
  return missing;
}
