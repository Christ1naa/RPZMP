"""Run four real Maps/UI tests on a selected Android device and collect SDK snapshots."""
import argparse
import hashlib
import json
import re
from datetime import datetime, timezone
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "ua.kpi.energyforecast.pw04"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb", required=True)
    args = parser.parse_args()

    def adb(*command):
        return subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True,
                              check=True, timeout=240).stdout

    if adb("get-state").strip() != b"device":
        raise SystemExit("Device is not connected")
    for apk in [ROOT / "app/build/outputs/apk/debug/app-debug.apk",
                ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
        adb("install", "-r", str(apk))
    classes = ",".join(PACKAGE + "." + name for name in
                       ["GoogleMapSdkTest", "GoogleMapBrowserTest", "MapInfoWindowTest"])
    result = adb("shell", "am", "instrument", "-w", "-r", "-e", "listener", PACKAGE + ".KeepAwakeListener", "-e", "class", classes,
                 PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner")
    log = ROOT / "build/verification/maps-phone-final-runner.txt"
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_bytes(result)
    if (b"OK (4 tests)" not in result or b"FAILURES" in result
            or re.search(rb"INSTRUMENTATION_STATUS_CODE:\s*-(?:3|4)\b", result)):
        raise SystemExit(f"Maps/UI test run failed; inspect {log}")
    destination = ROOT / "docs/evidence/maps-phone"
    destination.mkdir(parents=True, exist_ok=True)
    images = []
    for name in ["01-map-loaded", "02-marker-selected", "03-camera-refocused", "04-filtered-map", "05-empty-map"]:
        data = adb("exec-out", "run-as", PACKAGE, "cat", f"files/pw04-maps-evidence/{name}.png")
        if not data.startswith(b"\x89PNG\r\n\x1a\n"):
            raise SystemExit(f"Invalid PNG: {name}")
        (destination / (name + ".png")).write_bytes(data)
        images.append({"file": name + ".png", "sha256": hashlib.sha256(data).hexdigest()})
    metadata = {
        "captured_at_utc": datetime.now(timezone.utc).isoformat(),
        "model": adb("shell", "getprop", "ro.product.model").decode().strip(),
        "android_api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(),
        "method": "GoogleMap.snapshot; production EnergyMap in SDK/Browser test hosts, not MainActivity",
        "test_result": "OK (4 tests)",
        "apk_sha256": hashlib.sha256((ROOT / "app/build/outputs/apk/debug/app-debug.apk").read_bytes()).hexdigest(),
        "files": images,
    }
    (destination / "manifest.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n",
                                              encoding="utf-8")
    print("Maps/UI: OK (4 tests); collected 5 original SDK snapshots.")


if __name__ == "__main__":
    main()
