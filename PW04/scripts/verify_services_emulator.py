"""Verify unavailable Google Play services only on a disposable Android emulator."""
import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
PKG = "ua.kpi.energyforecast.pw04"
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--adb", required=True)
p.add_argument("--serial", required=True)
a = p.parse_args()
if not a.serial.startswith("emulator-"): raise SystemExit("Emulator serial required; physical devices are refused")
def adb(*cmd, check=True):
    return subprocess.run([a.adb, "-s", a.serial, *cmd], capture_output=True, check=check, timeout=150).stdout
for _ in range(60):
    if adb("shell", "getprop", "sys.boot_completed", check=False).strip() == b"1": break
    time.sleep(1)
else: raise SystemExit("Emulator not booted")
if adb("shell", "getprop", "ro.kernel.qemu").strip() != b"1": raise SystemExit("Not an emulator")
private = ROOT / "build/verification/final"
private.mkdir(parents=True, exist_ok=True)
original_disabled = b"package:com.google.android.gms" in adb("shell", "pm", "list", "packages", "-d", "com.google.android.gms")
try:
    for apk in ["app/build/outputs/apk/debug/app-debug.apk", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
        adb("install", "-r", str(ROOT / apk))
    adb("shell", "pm", "disable-user", "--user", "0", "com.google.android.gms")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    result = adb("shell", "am", "instrument", "-w", "-r", "-e", "listener", PKG + ".KeepAwakeListener",
                 "-e", "verificationHostSetup", "true", "-e", "class", PKG + ".ServiceUnavailableTest",
                 PKG + ".test/androidx.test.runner.AndroidJUnitRunner")
    (private / "services-unavailable-runner.txt").write_bytes(result)
    passed = b"OK (1 test)" in result and b"FAILURES" not in result and not re.search(rb"INSTRUMENTATION_STATUS_CODE:\s*-(?:3|4)\b", result)
    dest = ROOT / "docs/evidence/final-checks/services-unavailable-emulator"
    dest.mkdir(parents=True, exist_ok=True)
    files = []
    for name in ["60-play-services-unavailable", "61-services-unavailable-details-work"]:
        data = adb("exec-out", "run-as", PKG, "cat", "files/pw04-final-evidence/" + name + ".png", check=False)
        if not data.startswith(b"\x89PNG\r\n\x1a\n"): continue
        (dest / (name + ".png")).write_bytes(data)
        files.append({"file": name + ".png", "sha256": hashlib.sha256(data).hexdigest()})
    meta = {"captured_at_utc": datetime.now(timezone.utc).isoformat(), "passed": passed,
            "device": "Pixel_6 AVD", "api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(),
            "files": files}
finally:
    if not original_disabled: adb("shell", "pm", "enable", "--user", "0", "com.google.android.gms")
    restored = original_disabled == (b"package:com.google.android.gms" in adb("shell", "pm", "list", "packages", "-d", "com.google.android.gms"))
if 'meta' in globals():
    meta["services_state_restored"] = restored
    (dest / "manifest.json").write_text(json.dumps(meta, indent=2) + "\n")
print("Unavailable-services test passed:", passed, "; restored:", restored)
if not passed or not restored: raise SystemExit("Inspect private services runner log")
