/** Formats an API amount (JSON number or decimal string) with two decimals, e.g. "240.00". */
export function formatAmount(value: number | string): string {
  const amount = typeof value === "number" ? value : Number(value);
  return Number.isFinite(amount) ? amount.toFixed(2) : String(value);
}
