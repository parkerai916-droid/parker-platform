import unittest
from pathlib import Path


SOURCE = Path(__file__).parents[2] / "tools" / "hermes_bulk_ingest_ui.py"


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
        self.assertIn('os.path.basename(field.filename)', self.source)
        self.assertNotIn('shell=True', self.source)
        self.assertNotIn('subprocess.', self.source)


if __name__ == "__main__":
    unittest.main()
