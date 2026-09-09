import tempfile
import unittest
from pathlib import Path

from tools.parker_bulk_ingest import Ledger, SUPPORTED, media_type


class BulkIngestMechanicsTest(unittest.TestCase):
    def test_media_is_extension_deterministic_and_docx_is_not_supported(self):
        self.assertEqual(media_type(Path("mail.eml")), "message/rfc822")
        self.assertEqual(media_type(Path("scan.png")), "image/png")
        self.assertNotIn(media_type(Path("pending.docx")), SUPPORTED)

    def test_ledger_preserves_relative_paths_and_mechanical_state(self):
        with tempfile.TemporaryDirectory() as directory:
            ledger = Ledger(Path(directory) / "job.sqlite3", "b1", Path(directory), "case-1")
            ledger.put("Unicode/é.txt", "é.txt", 0, "0" * 64, "text/plain", "UNSUPPORTED", "unsupported media type: text/plain")
            row = ledger.rows()[0]
            self.assertEqual(row[0], "Unicode/é.txt")
            self.assertEqual(row[5], "UNSUPPORTED")
            self.assertEqual(row[9], "unsupported media type: text/plain")


if __name__ == "__main__":
    unittest.main()
