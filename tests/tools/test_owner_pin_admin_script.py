import os
import pty
import shutil
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WRAPPER = ROOT / "tools" / "owner-pin-admin.sh"


class OwnerPinAdminScriptPathTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(prefix="owner-pin-admin-wrapper-")
        self.root = Path(self.temp_dir.name)
        tools = self.root / "tools"
        tools.mkdir()
        shutil.copy2(WRAPPER, tools / WRAPPER.name)
        (tools / WRAPPER.name).chmod(0o755)
        (self.root / "gradlew").write_text(
            "#!/usr/bin/env bash\n"
            "printf '%s\\n' \"$*\" > \"$MARKER\"\n",
            encoding="utf-8",
        )
        (self.root / "gradlew").chmod(0o755)
        self.marker = self.root / "bootstrap-marker"
        (self.root / "nested").mkdir()

    def tearDown(self):
        self.temp_dir.cleanup()

    def run_wrapper(self, invocation, cwd):
        pid, fd = pty.fork()
        if pid == 0:
            os.chdir(cwd)
            os.environ["MARKER"] = str(self.marker)
            os.execv(invocation, [invocation, "verify-status"])
        output = bytearray()
        while True:
            try:
                output.extend(os.read(fd, 4096))
            except OSError:
                break
        _, status = os.waitpid(pid, 0)
        self.assertEqual(0, os.waitstatus_to_exitcode(status), output.decode(errors="replace"))
        self.assertEqual("--quiet ownerPinAdmin --args=verify-status\n", self.marker.read_text())
        self.marker.unlink()

    def test_absolute_and_relative_invocations_resolve_wrapper_root(self):
        absolute = str(self.root / "tools" / WRAPPER.name)
        self.run_wrapper(absolute, self.root / "nested")
        self.run_wrapper("tools/owner-pin-admin.sh", self.root)
        self.run_wrapper("../tools/owner-pin-admin.sh", self.root / "nested")


if __name__ == "__main__":
    unittest.main()
