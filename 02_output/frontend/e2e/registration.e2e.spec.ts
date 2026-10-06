import { expect, test } from "@playwright/test";
import {
  apiUrl,
  confirmedRegistrationNumber,
  fillPerson,
  mailpitUrl,
  mailTextTo,
  readRegistration,
  twoDecimals,
  uniqueEmail,
} from "./support";

// US-001 through the registration form (docs/02_contracts/registration-form.yaml).

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

test("AC-001-05 AC-001-03 AC-001-06 private participant registers through the form", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("private");
  await expect(page.getByTestId("first-name")).toBeVisible();
  await fillPerson(page, email);
  await page.getByTestId("payer-type-private").check();
  await page.getByTestId("submit").click();

  const number = await confirmedRegistrationNumber(page);
  const stored = await readRegistration(request, number);
  expect(stored).toMatchObject({
    registrationNumber: number,
    firstName: "Špela",
    lastName: "Žagar Čeč",
    email,
    payerType: "private",
    companyName: null,
    companyAddress: null,
    companyVatId: null,
    workshop: null,
  });

  const confirmation = page.getByTestId("confirmation");
  await expect(confirmation).toContainText(twoDecimals(stored.netFee));
  await expect(confirmation).toContainText(twoDecimals(stored.vat));
  await expect(confirmation).toContainText(twoDecimals(stored.grossFee));

  const mail = await mailTextTo(request, email);
  expect(mail).toContain(number);
  expect(mail).toContain(twoDecimals(stored.netFee));
  expect(mail).toContain("Žagar Čeč");
});

test("AC-001-04 company payer details are entered and stored for the invoice", async ({
  page,
  request,
}) => {
  const email = uniqueEmail("company");
  await expect(page.getByTestId("company-name")).toBeHidden();
  await expect(page.getByTestId("first-name")).toBeVisible();
  await fillPerson(page, email);
  await page.getByTestId("payer-type-company").check();
  await expect(page.getByTestId("company-name")).toBeVisible();
  await page.getByTestId("company-name").fill("Žičnice Čatež d.o.o.");
  await page
    .getByTestId("company-address")
    .fill("Šmartinska cesta 152, 1000 Ljubljana");
  await page.getByTestId("company-vat-id").fill("SI12345678");
  await page.getByTestId("submit").click();

  const number = await confirmedRegistrationNumber(page);
  const stored = await readRegistration(request, number);
  expect(stored).toMatchObject({
    payerType: "company",
    companyName: "Žičnice Čatež d.o.o.",
    companyAddress: "Šmartinska cesta 152, 1000 Ljubljana",
    companyVatId: "SI12345678",
  });
});

test("AC-001-07 workshop is chosen from the configured list and stored", async ({
  page,
  request,
}) => {
  const select = page.getByTestId("workshop");
  await expect(select).toBeVisible();
  const workshops = (await (
    await request.get(`${apiUrl}/api/workshops`)
  ).json()) as {
    id: string;
    title: string;
  }[];
  expect(workshops.length).toBeGreaterThan(0);
  for (const workshop of workshops) {
    await expect(
      select.locator("option", { hasText: workshop.title }),
    ).toHaveCount(1);
  }

  const email = uniqueEmail("workshop");
  await fillPerson(page, email);
  await select.selectOption({ label: workshops[0].title });
  await page.getByTestId("submit").click();

  const number = await confirmedRegistrationNumber(page);
  const stored = await readRegistration(request, number);
  expect(stored.workshop).toBe(workshops[0].id);
});

test("AC-001-08 invalid e-mail is reported and nothing is registered", async ({
  page,
  request,
}) => {
  const email = "not-an-address";
  await expect(page.getByTestId("first-name")).toBeVisible();
  await fillPerson(page, email);
  await page.getByTestId("submit").click();

  await expect(page.getByTestId("field-error-email")).toBeVisible();
  await expect(page.getByTestId("confirmation")).toHaveCount(0);
  const search = await request.get(
    `${mailpitUrl}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`,
  );
  expect(
    ((await search.json()) as { messages: unknown[] }).messages,
  ).toHaveLength(0);
});
