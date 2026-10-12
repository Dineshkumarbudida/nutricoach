#!/usr/bin/env python3
"""Read-only PubMed surveillance for NutriCoach evidence maintenance.

This script discovers newly indexed papers and creates an evidence inbox. It does
not edit target rules, food data, or recommendations. Any rule change requires a
separate reviewed pull request with tests and a version increment.
"""
from __future__ import annotations

import argparse
import json
import time
import urllib.parse
import urllib.request
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Any

BASE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
USER_AGENT = "NutriCoachEvidenceWatch/1.0 (+https://github.com/Dineshkumarbudida/nutricoach)"

TOPICS = {
    "energy-requirements": (
        '("energy requirement"[Title/Abstract] OR "energy expenditure"[Title/Abstract]) '
        'AND (adult[Title/Abstract] OR adults[Title/Abstract]) '
        'AND (nutrition[Title/Abstract] OR diet[Title/Abstract])'
    ),
    "protein-and-exercise": (
        '(protein[Title/Abstract] AND (resistance training[Title/Abstract] OR exercise[Title/Abstract])) '
        'AND (systematic review[Publication Type] OR meta-analysis[Publication Type] OR guideline[Publication Type])'
    ),
    "dietary-fiber": (
        '(dietary fiber[Title/Abstract] OR dietary fibre[Title/Abstract]) '
        'AND (systematic review[Publication Type] OR meta-analysis[Publication Type] OR guideline[Publication Type])'
    ),
    "micronutrient-reference-values": (
        '(micronutrient*[Title/Abstract] OR vitamin*[Title/Abstract] OR mineral*[Title/Abstract]) '
        'AND (dietary reference intake*[Title/Abstract] OR recommended dietary allowance*[Title/Abstract] '
        'OR adequate intake[Title/Abstract] OR guideline[Title/Abstract])'
    ),
    "food-composition-data": (
        '(food composition[Title/Abstract] OR nutrient database[Title/Abstract]) '
        'AND (validation[Title/Abstract] OR database[Title/Abstract] OR analytical[Title/Abstract])'
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--days", type=int, default=7)
    parser.add_argument("--retmax", type=int, default=50)
    parser.add_argument("--output", default="evidence-watch.json")
    parser.add_argument("--today", help="Override UTC date for a reproducible test, YYYY-MM-DD")
    return parser.parse_args()


def fetch_json(endpoint: str, params: dict[str, str], attempts: int = 3) -> dict[str, Any]:
    url = f"{BASE}/{endpoint}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    last_error: Exception | None = None
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.load(response)
        except Exception as exc:
            last_error = exc
            if attempt + 1 < attempts:
                time.sleep(1.5 * (attempt + 1))
    assert last_error is not None
    raise last_error


def parse_summary(summary: dict[str, Any], ids: list[str]) -> list[dict[str, Any]]:
    result = summary.get("result", {})
    papers: list[dict[str, Any]] = []
    for pmid in ids:
        item = result.get(pmid)
        if not isinstance(item, dict):
            continue
        article_ids = item.get("articleids", [])
        doi = next(
            (
                entry.get("value")
                for entry in article_ids
                if isinstance(entry, dict) and entry.get("idtype") == "doi"
            ),
            None,
        )
        authors = [
            author.get("name")
            for author in item.get("authors", [])
            if isinstance(author, dict) and author.get("name")
        ]
        papers.append(
            {
                "pmid": pmid,
                "doi": doi,
                "title": (item.get("title") or "").strip(),
                "journal": item.get("fulljournalname") or item.get("source") or "",
                "publicationDate": item.get("pubdate") or "",
                "publicationTypes": item.get("pubtype") or [],
                "authors": authors,
                "pubmedUrl": f"https://pubmed.ncbi.nlm.nih.gov/{pmid}/",
                "reviewStatus": "UNREVIEWED",
                "ruleChangeAllowed": False,
            }
        )
    return papers


def build_report(today: date, days: int, retmax: int) -> dict[str, Any]:
    if days < 1 or days > 90:
        raise ValueError("--days must be between 1 and 90")
    if retmax < 1 or retmax > 200:
        raise ValueError("--retmax must be between 1 and 200")

    start = today - timedelta(days=days)
    topic_results: list[dict[str, Any]] = []

    for topic, query in TOPICS.items():
        search = fetch_json(
            "esearch.fcgi",
            {
                "db": "pubmed",
                "retmode": "json",
                "sort": "pub date",
                "retmax": str(retmax),
                "datetype": "edat",
                "mindate": start.isoformat(),
                "maxdate": today.isoformat(),
                "term": query,
            },
        )
        search_result = search.get("esearchresult", {})
        ids = [str(identifier) for identifier in search_result.get("idlist", [])]
        papers: list[dict[str, Any]] = []
        if ids:
            time.sleep(0.4)
            summary = fetch_json(
                "esummary.fcgi",
                {"db": "pubmed", "retmode": "json", "id": ",".join(ids)},
            )
            papers = parse_summary(summary, ids)
        topic_results.append(
            {
                "topic": topic,
                "query": query,
                "reportedCount": int(search_result.get("count", 0)),
                "returnedCount": len(papers),
                "papers": papers,
            }
        )
        time.sleep(0.4)

    return {
        "watchVersion": "pubmed-watch/1.0.0",
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "window": {"start": start.isoformat(), "end": today.isoformat(), "days": days},
        "source": {
            "name": "PubMed via NCBI E-utilities",
            "documentation": "https://www.ncbi.nlm.nih.gov/books/NBK25501/",
        },
        "policy": {
            "purpose": "Literature surveillance only",
            "automaticRuleChanges": False,
            "requiredNextStep": (
                "A qualified reviewer must assess population, methods, risk of bias, consistency with the wider "
                "evidence, and applicability before proposing a versioned rule change."
            ),
        },
        "topics": topic_results,
    }


def main() -> int:
    args = parse_args()
    today = date.fromisoformat(args.today) if args.today else datetime.now(timezone.utc).date()
    report = build_report(today, args.days, args.retmax)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    total = sum(topic["returnedCount"] for topic in report["topics"])
    print(f"Evidence watch collected {total} PubMed records into {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
