// Runtime configuration served as /config.json (docs/02_contracts/frontend-config.schema.json).
// The frontend container writes it from APP_WORKSHOPS at start, so business values are
// never part of the bundle (AR-04).

export interface Workshop {
  id: string;
  name: string;
}

export interface AppConfig {
  workshops: Workshop[];
}

function isWorkshop(value: unknown): value is Workshop {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.id === "string" &&
    candidate.id.length > 0 &&
    typeof candidate.name === "string" &&
    candidate.name.length > 0
  );
}

export function parseConfig(value: unknown): AppConfig {
  if (typeof value !== "object" || value === null) {
    throw new Error("Invalid configuration");
  }
  const workshops = (value as Record<string, unknown>).workshops;
  if (!Array.isArray(workshops) || !workshops.every(isWorkshop)) {
    throw new Error("Invalid configuration");
  }
  return { workshops: workshops.map(({ id, name }) => ({ id, name })) };
}

export async function loadConfig(
  fetchImpl: typeof fetch = fetch,
): Promise<AppConfig> {
  const response = await fetchImpl("/config.json", {
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    throw new Error("Configuration not available");
  }
  return parseConfig(await response.json());
}
