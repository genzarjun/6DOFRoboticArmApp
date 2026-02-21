# 6DOF Robotic Arm Controller App

An Android app that controls a 6 degrees-of-freedom robotic arm via Bluetooth Low Energy. It acts as a bridge between an Xbox controller (or built-in touch controls) and an Arduino-powered arm, sending BLE commands that drive six servos for pick-and-place sequences.

---

## How It Works

### Architecture

```
Xbox Controller ──▶ Android App ──▶ BLE ──▶ Arduino ──▶ Servos
   (or touch)          (this app)            (firmware)   (6 motors)
```

1. **Input** – Xbox controller buttons or on-screen servo controls
2. **App** – Handles input and sends commands over BLE
3. **Arduino** – Runs firmware that receives commands and drives servos
4. **Servos** – Move the arm (Base, Shoulder, Elbow, Wrist, Wrist Rotation, Gripper)

---

## Control Modes

### 1. Xbox Controller (Pick & Place Sequences)

When an Xbox controller is connected, buttons trigger preprogrammed sequences on the Arduino:

| Button | Command | Action |
|--------|---------|--------|
| **A** | `BTN_A` | Approach object |
| **B** | `BTN_B` | Grab object |
| **X** | `BTN_X` | Move to drop position |
| **Y** | `BTN_Y` | Release object |
| **START** | `BTN_START` | Reset to home position |

Flow: **A** (approach) → **B** (grab) → **X** (drop) → **Y** (release) → **START** (reset).

The app handles controller input in `onKeyDown()` and sends short strings like `BTN_A` over BLE. The Arduino firmware executes the matching sequence.

### 2. Touch Controls (Manual Servo Control)

The app’s UI lets you control each servo directly while connected:

- **Min (0°)** – Move servo to 0°
- **Max (180°)** – Move servo to 180°
- **Position + SET** – Move servo to the entered angle (0–180°)

Commands are sent in the form `S<servoIndex>:<position>` (e.g. `S0:90`, `S5:115`).

---

## BLE Connection Flow

1. User taps **Connect to Arduino**
2. App starts BLE scan and looks for devices advertising the arm’s service
3. When a device matching the service UUID and name (“Arduino” or “Robotic”) is found, the app connects via GATT
4. After connection, it discovers services and gets the write characteristic
5. Once the characteristic is ready, the app is “connected” and can send commands

**BLE setup:**
- **Service UUID:** `12345678-1234-1234-1234-123456789ABC`
- **Characteristic UUID:** `87654321-4321-4321-4321-CBA987654321` (Read/Write)

---

## UI Components

| Component | Purpose |
|-----------|---------|
| **Connection Status** | Shows “Connected” or “Disconnected” |
| **Connect / Disconnect** | Start BLE scan or disconnect |
| **Servo Status Display** | Lists all 6 servos and current angles |
| **Servo Control Panels** | Min, Max, and custom position controls for each servo (S0–S5) |

---

## Background Behavior

- **Screen on** – Screen stays on while the app is in the foreground
- **Wake lock** – Partial wake lock keeps the connection active when the screen is off
- **Notification** – “Robotic Arm Connected” notification while connected
- **Battery optimization** – App can request exemption for more reliable BLE

---

## Permissions

The app needs:

- Bluetooth (scan, connect)
- Location (for BLE on Android)
- Notifications (Android 13+)
- Wake lock
- Battery optimization exemption (optional)

---

## Servo Mapping

| Servo | Joint | Shield Port |
|-------|-------|-------------|
| S0 | Base | Port 1 |
| S1 | Shoulder | Port 4 |
| S2 | Elbow | Port 7 |
| S3 | Wrist | Port 8 |
| S4 | Wrist Rotation | Port 11 |
| S5 | Gripper | Port 15 |

---

## Build & Install

```bash
# Build debug APK
./gradlew assembleDebug

# Install when device is connected
./install_app.sh
# or: adb install app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:** Android Studio or compatible Gradle setup, Android device with BLE, Arduino running compatible firmware.

---

## Requirements

- Android device with BLE (typically Android 5.0+)
- Arduino with BLE support (e.g. Arduino R4 WiFi) running matching firmware
- Xbox controller (USB or Bluetooth) for sequence mode
- 6DOF robotic arm with servos on an Adafruit PWM Servo Shield (or equivalent)
