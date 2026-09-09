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
