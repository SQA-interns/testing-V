import type { PayerType, RegistrationRequest } from "./api";

// Values of the registration form and their mapping to the API request
// (docs/02_contracts/registration-form.yaml, "request").

export interface Values {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType | "";
  companyName: string;
  companyAddress: string;
  companyVatId: string;
  workshop: string;
}

export const EMPTY: Values = {
  firstName: "",
  lastName: "",
  email: "",
  payerType: "",
  companyName: "",
  companyAddress: "",
  companyVatId: "",
  workshop: "",
};

export function toRequest(values: Values): RegistrationRequest {
  const request: RegistrationRequest = {
    firstName: values.firstName,
    lastName: values.lastName,
    email: values.email,
    payerType: values.payerType as PayerType,
    workshops: values.workshop === "" ? [] : [values.workshop],
  };
  if (values.payerType === "company") {
    request.companyName = values.companyName;
    request.companyAddress = values.companyAddress;
    request.companyVatId = values.companyVatId;
  }
  return request;
}
