import type { Registration } from "./api";
import { formatAmount } from "./format";

// Success state of the form contract: number and fees of the stored registration (AC-001-14).
export function Confirmation({ registration }: { registration: Registration }) {
  return (
    <section role="status" aria-labelledby="confirmation-heading">
      <h2 id="confirmation-heading">Registration confirmed</h2>
      <dl>
        <dt>Registration number</dt> <dd>{registration.registrationNumber}</dd>{" "}
        <dt>Net fee</dt> <dd>{formatAmount(registration.netFee)} EUR</dd>{" "}
        <dt>VAT</dt> <dd>{formatAmount(registration.vat)} EUR</dd>{" "}
        <dt>Gross fee</dt>{" "}
        <dd>{formatAmount(registration.grossFee)} EUR</dd>{" "}
      </dl>
      <p>A confirmation e-mail has been sent to {registration.email}.</p>
    </section>
  );
}
