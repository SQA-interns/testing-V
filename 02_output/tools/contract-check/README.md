# Contract check

> Written in: phase 2 · Source: `docs/02_specification.md` section 8 · Agent: writes

Validates the contracts in `../../docs/02_contracts/` with parsers (dev-only tool, D-25).

```sh
npm ci
npm run validate
```

The SQL contract is checked by applying it to the pinned PostgreSQL image:

```sh
docker run -d --name contract-pg -e POSTGRES_PASSWORD=contract-check postgres:16.15-alpine
docker cp ../../docs/02_contracts/registration-storage.sql contract-pg:/tmp/schema.sql
docker exec contract-pg sh -c 'until pg_isready -U postgres; do sleep 1; done; psql -U postgres -v ON_ERROR_STOP=1 -f /tmp/schema.sql'
docker rm -f contract-pg
```

The password above is a throwaway value for a container that exists only during the check.
