from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
REFRESH = ROOT / "tools" / "parker_refresh.sh"
COMPOSE = ROOT / "docker-compose.yml"
LAUNCHER = ROOT / "deploy" / "hermes-processing-v1" / "hermes-processing-v1-entrypoint"
ENV = ROOT / "deploy" / "hermes-processing-v1" / "hermes-processing-v1.env.example"
AUTHORIZED_KEYS = ROOT / "deploy" / "hermes-processing-v1" / "authorized_keys.template"


class HermesProcessingV1DeploymentTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.refresh = REFRESH.read_text()
        cls.compose = COMPOSE.read_text()
        cls.launcher = LAUNCHER.read_text()
        cls.env = ENV.read_text()
        cls.authorized_keys = AUTHORIZED_KEYS.read_text()

    def test_refresh_uses_remote_forced_command_for_readiness(self):
        self.assertIn("readiness_frame=", self.refresh)
        self.assertIn("docker exec -i", self.refresh)
        self.assertIn("/usr/bin/ssh -T", self.refresh)
        self.assertIn("parker_hermes_processing_ed25519", self.refresh)
        self.assertIn("parker_hermes_processing_known_hosts", self.refresh)
        self.assertNotIn(
            'docker exec "$PARKER_CONTAINER" /usr/local/libexec/hermes-processing-v1-entrypoint',
            self.refresh,
        )

    def test_processing_credentials_are_distinct_and_read_only(self):
        self.assertIn(
            "/mnt/parker-secrets/parker/parker_hermes_processing_ed25519:/home/steve/.ssh/parker_hermes_processing_ed25519:ro",
            self.compose,
        )
        self.assertIn(
            "/mnt/parker-secrets/parker/parker_hermes_processing_known_hosts:/home/steve/.ssh/parker_hermes_processing_known_hosts:ro",
            self.compose,
        )
        self.assertNotIn("parker_hermes_stt_ed25519:/home/steve/.ssh/parker_hermes_processing", self.compose)
        self.assertNotIn("parker_hermes_analysis_ed25519:/home/steve/.ssh/parker_hermes_processing", self.compose)

    def test_routing_is_explicitly_disabled_by_default(self):
        self.assertIn('HERMES_PROCESSING_V1_ENABLED: "${HERMES_PROCESSING_V1_ENABLED:-false}"', self.compose)
        self.assertIn("availability never", self.compose)
        self.assertIn("enables routing", self.compose)

    def test_launcher_has_fixed_root_owned_configuration_boundary(self):
        self.assertIn('CONFIG="/etc/hermes-processing-v1/hermes-processing-v1.env"', self.launcher)
        self.assertIn('HERMES_PROCESSING_V1_PRINCIPAL:-}', self.launcher)
        self.assertIn('parker-hermes-processing', self.env)
        self.assertIn('/var/lib/hermes-processing-v1/ledger', self.env)
        self.assertIn('/var/lib/hermes-processing-v1/workspace', self.env)
        self.assertIn('env -i', self.launcher)

    def test_authorized_key_stays_fixed_and_restricted(self):
        self.assertIn('command="/usr/local/libexec/hermes-processing-v1-entrypoint"', self.authorized_keys)
        for option in ("no-port-forwarding", "no-agent-forwarding", "no-X11-forwarding", "no-pty"):
            self.assertIn(option, self.authorized_keys)


if __name__ == "__main__":
    unittest.main()
