import { useEffect, useState, type FormEvent } from "react";
import {
  fetchWorkshops,
  submitRegistration,
  type PayerType,
  type Registration,
  type RegistrationRequest,
  type Workshop,
} from "./api";
import { checkRequired, type FormValues } from "./validation";

// The single registration page (docs/02_contracts/registration-form.yaml, AR-01).

const EMPTY: FormValues = {
  firstName: "",
  lastName: "",
  email: "",
  payerType: "private",
  companyName: "",
  companyAddress: "",
  companyVatId: "",
  workshop: "",
};

function toRequest(values: FormValues): RegistrationRequest {
  const company = values.payerType === "company";
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    email: values.email.trim(),
    payerType: values.payerType,
    companyName: company ? values.companyName.trim() : null,
    companyAddress: company ? values.companyAddress.trim() : null,
    companyVatId: company ? values.companyVatId.trim() : null,
    workshops: values.workshop ? [values.workshop] : [],
  };
}

function money(value: number | string): string {
  return Number(value).toFixed(2);
}

function FieldError({ name, invalid }: { name: string; invalid: string[] }) {
  if (!invalid.includes(name)) {
    return null;
  }
  return (
    <span
      className="field-error"
      data-testid={`field-error-${name}`}
      role="alert"
    >
      Please check this field.
    </span>
  );
}

export function App() {
  const [values, setValues] = useState<FormValues>(EMPTY);
  const [workshops, setWorkshops] = useState<Workshop[]>([]);
  const [invalid, setInvalid] = useState<string[]>([]);
  const [failed, setFailed] = useState(false);
  const [sending, setSending] = useState(false);
  const [registration, setRegistration] = useState<Registration | null>(null);

  useEffect(() => {
    let active = true;
    fetchWorkshops().then((list) => {
      if (active) {
        setWorkshops(list);
      }
    });
    return () => {
      active = false;
    };
  }, []);

  function set<K extends keyof FormValues>(name: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setFailed(false);
    const missing = checkRequired(values);
    if (missing.length > 0) {
      setInvalid(missing);
      return;
    }
    setSending(true);
    const result = await submitRegistration(toRequest(values));
    setSending(false);
    if (result.kind === "registered") {
      setInvalid([]);
      setRegistration(result.registration);
    } else if (result.kind === "invalid") {
      setInvalid(result.fields);
    } else {
      setInvalid([]);
      setFailed(true);
    }
  }

  if (registration) {
    return (
      <main>
        <h1>Conference registration</h1>
        <p data-testid="confirmation" role="status">
          Registration {registration.registrationNumber} is confirmed. Fee:{" "}
          {money(registration.netFee)} EUR + VAT {money(registration.vat)} EUR ={" "}
          {money(registration.grossFee)} EUR. A confirmation e-mail has been
          sent.
        </p>
      </main>
    );
  }

  const company = values.payerType === "company";
  return (
    <main>
      <h1>Conference registration</h1>
      <form onSubmit={onSubmit} noValidate>
        <label>
          First name
          <input
            data-testid="first-name"
            name="firstName"
            maxLength={100}
            required
            value={values.firstName}
            onChange={(e) => set("firstName", e.target.value)}
          />
        </label>
        <FieldError name="firstName" invalid={invalid} />

        <label>
          Last name
          <input
            data-testid="last-name"
            name="lastName"
            maxLength={100}
            required
            value={values.lastName}
            onChange={(e) => set("lastName", e.target.value)}
          />
        </label>
        <FieldError name="lastName" invalid={invalid} />

        <label>
          E-mail
          <input
            data-testid="email"
            name="email"
            type="email"
            maxLength={254}
            required
            value={values.email}
            onChange={(e) => set("email", e.target.value)}
          />
        </label>
        <FieldError name="email" invalid={invalid} />

        <fieldset data-testid="payer-type">
          <legend>Payer</legend>
          {(["private", "company"] as PayerType[]).map((type) => (
            <label key={type}>
              <input
                type="radio"
                name="payerType"
                value={type}
                data-testid={`payer-type-${type}`}
                checked={values.payerType === type}
                onChange={() => set("payerType", type)}
              />
              {type === "private" ? "Private person" : "Company"}
            </label>
          ))}
        </fieldset>
        <FieldError name="payerType" invalid={invalid} />

        {company && (
          <>
            <label>
              Company name
              <input
                data-testid="company-name"
                name="companyName"
                maxLength={200}
                required
                value={values.companyName}
                onChange={(e) => set("companyName", e.target.value)}
              />
            </label>
            <FieldError name="companyName" invalid={invalid} />

            <label>
              Company address
              <textarea
                data-testid="company-address"
                name="companyAddress"
                maxLength={500}
                required
                value={values.companyAddress}
                onChange={(e) => set("companyAddress", e.target.value)}
              />
            </label>
            <FieldError name="companyAddress" invalid={invalid} />

            <label>
              Company VAT ID
              <input
                data-testid="company-vat-id"
                name="companyVatId"
                maxLength={20}
                required
                value={values.companyVatId}
                onChange={(e) => set("companyVatId", e.target.value)}
              />
            </label>
            <FieldError name="companyVatId" invalid={invalid} />
          </>
        )}

        <label>
          Workshop
          <select
            data-testid="workshop"
            name="workshop"
            value={values.workshop}
            onChange={(e) => set("workshop", e.target.value)}
          >
            <option value="">No workshop</option>
            {workshops.map((workshop) => (
              <option key={workshop.id} value={workshop.id}>
                {workshop.title}
              </option>
            ))}
          </select>
        </label>
        <FieldError name="workshops" invalid={invalid} />

        {failed && (
          <p data-testid="error-message" role="alert">
            Registration is not possible right now. Please try again later.
          </p>
        )}

        <button type="submit" data-testid="submit" disabled={sending}>
          Register
        </button>
      </form>
    </main>
  );
}
