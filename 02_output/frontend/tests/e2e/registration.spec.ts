// End-to-end tests for US-001 (AC-001-14, AC-001-15, AC-001-04 at runtime) against the running
// stack: frontend (E2E_BASE_URL, default the compose frontend) with the real backend, PostgreSQL
// and Mailpit (E2E_MAILPIT_URL via playwright.config.ts). No business values are hard-coded: workshops and amounts are
// read from the page.
import { expect, test, type APIRequestContext } from "@playwright/test";

// Mailpit base URL from playwright.config.ts (project metadata, from E2E_MAILPIT_URL).
function mailpitUrl(): string {
  const url = test.info().project.metadata?.mailpitUrl;
  expect(url, "mailpitUrl in playwright.config.ts metadata").toBeTruthy();
  return String(url);
}

function uniqueEmail(): string {
  return `ana.novak+e2e${Date.now()}${Math.floor(Math.random() * 1e6)}@example.org`;
}

async function mailsTo(
  request: APIRequestContext,
  email: string,
): Promise<{ ID: string }[]> {
  const query = encodeURIComponent(`to:"${email}"`);
  const response = await request.get(
    `${mailpitUrl()}/api/v1/search?query=${query}`,
  );
  expect(response.ok()).toBeTruthy();
  return (await response.json()).messages;
}

test("AC-001-14 AC-001-04 participant registers through the form and gets one confirmation", async ({
  page,
  request,
}) => {
  const email = uniqueEmail();
  await page.goto("/");

  await page.getByRole("textbox", { name: "First name" }).fill("Ana");
  await page.getByRole("textbox", { name: "Last name" }).fill("Novak");
  await page.getByRole("textbox", { name: "E-mail" }).fill(email);
  await page
    .getByRole("radiogroup", { name: "Payer" })
    .getByRole("radio", { name: "Company" })
    .check();
  await page
    .getByRole("textbox", { name: "Company name" })
    .fill("Primer d.o.o.");
  await page
    .getByRole("textbox", { name: "Company address" })
    .fill("Koroška cesta 1, 2000 Maribor");
  await page.getByRole("textbox", { name: "VAT ID" }).fill("SI00000001");
  const workshops = page
    .getByRole("radiogroup", { name: "Workshop" })
    .getByRole("radio");
  expect(await workshops.count()).toBeGreaterThan(1);
  await workshops.first().check();
  await page.getByRole("button", { name: "Register" }).click();

  const confirmation = page.getByRole("status", {
    name: "Registration confirmed",
  });
  await expect(confirmation).toBeVisible();
  const text = (await confirmation.textContent()) ?? "";
  const number = /CR-[0-9A-Z]+/.exec(text)?.[0];
  const gross = /Gross fee\D*([0-9]+\.[0-9]{2}) EUR/.exec(text)?.[1];
  expect(number, "registration number shown").toBeTruthy();
  expect(gross, "gross fee shown with two decimals").toBeTruthy();

  await expect
    .poll(async () => (await mailsTo(request, email)).length, {
      timeout: 15_000,
    })
    .toBe(1);
  await page.waitForTimeout(2_000);
  const mails = await mailsTo(request, email);
  expect(mails).toHaveLength(1);
  const message = await (
    await request.get(`${mailpitUrl()}/api/v1/message/${mails[0].ID}`)
  ).json();
  expect(message.Text).toContain(number);
  expect(message.Text).toContain(gross);
});

test("AC-001-15 rejected e-mail is shown next to the field and nothing is registered", async ({
  page,
  request,
}) => {
  const email = `ana.novak+e2e${Date.now()}@example`;
  await page.goto("/");

  await page.getByRole("textbox", { name: "First name" }).fill("Ana");
  await page.getByRole("textbox", { name: "Last name" }).fill("Novak");
  await page.getByRole("textbox", { name: "E-mail" }).fill(email);
  await page.getByRole("button", { name: "Register" }).click();

  const field = page.getByRole("textbox", { name: "E-mail" });
  await expect(field).toHaveAttribute("aria-invalid", "true");
  await expect(field).toHaveValue(email);
  await expect(
    page.getByRole("status", { name: "Registration confirmed" }),
  ).toHaveCount(0);
  await page.waitForTimeout(3_000);
  expect(await mailsTo(request, email)).toHaveLength(0);
});
