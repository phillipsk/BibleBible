#!/usr/bin/env python3
"""
Verifies that no secret values from local.properties appear in tracked Git files
or build outputs (checked in both plaintext and legacy 0x5A XOR-masked forms).
Never prints secret values.
"""
import argparse
import subprocess
import sys
from pathlib import Path

SENSITIVE_KEYS = {
    "IQ_BIBLE_API_KEY",
    "api_key_api_bible",
    "GEMINI_API_KEY",
    "OPENAI_API_KEY",
    "ANDROID_RELEASE_STORE_PASSWORD",
    "ANDROID_RELEASE_KEY_PASSWORD",
}


def load_secrets(props_path: Path) -> dict[str, list[bytes]]:
    patterns: dict[str, list[bytes]] = {}
    if not props_path.is_file():
        return patterns
    for line in props_path.read_text(encoding="utf-8", errors="ignore").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, val = stripped.split("=", 1)
        key = key.strip()
        val = val.strip()
        if key in SENSITIVE_KEYS and len(val) >= 6:
            plain_bytes = val.encode("utf-8")
            xor_bytes = "".join(chr(ord(c) ^ 0x5A) for c in val).encode("utf-8")
            patterns[key] = [plain_bytes, xor_bytes]
    return patterns


def get_tracked_files(repo_root: Path) -> list[Path]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=repo_root,
        capture_output=True,
        text=True,
        check=True,
    )
    return [
        repo_root / p
        for p in result.stdout.splitlines()
        if p.strip() and (repo_root / p).is_file()
    ]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--properties", default="local.properties")
    parser.add_argument("extra_paths", nargs="*")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    props_path = (repo_root / args.properties).resolve()
    patterns = load_secrets(props_path)

    targets = get_tracked_files(repo_root)
    for extra in args.extra_paths:
        p = Path(extra).resolve()
        if p.is_file():
            targets.append(p)

    leaked = []
    for target in targets:
        try:
            data = target.read_bytes()
        except OSError:
            continue
        for key_name, variants in patterns.items():
            for idx, needle in enumerate(variants):
                if needle and needle in data:
                    kind = "plaintext" if idx == 0 else "xor-0x5A"
                    leaked.append((target.relative_to(repo_root), key_name, kind))

    if leaked:
        for rel_path, key_name, kind in leaked:
            print(f"FAIL: {key_name} ({kind}) found in {rel_path}", file=sys.stderr)
        return 1

    print(
        f"OK: Scanned {len(targets)} files against {len(patterns)} local.properties secret keys; zero leaks found."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
