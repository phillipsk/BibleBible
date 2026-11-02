#!/usr/bin/env python3
"""Upload an Android App Bundle through a transactional Play edit."""

import argparse
import json
import os
import pathlib
import urllib.error
import urllib.request


def request(url, token, method="GET", payload=None, content_type="application/json"):
    data = payload
    if isinstance(payload, dict):
        data = json.dumps(payload).encode()
    call = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Authorization": f"Bearer {token}", "Content-Type": content_type},
    )
    with urllib.request.urlopen(call, timeout=120) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--package", required=True)
    parser.add_argument("--bundle", required=True, type=pathlib.Path)
    parser.add_argument("--track", default="internal")
    parser.add_argument("--release-name", required=True)
    args = parser.parse_args()
    token = os.environ["PLAY_ACCESS_TOKEN"]
    api = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{args.package}"
    edit = request(f"{api}/edits", token, method="POST", payload={})["id"]
    upload = (
        f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/"
        f"{args.package}/edits/{edit}/bundles?uploadType=media"
    )
    bundle = request(upload, token, method="POST", payload=args.bundle.read_bytes(),
                     content_type="application/octet-stream")
    version_code = str(bundle["versionCode"])
    request(
        f"{api}/edits/{edit}/tracks/{args.track}", token, method="PUT",
        payload={"track": args.track, "releases": [{
            "name": args.release_name,
            "status": "completed",
            "versionCodes": [version_code],
        }]},
    )
    request(f"{api}/edits/{edit}:commit", token, method="POST", payload={})
    print(f"Uploaded version code {version_code} to the {args.track} track.")


if __name__ == "__main__":
    try:
        main()
    except urllib.error.HTTPError as error:
        # Keep OAuth tokens and response bodies out of CI logs.
        raise SystemExit(f"Google Play API returned HTTP {error.code}.")
