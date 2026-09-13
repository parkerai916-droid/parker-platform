from pathlib import Path
import unittest


SCRIPT = Path(__file__).resolve().parents[2] / "tools" / "parker_refresh.sh"


class ParkerRefreshScriptTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SCRIPT.read_text()

    def test_uses_exact_checkout_and_authoritative_deploy_helper(self):
        self.assertTrue(SCRIPT.stat().st_mode & 0o111)
        self.assertIn('PARKER_DIR="/home/steve/parker-live"', self.source)
        self.assertIn('PARKER_DEPLOY="/usr/local/sbin/parker-deploy"', self.source)
        self.assertIn('production_commit="$(/usr/bin/git -C "$PARKER_DIR" rev-parse --verify HEAD)"', self.source)
        self.assertIn('"$PARKER_DEPLOY" || fail', self.source)
        self.assertNotIn("git fetch", self.source)
        self.assertNotIn("git checkout", self.source)
        self.assertNotIn("git reset", self.source)

    def test_keeps_deployment_identity_and_mounts_fail_closed(self):
        self.assertIn('printenv PARKER_PRODUCTION_COMMIT', self.source)
        self.assertIn('[[ "$deployed_commit" == "$production_commit" ]]', self.source)
        self.assertEqual(self.source.count("require_read_only_mount"), 4)
        self.assertIn('[[ "$actual" == "$source|false" ]]', self.source)

    def test_restarts_user_service_without_touching_the_unit(self):
        self.assertIn('runuser -u steve -- env', self.source)
        self.assertIn('/usr/bin/systemctl --user "$@"', self.source)
        self.assertIn('restart "$CONSOLE_SERVICE"', self.source)
        self.assertIn('is-active --quiet "$CONSOLE_SERVICE"', self.source)

    def test_stt_probe_is_fixed_forced_command_and_preserves_stdin(self):
        self.assertIn('printf "{}\\n" | /usr/bin/ssh -T', self.source)
        self.assertIn('parker_hermes_stt_ed25519', self.source)
        self.assertIn('parker_hermes_analysis_known_hosts', self.source)
        self.assertIn('[[ "$stt_result" == \'{"status":"INVALID_REQUEST"}\' ]]', self.source)
        self.assertNotIn('ssh -n', self.source)


if __name__ == "__main__":
    unittest.main()
