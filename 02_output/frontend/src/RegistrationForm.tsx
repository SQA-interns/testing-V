import { useState, type FormEvent } from "react";
import {
  submitRegistration,
  type PayerType,
  type Registration,
  type RegistrationRequest,
} from "./api";
import { configuredWorkshops } from "./config";

// Registration form (contract registration-form.yaml; AC-001-14, AC-001-15).

type TextFieldName =
  | "firstName"
  | "lastName"
  | "email"
  | "companyName"
  | "companyAddress"
  | "companyVatId";

type Errors = Partial<Record<string, string>>;

const PARTICIPANT_FIELDS: {
  name: TextFieldName;
  label: string;
  type: string;
  max: number;
}[] = [
  { name: "firstName", label: "First name", type: "text", max: 100 },
  { name: "lastName", label: "Last name", type: "text", max: 100 },
  { name: "email", label: "E-mail", type: "email", max: 254 },
];

const COMPANY_FIELDS: {
  name: TextFieldName;
  label: string;
  type: string;
  max: number;
}[] = [
  { name: "companyName", label: "Company name", type: "text", max: 200 },
  { name: "companyAddress", label: "Company address", type: "text", max: 500 },
  { name: "companyVatId", label: "VAT ID", type: "text", max: 30 },
];

const NO_WORKSHOP = "";
const REQUIRED = "is required";
const FORM_FIELDS = new Set<string>([
  ...PARTICIPANT_FIELDS.map((field) => field.name),
  ...COMPANY_FIELDS.map((field) => field.name),
  "payerType",
  "workshops",
]);

interface Props {
  onRegistered: (registration: Registration) => void;
}

function TextField(props: {
  name: TextFieldName;
  label: string;
  type: string;
  max: number;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  const errorId = `${props.name}-error`;
  return (
    <div className="field">
      <label htmlFor={props.name}>{props.label}</label>
      <input
        id={props.name}
        name={props.name}
        type={props.type}
        maxLength={props.max}
        value={props.value}
        onChange={(event) => props.onChange(event.target.value)}
        aria-invalid={props.error ? "true" : undefined}
        aria-describedby={props.error ? errorId : undefined}
      />
      {props.error && (
        <p id={errorId} className="error">
          {props.error}
        </p>
      )}
    </div>
  );
}

export function RegistrationForm({ onRegistered }: Props) {
  const workshops = configuredWorkshops();
  const [values, setValues] = useState<Record<TextFieldName, string>>({
    firstName: "",
    lastName: "",
    email: "",
    companyName: "",
    companyAddress: "",
    companyVatId: "",
  });
  const [payerType, setPayerType] = useState<PayerType>("private");
  const [workshop, setWorkshop] = useState(NO_WORKSHOP);
  const [errors, setErrors] = useState<Errors>({});
  const [alert, setAlert] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const textFields =
    payerType === "company"
      ? [...PARTICIPANT_FIELDS, ...COMPANY_FIELDS]
      : PARTICIPANT_FIELDS;

  function setValue(name: TextFieldName, value: string) {
    setValues((current) => ({ ...current, [name]: value }));
  }

  function requiredErrors(): Errors {
    const missing: Errors = {};
    for (const field of textFields) {
      if (values[field.name].trim() === "") {
        missing[field.name] = REQUIRED;
      }
    }
    return missing;
  }

  function buildRequest(): RegistrationRequest {
    const request: RegistrationRequest = {
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      payerType,
      workshops: workshop === NO_WORKSHOP ? [] : [workshop],
    };
    if (payerType === "company") {
      request.companyName = values.companyName;
      request.companyAddress = values.companyAddress;
      request.companyVatId = values.companyVatId;
    }
    return request;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAlert(null);
    const missing = requiredErrors();
    setErrors(missing);
    if (Object.keys(missing).length > 0) {
      return;
    }
    setPending(true);
    const result = await submitRegistration(buildRequest());
    setPending(false);
    switch (result.kind) {
      case "created":
        onRegistered(result.registration);
        return;
      case "invalid": {
        const byField: Errors = {};
        const unknown: string[] = [];
        for (const error of result.errors) {
          if (FORM_FIELDS.has(error.field)) {
            byField[error.field] = error.message;
          } else {
            unknown.push(`${error.field} ${error.message}`);
          }
        }
        setErrors(byField);
        setAlert(
          unknown.length > 0 ? `Please check: ${unknown.join("; ")}` : null,
        );
        return;
      }
      case "rateLimited":
        setAlert(
          "Too many registrations from your network. Please try again later.",
        );
        return;
      default:
        setAlert("Registration failed. Please try again later.");
    }
  }

  return (
    <form aria-label="Registration" noValidate onSubmit={handleSubmit}>
      {alert && (
        <p role="alert" className="alert">
          {alert}
        </p>
      )}
      {PARTICIPANT_FIELDS.map((field) => (
        <TextField
          key={field.name}
          {...field}
          value={values[field.name]}
          error={errors[field.name]}
          onChange={(value) => setValue(field.name, value)}
        />
      ))}

      <fieldset
        role="radiogroup"
        aria-labelledby="payer-legend"
        aria-describedby={errors.payerType ? "payerType-error" : undefined}
      >
        <legend id="payer-legend">Payer</legend>
        <label>
          <input
            type="radio"
            name="payerType"
            value="private"
            checked={payerType === "private"}
            onChange={() => setPayerType("private")}
          />
          Private person
        </label>
        <label>
          <input
            type="radio"
            name="payerType"
            value="company"
            checked={payerType === "company"}
            onChange={() => setPayerType("company")}
          />
          Company
        </label>
        {errors.payerType && (
          <p id="payerType-error" className="error">
            {errors.payerType}
          </p>
        )}
      </fieldset>

      {payerType === "company" &&
        COMPANY_FIELDS.map((field) => (
          <TextField
            key={field.name}
            {...field}
            value={values[field.name]}
            error={errors[field.name]}
            onChange={(value) => setValue(field.name, value)}
          />
        ))}

      <fieldset
        role="radiogroup"
        aria-labelledby="workshop-legend"
        aria-describedby={errors.workshops ? "workshops-error" : undefined}
      >
        <legend id="workshop-legend">Workshop</legend>
        {workshops.map((option) => (
          <label key={option.id}>
            <input
              type="radio"
              name="workshop"
              value={option.id}
              checked={workshop === option.id}
              onChange={() => setWorkshop(option.id)}
            />
            {option.title}
          </label>
        ))}
        <label>
          <input
            type="radio"
            name="workshop"
            value={NO_WORKSHOP}
            checked={workshop === NO_WORKSHOP}
            onChange={() => setWorkshop(NO_WORKSHOP)}
          />
          No workshop
        </label>
        {errors.workshops && (
          <p id="workshops-error" className="error">
            {errors.workshops}
          </p>
        )}
      </fieldset>

      <button type="submit" disabled={pending}>
        Register
      </button>
    </form>
  );
}
