#!/bin/bash
# Build iOS aplikacije za simulator (bez potpisa) — poziva ga .github/workflows/ios.yml.
# Ako build padne, greške se ispisuju kao GitHub "annotations" (vidljive na stranici builda).
set -o pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew

(cd iosApp && xcodegen generate) || exit 1

xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES \
  CODE_SIGNING_ALLOWED=NO \
  build 2>&1 | tee build.log
STATUS=${PIPESTATUS[0]}

if [ "$STATUS" -ne 0 ]; then
  # Kotlin (e: ...), Swift/Xcode (error:), Gradle (What went wrong) — spojeno u jednu poruku
  PORUKA=$( { grep -E "^e: |error: |\*\* BUILD FAILED" build.log; grep -A8 "What went wrong" build.log; } \
    | sed 's|file://[^ ]*/composeApp/src/|src/|g' | awk '!seen[$0]++' | head -80 | sed ':a;N;$!ba;s/%/%25/g;s/\n/%0A/g')
  echo "::error title=Build greske::${PORUKA}"
  # Isto i u sažetak builda (vidljiv na stranici builda bez prijave)
  if [ -n "$GITHUB_STEP_SUMMARY" ]; then
    { echo "### Build greske"; echo '```'; printf '%b\n' "$(echo "$PORUKA" | sed 's/%0A/\\n/g;s/%25/%/g')"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
  fi
fi
exit "$STATUS"
