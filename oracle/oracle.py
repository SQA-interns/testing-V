#!/usr/bin/env python3
"""Black-box oracle for US-001 / REQ-REG-01: scenarios SC1-SC12.

Runs against the running local stack over HTTP. Standard library only (Python 3.10+).
Never show this file to the agent.

Phases (restart the backend with the listed settings before each phase):
  main       APP_TEST_CLOCK=enabled  APP_RATE_LIMIT_PER_HOUR=1000            -> SC1-SC9, SC11, SC12a
  deadline   APP_TEST_CLOCK=enabled  APP_RATE_LIMIT_PER_HOUR=1000  APP_EARLY_BIRD_DEADLINE=2026-08-15 -> SC10
  ratelimit  APP_TEST_CLOCK=enabled  APP_RATE_LIMIT_PER_HOUR=3                -> SC12b

Settings (environment variables):
  ORACLE_BASE_URL      backend URL                         default http://127.0.0.1:8080
  ORACLE_MAILPIT_URL   Mailpit URL                         default http://127.0.0.1:8025
  ORGANIZER_USERNAME / ORGANIZER_PASSWORD                  organizer credentials from .env
  ORACLE_LOG_CMD       command that prints the backend log, e.g.
                       "docker compose -f 02_output/docker-compose.yml logs backend --no-color"

Usage: python oracle.py <phase> [--out results.json]
"""
import base64, json, os, re, subprocess, sys, time, urllib.error, urllib.request, uuid
from decimal import Decimal, InvalidOperation

BASE = os.environ.get("ORACLE_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
MAIL = os.environ.get("ORACLE_MAILPIT_URL", "http://127.0.0.1:8025").rstrip("/")
USER = os.environ.get("ORGANIZER_USERNAME", "")
PASS = os.environ.get("ORGANIZER_PASSWORD", "")
LOG_CMD = os.environ.get("ORACLE_LOG_CMD", "")
AUTH = {"Authorization": "Basic " + base64.b64encode(f"{USER}:{PASS}".encode()).decode()}
EARLY = "2026-07-20T08:00:00Z"
RUN = uuid.uuid4().hex[:6]


def http(method, path, body=None, headers=None, base=BASE):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(base + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read()
            status = r.status
    except urllib.error.HTTPError as e:
        raw, status = e.read(), e.code
    try:
        js = json.loads(raw) if raw else None
    except ValueError:
        js = None
    return status, js


def dec(v):
    try:
        return Decimal(str(v).replace(",", ".")).quantize(Decimal("0.01"))
    except (InvalidOperation, ValueError, TypeError):
        return None


def person(tag, **extra):
    p = {"firstName": "Ana", "lastName": f"Novak{RUN}{tag}", "email": f"ana.{RUN}.{tag}@example.org",
         "payerType": "private", "companyName": None, "companyAddress": None, "companyVatId": None,
         "workshops": []}
    p.update(extra)
    return p


def company_si(tag):
    return person(tag, payerType="company", companyName="Primer d.o.o.",
                  companyAddress=f"Koroska cesta {RUN} 1, 2000 Maribor", companyVatId=f"SI{RUN}01")


def company_at(tag):
    return person(tag, payerType="company", companyName="Beispiel GmbH",
                  companyAddress="Ringstrasse 1, 1010 Wien", companyVatId="ATU00000001")


def register(payload, now=EARLY, auth=True):
    h = {"X-Test-Now": now}
    if auth:
        h.update(AUTH)
    return http("POST", "/api/registrations", payload, h)


def stored(number):
    return http("GET", f"/api/registrations/{number}", headers=AUTH)


def ok(status):
    return 200 <= status < 300


def mails_to(address, wait=20):
    """Messages addressed to `address` in Mailpit (waits for asynchronous sending)."""
    deadline = time.time() + wait
    found = []
    while time.time() < deadline:
        st, js = http("GET", "/api/v1/messages?limit=500", base=MAIL)
        msgs = (js or {}).get("messages", []) if st == 200 else []
        found = [m for m in msgs if any(t.get("Address", "").lower() == address.lower() for t in m.get("To") or [])]
        if found:
            time.sleep(2)  # allow duplicates to arrive
            st, js = http("GET", "/api/v1/messages?limit=500", base=MAIL)
            msgs = (js or {}).get("messages", [])
            return [m for m in msgs if any(t.get("Address", "").lower() == address.lower() for t in m.get("To") or [])]
        time.sleep(1)
    return found


def mail_text(msg):
    st, js = http("GET", f"/api/v1/message/{msg['ID']}", base=MAIL)
    if st != 200 or not js:
        return ""
    return (js.get("Subject") or "") + "\n" + (js.get("Text") or "") + "\n" + re.sub("<[^>]+>", " ", js.get("HTML") or "")


def amounts(text):
    return {dec(m) for m in re.findall(r"\d+[.,]\d{2}", text)} - {None}


# ------------------------------------------------------------------ scenarios
R = {}


def record(sc, passed, detail):
    R[sc] = {"result": passed if isinstance(passed, str) else ("pass" if passed else "fail"), "detail": detail}
    print(f"{sc:6s} {R[sc]['result']:7s} {detail}")


def clock_honoured():
    a = register(person("clk1"), now="2026-01-10T10:00:00Z")
    b = register(person("clk2"), now="2026-12-10T10:00:00Z")
    if not (ok(a[0]) and ok(b[0])):
        return None, f"registration failed ({a[0]}, {b[0]})"
    return dec(a[1].get("netFee")) != dec(b[1].get("netFee")), f"fees {a[1].get('netFee')} / {b[1].get('netFee')}"


def phase_main():
    honoured, info = clock_honoured()
    if honoured is None:
        print(f"PRECHECK registration through the fixed API failed: {info}")
    elif not honoured:
        print(f"PRECHECK test clock not honoured ({info}); time-dependent scenarios are marked 'harness'")
    timed = bool(honoured)
    # Without a working test clock the real date (after the deadline) applies: expect regular amounts.
    NET, VAT, GROSS = (("240.00", "52.80", "292.80") if timed else ("300.00", "66.00", "366.00"))

    # SC1 / SC2
    for sc, now, exp in (("SC1", "2026-07-31T21:30:00Z", "240.00"), ("SC2", "2026-07-31T22:10:00Z", "300.00")):
        if not timed:
            record(sc, "harness", "test clock not honoured")
            continue
        st, js = register(person(sc.lower()), now=now)
        record(sc, ok(st) and dec((js or {}).get("netFee")) == Decimal(exp), f"status {st}, netFee {(js or {}).get('netFee')} (expected {exp})")

    # SC3 confirmation states the fee breakdown
    p = person("sc3")
    st, js = register(p)
    js = js or {}
    body_ok = ok(st) and (dec(js.get("vat")), dec(js.get("grossFee"))) == (Decimal(VAT), Decimal(GROSS))
    msgs = mails_to(p["email"])
    text = mail_text(msgs[0]) if msgs else ""
    need = {Decimal(NET), Decimal(VAT), Decimal(GROSS)}
    record("SC3", body_ok and len(msgs) == 1 and need <= amounts(text),
           f"status {st}, vat {js.get('vat')}, gross {js.get('grossFee')}, e-mails {len(msgs)}, amounts in e-mail {sorted(str(a) for a in amounts(text) & need)}")

    # SC4 Austrian company: Slovenian VAT
    st, js = register(company_at("sc4"))
    js = js or {}
    record("SC4", ok(st) and (dec(js.get("vat")), dec(js.get("grossFee"))) == (Decimal(VAT), Decimal(GROSS)),
           f"status {st}, vat {js.get('vat')}, gross {js.get('grossFee')} (expected {VAT} / {GROSS})")

    # SC5 payer data stored, no invoice
    p = company_si("sc5")
    st, js = register(p)
    js = js or {}
    num = js.get("registrationNumber")
    gst, g = stored(num) if num else (0, None)
    g = g or {}
    data_ok = all(g.get(k) == p[k] for k in ("companyName", "companyAddress", "companyVatId"))
    invoice_keys = [k for k, v in {**js, **g}.items() if re.search("invoice|payment|paid", k, re.I) and v not in (None, False, "", [])]
    n_mail = len(mails_to(p["email"], wait=10))
    record("SC5", ok(st) and gst == 200 and data_ok and not invoice_keys and n_mail <= 1,
           f"status {st}, GET {gst}, payer data stored {data_ok}, invoice/payment fields {invoice_keys}, e-mails {n_mail}")

    # SC6 private person without company data
    st, _ = register(person("sc6"))
    record("SC6", ok(st), f"status {st}")

    # SC7 repeated registration kept with its own fee
    if not timed:
        record("SC7", "harness", "test clock not honoured")
    else:
        p = person("sc7")
        a = register(p, now="2026-07-20T08:00:00Z")
        b = register(p, now="2026-08-05T08:00:00Z")
        detail = f"statuses {a[0]}, {b[0]}"
        good = ok(a[0]) and ok(b[0])
        if good:
            na, nb = (a[1] or {}).get("registrationNumber"), (b[1] or {}).get("registrationNumber")
            sa, sb = stored(na)[1] or {}, stored(nb)[1] or {}
            good = na != nb and dec(sa.get("netFee")) == Decimal("240.00") and dec(sb.get("netFee")) == Decimal("300.00")
            detail += f", numbers differ {na != nb}, stored fees {sa.get('netFee')} / {sb.get('netFee')}"
        record("SC7", good, detail)

    # SC8 workshops
    a = register(person("sc8a", workshops=["W1"]))
    b = register(person("sc8b", workshops=[]))
    c = register(person("sc8c", workshops=["W1", "W2"]))
    cap = [register(person(f"sc8d{i}", workshops=["W2"]))[0] for i in range(60)]
    parts = {
        "one": ok(a[0]) and (a[1] or {}).get("workshop") == "W1",
        "none": ok(b[0]) and (b[1] or {}).get("workshop") in (None, ""),
        "two rejected": 400 <= c[0] < 500,
        "no capacity": all(ok(s) for s in cap),
    }
    record("SC8", all(parts.values()), ", ".join(f"{k} {v}" for k, v in parts.items()) + f" (statuses {a[0]}, {b[0]}, {c[0]})")

    # SC9 no personal data in logs
    p = company_si("sc9")
    st, _ = register(p)
    if not LOG_CMD:
        record("SC9", "n.a.", "ORACLE_LOG_CMD not set; check the backend log manually")
    else:
        time.sleep(2)
        log = subprocess.run(LOG_CMD, shell=True, capture_output=True, text=True).stdout
        leaked = [v for v in (p["lastName"], p["email"], p["companyVatId"], f"Koroska cesta {RUN}") if v in log]
        record("SC9", ok(st) and not leaked, f"status {st}, leaked values {leaked}")

    # SC11 no student discount
    st, js = register(person("sc11", student=True, studentId="12345"))
    js = js or {}
    record("SC11", (400 <= st < 500) or (ok(st) and dec(js.get("netFee")) == Decimal(NET)),
           f"status {st}, netFee {js.get('netFee')}")

    # SC12a public endpoint
    st, _ = register(person("sc12a"), auth=False)
    record("SC12a", ok(st), f"status {st} without authentication")


def phase_deadline():
    honoured, info = clock_honoured()
    if not honoured:
        record("SC10", "harness", f"test clock not honoured ({info})")
        return
    st, js = register(person("sc10"), now="2026-08-10T08:00:00Z")
    record("SC10", ok(st) and dec((js or {}).get("netFee")) == Decimal("240.00"),
           f"status {st}, netFee {(js or {}).get('netFee')} (expected 240.00 with deadline 2026-08-15)")


def phase_ratelimit():
    codes = [register(person(f"sc12b{i}"), auth=False)[0] for i in range(4)]
    record("SC12b", codes[-1] == 429, f"statuses {codes} (expected the 4th to be 429)")


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in ("main", "deadline", "ratelimit"):
        print(__doc__)
        sys.exit(2)
    {"main": phase_main, "deadline": phase_deadline, "ratelimit": phase_ratelimit}[sys.argv[1]]()
    out = sys.argv[sys.argv.index("--out") + 1] if "--out" in sys.argv else f"oracle-{sys.argv[1]}.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"phase": sys.argv[1], "base_url": BASE, "results": R}, f, indent=2)
    print(f"written {out}")
