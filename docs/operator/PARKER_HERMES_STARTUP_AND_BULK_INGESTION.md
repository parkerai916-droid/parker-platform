# Parker and Hermes operator startup

Normal startup is:

```text
Parker: ./start-parker.sh
Hermes: ./start-hermes.sh
```

`start-parker.sh` must run on the host named `parker`. It verifies a clean
working tree, starts only the Parker Compose service, checks the Owner UI and
Agent Gateway ports, and verifies the running production commit. It performs
no governed action and never prints the Owner high-authority secret.

`start-hermes.sh` must run on the host named `hermes`. It loads the local
Gateway token without printing it, performs only an authenticated harmless
404 readiness request, and enters the prepared Hermes operator shell.

The current Hermes ingestion invocation is:

```text
./parker-ingest-folder incoming --batch <Parker-minted-batch>
```

`--batch` is mandatory and must be a Parker-minted authorised batch ID. The
case is never supplied to Hermes or transmitted to the assignment endpoint;
Parker resolves it only from the durable batch binding. Acquisition is not
enabled by this wrapper.

For the browser workflow, run `python3 tools/hermes_bulk_ingest_ui.py` on the
Hermes host after `start-hermes.sh`, then open its printed local URL. The
default is loopback (`127.0.0.1:8765`). To use the Windows browser over the
LAN, explicitly run `python3 tools/hermes_bulk_ingest_ui.py --bind 192.168.178.45`.
It keeps the Gateway bearer token server-side, discovers Parker-authorised READY
batches, and submits files through the Agent Gateway. Wildcard binding is not
permitted.

LAN mode must be paired with a Hermes VM firewall rule restricting TCP port
8765 to the Owner's Windows machine (or another explicitly trusted management
source). Do not invent or assume the Windows machine's IP address until it is
known from the network configuration.

## Parker Owner batch handoff

1. Open the Parker Owner UI and open **Bulk Ingestion**.
2. Select an existing case and explicitly confirm it.
3. Parker mints one opaque authorised batch and displays **READY**.

Parker's Owner tab does not upload files. Hermes discovers the READY batch via
`GET /agent/ingestion-batches` using Hermes bearer authentication. The response
contains only the opaque batch ID, human-readable case name, and READY status.

## Normal Hermes browser ingestion

1. In the Hermes UI, select a Parker-authorised READY batch.
2. Choose or drag a folder.
3. Verify the folder name and discovered-file count.
4. Click **Start Ingestion**.
5. Review progress and the final registered/already-registered/assigned,
   unsupported, and failed counts.

Folder selection happens in the Hermes browser surface; Parker never accesses
the Windows folder path. Hermes sends each supported file to
`POST /agent/evidence` with `X-Parker-Ingestion-Batch-Id`, then requests
`POST /agent/evidence/{evidenceArtifactId}/assign` with that same batch header.
It never sends a CaseId, and no provider processing starts automatically.

## Canonical prepared-handoff workflow

For prepared Windows evidence, the canonical operational path is:

```text
local evidence
  → Windows prep launcher
  → Parker staging
  → governed prep job
  → prepared handoff
  → 8088 owner import action
  → Hermes
  → Parker governed ingestion
```

Open the combined Parker + Hermes console on port 8088 and use **Prepared
jobs**. The console discovers only validated handoffs below
`PARKER_INGESTION_PREP_ROOT` (default:
`/mnt/parker-data/ingestion-prep/jobs`). Review the exact job, Parker case,
READY, REVIEW_REQUIRED, FAILED, reconciliation, and unaccounted counts, then
explicitly confirm **Import prepared job**. The prepared handoff's verified
`readyContent` population is the import population; the console does not ask
the browser to re-upload the Windows files. Originals remain untouched.

The browser's existing Parker Owner Cookie is forwarded only for this
owner-authorised request; no manual cookie extraction is required. The console
reads the Agent Gateway credential server-side from the protected file named by
`PARKER_CONSOLE_GATEWAY_TOKEN_FILE` (default:
`/mnt/parker-secrets/parker/parker-agent-gateway-token`, mode 0600). No manual
token copying is required, and the token is never returned to the browser.

The importer remains the existing `parker_ingestion_handoff.py` implementation:
it validates the complete, reconciled handoff, uses a Parker-authorised batch,
and sends READY content through Hermes. REVIEW_REQUIRED and FAILED material is
withheld. Hermes processes content but does not become evidence authority;
Parker remains authoritative for cases, batches, evidence, associations, and
occurrences. The durable ledger and `reports/handoff-import.json` retain
provenance, make retries safe, and mark an imported job as used.
