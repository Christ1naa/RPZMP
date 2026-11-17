"""Collect original AVD screenshots from EvidenceCaptureTest; Python standard library only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "ua.kpi.energyforecast.pw04"
NAMES = ["01-monitor-live", "02-catalog-no-key", "03-filter-panel",
         "04-filtered-office", "05-details-top", "06-details-bottom", "07-empty-search"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default="emulator-5554")
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    sdk = Path(sdk) if sdk else Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk"
    parser.add_argument("--adb", default=str(sdk / "platform-tools/adb.exe"))
    args = parser.parse_args()

    def adb(*command):
        result = subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True, check=True)
        return result.stdout

    if adb("shell", "getprop", "sys.boot_completed").strip() != b"1":
        raise SystemExit("AVD is not booted")
    for apk in [ROOT / "app/build/outputs/apk/debug/app-debug.apk",
                ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
        if not apk.is_file():
            raise SystemExit(f"Build APK first: {apk}")
        adb("install", "-r", str(apk))
    output = adb("shell", "am", "instrument", "-w", "-r", "-e", "class",
                 f"{PACKAGE}.EvidenceCaptureTest", f"{PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner")
    log = ROOT / "build/verification/stage8-evidence-instrumentation.txt"
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_bytes(output)
    if b"OK (1 test)" not in output or b"FAILURES" in output:
        raise SystemExit(f"Evidence test did not pass; inspect {log}")
    destination = ROOT / "docs/evidence"
    destination.mkdir(parents=True, exist_ok=True)
    files = []
    for name in NAMES:
        data = adb("exec-out", "run-as", PACKAGE, "cat", f"files/pw04-evidence/{name}.png")
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            raise SystemExit(f"Not a PNG: {name}")
        (destination / f"{name}.png").write_bytes(data)
        files.append({"file": f"{name}.png", "sha256": hashlib.sha256(data).hexdigest()})
    metadata = {
        "captured_at_utc": datetime.now(timezone.utc).isoformat(),
        "serial": args.serial,
        "model": adb("shell", "getprop", "ro.product.model").decode().strip(),
        "android_api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(),
        "method": "UiAutomation.takeScreenshot in EvidenceCaptureTest; original PNG bytes",
        "maps_verified": False,
        "apk_sha256": hashlib.sha256((ROOT / "app/build/outputs/apk/debug/app-debug.apk").read_bytes()).hexdigest(),
        "files": files,
    }
    (destination / "manifest.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"EvidenceCaptureTest passed; collected {len(files)} original screenshots into {destination}")


if __name__ == "__main__":
    main()
