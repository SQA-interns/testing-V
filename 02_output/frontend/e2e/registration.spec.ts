import { expect, test, type Page } from "@playwright/test";
import { confirmationText, uniqueEmail } from "./mailpit";

// Element names come from docs/02_contracts/registration-form.yaml.

async function fillParticipant(page: Page, email: string): Promise<void> {
  await page.getByLabel("First name").fill("Ana");
  await page.getByLabel("Last name").fill("Kovač");
  await page.getByLabel("E-mail").fill(email);
}

async function registrationNumberAndFee(
  page: Page,
): Promise<{ registrationNumber: string; fee: string }> {
  const status = page.getByRole("status");
  await expect(status).toContainText("Registration completed");
  const text = (await status.textContent()) ?? "";
  const registrationNumber = /registration number is (\S+?)\./.exec(text)?.[1];
  const fee = /Fee: (\d+\.\d{2}) EUR/.exec(text)?.[1];
  expect(registrationNumber, text).toBeTruthy();
  expect(fee, text).toBeTruthy();
  return { registrationNumber: registrationNumber!, fee: fee! };
}

test("AC-001-06 AC-001-03 private registration through the form is confirmed by e-mail", async ({
  page,
  request,
}) => {
  const email = uniqueEmail();
  await page.goto("/");
  await fillParticipant(page, email);
  await page.getByLabel("Private person").check();
  await page.getByRole("button", { name: "Register" }).click();

  const { registrationNumber, fee } = await registrationNumberAndFee(page);
  const text = await confirmationText(request, email);
  expect(text).toContain(registrationNumber);
  expect(text).toContain(fee);
});

test("AC-001-11 company payer must give company data and is registered with it", async ({
  page,
  request,
}) => {
  const email = uniqueEmail();
  await page.goto("/");
  await fillParticipant(page, email);
  await page.getByLabel("Company", { exact: true }).check();

  for (const label of ["Company name", "Company address", "VAT ID"]) {
    await expect(page.getByLabel(label)).toBeVisible();
    await expect(page.getByLabel(label)).toHaveAttribute("required", "");
  }
  await page.getByLabel("Company name").fill("Primer d.o.o.");
  await page
    .getByLabel("Company address")
    .fill("Čopova ulica 1, 1000 Ljubljana");
  await page.getByLabel("VAT ID").fill("SI12345678");
  await page.getByRole("button", { name: "Register" }).click();

  const { registrationNumber } = await registrationNumberAndFee(page);
  const text = await confirmationText(request, email);
  expect(text).toContain(registrationNumber);
  expect(text).toContain("Primer d.o.o.");
});

test("AC-001-12 company fields are hidden for a private payer", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByLabel("Private person").check();

  for (const label of ["Company name", "Company address", "VAT ID"]) {
    await expect(page.getByLabel(label)).toHaveCount(0);
  }
});

test("AC-001-14 workshop chosen in the form is registered", async ({
  page,
  request,
}) => {
  const email = uniqueEmail();
  await page.goto("/");
  await fillParticipant(page, email);
  await page.getByLabel("Private person").check();
  const workshop = page.getByLabel("Workshop");
  const options = workshop.locator("option");
  await expect(options.nth(1)).toBeAttached();
  const workshopId = (await options.nth(1).getAttribute("value")) ?? "";
  expect(workshopId).not.toBe("");
  await workshop.selectOption(workshopId);
  await page.getByRole("button", { name: "Register" }).click();

  const { registrationNumber } = await registrationNumberAndFee(page);
  const text = await confirmationText(request, email);
  expect(text).toContain(registrationNumber);
  expect(text).toContain(workshopId);
});
