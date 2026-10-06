import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const apiUrl = process.env.E2E_API_URL ?? "http://127.0.0.1:8080";
export const mailpitUrl =
  process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

export function uniqueEmail(tag: string): string {
  return `e2e-${tag}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

function organizerAuthorization(): string {
  const user = process.env.ORGANIZER_USERNAME;
  const password = process.env.ORGANIZER_PASSWORD;
  if (!user || !password) {
    throw new Error(
      "ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set for the e2e tests",
    );
  }
  return "Basic " + Buffer.from(`${user}:${password}`).toString("base64");
}

export interface StoredRegistration {
  registrationNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  payerType: string;
  companyName: string | null;
  companyAddress: string | null;
  companyVatId: string | null;
  workshop: string | null;
  netFee: number | string;
  vat: number | string;
  grossFee: number | string;
}

export async function readRegistration(
  request: APIRequestContext,
  registrationNumber: string,
): Promise<StoredRegistration> {
  const response = await request.get(
    `${apiUrl}/api/registrations/${registrationNumber}`,
    {
      headers: { Authorization: organizerAuthorization() },
    },
  );
  expect(response.status()).toBe(200);
  return (await response.json()) as StoredRegistration;
}

export async function mailTextTo(
  request: APIRequestContext,
  address: string,
): Promise<string> {
  let text = "";
  await expect
    .poll(
      async () => {
        const search = await request.get(
          `${mailpitUrl}/api/v1/search?query=${encodeURIComponent(`to:"${address}"`)}`,
        );
        const found = (await search.json()) as { messages: { ID: string }[] };
        if (found.messages.length === 0) {
          return 0;
        }
        const message = await request.get(
          `${mailpitUrl}/api/v1/message/${found.messages[0].ID}`,
        );
        text = ((await message.json()) as { Text: string }).Text;
        return found.messages.length;
      },
      { timeout: 10_000 },
    )
    .toBe(1);
  return text;
}

export function twoDecimals(value: number | string): string {
  return Number(value).toFixed(2);
}

export async function fillPerson(page: Page, email: string): Promise<void> {
  await page.getByTestId("first-name").fill("Špela");
  await page.getByTestId("last-name").fill("Žagar Čeč");
  await page.getByTestId("email").fill(email);
}

export async function confirmedRegistrationNumber(page: Page): Promise<string> {
  const confirmation = page.getByTestId("confirmation");
  await expect(confirmation).toBeVisible({ timeout: 15_000 });
  const match = /REG-\d{6,}/.exec((await confirmation.textContent()) ?? "");
  expect(match, "registration number shown in the confirmation").not.toBeNull();
  return match![0];
}
