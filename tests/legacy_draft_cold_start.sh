#!/usr/bin/env bash
# Run on the disposable CI emulator after the regular instrumentation suite.
set -euo pipefail
apk=app/build/outputs/apk/debug/app-debug.apk
tests=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
badging=$("$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk")
package=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<< "$badging")
[[ "$package" =~ ^de\.naehe\.app\.gartenkueche\.lab[0-9]+$ ]]
adb install -r "$apk"
adb install -r "$tests"
adb shell am start -W -n "$package/de.naehe.gartenkueche.MainActivity"
adb shell pidof "$package" >/dev/null
adb shell run-as "$package" mkdir -p files
# Preserve any existing draft; this test requires fresh private app storage.
adb shell run-as "$package" test ! -e files/atelier_draft_v1.gz
adb shell -T run-as "$package" tee files/atelier_draft_v1.gz < tests/fixtures/atelier-lab10.gz >/dev/null
adb shell am force-stop "$package"
if adb shell pidof "$package" >/dev/null; then
  echo "The previous app process is still running." >&2
  exit 1
fi
adb shell am instrument -w -r -e class de.naehe.gartenkueche.LegacyDraftColdStartTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee app/build/legacy-cold-start.txt
grep -Eq '^OK \(1 tests?\)' app/build/legacy-cold-start.txt
