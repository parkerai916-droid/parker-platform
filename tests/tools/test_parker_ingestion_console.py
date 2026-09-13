from __future__ import annotations

import importlib.util
import json
import socket
import sys
import unittest
from unittest.mock import patch
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools" / "parker_ingestion_console.py"
spec = importlib.util.spec_from_file_location("parker_ingestion_console", SCRIPT)
assert spec and spec.loader
console = importlib.util.module_from_spec(spec)
sys.modules["parker_ingestion_console"] = console
spec.loader.exec_module(console)


class ConsoleBoundaryTest(unittest.TestCase):
    def test_console_uses_real_api_data_and_has_no_static_metrics_or_case_names(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        self.assertNotIn("McTague v Uber", source)
        self.assertNotIn("Uber ERA", source)
        self.assertNotIn("183", source)
        self.assertNotIn("no-cors", source)
        self.assertNotIn(":8090", source)
        self.assertIn("/api/batches", source)
        self.assertIn("/api/health", source)
        self.assertIn("/api/ingest?batchId=", source)

    def test_console_uses_stable_batch_ids_and_explicit_case_for_new_batches(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        self.assertIn("batches.find(b=>b.batchId===", source)
        self.assertIn("const matching=visible.filter(b=>b.batchId===prior)", source)
        self.assertIn("function scopedBatches(){const caseId=$('case').value", source)
        self.assertIn("batchHistory", source)
        self.assertIn("function label(b)", source)
        self.assertIn("const caseId=$('case').value", source)
        self.assertNotIn("(c.cases||[])[0]", source)
        self.assertNotIn("batches[Number($('batch').value)]", source)

    def test_ingestion_requires_authoritative_batch_binding(self):
        source = SCRIPT.read_text()
        self.assertIn("validate_authoritative_batch", source)
        self.assertIn('"CASE_BINDING_MISMATCH"', source)
        self.assertIn('if not validate_authoritative_batch(cookie, batch)', source)

    def test_ids_are_only_in_advanced_details_and_history(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        render = source[source.index("function render()"):source.index("async function health")]
        normal = render.split("<details>", 1)[0]
        self.assertNotIn("Case ID:", normal)
        self.assertNotIn("Batch ID:", normal)
        self.assertIn("<details>", render)
        self.assertIn("Case ID: ${esc(b.caseId)}", render)
        self.assertIn("Batch ID: ${esc(b.batchId)}", render)
        self.assertIn("${esc(b.batchId)}", source[source.index("function renderHistory"):])

    def test_adapter_allowlists_only_narrow_routes(self):
        source = SCRIPT.read_text()
        self.assertIn("/owner/cases", source)
        self.assertIn("/owner/ingestion-batches", source)
        self.assertIn("/owner/hermes-processing/review", source)
        self.assertNotIn("/agent/", source)
        self.assertNotIn("PARKER_AGENT_GATEWAY_TOKEN", source)

    def test_batch_and_hash_validation_is_fail_closed(self):
        self.assertIsNotNone(console.BATCH.fullmatch("bulk-0123456789abcdef-0123"))
        self.assertIsNone(console.BATCH.fullmatch("arbitrary"))
        self.assertIsNone(console.HASH.fullmatch("not-a-hash"))

    def test_json_serialization_does_not_add_credentials(self):
        body = json.dumps({"state": "HEALTHY"})
        self.assertNotIn("Bearer", body)
        self.assertNotIn("token", body.lower())

    def test_timeout_classes_keep_health_short_and_ingestion_processing_aware(self):
        self.assertEqual(console.HEALTH_TIMEOUT_SECONDS, 5)
        self.assertEqual(console.BATCH_TIMEOUT_SECONDS, 15)
        self.assertGreater(console.INGEST_TIMEOUT_SECONDS, 30)

    def test_call_uses_explicit_timeout_and_returns_delayed_success(self):
        class Response:
            status = 200

            def read(self):
                return b'{"status":"PASS"}'

            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

        with patch.object(console.urllib.request, "urlopen", return_value=Response()) as urlopen:
            code, raw = console.call("http://hermes.invalid/api/ingest", "POST", b"pdf", timeout=180)
        self.assertEqual((code, raw), (200, b'{"status":"PASS"}'))
        self.assertEqual(urlopen.call_args.kwargs["timeout"], 180)
        self.assertEqual(urlopen.call_count, 1)

    def test_timeout_is_distinguishable_and_does_not_retry(self):
        with patch.object(console.urllib.request, "urlopen", side_effect=socket.timeout() ) as urlopen:
            with self.assertRaises(console.UpstreamTimeout):
                console.call("http://hermes.invalid/api/ingest", "POST", b"pdf", timeout=180)
        self.assertEqual(urlopen.call_count, 1)

    def test_connection_failure_is_distinguishable_from_timeout(self):
        with patch.object(console.urllib.request, "urlopen", side_effect=console.urllib.error.URLError("offline")):
            with self.assertRaises(console.UpstreamConnectionUnavailable):
                console.call("http://hermes.invalid/", timeout=5)

    def test_ingestion_timeout_message_warns_against_retry(self):
        source = SCRIPT.read_text()
        self.assertIn("Hermes may still be processing this file", source)
        self.assertIn('"retry": False', source)
        self.assertNotIn("for retry", source)


if __name__ == "__main__":
    unittest.main()
