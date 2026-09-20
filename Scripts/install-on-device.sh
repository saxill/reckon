#!/bin/bash
# Build a signed Debug build and install it on the connected iPhone.
#
# Needs: the phone plugged in (or paired over Wi-Fi) and unlocked, and an Apple
# account signed in under Xcode → Settings → Accounts.
set -euo pipefail
cd "$(dirname "$0")/.."

DEVICE_ID=$(xcrun devicectl list devices --json-output /dev/stdout 2>/dev/null | python3 -c '
import json, sys
try:
    devices = json.load(sys.stdin)["result"]["devices"]
except Exception:
    sys.exit(0)
for d in devices:
    props = d.get("deviceProperties", {})
    hardware = d.get("hardwareProperties", {})
    if "iPhone" in hardware.get("deviceType", "") or "iPhone" in props.get("name", ""):
        print(d["identifier"])
        break
')

if [ -z "${DEVICE_ID}" ]; then
    echo "No paired iPhone found. Plug it in, unlock it, and trust this Mac."
    exit 1
fi

echo "Building for device…"
xcodebuild -project Reckon.xcodeproj -scheme Reckon -configuration Debug \
    -destination "generic/platform=iOS" \
    -allowProvisioningUpdates \
    -derivedDataPath .build-device build

echo "Installing…"
xcrun devicectl device install app --device "$DEVICE_ID" \
    .build-device/Build/Products/Debug-iphoneos/Reckon.app

echo
echo "Installed. On a free Apple ID the first launch needs trusting:"
echo "  iPhone → Settings → General → VPN & Device Management → trust your developer certificate."
