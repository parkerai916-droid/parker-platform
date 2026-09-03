# OI11R6R — First Real-Document Transcription Local Human Fidelity Review Package

## 1. Starting state

Host `parker`, branch `main`, HEAD and upstream were exactly `25f012a5da70be3040dd460e96f271c291f75448`; the worktree was clean. Production was container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, restart count zero, readiness PASS.

## 2. Exact source identity

`/home/steve/Deed of Representation Michael Kellec.pdf` was verified read-only: SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, size 1,887,733 bytes, MIME `application/pdf`, canonical custody page count 5, evidence ID `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`. The source was not altered.

## 3. Exact preparation identity

Canonical corrected preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, profile `full-page-achromatic-png-preparation-v1`, version 1, order `[1,2,3,4,5]`, remained unchanged. Direct copies of its five stored transport PNGs reproduced the accepted hashes `17dbc36d...`, `4de7ea45...`, `c481b51b...`, `b0c37fa3...`, and `3b4f9922...`. Owner acceptance SHA-256 remained `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`; corrected review manifest SHA-256 remained `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`.

## 4. Exact derivative identity

Canonical generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6` remained SHA-256 `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`; content remained SHA-256 `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. Type is `REGION_TRANSCRIPTION`, with 5/5/5 pages/regions/blocks and Parker order `[1,2,3,4,5]`. No derivative was created, changed, regenerated, or replaced.

## 5. Canonical readback result

One authenticated localhost-only production GET retrieved the existing generation with canonical status `RETRIEVED`. The exact JSON response was preserved without transformation. It binds the historical authorization, execution, provider state/raw response, OpenAI provider/profile/model, corrected preparation, capability/digest and Authorization Purpose. No provider was contacted.

## 6. Review package location

The local owner-review package is `/home/steve/parker-owner-review/oi11r6r/`. The directory is mode `0700`, owner/group `steve:steve`. Reference artifacts are mode `0400`; the blank owner-editable worksheet is mode `0600`. No file is world writable. Package files were not added to Git.

## 7. Source page inventory

| File | Bytes | SHA-256 |
|---|---:|---|
| `page-01-source.png` | 1,380,662 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| `page-02-source.png` | 2,236,986 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| `page-03-source.png` | 1,459,264 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| `page-04-source.png` | 2,084,632 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| `page-05-source.png` | 1,985,949 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

These are byte-exact copies of the canonical persisted transport images, not new renders.

## 8. Transcription export inventory

| File | Bytes | SHA-256 |
|---|---:|---|
| `page-01-transcription.txt` | 1,530 | `d064b5bd2d9d408f0a24c9fdbc6111f98181415da83b31a105fe450ed13d7cbd` |
| `page-02-transcription.txt` | 1,502 | `30d9a77eefb9a20bc7d25792844860819c6cb768b573c8d61665cebe4c66bd16` |
| `page-03-transcription.txt` | 1,874 | `017c168074182c250b48a9df4f0bf2bd9ba5c51f3a5cf54c364b51ea8ae21bc2` |
| `page-04-transcription.txt` | 1,940 | `6c5c44766963ef8a2781c2aec868e1d28cef2dd0371b7e2ab90cdf469ae4efc6` |
| `page-05-transcription.txt` | 890 | `bd565520b36193809495f330c4827f3aaa05e54ee57c229ef22c6b8a509249fd` |

Each file is exactly the corresponding canonical JSON transcription string encoded as UTF-8: no added newline, spelling correction, punctuation normalization, layout repair, inference, suppression, or explanatory text. Byte comparisons against all five source JSON strings passed.

## 9. Structured readback identity

`canonical-readback.json` is 13,055 bytes, mode `0400`, owner/group `steve:steve`, SHA-256 `8d31e0991c981587f18754aa2c35971ad79de370d0d502d92c05332df29f5571`. It contains the exact canonical response, including generation/region identities, Parker order, provenance and exact transcription content.

## 10. Mapping manifest identity

`review-manifest.json` is 5,591 bytes, mode `0400`, owner/group `steve:steve`, SHA-256 `12edd6a08217306e64e08d14048e496623efe7f48d5bb8f9c50ae8b8f35b5510`. Format `parker.ordinary-ingestion-human-fidelity-review-manifest.v1` deterministically maps each page's exact transport hash to its canonical page binding, region identity, Parker source identity, zero-based block index and transcription-file hash. It also binds evidence/source/preparation/generation/content/provider-state/raw-response identities, type, provider provenance and order `[1,2,3,4,5]`.

## 11. Human review worksheet

`HUMAN_FIDELITY_REVIEW.md` is 7,092 bytes, mode `0600`, owner/group `steve:steve`, SHA-256 `b530daa6824274619b0dd844d8ded7211293463b925824078d8c6014689d868f`. It is blank: no owner conclusion is pre-populated. Every page provides the required printed-text, names, addresses, dates, numbering, punctuation/layout, handwriting, signatures/marks, missing text, added text, uncertainty and overall-fidelity choices and note fields. The fidelity categories remain distinct.

## 12. Page 5 enhanced review

The page-5 section explicitly and separately calls for human review of handwritten witness name, witness address, occupation/descriptor, handwritten dates, signatures, derivative-marked uncertainty, and handwriting omitted rather than guessed. The package makes no inference about what handwriting says.

## 13. Package checksum inventory

`PACKAGE_SHA256SUMS.txt` covers every other package file, is 1,153 bytes, mode `0400`, owner/group `steve:steve`, and has SHA-256 `fcf19af617adc52474d567db67395c1d499602d56768bd4df20b49d3c749491a`. `sha256sum -c` passed for the complete package.

## 14. Readback purity

The readback path only retrieved and presented already-admitted canonical state. Evidence, preparation, authorization, execution, attempt, provider state, generation and content mutations were all zero. New derivatives, continuation invocations, provider calls, retries and egress were zero. Review-package writes occurred only outside governed production stores.

## 15. Governed-store accounting

Before/after counts and aggregate hashes were identical: evidence 29 (`e5d29f86...`), manifests 29 (`ec2a7dc1...`), corrected-preparation files 6 (`07018599...`), capability acceptances 14 (`14eeeb9c...`), authorizations 14 (`b9b30cce...`), attempts/executions 10 (`798ed0fd...`), provider state 8 (`be1b3310...`), derivative generations 23 (`801bdfd3...`), derivative content 21 (`80eee2bb...`), evidence audit 1 (`1d8b0f78...`), and document-ingestion audit 1 (`3ff333aa...`). Governed-store delta: zero.

## 16. Provider/egress accounting

OI11R6R activity: OpenAI 0; Claude 0; other providers 0; retries 0; external evidence egress 0. Historical real-document budget remains one authorized, one consumed, zero retries.

## 17. Production stability

Final production remains container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, restart count zero, readiness PASS. No deployment or restart occurred.

## 18. Verdict

**A — FIRST REAL-DOCUMENT TRANSCRIPTION HUMAN FIDELITY REVIEW PACKAGE PREPARED**
