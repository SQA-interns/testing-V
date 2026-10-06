import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  fetchOptions,
  formatAmount,
  submitRegistration,
  type Price,
  type Registration,
  type RegistrationRequest,
  type Workshop,
} from "./api";
import "./App.css";

// The single registration page (docs/02_contracts/registration-form.json, AC-001-20 … 24).

const DUPLICATE_MESSAGE =
  "A registration with this e-mail address already exists.";
const GENERIC_MESSAGE =
  "Registration is not possible right now. Please try again later.";

type PayerType = "private" | "company";

interface FormValues {
  firstName: string;
  lastName: string;
  email: string;
  student: boolean;
  payerType: PayerType;
  companyName: string;
  companyAddress: string;
  companyVatId: string;
  workshop: string;
}

const EMPTY: FormValues = {
  firstName: "",
  lastName: "",
  email: "",
  student: false,
  payerType: "private",
  companyName: "",
  companyAddress: "",
  companyVatId: "",
  workshop: "",
};

function toRequest(values: FormValues): RegistrationRequest {
  const request: RegistrationRequest = {
    firstName: values.firstName,
    lastName: values.lastName,
    email: values.email,
    payerType: values.payerType,
    workshops: values.workshop ? [values.workshop] : [],
    student: values.student,
  };
  if (values.payerType === "company") {
    request.companyName = values.companyName;
    request.companyAddress = values.companyAddress;
    request.companyVatId = values.companyVatId;
  }
  return request;
}

function FieldMessage({ id, message }: { id: string; message?: string }) {
  if (!message) {
    return null;
  }
  return (
    <p id={id} className="field-error" role="alert">
      {message}
    </p>
  );
}

export function App() {
  const [values, setValues] = useState<FormValues>(EMPTY);
  const [workshops, setWorkshops] = useState<Workshop[]>([]);
  const [price, setPrice] = useState<Price | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [registration, setRegistration] = useState<Registration | null>(null);
  const latestPriceRequest = useRef(0);

  useEffect(() => {
    const requestId = ++latestPriceRequest.current;
    fetchOptions(values.student)
      .then((options) => {
        if (requestId !== latestPriceRequest.current) {
          return;
        }
        setWorkshops(options.workshops);
        setPrice(options.price);
      })
      .catch(() => {
        if (requestId === latestPriceRequest.current) {
          setFormError(GENERIC_MESSAGE);
        }
      });
  }, [values.student]);

  function update<K extends keyof FormValues>(field: K, value: FormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSending(true);
    setFormError(null);
    setFieldErrors({});
    const result = await submitRegistration(toRequest(values));
    setSending(false);
    switch (result.kind) {
      case "created":
        setRegistration(result.registration);
        break;
      case "invalid":
        setFieldErrors(
          Object.fromEntries(result.errors.map((e) => [e.field, e.message])),
        );
        break;
      case "duplicate":
        setFormError(DUPLICATE_MESSAGE);
        break;
      default:
        setFormError(GENERIC_MESSAGE);
    }
  }

  const describedBy = (field: string) =>
    fieldErrors[field] ? `${field}-error` : undefined;

  const textField = (
    field: "firstName" | "lastName" | "email",
    label: string,
    type = "text",
  ) => (
    <div className="field">
      <label htmlFor={field}>{label}</label>
      <input
        id={field}
        name={field}
        type={type}
        autoComplete={
          field === "email"
            ? "email"
            : field === "firstName"
              ? "given-name"
              : "family-name"
        }
        value={values[field]}
        maxLength={field === "email" ? 254 : 100}
        required
        aria-invalid={fieldErrors[field] ? true : undefined}
        aria-describedby={describedBy(field)}
        onChange={(e) => update(field, e.target.value)}
      />
      <FieldMessage id={`${field}-error`} message={fieldErrors[field]} />
    </div>
  );

  if (registration) {
    const workshop = workshops.find((w) => w.id === registration.workshop);
    return (
      <main>
        <h1>Conference registration</h1>
        <section
          data-testid="confirmation"
          role="status"
          className="confirmation"
        >
          <h2>Thank you for registering</h2>
          <p>
            Registration number:{" "}
            <strong>{registration.registrationNumber}</strong>
          </p>
          <p>Workshop: {workshop ? workshop.title : "none"}</p>
          <p>Net fee: {formatAmount(registration.netFee)} EUR</p>
          <p>VAT: {formatAmount(registration.vat)} EUR</p>
          <p>
            Total: <strong>{formatAmount(registration.grossFee)} EUR</strong>
          </p>
          <p>A confirmation has been sent to your e-mail address.</p>
        </section>
      </main>
    );
  }

  return (
    <main>
      <h1>Conference registration</h1>
      {formError && (
        <p data-testid="form-error" role="alert" className="form-error">
          {formError}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        {textField("firstName", "First name")}
        {textField("lastName", "Last name")}
        {textField("email", "E-mail", "email")}

        <div className="field checkbox">
          <input
            id="student"
            name="student"
            type="checkbox"
            checked={values.student}
            onChange={(e) => update("student", e.target.checked)}
          />
          <label htmlFor="student">I am a student</label>
        </div>

        <fieldset aria-describedby={describedBy("payerType")}>
          <legend>Payer</legend>
          {(["private", "company"] as const).map((payer) => (
            <div className="choice" key={payer}>
              <input
                id={`payer-${payer}`}
                name="payerType"
                type="radio"
                value={payer}
                checked={values.payerType === payer}
                onChange={() => update("payerType", payer)}
              />
              <label htmlFor={`payer-${payer}`}>
                {payer === "private" ? "Private person" : "Company"}
              </label>
            </div>
          ))}
          <FieldMessage id="payerType-error" message={fieldErrors.payerType} />
        </fieldset>

        {values.payerType === "company" && (
          <div className="company">
            <div className="field">
              <label htmlFor="companyName">Company name</label>
              <input
                id="companyName"
                name="companyName"
                value={values.companyName}
                maxLength={200}
                required
                aria-invalid={fieldErrors.companyName ? true : undefined}
                aria-describedby={describedBy("companyName")}
                onChange={(e) => update("companyName", e.target.value)}
              />
              <FieldMessage
                id="companyName-error"
                message={fieldErrors.companyName}
              />
            </div>
            <div className="field">
              <label htmlFor="companyAddress">Company address</label>
              <textarea
                id="companyAddress"
                name="companyAddress"
                value={values.companyAddress}
                maxLength={500}
                rows={3}
                required
                aria-invalid={fieldErrors.companyAddress ? true : undefined}
                aria-describedby={describedBy("companyAddress")}
                onChange={(e) => update("companyAddress", e.target.value)}
              />
              <FieldMessage
                id="companyAddress-error"
                message={fieldErrors.companyAddress}
              />
            </div>
            <div className="field">
              <label htmlFor="companyVatId">Company VAT ID</label>
              <input
                id="companyVatId"
                name="companyVatId"
                value={values.companyVatId}
                maxLength={30}
                aria-invalid={fieldErrors.companyVatId ? true : undefined}
                aria-describedby={describedBy("companyVatId")}
                onChange={(e) => update("companyVatId", e.target.value)}
              />
              <FieldMessage
                id="companyVatId-error"
                message={fieldErrors.companyVatId}
              />
            </div>
          </div>
        )}

        <fieldset aria-describedby={describedBy("workshops")}>
          <legend>Workshop</legend>
          {workshops.map((workshop) => (
            <div className="choice" key={workshop.id}>
              <input
                id={`workshop-${workshop.id}`}
                name="workshop"
                type="radio"
                value={workshop.id}
                checked={values.workshop === workshop.id}
                onChange={() => update("workshop", workshop.id)}
              />
              <label htmlFor={`workshop-${workshop.id}`}>
                {workshop.title}
              </label>
            </div>
          ))}
          <div className="choice">
            <input
              id="workshop-none"
              name="workshop"
              type="radio"
              value=""
              checked={values.workshop === ""}
              onChange={() => update("workshop", "")}
            />
            <label htmlFor="workshop-none">No workshop</label>
          </div>
          <FieldMessage id="workshops-error" message={fieldErrors.workshops} />
        </fieldset>

        <section
          data-testid="price-summary"
          className="price"
          aria-live="polite"
        >
          <h2>Your fee</h2>
          {price ? (
            <dl>
              <dt>Net fee</dt>
              <dd>{formatAmount(price.netFee)} EUR</dd>
              <dt>VAT</dt>
              <dd>{formatAmount(price.vat)} EUR</dd>
              <dt>Total</dt>
              <dd>
                <strong>{formatAmount(price.grossFee)} EUR</strong>
              </dd>
            </dl>
          ) : (
            <p>Loading…</p>
          )}
        </section>

        <button type="submit" disabled={sending}>
          Register
        </button>
      </form>
    </main>
  );
}
