#!/bin/bash
# Release build (iPhone), potpis i slanje na TestFlight / App Store Connect.
# Poziva ga .github/workflows/ios.yml (job "testflight", ručno pokretanje).
# Potrebne GitHub tajne: APPLE_TEAM_ID, ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8
set -o pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew

greske() {
  PORUKA=$( { grep -E "^e: |error: |\*\* (ARCHIVE|EXPORT) FAILED|Error Domain|ERROR ITMS|IDEDistribution|No profiles|No Account|requires a" "$1" | grep -v "^ */Applications/Xcode"; grep -A8 "What went wrong" "$1"; echo "--- kraj loga ---"; tail -40 "$1"; } \
    | sed 's|file://[^ ]*/composeApp/src/|src/|g' | awk '!seen[$0]++' | head -140 | sed ':a;N;$!ba;s/%/%25/g;s/\n/%0A/g')
  echo "::error title=TestFlight greske::${PORUKA}"
  # Isto i u sažetak builda (vidljiv na stranici builda bez prijave)
  if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    { echo "### TestFlight greske"; echo '```'; printf '%b\n' "$(echo "$PORUKA" | sed 's/%0A/\\n/g;s/%25/%/g')"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
  fi
}

for v in APPLE_TEAM_ID ASC_KEY_ID ASC_ISSUER_ID ASC_KEY_P8; do
  if [ -z "${!v}" ]; then echo "::error title=TestFlight greske::Nedostaje GitHub tajna $v"; exit 1; fi
done

ASC_KEY_ID=$(printf '%s' "$ASC_KEY_ID" | tr -d ' \r\n\t')
ASC_ISSUER_ID=$(printf '%s' "$ASC_ISSUER_ID" | tr -d ' \r\n\t')
APPLE_TEAM_ID=$(printf '%s' "$APPLE_TEAM_ID" | tr -d ' \r\n\t')
KLJUC="$RUNNER_TEMP/AuthKey_${ASC_KEY_ID}.p8"
# Ključ se normalizuje (Windows CRLF, razmaci, sve u jednom redu…) u ispravan PEM format
TIJELO=$(printf '%s' "$ASC_KEY_P8" | tr -d '\r' | sed -e 's/-----BEGIN PRIVATE KEY-----//' -e 's/-----END PRIVATE KEY-----//' | tr -d ' \n\t')
{ echo "-----BEGIN PRIVATE KEY-----"; echo "$TIJELO" | fold -w 64; echo "-----END PRIVATE KEY-----"; } > "$KLJUC"
if [ ${#TIJELO} -lt 150 ]; then echo "::error title=TestFlight greske::ASC_KEY_P8 izgleda prekratak (${#TIJELO} znakova) — provjeri da je kopiran cijeli .p8 fajl"; exit 1; fi
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
