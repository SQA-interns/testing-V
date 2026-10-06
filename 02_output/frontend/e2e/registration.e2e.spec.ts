/**
 * End-to-end tests for US-001 through the real page, backend and mail catcher
 * (AC-001-11 with AC-001-04, AC-001-07). Needs the local stack (`docker compose up` in
 * 02_output/): backend on 127.0.0.1:8080, Mailpit API on MAILPIT_URL (default
 * http://127.0.0.1:8025). Element ids: docs/02_contracts/registration-form.json.
 * No business value is written here: amounts shown on the page are compared with the
 * confirmation e-mail, workshops with GET /api/workshops.
 */
import {
  expect,
  test,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

const mailpitUrl = process.env.MAILPIT_URL ?? "http://127.0.0.1:8025";

type Workshop = { id: string; title: string };

function uniqueEmail(tag: string): string {
  return `e2e.${tag}.${Date.now()}.${Math.floor(Math.random() * 1e6)}@example.org`;
}

async function mailTextsTo(
  request: APIRequestContext,
  address: string,
): Promise<string[]> {
  const list = await request.get(`${mailpitUrl}/api/v1/messages?limit=500`);
  expect(list.ok(), "Mailpit reachable").toBe(true);
  const messages = (await list.json()).messages as {
    ID: string;
    To: { Address: string }[];
  }[];
  const texts: string[] = [];
  for (const message of messages) {
    if (
      message.To.some(
        (to) => to.Address.toLowerCase() === address.toLowerCase(),
      )
    ) {
      const full = await request.get(
        `${mailpitUrl}/api/v1/message/${message.ID}`,
      );
      const body = await full.json();
      texts.push(`${body.Subject}\n${body.Text}`);
    }
  }
  return texts;
}

async function openForm(page: Page) {
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Conference registration" }),
  ).toBeVisible();
  await expect(page.locator("#registration-form")).toBeVisible();
}

async function fillParticipant(page: Page, email: string) {
  await page.locator("#firstName").fill("Ana");
  await page.locator("#lastName").fill("Novak");
  await page.locator("#email").fill(email);
}

async function apiWorkshops(request: APIRequestContext): Promise<Workshop[]> {
  const response = await request.get("/api/workshops");
  expect(response.status(), "GET /api/workshops").toBe(200);
  return (await response.json()) as Workshop[];
}

function amountOf(text: string | null): string {
  const match = /(\d+\.\d{2}) EUR/.exec(text ?? "");
  expect(match, `amount in "${text}"`).not.toBeNull();
  return (match as RegExpExecArray)[1];
}

test("AC-001-11 the form offers exactly the workshops of the API", async ({
  page,
  request,
}) => {
  const workshops = await apiWorkshops(request);

  await openForm(page);

  await expect(page.locator("#workshop-none")).toBeChecked();
  for (const workshop of workshops) {
    await expect(page.getByLabel(workshop.title)).toHaveAttribute(
      "id",
      `workshop-${workshop.id}`,
    );
  }
  await expect(page.locator("input[type=radio][id^='workshop-']")).toHaveCount(
    workshops.length + 1,
  );
});

test("AC-001-11 AC-001-04 registration through the page shows the confirmation and sends one e-mail", async ({
  page,
  request,
}) => {
  const workshops = await apiWorkshops(request);
  const email = uniqueEmail("company");
  await openForm(page);
  await fillParticipant(page, email);
  await page.locator("#payerType-company").check();
  await page.locator("#companyName").fill("Primer d.o.o.");
  await page.locator("#companyAddress").fill("Koroška cesta 1, 2000 Maribor");
  await page.locator("#companyVatId").fill("SI00000001");
  await page.locator(`#workshop-${workshops[0].id}`).check();

  await page.locator("#submit").click();

  await expect(page.locator("#confirmation")).toBeVisible();
  await expect(page.locator("#registration-form")).toHaveCount(0);
  const number = (
    (await page.locator("#confirmation-registration-number").textContent()) ??
    ""
  ).match(/REG-[0-9A-Z]+/)?.[0];
  expect(number, "registration number on the page").toBeTruthy();
  const net = amountOf(
    await page.locator("#confirmation-net-fee").textContent(),
  );
  const vat = amountOf(await page.locator("#confirmation-vat").textContent());
  const gross = amountOf(
    await page.locator("#confirmation-gross-fee").textContent(),
  );

  await expect
    .poll(async () => (await mailTextsTo(request, email)).length, {
      timeout: 10_000,
    })
    .toBe(1);
  const [mail] = await mailTextsTo(request, email);
  expect(mail).toContain(number as string);
  expect(mail).toContain(net);
  expect(mail).toContain(vat);
  expect(mail).toContain(gross);
});

test("AC-001-11 AC-001-07 server-side validation error is shown next to the field", async ({
  page,
  request,
}) => {
  // Accepted by the browser's e-mail input, rejected by the server (no dot in the domain, D-08).
  const email = `e2e.invalid.${Date.now()}@localdomain`;
  await openForm(page);
  await fillParticipant(page, email);

  await page.locator("#submit").click();

  await expect(page.locator("#email-error")).toBeVisible();
  await expect(page.locator("#email")).toHaveValue(email);
  await expect(page.locator("#confirmation")).toHaveCount(0);
  expect(await mailTextsTo(request, email)).toHaveLength(0);
});
