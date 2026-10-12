from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("evidence_watch", ROOT / "scripts" / "evidence_watch.py")
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class EvidenceWatchTest(unittest.TestCase):
    def test_parse_summary_keeps_review_gate_closed(self) -> None:
        payload = {
            "result": {
                "uids": ["123"],
                "123": {
                    "title": "A systematic review",
                    "fulljournalname": "Test Journal",
                    "pubdate": "2026 Oct",
                    "pubtype": ["Meta-Analysis"],
                    "authors": [{"name": "Example A"}],
                    "articleids": [{"idtype": "doi", "value": "10.1/example"}],
                },
            }
        }
        papers = MODULE.parse_summary(payload, ["123"])
        self.assertEqual(1, len(papers))
        self.assertEqual("UNREVIEWED", papers[0]["reviewStatus"])
        self.assertFalse(papers[0]["ruleChangeAllowed"])
        self.assertEqual("10.1/example", papers[0]["doi"])

    def test_topic_queries_are_defined_and_nonempty(self) -> None:
        self.assertGreaterEqual(len(MODULE.TOPICS), 4)
        self.assertTrue(all(query.strip() for query in MODULE.TOPICS.values()))


if __name__ == "__main__":
    unittest.main()
