#!/usr/bin/env python3
"""Opt-in live contract check. Sends 9 read-only requests; never prints credentials."""
import json
import os
import re
import urllib.error
import urllib.request

host = os.environ.get("IQ_BIBLE_V2_HOST", "")
key = os.environ.get("IQ_BIBLE_API_KEY", "")
if not re.fullmatch(r"[a-z0-9-]+\.p\.rapidapi\.com", host) or not key:
    raise SystemExit("Set IQ_BIBLE_V2_HOST from the v2 listing and IQ_BIBLE_API_KEY in the environment.")


def get(path):
    request = urllib.request.Request("https://" + host + "/api/v2/" + path,
        headers={"X-RapidAPI-Host": host, "X-RapidAPI-Key": key, "Accept": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=25) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise SystemExit(f"Live verification blocked: HTTP {error.code} for {path}") from None
    except urllib.error.URLError:
        raise SystemExit("Live verification blocked by network access or connectivity.") from None


versions = ["eng_kjv", "eng_bbe", "spa_rv", "arb_svd", "eng_asv"]
catalog = {item["version_id"]: item for item in get("bibles")["data"]}
for version in versions:
    if version not in catalog:
        raise SystemExit(f"Required translation missing: {version}; verify its replacement before shipping.")
    chapter = get(f"bibles/{version}/GEN/1")
    assert chapter["version"] == version and chapter["data"]
    assert all(verse["chapter_number"] == 1 and verse["text"] for verse in chapter["data"])
    print(f"PASS {version}: {catalog[version]['title']} ({len(chapter['data'])} verses)")
meta = get("bibles/eng_kjv/GEN/meta")
assert meta["chapter_count"] == 50
for version in ("eng_kjv", "eng_bbe"):
    daily = get(f"bibles/{version}/verse-of-the-day")
    assert daily["version"] == version and daily["date"] and daily["data"]["book_usfm"] and daily["data"]["text"]
print("PASS chapter metadata and daily verses")
