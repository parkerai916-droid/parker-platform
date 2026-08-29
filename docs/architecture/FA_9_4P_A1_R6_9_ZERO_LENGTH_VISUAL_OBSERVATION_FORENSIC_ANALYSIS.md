# FA.9.4P-A1 R6.9 zero-length visual-observation forensic analysis

**Unit:** FA.9.4P-A1E-R6.9A  
**Classification:** SCHEMA_EXPRESSIVENESS_GAP (primary); DOMAIN_MODEL_MISMATCH and under-specified provider observation anchoring (secondary).  
**Execution:** offline replay of the already-consumed R6.9 response only. No provider request, retry, authority, lifecycle, deployment, restart, or production-store mutation occurred.

## 1. Baseline and consumed evidence

The authoritative repository began at clean HEAD == origin/main == ac3f596c0353770ee020da83009e593bea021449. Production remained container 192237eea021e46057c6cb84af86379dcf9741f481d7e262c0e87a8e350986ad, image sha256:2e17e156cdceda160e9a354c2458b9dbec09b73156ff33d3522d4e91bc4ff923, restart count zero, lifecycle ACCEPTANCE_PENDING.

Consumed identity: authority authority-fa-9.4p-a1e-r6.8c1, execution execution-fa-9.4p-a1e-r6.8c1, correlation correlation-fa-9-4p-a1e-r6-8c1, request digest 1a691388478370add9bae4e920fb1071369efa543057403727b422e9000a3d36. The attempt ledger already ended at PROVIDER_RESPONSE_RECEIVED; no retry is available.

Provider state was copied byte-for-byte to mode-0600 files in an isolated /tmp workspace. Copy hashes matched production:

- provider-state file: edd6b3fcf357edfcfce0e6825e2b8734462fb2654f6816255d8e6305b080b4c9;
- assessment file: 62a5b9a266db3cf4702eb5583f61f8a2f23d96e4954371b2750ddf07ebb50ae8;
- attempt ledger: e6e3a6eba4a2033e58c23a06c39aed95fc1a7c880cd3e568ee0ae01d7806741d.

The verified provider record is 31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd; raw response SHA-256 500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f; canonical structured-state SHA-256 7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476; complete record SHA-256 ad2542015546250bfe0640e5c31636bb6401a20d537d95db04248c81883ad135; assessment SHA-256 39fbc01c7cf831ebf5fc0751cfcca73310bc1a1a1846508ff46d64c61bd09da7.

## 2. Where the values arose

The response envelope contains one output_text segment. Parsing that exact segment gives a map equal, recursively and value-for-value, to assessment.exact_structured_state. Every offending observation has the same kind/start/end in both forms.

| Stage | Zero-length observations attributable to stage |
|---|---:|
| A  literal provider structured output | 70 |
| B  adapter/parser introduction or transformation | 0 |
| C  persistence/codec introduction or transformation | 0 |
| D  validator/reconstruction introduction | 0 |

OpenAiRegionTranscriptionAdapter.outputText selects the segment, RegionJson.parse maps it without an observation transformation, and structuredForPersistence independently performs the same parse. FileSystemRegionProviderStateStore.recordAssessment canonicalizes that map but does not reinterpret fields. RegionTranscriptionValidator.parseObservation is where the unchanged [n,n) pair is rejected.

## 3. Exact object and accepted contract

The affected object is RegionVisualObservation, not RegionTranscriptionUncertainty, a warning, or an evidential text span. It contains a RegionVisualObservationKind plus nullable startCodePoint and endCodePointExclusive. Its data class has no constructor inequality invariant.

R6.3 calls these optional bounded visual observations and limits their kinds, but only uncertainty spans are explicitly defined as exact non-empty substring references with an exactSubstring. Visual observations have no associated substring field. The current validator permits either both offsets null (an unanchored/region-level assertion), or both non-null with 0 <= start < end <= text code-point length (a non-empty text range).

It does not model a zero-width point anchor. Thus [n,n) is prohibited by the current validator for this object type, while null/null is explicitly representable. The accepted prose does not establish that every LINE_BREAK must cover a non-empty substring, and a line boundary is naturally point-like. The captured points are not reliable newline indices, however: many occur inside non-newline text, so the existing evidence cannot be repaired by assumption.

## 4. Frozen schema constraint

The frozen wire-v4 schema digest was re-established as 672a626bd8a6183ff636a4617d017706897fe658e0036f3693c51d5c0d8bfad1.

For visual observations it independently constrains start_code_point to integer or null with minimum 0, and end_code_point_exclusive to integer or null with minimum 1. Both fields are required and additional properties are forbidden. It contains no cross-field end > start constraint. Standard JSON Schema used here has no data-relative comparison in this schema. Consequently [72,72) is schema-valid even though Parker's post-parse validator rejects it. strict:true therefore did not structurally prevent this result.

## 5. Exact accounting

There are 70 offending observations across 14 regions:

- page 1 = 40, page 2 = 15, page 3 = 15;
- LINE_BREAK = 70;
- empty transcription text = 0; non-empty transcription text = 70;
- at index 0 = 0; at transcription end = 0; elsewhere = 70.
*Text at range is empty for every row because [n,n) selects no code point. Boundary context shows up to twelve code points on either side separated by |; it is diagnostic only and is not a correction.

| # | Page | Region ID | Status | CP length | Kind | Start | End | Raw same | Text at range | Boundary context | Semantics | Ordinal |
|---:|---:|---|---|---:|---|---:|---:|---|---|---|---|---:|
| 1 | 1 | bb16752357a487e70765b1cfd650b5f26db9a8958fc78b36238dac76c2f59b97 | TRANSCRIBED | 183 | LINE_BREAK | 72 | 72 | True |  | oning Protoc\|ol\nprogramme | point_between_code_points | 1 |
| 2 | 1 | bb16752357a487e70765b1cfd650b5f26db9a8958fc78b36238dac76c2f59b97 | TRANSCRIBED | 183 | LINE_BREAK | 160 | 160 | True |  | ly only to i\|ts\nown Plann | point_between_code_points | 1 |
| 3 | 1 | 89e890b37bec0a16404d0f065c9d5e4e3b57538a702712ba73dd2776d431b124 | TRANSCRIBED | 244 | LINE_BREAK | 88 | 88 | True |  | into one gia\|nt\nauthorisa | point_between_code_points | 2 |
| 4 | 1 | 89e890b37bec0a16404d0f065c9d5e4e3b57538a702712ba73dd2776d431b124 | TRANSCRIBED | 244 | LINE_BREAK | 175 | 175 | True |  | es the decis\|ion to\nreope | point_between_code_points | 2 |
| 5 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 28 | 28 | True |  | ol programme\|\n        \|\n  | point_between_code_points | 5 |
| 6 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 38 | 38 | True |  | me\n        \|\|\n        \| c | point_between_code_points | 5 |
| 7 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 65 | 65 | True |  | ently PAUSED\|\n        ▼\n1 | point_between_code_points | 5 |
| 8 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 75 | 75 | True |  | ED\n        ▼\|\n1. REOPENIN | point_between_code_points | 5 |
| 9 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 97 | 97 | True |  | ING DECISION\|\n        \|\n  | point_between_code_points | 5 |
| 10 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 107 | 107 | True |  | ON\n        \|\|\n        ├─  | point_between_code_points | 5 |
| 11 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 145 | 145 | True |  | US = ACTIVE\n\|        \|\n   | point_between_code_points | 5 |
| 12 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 155 | 155 | True |  | E\n        \|\n\|        ├─ R | point_between_code_points | 5 |
| 13 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 196 | 196 | True |  | = SATISFIED\n\|        \|\n   | point_between_code_points | 5 |
| 14 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 206 | 206 | True |  | D\n        \|\n\|        ├─ A | point_between_code_points | 5 |
| 15 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 242 | 242 | True |  | TH = FAMILY \|F\n        \|\n | point_between_code_points | 5 |
| 16 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 252 | 252 | True |  | Y F\n        \|\|\n        └─ | point_between_code_points | 5 |
| 17 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 288 | 288 | True |  | thorised to:\|\n            | point_between_code_points | 5 |
| 18 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 321 | 321 | True |  |  REVIEW ONLY\|\n        \|\n  | point_between_code_points | 5 |
| 19 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 331 | 331 | True |  | LY\n        \|\|\n        \| N | point_between_code_points | 5 |
| 20 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 353 | 353 | True |  |  NO model ru\|n\n        \|  | point_between_code_points | 5 |
| 21 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 381 | 381 | True |  | mplementatio\|n\n        ▼\n | point_between_code_points | 5 |
| 22 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 391 | 391 | True |  | ion\n        \|▼\n2. FAMILY  | point_between_code_points | 5 |
| 23 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 419 | 419 | True |  | ANNING REVIE\|W\n        \|\n | point_between_code_points | 5 |
| 24 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 429 | 429 | True |  | IEW\n        \|\|\n        \|  | point_between_code_points | 5 |
| 25 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 449 | 449 | True |  |   \| Question\|:\n        \|  | point_between_code_points | 5 |
| 26 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 498 | 498 | True |  | the existing\|\n        \| P | point_between_code_points | 5 |
| 27 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 549 | 549 | True |  | ed diagnosti\|c\n        \|  | point_between_code_points | 5 |
| 28 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 596 | 596 | True |  | known failur\|es?\n         | point_between_code_points | 5 |
| 29 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 606 | 606 | True |  | ures?\n      \|  ▼\n3. FAMIL | point_between_code_points | 5 |
| 30 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 639 | 639 | True |  | OSTIC GOVERN\|ANCE\n        | point_between_code_points | 5 |
| 31 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 649 | 649 | True |  | RNANCE\n     \|   \|\n        | point_between_code_points | 5 |
| 32 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 689 | 689 | True |  | ssary Scope \|Lock\n        | point_between_code_points | 5 |
| 33 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 727 | 727 | True |  | /evidence pl\|an\n        ├ | point_between_code_points | 5 |
| 34 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 761 | 761 | True |  | s verificati\|on\n        └ | point_between_code_points | 5 |
| 35 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 800 | 800 | True |  | ution approv\|al\n        ▼ | point_between_code_points | 5 |
| 36 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 810 | 810 | True |  | oval\n       \| ▼\n4. SERVER | point_between_code_points | 5 |
| 37 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 831 | 831 | True |  | VER DIAGNOST\|IC\n        \| | point_between_code_points | 5 |
| 38 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 841 | 841 | True |  | STIC\n       \| \|\n        └ | point_between_code_points | 5 |
| 39 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 887 | 887 | True |  | ough Ollama\n\|        ▼\n5. | point_between_code_points | 5 |
| 40 | 1 | e4c3079b9cf2d7b5504f45c9150156db689920e45ed74d8c049347ee25657278 | TRANSCRIBED | 915 | LINE_BREAK | 897 | 897 | True |  | a\n        ▼\n\|5. EVIDENCE  | point_between_code_points | 5 |
| 41 | 2 | abfcdae2bc50821705e749471b3ab4e7ab7cb4599cc14ec76f377414e759f0d1 | TRANSCRIBED | 262 | LINE_BREAK | 75 | 75 | True |  | y says an AC\|TIVE\ndecisio | point_between_code_points | 6 |
| 42 | 2 | abfcdae2bc50821705e749471b3ab4e7ab7cb4599cc14ec76f377414e759f0d1 | TRANSCRIBED | 262 | LINE_BREAK | 147 | 147 | True |  | oceed to its\| separate\nPl | point_between_code_points | 6 |
| 43 | 2 | abfcdae2bc50821705e749471b3ab4e7ab7cb4599cc14ec76f377414e759f0d1 | TRANSCRIBED | 262 | LINE_BREAK | 229 | 229 | True |  | orise a mode\|l call, or c | point_between_code_points | 6 |
| 44 | 2 | 156de26c64bd1b6a312183fe3f71fff7184d1c1e52c61bce2230d8bf1d6dc22b | TRANSCRIBED | 236 | LINE_BREAK | 84 | 84 | True |  | rker from Ll\|ama\nto Qwen. | point_between_code_points | 7 |
| 45 | 2 | 156de26c64bd1b6a312183fe3f71fff7184d1c1e52c61bce2230d8bf1d6dc22b | TRANSCRIBED | 236 | LINE_BREAK | 161 | 161 | True |  | ing Qwen. An\|d we are\ncer | point_between_code_points | 7 |
| 46 | 2 | 5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668 | TRANSCRIBED | 212 | LINE_BREAK | 75 | 75 | True |  |  environment\|,\nwarrants g | point_between_code_points | 9 |
| 47 | 2 | 5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668 | TRANSCRIBED | 212 | LINE_BREAK | 143 | 143 | True |  | alternative-\|model\ncandid | point_between_code_points | 9 |
| 48 | 2 | cf81d18a202dd3bee964c95501225016b311021585c8d27f0bf062a30c7faf15 | TRANSCRIBED | 276 | LINE_BREAK | 85 | 85 | True |  | separate Fam\|ily\nF eviden | point_between_code_points | 10 |
| 49 | 2 | cf81d18a202dd3bee964c95501225016b311021585c8d27f0bf062a30c7faf15 | TRANSCRIBED | 276 | LINE_BREAK | 157 | 157 | True |  | th the llama\|3.2:3b\nContr | point_between_code_points | 10 |
| 50 | 2 | cf81d18a202dd3bee964c95501225016b311021585c8d27f0bf062a30c7faf15 | TRANSCRIBED | 276 | LINE_BREAK | 242 | 242 | True |  | y requires t\|hat sort of\n | point_between_code_points | 10 |
| 51 | 2 | 78fcff1e96bcb15bde58d43d44575cb072ed04f97bf837e674fe63546658f2a2 | TRANSCRIBED | 322 | LINE_BREAK | 76 | 76 | True |  | m governance\| is\nactually | point_between_code_points | 13 |
| 52 | 2 | 78fcff1e96bcb15bde58d43d44575cb072ed04f97bf837e674fe63546658f2a2 | TRANSCRIBED | 322 | LINE_BREAK | 160 | 160 | True |  | pository alr\|eady says\nFa | point_between_code_points | 13 |
| 53 | 2 | 78fcff1e96bcb15bde58d43d44575cb072ed04f97bf837e674fe63546658f2a2 | TRANSCRIBED | 322 | LINE_BREAK | 239 | 239 | True |  | ew should es\|tablish the  | point_between_code_points | 13 |
| 54 | 2 | 868200fc49f52da1d9b7772c631cfaafa8f0cab62e71af170384d811ca37df4a | TRANSCRIBED | 105 | LINE_BREAK | 78 | 78 | True |  | ion convenie\|ntly\nstuffed | point_between_code_points | 14 |
| 55 | 2 | 84be5cd59aa845e23a288b5a0a781b9620fa67e9609e5d5a5d9839096874a563 | TRANSCRIBED | 51 | LINE_BREAK | 28 | 28 | True |  | R=SATISFIED\n\|PROGRAMME_ST | point_between_code_points | 20 |
| 56 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 42 | 42 | True |  | NATIVE_MODEL\|\n\nFAMILY_F_C | point_between_code_points | 21 |
| 57 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 71 | 71 | True |  | T_AUTHORITY=\|\nPROCEED TO  | point_between_code_points | 21 |
| 58 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 115 | 115 | True |  |  REVIEW ONLY\|\n\nREMEDY_SEL | point_between_code_points | 21 |
| 59 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 135 | 135 | True |  | _SELECTED=NO\|\nMODEL_SELEC | point_between_code_points | 21 |
| 60 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 153 | 153 | True |  | _SELECTED=NO\|\nMODEL_QUALI | point_between_code_points | 21 |
| 61 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 172 | 172 | True |  | QUALIFIED=NO\|\n\nIMPLEMENTA | point_between_code_points | 21 |
| 62 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 202 | 202 | True |  | UTHORIZED=NO\|\nMODEL_RUN_A | point_between_code_points | 21 |
| 63 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 226 | 226 | True |  | UTHORIZED=NO\|\nCAMPAIGN_AU | point_between_code_points | 21 |
| 64 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 249 | 249 | True |  | UTHORIZED=NO\|\n\nQWEN_2_5_C | point_between_code_points | 21 |
| 65 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 284 | 284 | True |  | _AUTHORIZED=\|NO\n\nKNOWLEDG | point_between_code_points | 21 |
| 66 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 334 | 334 | True |  | 3_AUTHORIZED\|=NO\n\nNEXT_LA | point_between_code_points | 21 |
| 67 | 3 | e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff | TRANSCRIBED | 412 | LINE_BREAK | 354 | 354 | True |  | T_LAWFUL_ACT\|ION=\nFAMILY  | point_between_code_points | 21 |
| 68 | 3 | b78c8e9db83b018303137291c264750d7d915befb8ccc0c2348b02b1067f77c2 | TRANSCRIBED | 113 | LINE_BREAK | 77 | 77 | True |  | ed, committe\|d and\nmerged | point_between_code_points | 22 |
| 69 | 3 | 29e27784f702b78f15da39de43ca36a00a884dd7482b454ba3b81006d8c78e13 | TRANSCRIBED | 94 | LINE_BREAK | 82 | 82 | True |  | rker’s gover\|nance\nchain. | point_between_code_points | 23 |
| 70 | 3 | cf9708d64b9225de5c534717d564c485f0108f8a8d1a8585c5ff9636aa415fec | TRANSCRIBED | 129 | LINE_BREAK | 79 | 79 | True |  | or Qwen yet.\| The\nnext op | point_between_code_points | 24 |

## 6. Transcription structure and order

The persisted structured state contains exactly 24 blocks, 24 distinct requested region IDs, 24 populated transcription strings, and 24 TRANSCRIBED statuses. Provider ordinals form the expected 124 sequence. Provider-returned block order equals the request's Parker source-region order in this response.

After removing only the 70 zero-length observations, the current validator accepts the exact remaining structure. Removing all observations also validates. Converting only the 70 pairs to the type system's existing unanchored null/null representation also validates. This proves there is no second structural validation defect in region accounting, status/text rules, ordinals, provenance, or remaining observations. It does not constitute fidelity admission or review.

## 7. Deterministic replay

R69AConsumedResponseForensicReplayTest uses a fake in-memory transport and sentinel credential. With R69A_PROVIDER_STATE_FIXTURE pointing to the isolated copy, it verifies the raw SHA, rebuilds a non-custodial request carrying exact validation identities, sends copied raw bytes through the current parser, reproduces VALIDATION_MALFORMED_SCHEMA, proves raw structured output equals persisted state, counts 70 observations and 24 blocks, and executes three diagnostic-only in-memory variants.

A sanitized fixture separately proves [1,1) rejects as MALFORMED_SCHEMA, while [1,2) and null/null validate. No test transport has a network implementation.

## 8. Root cause and next unit

Primary: **SCHEMA_EXPRESSIVENESS_GAP**. The provider emitted values allowed by the frozen strict schema but necessarily rejected by the post-parse cross-field validator.

Secondary: **DOMAIN_MODEL_MISMATCH**. LINE_BREAK can describe a point boundary, while the current object supports only an unanchored observation or a non-empty substring range. The provider instruction/schema do not define which representation to use. This is not a persistence or adapter mapping defect: exact values survived both. It is not primarily a provider contract violation because the strict provider-visible schema allowed the values.

Recommended smallest next unit: a bounded R6.9B domain/schema decision, with no live call, that explicitly chooses and documents visual-observation anchoring semantics. If point anchors are governed, represent them explicitly rather than overloading a half-open range; if only range/unanchored observations are governed, make the provider schema and instruction enforce that choice. Any wire/schema change must receive a new version/digest and offline adapter/validator fixtures. The consumed response remains immutable and must not be repaired or admitted. A future live test requires a separately authorized successor authority only after remediation.
