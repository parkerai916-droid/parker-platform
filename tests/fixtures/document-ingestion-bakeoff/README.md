# Parker Document Ingestion Controlled Fixture Pack

These seven fixtures are entirely synthetic and contain no real personal, case, or evidence data.

After human acceptance, the exact source bytes under `fixtures/` are immutable test evidence. Parser, extraction, OCR, normalization, reconstruction, Markdown, JSON, metadata, and other processed outputs are derivatives and must never overwrite or be confused with the fixture files. Parser and product outputs must be stored separately, never in `fixtures/`.

The SHA-256 values in `manifest.json` define the ground truth for source-byte identity. No single canonical plain-text serialization is prescribed where a source format permits multiple legitimate representations.

Never edit or regenerate an accepted fixture in place. If a future test requires a modified source, create a new fixture with a new fixture ID and SHA-256 hash.
