// Runtime configuration from /config.js (contract registration-form.yaml, D-14): the configured
// workshops are never hard-coded in the frontend (AR-04).

export interface Workshop {
  id: string;
  title: string;
}

interface RuntimeConfig {
  workshops?: unknown;
}

function isWorkshop(value: unknown): value is Workshop {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.id === "string" && typeof candidate.title === "string"
  );
}

export function configuredWorkshops(): Workshop[] {
  const config = (window as Window & { APP_CONFIG?: RuntimeConfig }).APP_CONFIG;
  const workshops = config?.workshops;
  return Array.isArray(workshops) ? workshops.filter(isWorkshop) : [];
}
