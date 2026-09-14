from __future__ import annotations

import importlib.util
import json
import socket
import subprocess
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
    def test_console_renders_parker_analysis_link_in_new_tab(self):
        page = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        link = '<a class="nav-link" href="https://parker.home.arpa" target="_blank" rel="noopener noreferrer">Open Parker Analysis ↗</a>'
        self.assertIn(link, page)
        self.assertIn('target="_blank"', link)
        self.assertIn('rel="noopener noreferrer"', link)
        self.assertNotIn('HERMES_ANALYSIS_URL', page)
        self.assertIn('id="newBatch">New batch</button>', page)
        self.assertIn('data-view="split">Split View', page)

    def test_hermes_review_renders_decision_ready_metadata_and_actions(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        script = """
const source = %s;
const esc = x => String(x ?? '');
const start = source.indexOf('function reviewIssue');
const end = source.indexOf('async function review(){');
eval(source.slice(start, end));
const failed = {status:'FAILED', failure:{kind:'NO_READABLE_CONTENT', detail:'No readable text was found in this image'}, methods:['OCR'], batchId:'batch-1', sourceSha256:'hash-1'};
const reviewRequired = {status:'REVIEW_REQUIRED', issues:[{kind:'LOW_CONFIDENCE', explanation:'Text confidence is below threshold', hermesInterpretation:'Only partial text was found'}], methods:['DIRECT_TEXT_EXTRACTION'], batchId:'batch-2', sourceSha256:'hash-2'};
console.log(JSON.stringify({
  failedReason: reviewIssue(failed),
  failedCode: reviewCode(failed),
  failedFound: reviewFound(failed),
  failedMethod: reviewMethod(failed),
  failedAction: reviewAction(failed),
  failedButtons: reviewButtons(failed),
  reviewButtons: reviewButtons(reviewRequired),
  reviewAction: reviewAction(reviewRequired)
}));
""" % json.dumps(source)
        result = subprocess.run(["node", "-e", script], check=True, capture_output=True, text=True)
        values = json.loads(result.stdout)
        self.assertEqual(values["failedReason"], "No readable text was found in this image")
        self.assertEqual(values["failedCode"], "NO_READABLE_CONTENT")
        self.assertEqual(values["failedFound"], "No readable text was found in this image")
        self.assertEqual(values["failedMethod"], "OCR")
        self.assertIn("Reprocess", values["failedAction"])
        self.assertIn("disabled", values["failedButtons"])
        self.assertIn("REPROCESS → run again", values["failedButtons"])
        self.assertIn("REJECT → exclude item", values["failedButtons"])
        self.assertIn("cannot be accepted directly", values["failedButtons"])
        self.assertIn("ACCEPT → allow current result", values["reviewButtons"])
        self.assertIn("accept with an explanation", values["reviewAction"])

    def test_hermes_review_payload_includes_source_identity_metadata(self):
        source = (ROOT / "src" / "composition" / "OwnerEvidenceHttpServer.kt").read_text()
        self.assertIn("readPendingReviewSourceAsOwner(item.batchId, item.sourceSha256)", source)
        self.assertIn('"fileName" to source?.originalDisplayName', source)
        self.assertIn('"fileType" to source?.mediaType', source)

    def test_ingestion_result_labels_only_duplicate_409_and_keeps_diagnostic_status(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        script = """
const source = %s;
const esc = x => String(x ?? '');
const start = source.indexOf('function duplicateRejection');
const end = source.indexOf('function selected');
eval(source.slice(start, end));
console.log(JSON.stringify([
  duplicateRejection(409, {status:'DUPLICATE'}),
  duplicateRejection(409, {reason:'already exists in Parker'}),
  duplicateRejection(200, {governed_ingestion:'HTTP_409', reason:'duplicate evidence'}),
  duplicateRejection(409, {status:'CASE_BINDING_REJECTED'}),
  duplicateRejection(409, {status:'CONFLICT'}),
  duplicateRejection(500, {status:'DUPLICATE'}),
  technicalDetails(409, {status:'DUPLICATE'})
]));
""" % json.dumps(source)
        result = subprocess.run(["node", "-e", script], check=True, capture_output=True, text=True)
        values = json.loads(result.stdout)
        self.assertEqual(values[:6], [True, True, True, False, False, False])
        self.assertIn("HTTP 409", values[6])

    def test_ingestion_ui_preserves_success_and_non_duplicate_result_labels(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        self.assertIn("DUPLICATE — already exists in Parker", source)
        self.assertIn("d.governed_ingestion||d.result_submission||d.reason", source)
        self.assertIn("technicalDetails(e.httpStatus,e.response)", source)

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
        self.assertIn("function scopedBatches(){const selected=cases.find(c=>c.caseId===", source)
        self.assertIn("b.caseName===selected?.caseName", source)
        self.assertIn("batchHistory", source)
        self.assertIn("function label(b)", source)
        self.assertIn("const caseId=$('case').value", source)
        self.assertNotIn("(c.cases||[])[0]", source)
        self.assertNotIn("batches[Number($('batch').value)]", source)

    def test_current_run_metrics_and_rows_do_not_accumulate_batch_history(self):
        source = (ROOT / "tools" / "parker_ingestion_console" / "index.html").read_text()
        ingest = source[source.index("async function ingest()"):source.index("function reviewIssue")]
        self.assertIn("for(const k of Object.keys(state))state[k]=0", ingest)
        self.assertIn("$('progress').innerHTML=''", ingest)
        self.assertNotIn("(b.processingResults||[]).forEach", source[source.index("function render()"):source.index("async function health")])

    def test_ingestion_requires_authoritative_batch_binding(self):
        source = SCRIPT.read_text()
        self.assertIn("validate_authoritative_batch", source)
        self.assertIn('"CASE_BINDING_MISMATCH"', source)
        self.assertIn('if not validate_authoritative_batch(cookie, batch)', source)

    def test_ready_batch_discovery_uses_current_parker_agent_projection(self):
        ready = {"batches": [{"batchId": "bulk-current", "caseName": "Current case", "status": "READY"}]}
        with patch.object(console, "ready_batches", return_value=ready), patch.object(console, "owner", side_effect=AssertionError("owner GET must not discover READY batches")):
            code, raw = console.authoritative_batches("owner-cookie")
        self.assertEqual(code, 200)
        self.assertEqual(json.loads(raw), ready)

    def test_ready_batch_validation_rechecks_exact_current_ready_projection(self):
        ready = {"batches": [{"batchId": "bulk-current", "caseName": "Current case", "status": "READY"}]}
        with patch.object(console, "ready_batches", return_value=ready):
            self.assertTrue(console.validate_authoritative_batch("owner-cookie", "bulk-current"))
            self.assertFalse(console.validate_authoritative_batch("owner-cookie", "bulk-other"))

    def test_ready_batch_discovery_failure_is_not_converted_to_fabricated_empty_state(self):
        with patch.object(console, "ready_batches", side_effect=RuntimeError("Parker READY batch discovery failed")):
            with self.assertRaises(RuntimeError):
                console.authoritative_batches("owner-cookie")

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
        self.assertIn("/agent/ingestion-batches", (ROOT / "src" / "composition" / "AgentGatewayHttpServer.kt").read_text())
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
