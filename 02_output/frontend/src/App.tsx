import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  fetchWorkshops,
  formatAmount,
  submitRegistration,
  type PayerType,
  type Registration,
  type RegistrationInput,
  type Workshop,
} from "./api";
import "./styles.css";

type Values = {
  firstName: string;
  lastName: string;
  email: string;
  payerType: PayerType;
  companyName: string;
  companyAddress: string;
  companyVatId: string;
  workshop: string;
};

const EMPTY: Values = {
  firstName: "",
  lastName: "",
  email: "",
  payerType: "private",
  companyName: "",
  companyAddress: "",
  companyVatId: "",
  workshop: "",
};

/** API field name → element id of the field (registration-form.json). */
function fieldIdOf(requestField: string): string {
  return requestField === "workshops" ? "workshop" : requestField;
}

const GENERAL_ERROR =
  "The registration could not be submitted. Please try again later.";
const WORKSHOPS_ERROR =
  "The workshops could not be loaded. You can register without a workshop.";

export function App() {
  const [workshops, setWorkshops] = useState<Workshop[]>([]);
  const [workshopsFailed, setWorkshopsFailed] = useState(false);
  const [values, setValues] = useState<Values>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [registration, setRegistration] = useState<Registration | null>(null);
  const submitting = useRef(false);

  useEffect(() => {
    let active = true;
    fetchWorkshops()
      .then((list) => active && setWorkshops(list))
      .catch(() => active && setWorkshopsFailed(true));
    return () => {
      active = false;
    };
  }, []);

  function set<K extends keyof Values>(key: K, value: Values[K]) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) {
      return;
    }
    submitting.current = true;
    setPending(true);
    setGeneralError(null);
    const input: RegistrationInput = {
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      payerType: values.payerType,
      workshops: values.workshop === "" ? [] : [values.workshop],
    };
    if (values.payerType === "company") {
      input.companyName = values.companyName;
      input.companyAddress = values.companyAddress;
      input.companyVatId = values.companyVatId;
    }
    const result = await submitRegistration(input);
    submitting.current = false;
    setPending(false);
    if (result.kind === "created") {
      setErrors({});
      setRegistration(result.registration);
    } else if (result.kind === "invalid") {
      const byField: Record<string, string> = {};
      for (const [field, message] of Object.entries(result.errors)) {
        byField[fieldIdOf(field)] = message;
      }
      setErrors(byField);
    } else {
      setErrors({});
      setGeneralError(GENERAL_ERROR);
    }
  }

  function textField(
    id: keyof Values,
    label: string,
    type = "text",
    autoComplete?: string,
  ) {
    const error = errors[id];
    return (
      <div className="field">
        <label htmlFor={id}>{label}</label>
        <input
          id={id}
          name={id}
          type={type}
          required
          autoComplete={autoComplete}
          value={values[id]}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          onChange={(e) => set(id, e.target.value)}
        />
        {error && (
          <p id={`${id}-error`} className="field-error">
            {error}
          </p>
        )}
      </div>
    );
  }

  return (
    <main>
      <h1>Conference registration</h1>

      {registration ? (
        <section id="confirmation" aria-live="polite">
          <h2>Thank you for registering</h2>
          <dl>
            <dt>Registration number</dt>
            <dd id="confirmation-registration-number">
              {registration.registrationNumber}
            </dd>
            <dt>Net fee</dt>
            <dd id="confirmation-net-fee">
              {formatAmount(registration.netFee)}
            </dd>
            <dt>VAT</dt>
            <dd id="confirmation-vat">{formatAmount(registration.vat)}</dd>
            <dt>Gross fee</dt>
            <dd id="confirmation-gross-fee">
              {formatAmount(registration.grossFee)}
            </dd>
          </dl>
          <p>A confirmation has been sent to your e-mail address.</p>
        </section>
      ) : (
        <form id="registration-form" onSubmit={onSubmit}>
          {generalError && (
            <div id="general-error" role="alert" className="general-error">
              {generalError}
            </div>
          )}

          {textField("firstName", "First name", "text", "given-name")}
          {textField("lastName", "Last name", "text", "family-name")}
          {textField("email", "E-mail", "email", "email")}

          <fieldset
            id="payerType"
            aria-describedby={errors.payerType ? "payerType-error" : undefined}
          >
            <legend>Payer</legend>
            <label>
              <input
                id="payerType-private"
                type="radio"
                name="payerType"
                value="private"
                checked={values.payerType === "private"}
                onChange={() => set("payerType", "private")}
              />
              Private person
            </label>
            <label>
              <input
                id="payerType-company"
                type="radio"
                name="payerType"
                value="company"
                checked={values.payerType === "company"}
                onChange={() => set("payerType", "company")}
              />
              Company
            </label>
            {errors.payerType && (
              <p id="payerType-error" className="field-error">
                {errors.payerType}
              </p>
            )}
          </fieldset>

          {values.payerType === "company" && (
            <>
              {textField("companyName", "Company name", "text", "organization")}
              {textField(
                "companyAddress",
                "Company address",
                "text",
                "street-address",
              )}
              {textField("companyVatId", "Company VAT ID")}
            </>
          )}

          <fieldset
            id="workshop"
            aria-describedby={errors.workshop ? "workshop-error" : undefined}
          >
            <legend>Workshop</legend>
            <label>
              <input
                id="workshop-none"
                type="radio"
                name="workshop"
                value=""
                checked={values.workshop === ""}
                onChange={() => set("workshop", "")}
              />
              No workshop
            </label>
            {workshops.map((workshop) => (
              <label key={workshop.id} htmlFor={`workshop-${workshop.id}`}>
                <input
                  id={`workshop-${workshop.id}`}
                  type="radio"
                  name="workshop"
                  value={workshop.id}
                  checked={values.workshop === workshop.id}
                  onChange={() => set("workshop", workshop.id)}
                />
                {workshop.title}
              </label>
            ))}
            {workshopsFailed && <p className="note">{WORKSHOPS_ERROR}</p>}
            {errors.workshop && (
              <p id="workshop-error" className="field-error">
                {errors.workshop}
              </p>
            )}
          </fieldset>

          <button id="submit" type="submit" disabled={pending}>
            Register
          </button>
        </form>
      )}
    </main>
  );
}
