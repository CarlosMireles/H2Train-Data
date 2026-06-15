import hashlib
import json
import tempfile
import unittest
from datetime import date
from pathlib import Path

import generate_demo_data


class GenerateDemoDataTest(unittest.TestCase):

    def setUp(self):
        self.start = date(2026, 1, 1)
        self.end = date(2026, 6, 1)
        self.seed = 20260615

    def test_generated_events_match_documented_scenarios(self):
        events = generate_demo_data.generate_events(self.start, self.end, self.seed)

        self.assertGreater(len(events), 5000)
        self.assertEqual(
            generate_demo_data.EXPECTED_MATCHES,
            generate_demo_data.actual_matches(events),
        )
        self.assertEqual(12, len({event["userId"] for event in events}))
        self.assertTrue(all(
            not event["userId"].startswith("demo-")
            for event in events
        ))
        self.assertIn("HeartRate", {event["eventName"] for event in events})
        self.assertIn("Nutrition", {event["eventName"] for event in events})
        self.assertIn("BloodGlucose", {event["eventName"] for event in events})

    def test_writing_is_idempotent_and_creates_manifest(self):
        events = generate_demo_data.generate_events(self.start, self.end, self.seed)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "datalake"

            first_hash = self.write_and_hash(root, events)
            second_hash = self.write_and_hash(root, events)

            self.assertEqual(first_hash, second_hash)
            manifest = json.loads(
                (Path(temporary) / "demo" / "manifest.json").read_text(encoding="utf-8")
            )
            self.assertEqual(12, manifest["subjectCount"])
            self.assertEqual(len(events), manifest["eventCount"])
            self.assertEqual(
                generate_demo_data.EXPECTED_MATCHES,
                manifest["expectedMatches"],
            )

    def write_and_hash(self, root: Path, events: list[dict]) -> str:
        generate_demo_data.reset_demo_data(root)
        generate_demo_data.write_events(root, events)
        generate_demo_data.write_manifest(root, events, self.start, self.end, self.seed)
        digest = hashlib.sha256()
        for event_file in sorted((root / "events").rglob("events.jsonl")):
            digest.update(event_file.read_bytes())
        return digest.hexdigest()


if __name__ == "__main__":
    unittest.main()
