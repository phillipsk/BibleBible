#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

PKG_VERSION="${PKG_VERSION:-3.2.0}"
PKG_BUILD="${PKG_BUILD:-25}"
APP_NAME="${APP_NAME:-BibleBible}"
BUNDLE_ID="${BUNDLE_ID:-email.kevinphillips.biblebible}"

APP_DIR="$SCRIPT_DIR/build/compose/binaries/main-release/app/${APP_NAME}.app"
PKG_PATH="$SCRIPT_DIR/build/compose/binaries/main-release/pkg/${APP_NAME}-${PKG_VERSION}.pkg"

echo "Building signed Mac App Store package (${PKG_VERSION}, build ${PKG_BUILD})..."
cd "$ROOT_DIR"
./gradlew :desktopApp:packageReleasePkg -PMAC_APPSTORE_SIGN=true --console=plain --stacktrace

if [[ ! -d "$APP_DIR" ]]; then
  echo "Expected app bundle not found at: $APP_DIR" >&2
  exit 1
fi

if [[ ! -f "$PKG_PATH" ]]; then
  echo "Expected pkg archive not found at: $PKG_PATH" >&2
  exit 1
fi

echo "Verifying app bundle signature and entitlements..."
codesign --verify --deep --strict --verbose=2 "$APP_DIR"
codesign -d --entitlements - "$APP_DIR"

echo "Verifying pkg installer signature..."
pkgutil --check-signature "$PKG_PATH"

echo "Checking Info.plist metadata..."
/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" \
  -c "Print :CFBundleShortVersionString" \
  -c "Print :CFBundleVersion" \
  "$APP_DIR/Contents/Info.plist"

if [[ "${VALIDATE_APP:-false}" == "true" || "${UPLOAD_APP:-false}" == "true" ]]; then
  ASC_KEY_ID="${ASC_KEY_ID:?Set ASC_KEY_ID for App Store Connect validation/upload}"
  ASC_ISSUER_ID="${ASC_ISSUER_ID:?Set ASC_ISSUER_ID for App Store Connect validation/upload}"

  echo "Validating pkg with Apple App Store servers..."
  xcrun altool --validate-app \
    -f "$PKG_PATH" \
    -t macos \
    --apiKey "$ASC_KEY_ID" \
    --apiIssuer "$ASC_ISSUER_ID"

  if [[ "${UPLOAD_APP:-false}" == "true" ]]; then
    echo "Uploading pkg to App Store Connect / TestFlight..."
    xcrun altool --upload-app \
      -f "$PKG_PATH" \
      -t macos \
      --apiKey "$ASC_KEY_ID" \
      --apiIssuer "$ASC_ISSUER_ID"
  fi
fi

echo "Done: $PKG_PATH"
