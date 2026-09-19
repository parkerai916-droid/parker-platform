import unittest
import importlib.util
import json
import http.client
import threading
from pathlib import Path


SOURCE = Path(__file__).parents[2] / "tools" / "hermes_bulk_ingest_ui.py"


spec = importlib.util.spec_from_file_location("hermes_bulk_ingest_ui", SOURCE)
UI = importlib.util.module_from_spec(spec)
spec.loader.exec_module(UI)


def multipart(media="application/pdf", filename="report.pdf", payload=b"pdf"):
    boundary = b"----hermes-test-boundary"
    body = (
        b"--" + boundary + b"\r\n"
        b'Content-Disposition: form-data; name="file"; filename="' + filename.encode() + b'"\r\n'
        b"Content-Type: " + media.encode() + b"\r\n\r\n" + payload + b"\r\n"
        b"--" + boundary + b"--\r\n"
    )
    return "multipart/form-data; boundary=" + boundary.decode(), body


def nested_message_multipart(payload):
    boundary = b"----hermes-eml-boundary"
    body = (
        b"--" + boundary + b"\r\n"
        b'Content-Disposition: form-data; name="file"; filename="message.eml"\r\n'
        b"Content-Type: message/rfc822\r\n\r\n" + payload + b"\r\n"
        b"--" + boundary + b"--\r\n"
    )
    return "multipart/form-data; boundary=" + boundary.decode(), body


class HermesBulkUiSecurityTest(unittest.TestCase):
    def setUp(self):
        self.source = SOURCE.read_text()

    def test_ui_defaults_to_loopback_and_requires_explicit_non_wildcard_lan_bind(self):
        self.assertIn('default="127.0.0.1"', self.source)
        self.assertIn('explicit LAN address permitted', self.source)
        self.assertIn('0.0.0.0', self.source)
        self.assertIn('wildcard binding is not permitted', self.source)
        self.assertIn('X-Hermes-Ui-Nonce', self.source)
        self.assertIn('secrets.token_urlsafe', self.source)

    def test_browser_never_receives_gateway_token_or_case_authority(self):
        page = self.source.split("PAGE = r'''", 1)[1].split("'''", 1)[0]
        self.assertNotIn('PARKER_AGENT_GATEWAY_TOKEN', page)
        self.assertNotIn('caseId', page)
        self.assertIn('/api/ready-batches', page)
        self.assertIn('/api/ingest?batchId=', page)
        self.assertIn('batch is not currently Parker-authorised and READY', self.source)

    def test_server_uses_processing_result_then_governed_ingestion(self):
        self.assertIn('process_one', self.source)
        self.assertIn('ParkerClient', self.source)
        self.assertIn('governed_ingestion', self.source)
        self.assertNotIn('X-Parker-Ingestion-Batch-Id', self.source)
        self.assertNotIn('/agent/evidence', self.source)
        self.assertNotIn('--acquire', self.source)

    def test_browser_values_do_not_become_server_paths_or_commands(self):
        self.assertIn('os.path.basename(raw_filename.replace("\\\\", "/"))', self.source)
        self.assertNotIn('shell=True', self.source)
        self.assertNotIn('subprocess.', self.source)

    def test_valid_multipart_upload_parses(self):
        content_type, body = multipart(payload=b"hello")
        self.assertEqual(UI.parse_multipart_upload(content_type, body), ("application/pdf", "report.pdf", b"hello"))

    def test_message_rfc822_file_part_is_not_rejected_as_ambiguous(self):
        source = (b"From: sender@example.test\r\n"
                  b"To: recipient@example.test\r\n"
                  b"Subject: EML multipart regression\r\n"
                  b"MIME-Version: 1.0\r\n"
                  b"Content-Type: text/plain; charset=utf-8\r\n\r\n"
                  b"PARKER EML REGRESSION VALUE 48\r\n")
        content_type, body = nested_message_multipart(source)
        media, filename, data = UI.parse_multipart_upload(content_type, body)
        self.assertEqual(media, "message/rfc822")
        self.assertEqual(filename, "message.eml")
        self.assertIn(b"PARKER EML REGRESSION VALUE 48", data)

    def test_missing_file_part_fails(self):
        boundary = "----hermes-test-boundary"
        body = (
            b"--" + boundary.encode() + b"\r\n"
            b"Content-Disposition: form-data; name=other\r\n\r\nvalue\r\n--"
            + boundary.encode() + b"--\r\n"
        )
        with self.assertRaises(UI.MultipartUploadError):
            UI.parse_multipart_upload("multipart/form-data; boundary=" + boundary, body)

    def test_malformed_multipart_fails_safely(self):
        with self.assertRaises(UI.MultipartUploadError):
            UI.parse_multipart_upload("multipart/form-data; boundary=missing", b"not multipart")

    def test_unsupported_media_is_returned_for_rejection(self):
        content_type, body = multipart(media="application/octet-stream")
        media, _, _ = UI.parse_multipart_upload(content_type, body)
        self.assertNotIn(media, UI.SUPPORTED)

    def test_oversized_file_remains_rejected(self):
        content_type, body = multipart(payload=b"x" * (UI.MAX_FILE + 1))
        _, _, data = UI.parse_multipart_upload(content_type, body)
        self.assertGreater(len(data), UI.MAX_FILE)

    def test_filename_is_reduced_to_basename(self):
        content_type, body = multipart(filename="C:\\\\private\\\\report.pdf")
        _, filename, _ = UI.parse_multipart_upload(content_type, body)
        self.assertEqual(filename, "report.pdf")

    def test_no_cgi_import_remains(self):
        self.assertNotIn("import cgi", self.source)

    def test_ui_uses_shared_catalogue_and_advertises_all_catalogued_extensions(self):
        self.assertIn("supported_extensions", self.source)
        self.assertIn("__SUPPORTED_EXTENSIONS__", self.source)
        for extension in (".doc", ".xls", ".xlsx", ".eml", ".msg", ".rtf", ".tif", ".tiff"):
            self.assertIn(extension, UI.supported_extensions())

    def test_governed_external_eml_is_dispatchable(self):
        self.assertIn("message/rfc822", UI.SUPPORTED)


class HermesBulkUiHealthTest(unittest.TestCase):
    def setUp(self):
        self.server = type("Server", (), {})()
        self.server.hermes_token = "test-token"
        self.server.ui_nonce = "test-nonce"
        self.handler = object.__new__(UI.Handler)
        self.handler.server = self.server
        self.handler.headers = {}

    def get(self, path, headers=None):
        result = {}
        self.handler.path = path
        self.handler.headers = headers or {}
        self.handler.send_json = lambda status, value: result.update(status=status, value=value)
        self.handler.do_GET()
        return result["status"], result["value"]

    def test_health_returns_200_and_healthy_for_passing_preflight(self):
        original = UI.docling_health
        UI.docling_health = lambda: {"status": "PASS", "problems": []}
        try:
            status, payload = self.get("/api/health")
        finally:
            UI.docling_health = original
        self.assertEqual(status, 200)
        self.assertEqual(payload["state"], "HEALTHY")
        self.assertEqual(payload["docling"]["state"], "HEALTHY")

    def test_health_returns_200_and_diagnostic_for_failing_preflight(self):
        original = UI.docling_health
        UI.docling_health = lambda: {"status": "FAIL", "problems": ["model unavailable"]}
        try:
            status, payload = self.get("/api/health")
        finally:
            UI.docling_health = original
        self.assertEqual(status, 200)
        self.assertEqual(payload["state"], "HEALTHY")
        self.assertEqual(payload["docling"]["state"], "DEGRADED")
        self.assertEqual(payload["docling"]["problems"], ["model unavailable"])

    def test_ready_batches_behavior_remains_unchanged(self):
        original = UI.parker_request
        calls = []
        UI.parker_request = lambda path, token: (calls.append((path, token)) or (200, b'{"batches": []}'))
        try:
            status, payload = self.get("/api/ready-batches", {"X-Hermes-Ui-Nonce": "test-nonce"})
        finally:
            UI.parker_request = original
        self.assertEqual(status, 200)
        self.assertEqual(payload, {"batches": []})
        self.assertEqual(calls, [("/agent/ingestion-batches", "test-token")])

    def test_ready_selector_excludes_durably_used_batches(self):
        self.assertIn("(d.batches||[]).filter(b=>b.status==='READY')", UI.PAGE)


class HermesBulkUiMultipartHttpTest(unittest.TestCase):
    def setUp(self):
        self.server = UI.ThreadingHTTPServer(("127.0.0.1", 0), UI.Handler)
        self.server.hermes_token = "test-token"
        self.server.ui_nonce = "test-nonce"
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.calls = []
        self.original_parker_request = UI.parker_request
        self.original_process_one = UI.process_one
        UI.parker_request = lambda path, token: (200, b'{"batches":[{"batchId":"bulk-dead-1"}]}')

    def tearDown(self):
        UI.parker_request = self.original_parker_request
        UI.process_one = self.original_process_one
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def post(self, media, filename, payload=b"{\\rtf1\\ansi Valid RTF}"):
        content_type, body = multipart(media=media, filename=filename, payload=payload)
        connection = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=5)
        connection.request("POST", "/api/ingest?batchId=bulk-dead-1", body, {
            "Content-Type": content_type,
            "Content-Length": str(len(body)),
            "X-Hermes-Ui-Nonce": "test-nonce",
        })
        response = connection.getresponse()
        result = response.status, json.loads(response.read())
        connection.close()
        return result

    def install_result(self, status="PASS", governed_ingestion="ANALYSIS_READY"):
        class Item:
            pending_source_retained = False

            def __init__(self, item_status, item_governed_ingestion):
                self.item_status = item_status
                self.item_governed_ingestion = item_governed_ingestion

            def json(self):
                return {"status": self.item_status, "governed_ingestion": self.item_governed_ingestion}

        def fake_process(client, batch, source_path, timeout, original_filename):
            self.calls.append((batch, source_path.read_bytes(), original_filename))
            return Item(status, governed_ingestion)

        UI.process_one = fake_process

    def test_all_rtf_mime_aliases_cross_real_http_boundary(self):
        self.install_result()
        for media in ("application/rtf", "text/rtf", "application/x-rtf"):
            with self.subTest(media=media):
                status, payload = self.post(media, "document.rtf")
                self.assertEqual(status, 200)
                self.assertEqual(payload["governed_ingestion"], "ANALYSIS_READY")
        self.assertEqual([call[2] for call in self.calls], ["document.rtf"] * 3)

    def test_catalogue_alias_for_msg_is_also_accepted(self):
        self.install_result()
        status, payload = self.post("application/x-ole-storage", "message.msg", b"msg fixture")
        self.assertEqual(status, 200)
        self.assertEqual(payload["governed_ingestion"], "ANALYSIS_READY")

    def test_cross_format_mime_declarations_are_rejected_before_processing(self):
        self.install_result()
        for media, filename in (("image/png", "document.rtf"), ("text/rtf", "image.png")):
            with self.subTest(media=media, filename=filename):
                status, payload = self.post(media, filename, b"not a real document")
                self.assertEqual(status, 200)
                self.assertEqual(payload, {"status": "FAILED", "reason": "unsupported media type"})
        self.assertEqual(self.calls, [])

    def test_malformed_rtf_reaches_downstream_processing_and_fails_truthfully(self):
        self.install_result(status="FAILED", governed_ingestion="BLOCKED")
        status, payload = self.post("text/rtf", "corrupt.rtf", b"not RTF")
        self.assertEqual(status, 200)
        self.assertEqual(payload["status"], "FAILED")
        self.assertEqual(payload["governed_ingestion"], "BLOCKED")
        self.assertEqual(len(self.calls), 1)

    def test_other_supported_formats_keep_their_declared_mime_boundary(self):
        self.install_result()
        for media, filename in (
            ("text/plain", "notes.txt"),
            ("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "letter.docx"),
            ("application/vnd.ms-excel", "sheet.xls"),
            ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "sheet.xlsx"),
            ("message/rfc822", "message.eml"),
            ("application/pdf", "report.pdf"),
            ("image/png", "scan.png"),
        ):
            with self.subTest(media=media, filename=filename):
                status, payload = self.post(media, filename, b"fixture")
                self.assertEqual(status, 200)
                self.assertEqual(payload["governed_ingestion"], "ANALYSIS_READY")


if __name__ == "__main__":
    unittest.main()
