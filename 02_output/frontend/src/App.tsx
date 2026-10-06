import { useEffect, useState } from "react";
import { loadConfig, type AppConfig } from "./config";
import { RegistrationForm } from "./RegistrationForm";

type State =
  | { kind: "loading" }
  | { kind: "ready"; config: AppConfig }
  | { kind: "error" };

export function App({
  load = loadConfig,
}: {
  load?: () => Promise<AppConfig>;
}) {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    let active = true;
    load()
      .then((config) => active && setState({ kind: "ready", config }))
      .catch(() => active && setState({ kind: "error" }));
    return () => {
      active = false;
    };
  }, [load]);

  return (
    <main>
      <h1>Conference registration</h1>
      {state.kind === "loading" && <p>Loading…</p>}
      {state.kind === "error" && (
        <p role="alert">
          The registration form is not available. Please try again later.
        </p>
      )}
      {state.kind === "ready" && (
        <RegistrationForm workshops={state.config.workshops} />
      )}
    </main>
  );
}
