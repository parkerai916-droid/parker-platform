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
        self.assertEqual(self.source.count("require_read_only_mount"), 6)
        self.assertIn('[[ "$actual" == "$source|false" ]]', self.source)

    def test_restarts_user_service_without_touching_the_unit(self):
        self.assertIn('RUNUSER_BIN="$(command -v runuser || true)"', self.source)
        self.assertIn('[[ -n "$RUNUSER_BIN" ]] || fail "runuser command not found"', self.source)
        self.assertIn('"$RUNUSER_BIN" -u steve -- env', self.source)
        self.assertNotIn('/usr/bin/runuser', self.source)
        self.assertIn('/usr/bin/systemctl --user "$@"', self.source)
        self.assertIn('restart "$CONSOLE_SERVICE"', self.source)
        self.assertIn('is-active --quiet "$CONSOLE_SERVICE"', self.source)

    def test_stt_probe_is_fixed_forced_command_and_preserves_stdin(self):
        self.assertIn('printf "{}\\n" | /usr/bin/ssh -T', self.source)
        self.assertIn('parker_hermes_stt_ed25519', self.source)
        self.assertIn('parker_hermes_analysis_known_hosts', self.source)
        self.assertIn('set +e', self.source)
        self.assertIn('stt_exit=$?', self.source)
        self.assertIn('[[ "$stt_exit" -eq 1 ]]', self.source)
        self.assertIn('[[ "$stt_result" == \'{"status":"INVALID_REQUEST"}\' ]]', self.source)
        self.assertNotIn('2>/dev/null)" || true', self.source)
        self.assertNotIn('ssh -n', self.source)

    def test_startup_readiness_retries_until_all_signals_are_present(self):
        self.assertIn('STARTUP_TIMEOUT_SECONDS=30', self.source)
        self.assertIn('STARTUP_POLL_SECONDS=1', self.source)
        self.assertIn('while (( SECONDS < startup_deadline )); do', self.source)
        self.assertIn('Runtime started', self.source)
        self.assertIn('Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080', self.source)
        self.assertIn('Agent Gateway HTTP server started on 0.0.0.0:8090', self.source)
        self.assertIn('sleep "$STARTUP_POLL_SECONDS"', self.source)
        self.assertIn('within ${STARTUP_TIMEOUT_SECONDS}s', self.source)

    def test_existing_current_container_can_reuse_prior_health(self):
        self.assertIn('pre_deploy_healthy=false', self.source)
        self.assertIn('pre_deploy_commit', self.source)
        self.assertIn('pre_deploy_running', self.source)
        self.assertIn('if [[ "$pre_deploy_healthy" != true || "$container_id" != "$pre_deploy_container_id" ]]; then', self.source)

    def test_readiness_keeps_commit_port_and_running_checks_before_logs(self):
        readiness = self.source.index('if [[ "$pre_deploy_healthy" != true')
        self.assertLess(self.source.index('[[ "$deployed_commit" == "$production_commit" ]]'), readiness)
        self.assertLess(self.source.index('Parker runtime container is not running'), readiness)
        self.assertLess(self.source.index('Parker port $port is not published'), readiness)


if __name__ == "__main__":
    unittest.main()
