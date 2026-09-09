import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def fake_command(directory, name, body):
    path = Path(directory) / name
    path.write_text("#!/usr/bin/env bash\nset -euo pipefail\n" + body)
    path.chmod(0o755)
    return path


class OperatorWrapperTest(unittest.TestCase):
    def run_script(self, script, *args, bin_dir=None, env=None, cwd=ROOT):
        process_env = os.environ.copy()
        if bin_dir:
            process_env["PATH"] = str(bin_dir) + os.pathsep + process_env["PATH"]
        if env:
            process_env.update(env)
        return subprocess.run([str(ROOT / script), *args], cwd=cwd, env=process_env,
                              text=True, capture_output=True)

    def test_hermes_wrong_host_and_missing_token_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fake_command(directory, "hostname", 'echo "parker"')
            result = self.run_script("start-hermes.sh", bin_dir=directory,
                                     env={"HOME": directory})
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("hostname hermes", result.stderr)

            fake_command(directory, "hostname", 'echo "hermes"')
            result = self.run_script("start-hermes.sh", bin_dir=directory,
                                     env={"HOME": directory})
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("token file is missing", result.stderr)

    def test_hermes_rejects_unsafe_permissions_and_accepts_authenticated_404_only(self):
        with tempfile.TemporaryDirectory() as directory:
            token_file = Path(directory) / ".hermes" / "parker-agent-gateway-token"
            token_file.parent.mkdir()
            token_file.write_text("test-token\n")
            token_file.chmod(0o644)
            fake_command(directory, "hostname", 'echo "hermes"')
            result = self.run_script("start-hermes.sh", bin_dir=directory,
                                     env={"HOME": directory})
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("broader than 0600", result.stderr)
            self.assertNotIn("test-token", result.stdout + result.stderr)

            token_file.chmod(0o600)
            fake_command(directory, "curl", 'for arg in "$@"; do case "$arg" in http*) printf "%s" "$arg" > "$CURL_URL_FILE" ;; esac; done; printf "${FAKE_HTTP_STATUS:-404}"')
            result = self.run_script("start-hermes.sh", bin_dir=directory,
                                     env={"HOME": directory, "CURL_URL_FILE": str(Path(directory) / "curl-url")})
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("Authentication: OK", result.stdout)
            self.assertEqual((Path(directory) / "curl-url").read_text(), "http://192.168.178.44:8090/agent/evidence/test")
            result = self.run_script("start-hermes.sh", bin_dir=directory,
                                     env={"HOME": directory, "FAKE_HTTP_STATUS": "401", "CURL_URL_FILE": str(Path(directory) / "curl-url")})
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("authentication failed", result.stderr)

    def test_ingestion_requires_explicit_parker_batch_and_passes_case_as_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "incoming"
            source.mkdir()
            fake_command(directory, "hostname", 'echo "hermes"')
            fake_command(directory, "python3", 'printf "%s\\n" "$@"')
            env = {"HOME": directory, "PARKER_AGENT_GATEWAY_TOKEN": "test-token"}
            result = self.run_script("parker-ingest-folder", str(source), "--case-id", "case-1",
                                     bin_dir=directory, env=env)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("--batch is required", result.stderr)
            self.assertNotIn("test-token", result.stdout + result.stderr)

            result = self.run_script("parker-ingest-folder", str(source), "--batch", "bulk-1234",
                                     "--case-id", "case-1", bin_dir=directory, env=env, cwd=directory)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("bulk-1234", result.stdout)
            self.assertIn("case-1", result.stdout)
            self.assertIn("/agent/evidence/{evidenceArtifactId}/assign", result.stdout)
            self.assertIn(str(ROOT / "tools/parker_bulk_ingest.py"), result.stdout)
            self.assertNotIn("--acquire", result.stdout)

    def test_parker_fails_on_running_commit_mismatch(self):
        with tempfile.TemporaryDirectory() as directory:
            secret = Path(directory) / "owner.secret"
            secret.write_text("test-secret\n")
            bin_dir = Path(directory) / "bin"
            bin_dir.mkdir()
            fake_command(bin_dir, "hostname", 'echo "parker"')
            fake_command(bin_dir, "git", 'if [[ "$*" == *"status --porcelain"* ]]; then exit 0; elif [[ "$*" == *"--show-toplevel"* ]]; then echo "$PWD"; else echo "a".repeat 40; fi')
            fake_command(bin_dir, "sudo", 'exec "$@"')
            fake_command(bin_dir, "curl", 'exit 0')
            fake_command(bin_dir, "docker", 'if [[ "$1" == "inspect" ]]; then echo true; elif [[ "$1" == "exec" ]]; then echo wrong-commit; fi')
            result = self.run_script("start-parker.sh", bin_dir=bin_dir,
                                     env={"PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET_FILE": str(secret),
                                          "PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID": "test-owner"})
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("PARKER_PRODUCTION_COMMIT does not match", result.stderr)

    def test_hermes_sources_existing_repo_venv_before_readiness(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory) / "repo"
            repo.mkdir()
            subprocess.run(["git", "init", "-q", str(repo)], check=True)
            (repo / "marker").write_text("marker")
            subprocess.run(["git", "-C", str(repo), "-c", "user.email=test@example.invalid", "-c",
                            "user.name=test", "add", "marker"], check=True)
            subprocess.run(["git", "-C", str(repo), "-c", "user.email=test@example.invalid", "-c",
                            "user.name=test", "commit", "-qm", "marker"], check=True)
            shutil.copy2(ROOT / "start-hermes.sh", repo / "start-hermes.sh")
            (repo / ".venv/bin").mkdir(parents=True)
            (repo / ".venv/bin/activate").write_text(
                'export VIRTUAL_ENV="$PWD/.venv"\nexport PATH="$VIRTUAL_ENV/bin:$PATH"\n'
            )
            (repo / ".venv/bin/activate").chmod(0o644)
            token_file = Path(directory) / ".hermes/parker-agent-gateway-token"
            token_file.parent.mkdir()
            token_file.write_text("test-token\n")
            token_file.chmod(0o600)
            bin_dir = Path(directory) / "bin"
            bin_dir.mkdir()
            fake_command(bin_dir, "hostname", 'echo "hermes"')
            fake_command(bin_dir, "curl", '[[ "$VIRTUAL_ENV" == *"/.venv" ]] && [[ "$PATH" == "$VIRTUAL_ENV/bin:"* ]] && [[ -n "$PARKER_AGENT_GATEWAY_TOKEN" ]] && [[ "$PARKER_GATEWAY_URL" == "http://192.168.178.44:8090" ]] && printf 404 || exit 1')
            env = {"HOME": directory, "PATH": str(bin_dir) + os.pathsep + os.environ["PATH"]}
            result = subprocess.run([str(repo / "start-hermes.sh")], cwd=Path(directory), env=env,
                                    text=True, capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
