import { expect, test } from "@playwright/test";

// End-to-end: browser -> nginx -> backend -> PostgreSQL and Mailpit, on the local stack
// started with `docker compose up` in 02_output/ (environments.md: local).

const mailpitUrl = process.env.E2E_MAILPIT_URL ?? "http://127.0.0.1:8025";

type MailpitSearch = { messages: { ID: string; Subject: string }[] };

async function messagesTo(address: string): Promise<MailpitSearch["messages"]> {
  const query = encodeURIComponent(`to:"${address}"`);
  const response = await fetch(`${mailpitUrl}/api/v1/search?query=${query}`);
  return ((await response.json()) as MailpitSearch).messages ?? [];
}

test("AC-001-01 AC-001-04 a participant registers through the form and receives the confirmation", async ({
  page,
  request,
}) => {
  const email = `e2e-${Date.now()}@example.org`;
  const optionsResponse = await request.get(
    "/api/registration-options?student=false",
  );
  expect(optionsResponse.ok()).toBe(true);
  const options = (await optionsResponse.json()) as {
    workshops: { id: string; title: string }[];
    price: { grossFee: number };
  };
  const grossFee = options.price.grossFee.toFixed(2);

  await page.goto("/");
  await page.getByLabel("First name").fill("Žiga");
  await page.getByLabel("Last name").fill("Čebašek");
  await page.getByLabel("E-mail").fill(email);
  await page.getByRole("radio", { name: options.workshops[0].title }).check();
  await expect(page.getByTestId("price-summary")).toContainText(grossFee);
  await page.getByRole("button", { name: "Register" }).click();

  const confirmation = page.getByTestId("confirmation");
  await expect(confirmation).toContainText(/CR-\d{6}/);
  await expect(confirmation).toContainText(grossFee);
  const registrationNumber = (await confirmation.textContent())?.match(
    /CR-\d{6}/,
  )?.[0];

  await expect
    .poll(async () => (await messagesTo(email)).length, { timeout: 30_000 })
    .toBe(1);
  const [message] = await messagesTo(email);
  expect(message.Subject).toContain(registrationNumber);
});

test("AC-001-20 the page lists the configured workshops and an unchecked student box", async ({
  page,
  request,
}) => {
  const options = (await (
    await request.get("/api/registration-options")
  ).json()) as {
    workshops: { id: string; title: string }[];
  };

  await page.goto("/");

  for (const workshop of options.workshops) {
    await expect(
      page.getByRole("radio", { name: workshop.title }),
    ).toBeVisible();
  }
  await expect(
    page.getByRole("checkbox", { name: "I am a student" }),
  ).not.toBeChecked();
});
