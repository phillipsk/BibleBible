#!/bin/bash

set -e

WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK_PATH=${JDK_PATH:-"/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home/bin"}
APP_NAME=${APP_NAME:-"BibleBible"}
VERSION=${VERSION:-"1.0"}
LICENSE_FILE="$WORKING_DIR/../LICENSE.md"
MAC_DEVELOPER_ID=${MAC_DEVELOPER_ID:-"SetYourDeveloperIDHere"}
EXPECTED_FINGERPRINT=${EXPECTED_FINGERPRINT:-"SetExpectedFingerprintHere"}

INPUT_DIR="$WORKING_DIR/build/libs"
MAIN_JAR="$INPUT_DIR/desktopApp-jvm.jar"
DEST_DIR="$WORKING_DIR/build/compose/binaries/main/pkg"
RESOURCES_DIR="$WORKING_DIR/build/compose/tmp/resources"
ENTITLEMENTS_FILE="$WORKING_DIR/build/compose/default-resources/1.6.1/default-entitlements.plist"

function check_command() {
    command -v "$1" >/dev/null 2>&1 || { echo "$1 not found! Please install it. Aborting."; exit 1; }
}

echo "Checking dependencies..."
check_command "$JDK_PATH/jpackage"
check_command productsign
check_command pkgutil
echo "All dependencies are installed."

echo "Starting the packaging process for $APP_NAME..."
"$JDK_PATH/jpackage" \
    --type pkg \
    --input "$INPUT_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "email.kevinphillips.biblebible.BibleBibleKt" \
    --dest "$DEST_DIR" \
    --name "$APP_NAME" \
    --license-file "$LICENSE_FILE" \
    --resource-dir "$RESOURCES_DIR" \
    --description "Daily Bible Reading App" \
    --copyright "© 2025 Kevin Phillips" \
    --vendor "Kevin Phillips" \
    --mac-package-identifier "email.kevinphillips.biblebible" \
    --mac-app-category "public.app-category.reference" \
    --mac-entitlements "$ENTITLEMENTS_FILE"
echo "Package created successfully."

SIGNED_PKG="$DEST_DIR/${APP_NAME}-${VERSION}-signed.pkg"
UNSIGNED_PKG="$DEST_DIR/${APP_NAME}-${VERSION}.pkg"

echo "Signing the package..."
productsign --sign "$MAC_DEVELOPER_ID" "$UNSIGNED_PKG" "$SIGNED_PKG" >/dev/null
echo "Package signed successfully."

echo "Verifying the package signature..."
FULL_CERT_INFO=$(pkgutil --check-signature "$SIGNED_PKG")
echo "Full pkgutil output:"
echo "$FULL_CERT_INFO"

# Extract the first SHA256 fingerprint (corresponding to the signing certificate)
CERT_INFO=$(echo "$FULL_CERT_INFO" | awk '/SHA256 Fingerprint:/ {getline; print}' | head -n 1 | tr -d ' ')
EXPECTED_FINGERPRINT_NO_SPACES=$(echo "$EXPECTED_FINGERPRINT" | tr -d ':')

echo "CERT_INFO: $CERT_INFO"
echo "EXPECTED_FINGERPRINT_NO_SPACES: $EXPECTED_FINGERPRINT_NO_SPACES"

# check is failing here
if [ "$CERT_INFO" == "$EXPECTED_FINGERPRINT_NO_SPACES" ]; then
    echo "Signature verified successfully. Fingerprint matches expected value."
else
    echo "Signature verification failed. Fingerprint does not match expected value!"
    echo "Expected: $EXPECTED_FINGERPRINT"
    echo "Found: $CERT_INFO"
    exit 1
fi

echo "Signature verification complete. Process finished successfully!"