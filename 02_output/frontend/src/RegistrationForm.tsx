import { useState, type FormEvent } from "react";
import { formatAmount, submitRegistration, type FieldError } from "./api";
import type { Workshop } from "./config";
import { EMPTY, toRequest, type Values } from "./formModel";

// Form contract: docs/02_contracts/registration-form.yaml.

type Outcome =
  | { kind: "none" }
  | { kind: "success"; text: string }
  | { kind: "error"; text: string };

const RATE_LIMITED = "Too many registration attempts. Please try again later.";
const FAILED =
  "The registration could not be completed. Please try again later.";

interface Props {
  workshops: Workshop[];
  submit?: typeof submitRegistration;
}

export function RegistrationForm({
  workshops,
  submit = submitRegistration,
}: Props) {
  const [values, setValues] = useState<Values>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [outcome, setOutcome] = useState<Outcome>({ kind: "none" });
  const [busy, setBusy] = useState(false);

  const company = values.payerType === "company";

  function update(field: keyof Values, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setErrors({});
    setOutcome({ kind: "none" });
    const result = await submit(toRequest(values));
    setBusy(false);
    switch (result.kind) {
      case "created": {
        const { registrationNumber, grossFee, email } = result.registration;
        setOutcome({
          kind: "success",
          text:
            `Registration completed. Your registration number is ${registrationNumber}. ` +
            `Fee: ${formatAmount(grossFee)} EUR (including VAT). ` +
            `A confirmation e-mail has been sent to ${email}.`,
        });
        setValues(EMPTY);
        break;
      }
      case "invalid":
        setErrors(byField(result.errors));
        if (result.errors.length === 0) {
          setOutcome({ kind: "error", text: FAILED });
        }
        break;
      case "rateLimited":
        setOutcome({ kind: "error", text: RATE_LIMITED });
        break;
      default:
        setOutcome({ kind: "error", text: FAILED });
    }
  }

  function describedBy(field: string): string | undefined {
    return errors[field] ? `${field}-error` : undefined;
  }

  function errorText(field: string) {
    return errors[field] ? (
      <p id={`${field}-error`} className="field-error">
        {errors[field]}
      </p>
    ) : null;
  }

  return (
    <>
      <form aria-label="Registration" onSubmit={onSubmit}>
        <div className="field">
          <label htmlFor="firstName">First name</label>
          <input
            id="firstName"
            name="firstName"
            type="text"
            required
            maxLength={100}
            autoComplete="given-name"
            value={values.firstName}
            aria-invalid={Boolean(errors.firstName)}
            aria-describedby={describedBy("firstName")}
            onChange={(e) => update("firstName", e.target.value)}
          />
          {errorText("firstName")}
        </div>
        <div className="field">
          <label htmlFor="lastName">Last name</label>
          <input
            id="lastName"
            name="lastName"
            type="text"
            required
            maxLength={100}
            autoComplete="family-name"
            value={values.lastName}
            aria-invalid={Boolean(errors.lastName)}
            aria-describedby={describedBy("lastName")}
            onChange={(e) => update("lastName", e.target.value)}
          />
          {errorText("lastName")}
        </div>
        <div className="field">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            name="email"
            type="email"
            required
            maxLength={254}
            autoComplete="email"
            value={values.email}
            aria-invalid={Boolean(errors.email)}
            aria-describedby={describedBy("email")}
            onChange={(e) => update("email", e.target.value)}
          />
          {errorText("email")}
        </div>
        <fieldset
          className="field"
          aria-invalid={Boolean(errors.payerType)}
          aria-describedby={describedBy("payerType")}
        >
          <legend>Payer</legend>
          <label>
            <input
              type="radio"
              name="payerType"
              value="private"
              required
              checked={values.payerType === "private"}
              onChange={() => update("payerType", "private")}
            />
            Private person
          </label>
          <label>
            <input
              type="radio"
              name="payerType"
              value="company"
              checked={values.payerType === "company"}
              onChange={() => update("payerType", "company")}
            />
            Company
          </label>
          {errorText("payerType")}
        </fieldset>
        {company && (
          <>
            <div className="field">
              <label htmlFor="companyName">Company name</label>
              <input
                id="companyName"
                name="companyName"
                type="text"
                required
                maxLength={200}
                autoComplete="organization"
                value={values.companyName}
                aria-invalid={Boolean(errors.companyName)}
                aria-describedby={describedBy("companyName")}
                onChange={(e) => update("companyName", e.target.value)}
              />
              {errorText("companyName")}
            </div>
            <div className="field">
              <label htmlFor="companyAddress">Company address</label>
              <textarea
                id="companyAddress"
                name="companyAddress"
                required
                maxLength={500}
                rows={3}
                autoComplete="street-address"
                value={values.companyAddress}
                aria-invalid={Boolean(errors.companyAddress)}
                aria-describedby={describedBy("companyAddress")}
                onChange={(e) => update("companyAddress", e.target.value)}
              />
              {errorText("companyAddress")}
            </div>
            <div className="field">
              <label htmlFor="companyVatId">VAT ID</label>
              <input
                id="companyVatId"
                name="companyVatId"
                type="text"
                required
                maxLength={30}
                value={values.companyVatId}
                aria-invalid={Boolean(errors.companyVatId)}
                aria-describedby={describedBy("companyVatId")}
                onChange={(e) => update("companyVatId", e.target.value)}
              />
              {errorText("companyVatId")}
            </div>
          </>
        )}
        <div className="field">
          <label htmlFor="workshop">Workshop</label>
          <select
            id="workshop"
            name="workshop"
            value={values.workshop}
            aria-invalid={Boolean(errors.workshops)}
            aria-describedby={describedBy("workshops")}
            onChange={(e) => update("workshop", e.target.value)}
          >
            <option value="">No workshop</option>
            {workshops.map((workshop) => (
              <option key={workshop.id} value={workshop.id}>
                {`${workshop.id} - ${workshop.name}`}
              </option>
            ))}
          </select>
          {errorText("workshops")}
        </div>
        <button type="submit" disabled={busy}>
          Register
        </button>
      </form>
      <div role="status" aria-live="polite">
        {outcome.kind === "success" ? outcome.text : null}
      </div>
      {outcome.kind === "error" && <div role="alert">{outcome.text}</div>}
    </>
  );
}

function byField(errors: FieldError[]): Record<string, string> {
  const result: Record<string, string> = {};
  for (const error of errors) {
    result[error.field] = result[error.field]
      ? `${result[error.field]}; ${error.message}`
      : error.message;
  }
  return result;
}
