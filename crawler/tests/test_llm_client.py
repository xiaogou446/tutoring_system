import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tutor_crawler.llm_client import RelayLlmClient


class RelayLlmClientConfigTest(unittest.TestCase):
    def test_should_load_config_from_project_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "llm_config.json"
            config_path.write_text(
                json.dumps(
                    {
                        "base_url": "https://relay.example.com/v1",
                        "api_key": "k-test",
                        "model": "m-test",
                        "timeout_seconds": 25,
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )

            client = RelayLlmClient(config_path=str(config_path))
            self.assertEqual(client.base_url, "https://relay.example.com/v1")
            self.assertEqual(client.api_key, "k-test")
            self.assertEqual(client.model, "m-test")
            self.assertEqual(client.timeout_seconds, 25)
            self.assertTrue(client.is_configured())

    def test_args_should_override_config_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "llm_config.json"
            config_path.write_text(
                json.dumps(
                    {
                        "base_url": "https://relay.example.com/v1",
                        "api_key": "k-file",
                        "model": "m-file",
                        "timeout_seconds": 25,
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )

            client = RelayLlmClient(
                config_path=str(config_path),
                api_key="k-arg",
                model="m-arg",
            )
            self.assertEqual(client.api_key, "k-arg")
            self.assertEqual(client.model, "m-arg")


if __name__ == "__main__":
    unittest.main()
