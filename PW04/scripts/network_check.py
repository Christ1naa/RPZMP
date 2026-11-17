"""Launch/collect a detached on-device network-loss test; Wi-Fi ADB can disconnect safely."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PKG = "ua.kpi.energyforecast.pw04"
REMOTE_LOG = "/data/local/tmp/pw04-network-result.txt"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--action", choices=["launch", "collect"], required=True)
    args = parser.parse_args()

    def adb(*command, check=True):
        return subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True,
                              check=check, timeout=30).stdout

    private = ROOT / "build/verification/final"
    private.mkdir(parents=True, exist_ok=True)
    if args.action == "launch":
        settings = {name: adb("shell", "settings", "get", "global", name).decode().strip()
                    for name in ["wifi_on", "mobile_data"]}
        if settings["wifi_on"] != "1": raise SystemExit("Connect phone to Wi-Fi first")
        if settings["mobile_data"] not in ["0", "1"]:
            raise SystemExit("Cannot determine original mobile-data setting; network test not launched")
        (private / "network-original.json").write_text(json.dumps(settings, indent=2) + "\n")
        if not adb("shell", "which", "setsid").strip():
            raise SystemExit("Device lacks setsid; detached network test was not started")
        adb("shell", "am", "force-stop", PKG)
        for name in ["50-network-off-map-failed", "51-network-retry-loading", "52-network-restored-map-loaded"]:
            adb("shell", "run-as", PKG, "rm", "-f", "files/pw04-final-evidence/" + name + ".png")
        # An independent device-local watchdog restores the original network if the app process dies.
        adb("shell", "rm", "-f", "/data/local/tmp/pw04-network-restored")
        watchdog = private / "network-watchdog.sh"
        data_action = "enable" if settings["mobile_data"] == "1" else "disable"
        watchdog.write_text("#!/system/bin/sh\nsleep 120\nif [ ! -f /data/local/tmp/pw04-network-restored ]; then\n"
                            "  svc wifi enable\n  svc data " + data_action + "\nfi\n", encoding="utf-8")
        adb("push", str(watchdog), "/data/local/tmp/pw04-network-watchdog.sh")
        adb("shell", "nohup setsid sh /data/local/tmp/pw04-network-watchdog.sh > /data/local/tmp/pw04-network-watchdog.log 2>&1 < /dev/null &")
        # Static, device-local command: no user-supplied text or secrets are embedded.
        command = ("nohup setsid am instrument -w -r -e listener " + PKG + ".KeepAwakeListener -e verificationHostSetup true -e allowNetworkDisruption true -e class "
                   + PKG + ".NetworkRecoveryTest " + PKG + ".test/androidx.test.runner.AndroidJUnitRunner"
                   + " > " + REMOTE_LOG + " 2>&1 < /dev/null &")
        adb("shell", command)
        print("Detached network test launched. It restores Wi-Fi/mobile data in finally; reconnect ADB afterward.")
        return
    result = adb("shell", "cat", REMOTE_LOG)
    (private / "network-detached-runner.txt").write_bytes(result)
    success = (b"OK (1 test)" in result and b"FAILURES" not in result and
               not re.search(rb"INSTRUMENTATION_STATUS_CODE:\s*-(?:3|4)\b", result))
    settings = {name: adb("shell", "settings", "get", "global", name).decode().strip()
                for name in ["wifi_on", "mobile_data"]}
    original = json.loads((private / "network-original.json").read_text())
    destination = ROOT / "docs/evidence/final-checks/network-recovery"
    destination.mkdir(parents=True, exist_ok=True)
    files = []
    for name in ["50-network-off-map-failed", "51-network-retry-loading", "52-network-restored-map-loaded"]:
        data = adb("exec-out", "run-as", PKG, "cat", "files/pw04-final-evidence/" + name + ".png", check=False)
        if data.startswith(b"\x89PNG\r\n\x1a\n"):
            (destination / (name + ".png")).write_bytes(data)
            files.append({"file": name + ".png", "sha256": hashlib.sha256(data).hexdigest()})
    metadata = {"captured_at_utc": datetime.now(timezone.utc).isoformat(), "passed": success,
                "model": adb("shell", "getprop", "ro.product.model").decode().strip(),
                "api": adb("shell", "getprop", "ro.build.version.sdk").decode().strip(),
                "original_network_settings_restored": settings == original, "files": files}
    (destination / "manifest.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print("Network test passed:", success, "; settings restored:", settings == original)
    if not success or settings != original: raise SystemExit("Inspect private network log/settings")


if __name__ == "__main__":
    main()
