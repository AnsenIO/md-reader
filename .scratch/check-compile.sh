#!/usr/bin/env bash
# Local compile-only check for the app's Java sources (gx10 aarch64 can't run AAPT2).
set -u
cd "$(dirname "$0")/.."
JDK=/home/ansen/tools/jdk-17.0.12+7
SDK=/home/ansen/android-sdk/platforms/android-34/android.jar
OUT=/tmp/mdr-javac-check
rm -rf "$OUT"; mkdir -p "$OUT"

# androidx/cordova jars + aar-extracted classes from the gradle cache
CP=$(find /home/ansen/.gradle/caches/modules-2/files-2.1 \( -name "*.jar" \) 2>/dev/null | grep -viE "sources|javadoc" | tr '\n' ':')
AARS=$(find /home/ansen/.gradle/caches/modules-2/files-2.1 -name "*.aar" 2>/dev/null)
for aar in $AARS; do
    unzip -o -q "$aar" classes.jar -d "$OUT/aar-$(echo "$aar" | md5sum | cut -c1-8)" 2>/dev/null && \
        CP="$CP$OUT/aar-$(echo "$aar" | md5sum | cut -c1-8)/classes.jar:"
done

# Capacitor's R class is generated at build time — stub it so BridgeActivity compiles.
mkdir -p "$OUT/stub/com/getcapacitor/android"
cat > "$OUT/stub/com/getcapacitor/android/R.java" <<'EOF'
package com.getcapacitor.android;
public final class R {
    public static final class style { public static int AppTheme_NoActionBar = 0x7f0a0001; }
    public static final class layout { public static int bridge_layout_main = 0x7f0b0001; public static int fragment_bridge = 0x7f0b0002; }
    public static final class styleable { public static int[] bridge_fragment = new int[0]; public static int bridge_fragment_start_dir = 0x0; }
    public static final class id { public static int webview = 0x7f080001; }
}
EOF

"$JDK/bin/javac" --release 17 -nowarn -cp "$SDK:$CP" -d "$OUT/classes" \
  $(find node_modules/@capacitor/android/capacitor/src/main/java -name "*.java") \
  $(find node_modules/@capacitor/filesystem/android/src/main/java -name "*.java") \
  android/app/src/main/java/com/squadmdreader/app/MainActivity.java \
  android/app/src/main/java/com/squadmdreader/app/FileOpenPlugin.java "$OUT/stub/com/getcapacitor/android/R.java" 2>&1 | grep -vE "^Note:|uses unchecked|Recompile with" | head -40
echo "javac exit: ${PIPESTATUS[0]}"
