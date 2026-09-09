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

The current temporary ingestion invocation is:

```text
./parker-ingest-folder incoming --batch <Parker-minted-batch> --case-id <case>
```

`--batch` is mandatory and must be a Parker-minted authorised batch ID. The
`--case-id` value is local ledger/report metadata only; Hermes never transmits
it to the assignment endpoint and never uses it to select or reassign Parker's
target case. Acquisition is not enabled by this wrapper.

The next stage will eliminate manual batch and case IDs by adding an
Owner-facing case selection and confirmation workflow.
