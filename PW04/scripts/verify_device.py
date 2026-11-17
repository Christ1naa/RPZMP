"""Reproducible PW04 device verification; changes only PW04 permissions and restores device settings."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
PKG = "ua.kpi.energyforecast.pw04"
JBR = "C:/Program Files/Android/Android Studio/jbr"
PERMISSIONS = ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--phase", choices=["baseline", "targeted", "keyed", "device", "absent", "invalid", "state", "revoked", "restore"], required=True)
    args = parser.parse_args()
    private = ROOT / "build/verification/final"
    private.mkdir(parents=True, exist_ok=True)
    state_file = private / "phone-state.json"
    public = ROOT / "docs/evidence/final-checks"
    public.mkdir(parents=True, exist_ok=True)
    results_file = private / "results.json"
    results = json.loads(results_file.read_text()) if results_file.exists() else []

    def adb(*command, timeout=45, check=True):
        return subprocess.run([args.adb, "-s", args.serial, *command], capture_output=True,
                              timeout=timeout, check=check)

    def shell(*command, check=True):
        return adb("shell", *command, check=check).stdout.decode(errors="replace").strip()

    def save_result(row):
        results.append(row)
        results_file.write_text(json.dumps(results, indent=2) + "\n")

    def permission_state():
        dump = shell("dumpsys", "package", PKG)
        found = {}
        for permission in PERMISSIONS:
            match = re.search(re.escape(permission) + r": granted=(true|false), flags=\[([^\]]*)\]", dump)
            if not match:
                raise RuntimeError("Cannot read PW04 permission state")
            found[permission] = {"granted": match[1] == "true",
                                 "user_flags": [x.strip().lower().replace("_", "-") for x in match[2].split("|")
                                                if x.strip() in ["USER_SET", "USER_FIXED"]]}
        return found

    def reset_permissions(fine=False):
        shell("am", "force-stop", PKG)
        for p in PERMISSIONS:
            shell("pm", "revoke", PKG, p, check=False)
            shell("pm", "clear-permission-flags", PKG, p, "user-set", "user-fixed")
        if fine:
            shell("pm", "grant", PKG, PERMISSIONS[1])
            shell("pm", "grant", PKG, PERMISSIONS[0])

    def restore(state):
        shell("am", "force-stop", PKG, check=False)
        shell("settings", "put", "system", "font_scale", state["font_scale"])
        shell("cmd", "location", "set-location-enabled", state["location_enabled"])
        reset_permissions()
        for p in reversed(PERMISSIONS):
            if state["permissions"][p]["granted"]:
                shell("pm", "grant", PKG, p)
            flags = state["permissions"][p]["user_flags"]
            if flags:
                shell("pm", "set-permission-flags", PKG, p, *flags)
        shell("appops", "set", PKG, "android:mock_location", state["mock_op"])

    def build(mode):
        print("BUILD", mode, flush=True)
        env = os.environ.copy()
        env["JAVA_HOME"] = JBR
        if mode == "absent": env["MAPS_API_KEY"] = " "
        if mode == "invalid": env["MAPS_API_KEY"] = "PW04_INVALID_KEY_FOR_RECOVERY_CHECK"
        tasks = [":app:assembleDebug", ":app:assembleDebugAndroidTest"]
        command = [str(ROOT / "gradlew.bat"), *tasks, "--offline", "--console=plain", "--no-daemon",
                   "-Porg.gradle.java.installations.paths=" + JBR, "-Dorg.gradle.java.home=" + JBR]
        result = subprocess.run(command, cwd=ROOT, env=env, capture_output=True, timeout=300)
        (private / ("build-" + mode + ".log")).write_bytes(result.stdout + result.stderr)
        if result.returncode: raise RuntimeError("Gradle build failed; inspect private build log")
        for apk in [ROOT / "app/build/outputs/apk/debug/app-debug.apk",
                    ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
            adb("install", "-r", str(apk), timeout=90)

    def collect(label):
        destination = public / label
        destination.mkdir(parents=True, exist_ok=True)
        entries = []
        for folder in ["pw04-final-evidence", "pw04-evidence", "pw04-maps-evidence"]:
            listing = adb("shell", "run-as", PKG, "ls", "files/" + folder, check=False)
            if listing.returncode: continue
            for name in listing.stdout.decode().splitlines():
                if not re.fullmatch(r"[A-Za-z0-9_-]+\.png", name): continue
                data = adb("exec-out", "run-as", PKG, "cat", "files/" + folder + "/" + name).stdout
                if not data.startswith(b"\x89PNG\r\n\x1a\n"): raise RuntimeError("Invalid screenshot")
                target = folder + "--" + name
                (destination / target).write_bytes(data)
                entries.append({"file": target, "sha256": hashlib.sha256(data).hexdigest()})
        metadata = {"captured_at_utc": datetime.now(timezone.utc).isoformat(),
                    "model": shell("getprop", "ro.product.model"), "api": shell("getprop", "ro.build.version.sdk"),
                    "scenario": label, "files": entries, "passed": results[-1]["passed"],
                    "apk_sha256": hashlib.sha256((ROOT / "app/build/outputs/apk/debug/app-debug.apk").read_bytes()).hexdigest()}
        (destination / "manifest.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    def clean_images():
        # Delete only this verification's own app-private screenshot directories.
        for folder in ["pw04-final-evidence", "pw04-evidence", "pw04-maps-evidence"]:
            shell("run-as", PKG, "rm", "-rf", "files/" + folder)

    def instrument(label, *, method=None, exclude=None, expected=None, extras=None):
        print("RUN", label, flush=True)
        power = shell("dumpsys", "power")
        if "mWakefulness=Awake" not in power:
            print("WAIT: unlock the phone to start UI tests", flush=True)
            deadline = time.monotonic() + 120
            while "mWakefulness=Awake" not in shell("dumpsys", "power") and time.monotonic() < deadline:
                time.sleep(2)
            if "mWakefulness=Awake" not in shell("dumpsys", "power"):
                raise RuntimeError("Phone stayed asleep; UI tests were not started")
        clean_images()
        command = ["shell", "am", "instrument", "-w", "-r", "-e", "listener", PKG + ".KeepAwakeListener"]
        if method: command += ["-e", "class", PKG + "." + method, "-e", "verificationHostSetup", "true"]
        if exclude: command += ["-e", "notAnnotation", ",".join(PKG + "." + x for x in exclude)]
        for key, value in (extras or {}).items(): command += ["-e", key, value]
        command += [PKG + ".test/androidx.test.runner.AndroidJUnitRunner"]
        began = time.monotonic()
        result = adb(*command, timeout=900, check=False)
        output = result.stdout + result.stderr
        log = private / (label + ".txt")
        if log.exists():
            (private / (label + ".previous-" + str(time.time_ns()) + ".txt")).write_bytes(log.read_bytes())
        log.write_bytes(output)
        match = re.search(rb"OK \((\d+) tests?\)", output)
        skipped = re.search(rb"INSTRUMENTATION_STATUS_CODE:\s*-(?:3|4)\b", output) is not None
        failure_summary = re.search(rb"Tests run:\s*(\d+),\s*Failures:\s*(\d+)", output)
        count = int(match[1]) if match else int(failure_summary[1]) if failure_summary else 0
        passed = bool(match) and b"FAILURES" not in output and not skipped and (expected is None or count == expected)
        save_result({"scenario": label, "passed": passed, "tests": count, "skipped": skipped,
                     "failures": int(failure_summary[2]) if failure_summary else 0,
                     "duration_seconds": round(time.monotonic() - began, 2)})
        collect(label)
        print("PASS" if passed else "FAIL", label, count, flush=True)
        if not passed: raise RuntimeError("Instrumented scenario failed; inspect private runner log " + label)

    if args.phase == "baseline":
        if state_file.exists(): raise SystemExit("Baseline already exists; preserve it for restoration")
        mock = shell("appops", "get", PKG, "android:mock_location", check=False)
        match = re.search(r"MOCK_LOCATION:\s*(allow|deny|ignore|default)", mock, re.I)
        state = {"font_scale": shell("settings", "get", "system", "font_scale"),
                 "location_enabled": shell("cmd", "location", "is-location-enabled"),
                 "permissions": permission_state(), "mock_op": match[1].lower() if match else "default"}
        state_file.write_text(json.dumps(state, indent=2) + "\n")
        print("Original PW04 permissions, font and Location settings saved privately.")
        return

    state = json.loads(state_file.read_text())
    if args.phase == "restore":
        build("valid"); restore(state)
        print("Restored valid-key APK and original device settings.")
        return
    try:
        shell("settings", "put", "system", "font_scale", state["font_scale"])
        shell("cmd", "location", "set-location-enabled", "true")
        if args.phase == "targeted":
            build("valid"); reset_permissions()
            instrument("targeted-ui-sync", method="ActivityLifecycleTest," + PKG + ".CompleteEvidenceTest",
                       exclude=["NeedsHostSetup"], expected=3)
        elif args.phase in ["keyed", "absent"]:
            mode = "valid" if args.phase == "keyed" else "absent"
            build(mode); reset_permissions()
            excluded = ["NeedsHostSetup", "NeedsNoMapsKey" if mode == "valid" else "NeedsMapsKey"]
            instrument("general-" + mode, exclude=excluded, expected=48)
        elif args.phase == "device":
            build("valid")
            for method in ["precisePermissionThroughSystemDialog", "approximatePermissionThroughSystemDialog",
                           "denyTwiceThenOpenOwnSettingsAndReturn", "revokedPermissionIsRecheckedOnNewActivity"]:
                if method == "revokedPermissionIsRecheckedOnNewActivity": reset_permissions(fine=True)
                reset_permissions()
                instrument(method, method="DevicePermissionTest#" + method, expected=1)
            reset_permissions(fine=True)
            shell("cmd", "location", "set-location-enabled", "false")
            instrument("location-disabled-settings", method="DevicePermissionTest#locationDisabledAndReturnFromGpsSettings", expected=1)
            shell("cmd", "location", "set-location-enabled", "true")
            reset_permissions(fine=True)
            instrument("real-device-fix", method="DeviceLocationEvidenceTest#freshRealDeviceFixWithoutSavingCoordinates", expected=1)
            instrument("synthetic-position-ui", method="DeviceLocationEvidenceTest#labelledSyntheticPositionUpdatesAndProvidersRecover", expected=1)
            instrument("system-gps-adapter", method="AndroidLocationTest", expected=1)
            reset_permissions()
            shell("settings", "put", "system", "font_scale", "1.6")
            instrument("system-large-font", method="CompleteEvidenceTest#actualSystemLargeFontScreens", expected=1)
        elif args.phase == "revoked":
            build("valid"); reset_permissions(fine=True); reset_permissions()
            instrument("revokedPermissionIsRecheckedOnNewActivity", method="DevicePermissionTest#revokedPermissionIsRecheckedOnNewActivity", expected=1)
        elif args.phase == "state":
            build("valid"); reset_permissions()
            instrument("controlled-states", method="StateEvidenceTest", expected=1)
        elif args.phase == "invalid":
            build("invalid")
            # PW04 stores no user records/credentials; all generated screenshots are already copied.
            # Clear only this demo app's data to avoid using previously authorized cached map tiles.
            shell("pm", "clear", PKG)
            reset_permissions()
            instrument("invalid-key-retry", method="MapFailureEvidenceTest", expected=1,
                       extras={"evidencePrefix": "invalid-key"})
    finally:
        restore(state)


if __name__ == "__main__":
    main()
