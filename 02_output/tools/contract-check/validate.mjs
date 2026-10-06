// Validates every contract in docs/02_contracts with a parser; exits non-zero on the first failure.
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import SwaggerParser from "@apidevtools/swagger-parser";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";
import { parse } from "yaml";

const contracts = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "docs", "02_contracts");
const read = (name) => readFileSync(join(contracts, name), "utf8");

function requireKeys(name, doc, keys) {
  const missing = keys.filter((key) => !(key in doc));
  if (missing.length > 0) {
    throw new Error(`${name}: missing keys ${missing.join(", ")}`);
  }
}

const api = await SwaggerParser.validate(join(contracts, "registration-api.openapi.yaml"));
console.log(`registration-api.openapi.yaml: valid OpenAPI ${api.openapi}, ${Object.keys(api.paths).length} paths`);

const email = parse(read("confirmation-email.yaml"), { strict: true, uniqueKeys: true });
requireKeys("confirmation-email.yaml", email, ["interface", "headers", "body", "must_contain"]);
requireKeys("confirmation-email.yaml headers", email.headers, ["From", "To", "Subject", "Content-Type"]);
for (const key of email.must_contain) {
  if (!email.body.includes(`{${key}}`)) throw new Error(`confirmation-email.yaml: body lacks {${key}}`);
}
console.log("confirmation-email.yaml: valid YAML, required keys present");

const form = parse(read("registration-form.yaml"), { strict: true, uniqueKeys: true });
requireKeys("registration-form.yaml", form, ["interface", "page", "form", "request", "outcomes"]);
const apiFields = Object.keys(api.components.schemas.RegistrationRequest.properties);
for (const field of form.form.fields) {
  const apiName = field.name === "workshop" ? "workshops" : field.name;
  if (!apiFields.includes(apiName)) throw new Error(`registration-form.yaml: ${field.name} not in the API`);
}
console.log(`registration-form.yaml: valid YAML, ${form.form.fields.length} fields all in the API`);

const ajv = new Ajv2020({ strict: true });
addFormats(ajv);
const validateConfig = ajv.compile(JSON.parse(read("frontend-config.schema.json")));
const sample = { workshops: [{ id: "W1", name: "Requirements engineering for AI coding agents" }] };
if (!validateConfig(sample)) throw new Error(`frontend-config.schema.json: sample rejected ${ajv.errorsText(validateConfig.errors)}`);
console.log("frontend-config.schema.json: valid JSON Schema 2020-12, sample accepted");
