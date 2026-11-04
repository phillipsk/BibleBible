#!/usr/bin/env python3
"""Exchange a Google service-account JWT for a short-lived Play access token."""

import argparse
import base64
import json
import os
import pathlib
import subprocess
import tempfile
import time
import urllib.parse
import urllib.request


def encoded(value):
    raw = json.dumps(value, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).rstrip(b"=")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--credentials-file", type=pathlib.Path)
    args = parser.parse_args()
    if args.credentials_file:
        credential = json.loads(args.credentials_file.read_text())
    else:
        raw = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
        if not raw:
            raise SystemExit("Missing GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
        credential = json.loads(raw)

    now = int(time.time())
    header = encoded({"alg": "RS256", "typ": "JWT"})
    claims = encoded({
        "iss": credential["client_email"],
        "scope": "https://www.googleapis.com/auth/androidpublisher",
        "aud": credential["token_uri"],
        "iat": now,
        "exp": now + 3600,
    })
    unsigned = header + b"." + claims
    with tempfile.NamedTemporaryFile(mode="w", delete=False) as key_file:
        key_file.write(credential["private_key"])
        key_path = pathlib.Path(key_file.name)
    key_path.chmod(0o600)
    try:
        signature = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_path)],
            input=unsigned, check=True, capture_output=True,
        ).stdout
    finally:
        key_path.unlink(missing_ok=True)
    assertion = unsigned + b"." + base64.urlsafe_b64encode(signature).rstrip(b"=")
    payload = urllib.parse.urlencode({
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion.decode(),
    }).encode()
    request = urllib.request.Request(credential["token_uri"], data=payload, method="POST")
    with urllib.request.urlopen(request, timeout=60) as response:
        token = json.load(response)["access_token"]
    env_file = os.environ.get("GITHUB_ENV")
    if env_file:
        print(f"::add-mask::{token}")
        with pathlib.Path(env_file).open("a", encoding="utf-8") as output:
            output.write(f"PLAY_ACCESS_TOKEN={token}\n")
        print("Authenticated to Google Play.")
    else:
        print("Google service-account token exchange succeeded.")


if __name__ == "__main__":
    main()
