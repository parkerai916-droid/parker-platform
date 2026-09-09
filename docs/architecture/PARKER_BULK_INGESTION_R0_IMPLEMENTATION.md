# Parker / Hermes Bulk Ingestion R0

`tools/parker_bulk_ingest.py` is the subordinate deterministic operator. It
recursively scans one canonical source root, excludes symbolic links, orders
relative paths, hashes exact bytes for advisory retry checks, and stores only
mechanical facts in a SQLite WAL ledger. It calls the existing Agent Gateway
source submission and acquisition routes; Parker remains authoritative for
source identity, permissions, routing, and acquisition.

The runner requires an explicit `--case-id` and an Owner-preauthorised
`--assignment-endpoint`. It refuses to submit when that endpoint is absent and
refuses to report success when assignment fails. The endpoint must bind the
batch to one existing CaseId and delegate to Parker's existing assignment
coordinator; Hermes must not be allowed to create or freely select cases. No
such Gateway endpoint exists at the current baseline, so the Parker-side
binding is a remaining implementation gate before production use.

Supported source media are discovered from the production capability catalogue:
PDF, JPEG, PNG, WebP, CSV, and `message/rfc822`. Other files, including DOCX
while its separate programme is pending, are recorded as `UNSUPPORTED` with
their deterministic media type. The runner never converts, renames, executes,
or reads document semantics.

Example (after the Parker assignment endpoint is implemented):

```text
python3 tools/parker_bulk_ingest.py /path/to/extracted \
  --case-id case-opaque-id \
  --gateway http://127.0.0.1:8080 \
  --assignment-endpoint http://127.0.0.1:8080/agent/ingestion/assignment \
  --token "$PARKER_AGENT_GATEWAY_TOKEN" --acquire
```
