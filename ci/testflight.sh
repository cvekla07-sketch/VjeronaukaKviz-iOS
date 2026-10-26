#!/bin/bash
# Release build (iPhone), potpis i slanje na TestFlight / App Store Connect.
# Poziva ga .github/workflows/ios.yml (job "testflight", ručno pokretanje).
# Potrebne GitHub tajne: APPLE_TEAM_ID, ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8
set -o pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew

greske() {
  PORUKA=$( { grep -E "^e: |error: |\*\* (ARCHIVE|EXPORT) FAILED|Error Domain|ERROR ITMS|IDEDistribution|No profiles|Provisioning" "$1"; grep -A8 "What went wrong" "$1"; } \
    | sed 's|file://[^ ]*/composeApp/src/|src/|g' | awk '!seen[$0]++' | head -80 | sed ':a;N;$!ba;s/%/%25/g;s/\n/%0A/g')
  echo "::error title=TestFlight greske::${PORUKA}"
}

for v in APPLE_TEAM_ID ASC_KEY_ID ASC_ISSUER_ID ASC_KEY_P8; do
  if [ -z "${!v}" ]; then echo "::error title=TestFlight greske::Nedostaje GitHub tajna $v"; exit 1; fi
done

KLJUC="$RUNNER_TEMP/AuthKey_${ASC_KEY_ID}.p8"
printf '%s\n' "$ASC_KEY_P8" > "$KLJUC"
AUTH=(-allowProvisioningUpdates -authenticationKeyPath "$KLJUC" -authenticationKeyID "$ASC_KEY_ID" -authenticationKeyIssuerID "$ASC_ISSUER_ID")
BROJ="${BUILD_BROJ:-1}"

(cd iosApp && xcodegen generate) || exit 1

xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/VjeronaukaKviz.xcarchive \
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
  CURRENT_PROJECT_VERSION="$BROJ" \
  "${AUTH[@]}" 2>&1 | tee archive.log
[ "${PIPESTATUS[0]}" -eq 0 ] || { greske archive.log; exit 1; }

cat > build/ExportOptions.plist <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key><string>app-store-connect</string>
  <key>destination</key><string>upload</string>
  <key>teamID</key><string>${APPLE_TEAM_ID}</string>
  <key>signingStyle</key><string>automatic</string>
  <key>uploadSymbols</key><true/>
  <key>manageAppVersionAndBuildNumber</key><false/>
</dict>
</plist>
PLIST

xcodebuild -exportArchive \
  -archivePath build/VjeronaukaKviz.xcarchive \
  -exportOptionsPlist build/ExportOptions.plist \
  -exportPath build/export \
  "${AUTH[@]}" 2>&1 | tee export.log
[ "${PIPESTATUS[0]}" -eq 0 ] || { greske export.log; exit 1; }

echo "::notice title=TestFlight::Build $BROJ poslan na App Store Connect"
rm -f "$KLJUC"
