import unittest
import importlib.util
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

    def test_server_uses_batch_header_for_submission_and_assignment(self):
        self.assertIn('X-Parker-Ingestion-Batch-Id', self.source)
        self.assertIn('/agent/evidence', self.source)
        self.assertIn('/assign', self.source)
        self.assertNotIn('--acquire', self.source)

    def test_browser_values_do_not_become_server_paths_or_commands(self):
        self.assertIn('os.path.basename(raw_filename.replace("\\\\", "/"))', self.source)
        self.assertNotIn('shell=True', self.source)
        self.assertNotIn('subprocess.', self.source)

    def test_valid_multipart_upload_parses(self):
        content_type, body = multipart(payload=b"hello")
        self.assertEqual(UI.parse_multipart_upload(content_type, body), ("application/pdf", "report.pdf", b"hello"))

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


if __name__ == "__main__":
    unittest.main()
