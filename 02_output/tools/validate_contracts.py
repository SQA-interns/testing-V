"""Parse and validate every contract in docs/02_contracts/ (docs/02_specification.md, section 9).

Usage, from the repository root:  python 02_output/tools/validate_contracts.py
Needs PyYAML, jsonschema and Docker (for the SQL contract). Exit code 0 only if every contract is valid.
"""

import json
import subprocess
import sys
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator
from jsonschema.validators import validator_for

OUT = Path(__file__).resolve().parents[1]
CONTRACTS = OUT / "docs" / "02_contracts"
SCHEMAS = OUT / "tools" / "schemas"
POSTGRES_IMAGE = "postgres:16.15-alpine"


def load_yaml(path):
    with path.open(encoding="utf-8") as f:
        return yaml.safe_load(f)


def resolve_refs(doc, node, where="#"):
    """Fail on any local $ref that does not resolve."""
    errors = []
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str) and ref.startswith("#/"):
            target = doc
            for part in ref[2:].split("/"):
                if not isinstance(target, dict) or part not in target:
                    errors.append(f"{where}: unresolved $ref {ref}")
                    break
                target = target[part]
        for key, value in node.items():
            errors += resolve_refs(doc, value, f"{where}/{key}")
    elif isinstance(node, list):
        for i, value in enumerate(node):
            errors += resolve_refs(doc, value, f"{where}/{i}")
    return errors


def check_openapi(path):
    doc = load_yaml(path)
    meta = json.loads((SCHEMAS / "openapi-3.1.json").read_text(encoding="utf-8"))
    errors = [f"OpenAPI schema: {e.message} at {list(e.path)}" for e in validator_for(meta)(meta).iter_errors(doc)]
    errors += resolve_refs(doc, doc)
    # every component schema must itself be a valid JSON Schema 2020-12 document
    for name, schema in doc.get("components", {}).get("schemas", {}).items():
        for e in Draft202012Validator(Draft202012Validator.META_SCHEMA).iter_errors(schema):
            errors.append(f"components/schemas/{name}: {e.message}")
    # request examples must match the request schema
    defs = {"components": doc["components"]}
    request = doc["paths"]["/api/registrations"]["post"]["requestBody"]["content"]["application/json"]
    schema = dict(request["schema"], **defs)
    for name, example in request.get("examples", {}).items():
        for e in Draft202012Validator(schema).iter_errors(example["value"]):
            errors.append(f"request example {name}: {e.message}")
    return errors


def check_yaml_contract(path, schema_name):
    doc = load_yaml(path)
    schema = json.loads((SCHEMAS / schema_name).read_text(encoding="utf-8"))
    return [f"{e.message} at {list(e.path)}" for e in Draft202012Validator(schema).iter_errors(doc)]


def check_sql(path):
    sql = path.read_text(encoding="utf-8")
    cmd = [
        "docker", "run", "--rm", "-i", "-e", "POSTGRES_HOST_AUTH_METHOD=trust",
        "-e", "POSTGRES_INITDB_ARGS=--encoding=UTF8", "--entrypoint", "sh", POSTGRES_IMAGE, "-c",
        "docker-entrypoint.sh postgres >/tmp/pg.log 2>&1 & "
        "until pg_isready -q -U postgres; do sleep 0.5; done; sleep 1; "
        "psql -v ON_ERROR_STOP=1 -q -U postgres -f - && psql -U postgres -At -c "
        "\"select table_name||'.'||column_name||':'||data_type from information_schema.columns "
        "where table_schema='public' order by table_name, ordinal_position\"",
    ]
    result = subprocess.run(cmd, input=sql, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        return [f"psql: {result.stderr.strip()}"]
    print("    columns: " + ", ".join(result.stdout.splitlines()))
    return []


CHECKS = [
    ("registration-api.openapi.yaml", check_openapi),
    ("registration-storage.sql", check_sql),
    ("confirmation-email.yaml", lambda p: check_yaml_contract(p, "confirmation-email.schema.json")),
    ("registration-form.yaml", lambda p: check_yaml_contract(p, "registration-form.schema.json")),
]


def main(only=None):
    failed = 0
    for name, check in CHECKS:
        path = CONTRACTS / name
        if only and name not in only:
            continue
        if not path.exists():
            print(f"MISSING {name}")
            failed += 1
            continue
        errors = check(path)
        print(f"{'OK     ' if not errors else 'INVALID'} {name}")
        for e in errors:
            print(f"    {e}")
        failed += bool(errors)
    print(f"{failed} contract(s) failed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
