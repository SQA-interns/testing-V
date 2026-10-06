import { useState } from "react";
import type { Registration } from "./api";
import { Confirmation } from "./Confirmation";
import { RegistrationForm } from "./RegistrationForm";

// The single registration page (AR-01): the form, then the confirmation.
export function App() {
  const [registration, setRegistration] = useState<Registration | null>(null);
  return (
    <main>
      <h1>Conference registration</h1>
      {registration ? (
        <Confirmation registration={registration} />
      ) : (
        <RegistrationForm onRegistered={setRegistration} />
      )}
    </main>
  );
}
