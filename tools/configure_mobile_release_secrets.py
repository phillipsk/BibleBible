#!/usr/bin/env python3
"""Install mobile release credentials as GitHub Actions repository secrets."""

import argparse
import base64
import json
import os
import pathlib
import subprocess


def set_secret(repo: str, name: str, value: bytes) -> None:
    subprocess.run(
        ["gh", "secret", "set", name, "--repo", repo],
        input=value,
        check=True,
        stdout=subprocess.DEVNULL,
    )
    print(f"Configured {name}")


def required_env(name: str) -> bytes:
    value = os.environ.get(name)
    if not value:
        raise SystemExit(f"Missing environment variable: {name}")
    return value.encode()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default="phillipsk/BibleBible")
    parser.add_argument("--signing-dir", required=True, type=pathlib.Path)
    parser.add_argument("--google-service-account", required=True, type=pathlib.Path)
    args = parser.parse_args()

    signing = args.signing_dir
    android = json.loads((signing / "android-signing.private.json").read_text())
    apple = json.loads((signing / "apple-signing.private.json").read_text())
    identifiers = json.loads((signing / "publishing-identifiers.json").read_text())

    keystore = signing / android["storeFile"]
    certificate = signing / apple["certificate"]
    private_key = signing / identifiers["apple_api_key_name"]
    secrets = {
        "ANDROID_UPLOAD_KEYSTORE_BASE64": base64.b64encode(keystore.read_bytes()),
        "ANDROID_UPLOAD_STORE_PASSWORD": android["storePassword"].encode(),
        "ANDROID_UPLOAD_KEY_ALIAS": android["keyAlias"].encode(),
        "ANDROID_UPLOAD_KEY_PASSWORD": android["keyPassword"].encode(),
        "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON": args.google_service_account.read_bytes(),
        "IOS_DISTRIBUTION_CERTIFICATE_BASE64": base64.b64encode(certificate.read_bytes()),
        "IOS_DISTRIBUTION_CERTIFICATE_PASSWORD": apple["exportPassword"].encode(),
        "ASC_PRIVATE_KEY": private_key.read_bytes(),
        "ASC_KEY_ID": identifiers["apple_api_key_id"].encode(),
        "ASC_ISSUER_ID": identifiers["apple_issuer_id"].encode(),
        "GEMINI_API_KEY": required_env("GEMINI_API_KEY"),
        "IQ_BIBLE_API_KEY": required_env("IQ_BIBLE_API_KEY"),
        "API_BIBLE_KEY": required_env("API_BIBLE_KEY"),
    }
    for name, value in secrets.items():
        set_secret(args.repo, name, value)


if __name__ == "__main__":
    main()
