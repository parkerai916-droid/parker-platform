import os
import pty
import subprocess
import shutil
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WRAPPER = ROOT / "tools" / "owner-pin-admin.sh"


class OwnerPinAdminScriptPathTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory(prefix="owner-pin-admin-wrapper-")
        self.caller_dir = tempfile.TemporaryDirectory(prefix="owner-pin-admin-caller-")
        self.root = Path(self.temp_dir.name)
        tools = self.root / "tools"
        tools.mkdir()
        shutil.copy2(WRAPPER, tools / WRAPPER.name)
        (tools / WRAPPER.name).chmod(0o755)
        source = self.root / "OwnerPinAdminToolKt.java"
        source.write_text(
            "package parker.composition;\n"
            "import java.nio.file.*;\n"
            "public final class OwnerPinAdminToolKt {\n"
            "  public static void main(String[] args) throws Exception {\n"
            "    boolean console = System.console() != null;\n"
            "    Files.writeString(Path.of(System.getenv(\"MARKER\")), String.join(\"|\", args) + \"|console=\" + console + \"\\n\");\n"
            "    if (args.length == 1 && !\"verify-status\".equals(args[0]) && !console) System.exit(2);\n"
            "  }\n"
            "}\n",
            encoding="utf-8",
        )
        classes = self.root / "classes"
        subprocess.run(["javac", "-d", str(classes), str(source)], check=True)
        fake_jar = self.root / "admin.jar"
        subprocess.run(["jar", "--create", "--file", str(fake_jar), "-C", str(classes), "."], check=True)
        (self.root / "gradlew").write_text(
            "#!/usr/bin/env bash\n"
            "mkdir -p \"$ROOT/build/install/parker/lib\"\n"
            "cp \"$FAKE_JAR\" \"$ROOT/build/install/parker/lib/parker.jar\"\n",
            encoding="utf-8",
        )
        (self.root / "gradlew").chmod(0o755)
        self.marker = self.root / "bootstrap-marker"
        (self.root / "nested").mkdir()

    def tearDown(self):
        self.caller_dir.cleanup()
        self.temp_dir.cleanup()

    def run_wrapper(self, invocation, cwd):
        pid, fd = pty.fork()
        if pid == 0:
            os.chdir(cwd)
            os.environ["MARKER"] = str(self.marker)
            os.environ["ROOT"] = str(self.root)
            os.environ["FAKE_JAR"] = str(self.root / "admin.jar")
            os.execv(invocation, [invocation, "verify-status"])
        output = bytearray()
        while True:
            try:
                output.extend(os.read(fd, 4096))
            except OSError:
                break
        _, status = os.waitpid(pid, 0)
        self.assertEqual(0, os.waitstatus_to_exitcode(status), output.decode(errors="replace"))
        self.assertEqual("verify-status|console=true\n", self.marker.read_text())
        self.marker.unlink()

    def test_absolute_and_relative_invocations_resolve_wrapper_root(self):
        absolute = str(self.root / "tools" / WRAPPER.name)
        self.run_wrapper(absolute, self.caller_dir.name)
        self.run_wrapper("tools/owner-pin-admin.sh", self.root)
        self.run_wrapper("../tools/owner-pin-admin.sh", self.root / "nested")

    def test_verify_status_works_without_a_console_but_mutations_do_not(self):
        environment = os.environ | {
            "MARKER": str(self.marker),
            "ROOT": str(self.root),
            "FAKE_JAR": str(self.root / "admin.jar"),
        }
        wrapper = str(self.root / "tools" / WRAPPER.name)
        status = subprocess.run([wrapper, "verify-status"], cwd=self.caller_dir.name,
                                env=environment, stdin=subprocess.DEVNULL,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.assertEqual(0, status.returncode, status.stderr)
        self.assertEqual("verify-status|console=false\n", self.marker.read_text())
        self.marker.unlink()

        mutation = subprocess.run([wrapper, "set"], cwd=self.caller_dir.name,
                                   env=environment, stdin=subprocess.DEVNULL,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.assertEqual(2, mutation.returncode)
        self.assertEqual("set|console=false\n", self.marker.read_text())


if __name__ == "__main__":
    unittest.main()
