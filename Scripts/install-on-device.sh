#!/bin/bash
# Build a signed Debug build and install it on the connected iPhone.
#
# Needs: the phone plugged in and unlocked, and an Apple account signed in
# under Xcode → Settings → Accounts (provisioning profiles are created there).
set -euo pipefail
cd "$(dirname "$0")/.."

JSON=$(mktemp -t reckon-devices)
trap 'rm -f "$JSON"' EXIT
# devicectl cannot write its JSON to a pipe, so it goes to a real file.
xcrun devicectl list devices --json-output "$JSON" >/dev/null 2>&1 || true

read -r DEVICE_ID DEVICE_NAME TUNNEL < <(python3 - "$JSON" <<'PY'
import json, sys
try:
    devices = json.load(open(sys.argv[1]))["result"]["devices"]
except Exception:
    devices = []
for d in devices:
    if d.get("hardwareProperties", {}).get("platform") != "iOS":
        continue
    conn = d.get("connectionProperties", {})
    print(d["identifier"],
          (d.get("deviceProperties", {}).get("name") or "iPhone").replace(" ", "_"),
          conn.get("tunnelState", "unknown"))
    break
PY
)

if [ -z "${DEVICE_ID:-}" ]; then
    echo "No paired iPhone found. Plug it in, unlock it, and tap Trust."
    exit 1
fi

echo "Device: ${DEVICE_NAME//_/ } ($TUNNEL)"
if [ "$TUNNEL" = "disconnected" ]; then
    echo "  …not connected right now. Plug the phone in and unlock it, then re-run."
    echo "  (Wi-Fi installs work too if you ticked 'Connect via network' in Xcode → Devices.)"
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
echo "  iPhone → Settings → General → VPN & Device Management → trust your certificate."
