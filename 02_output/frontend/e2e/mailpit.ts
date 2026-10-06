import { expect, type APIRequestContext } from "@playwright/test";

const mailpitUrl = process.env.MAILPIT_URL ?? "http://127.0.0.1:8025";

export function uniqueEmail(): string {
  const random = Math.random().toString(36).slice(2, 10);
  return `e2e-${Date.now()}-${random}@example.com`;
}

/** Waits for exactly one message to the address and returns its plain-text body. */
export async function confirmationText(
  request: APIRequestContext,
  address: string,
): Promise<string> {
  const query = encodeURIComponent(`to:"${address}"`);
  let ids: string[] = [];
  await expect
    .poll(
      async () => {
        const response = await request.get(
          `${mailpitUrl}/api/v1/search?query=${query}`,
        );
        const list = (await response.json()) as {
          messages?: { ID: string }[];
        };
        ids = (list.messages ?? []).map((message) => message.ID);
        return ids.length;
      },
      { timeout: 10_000 },
    )
    .toBe(1);
  const message = await request.get(`${mailpitUrl}/api/v1/message/${ids[0]}`);
  const body = (await message.json()) as { Text: string };
  return body.Text;
}
