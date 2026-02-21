# Arduino Setup Guide

## Hardware Connections

### Servo Connections
Connect your 6 servos to the Arduino as follows:

| Servo | Arduino Pin | Color Code | Notes |
|-------|-------------|------------|-------|
| Servo 0 | Pin 0 | Red: 5V, Black: GND, Yellow: Pin 0 | Base rotation |
| Servo 1 | Pin 1 | Red: 5V, Black: GND, Yellow: Pin 1 | Shoulder |
| Servo 2 | Pin 2 | Red: 5V, Black: GND, Yellow: Pin 2 | Elbow |
| Servo 3 | Pin 3 | Red: 5V, Black: GND, Yellow: Pin 3 | Wrist |
| Servo 4 | Pin 4 | Red: 5V, Black: GND, Yellow: Pin 4 | Wrist rotation |
| Servo 5 | Pin 5 | Red: 5V, Black: GND, Yellow: Pin 5 | Gripper (inverted) |

### Power Supply
- **Important**: Servos require significant current (1-2A total)
- Use an external 5V power supply for the servos
- Connect the external power supply's ground to Arduino ground
- Do NOT power servos directly from Arduino 5V pin

### BLE Configuration
- **No external Bluetooth module needed** - Arduino Uno R4 has built-in BLE
- Device name: "Arduino Robotic Arm"
- Service UUID: `12345678-1234-1234-1234-123456789ABC`
- Characteristic UUID: `87654321-4321-4321-4321-CBA987654321`

## Software Setup

1. **Install Arduino IDE** (if not already installed)
   - Download from: https://www.arduino.cc/en/software

2. **Install Required Libraries**
   - The code uses the built-in `Servo` library (no additional installation needed)
   - The code uses `ArduinoBLE` library for BLE communication
   - Install ArduinoBLE library: Tools → Manage Libraries → Search "ArduinoBLE" → Install

3. **Upload the Code**
   - Open `RoboticArmController.ino` in Arduino IDE
   - Select your Arduino board: Tools → Board → Arduino Uno
   - Select the correct port: Tools → Port → (your Arduino port)
   - Click Upload

4. **Test the Setup**
   - Open Serial Monitor (Tools → Serial Monitor)
   - Set baud rate to 9600
   - You should see "6DOF Robotic Arm Controller Ready"
   - You should see "BLE device is now discoverable as 'Arduino Robotic Arm'"
   - You should see "Waiting for BLE connection..."

## BLE Connection

1. **Connect with Android Device**
   - The Arduino will automatically advertise as "Arduino Robotic Arm"
   - No manual pairing required - BLE handles this automatically

2. **Test Connection**
   - Open the Robotic Arm Controller app
   - Tap "Connect to Arduino"
   - The app will scan and automatically connect to "Arduino Robotic Arm"
   - You should see "Connected to Arduino" message

## Troubleshooting

### Servo Issues
- **Servos not moving**: Check power supply and connections
- **Servos moving erratically**: Check for loose connections
- **Servos not reaching full range**: Adjust servo horn position or check mechanical limits

### BLE Issues
- **Cannot find device**: Make sure Arduino is powered on and BLE is advertising
- **Connection fails**: Try restarting both devices and scanning again
- **Commands not working**: Check Serial Monitor for error messages
- **App not connecting**: Ensure ArduinoBLE library is installed correctly

### Power Issues
- **Arduino resets when servos move**: Insufficient power supply
- **Servos jitter**: Add capacitors (1000µF) across power supply
- **Servos don't move at all**: Check power supply voltage (should be 5V)

## Safety Notes

⚠️ **Important Safety Guidelines**:
- Always power off the system when making connections
- Be careful when testing servo movements
- Ensure adequate space around the robotic arm
- Start with small movements to test functionality
- Never put fingers near moving parts during testing

## Servo Calibration

If your servos don't move to the expected positions:

1. **Check mechanical limits**: Ensure servos can physically reach the programmed positions
2. **Adjust code ranges**: Modify `CFG_MIN_DEG` and `CFG_MAX_DEG` arrays if needed
3. **Test individual servos**: Use Serial Monitor to send individual commands
   - Example: Send "S0:90" to move servo 0 to 90 degrees

## Advanced Configuration

### Custom Servo Ranges
To modify servo ranges, edit these lines in the Arduino code:
```cpp
const int CFG_MIN_DEG[6] = { 0,    0,    0,    0,   0,   180 };
const int CFG_MAX_DEG[6] = {150,  200,  270,  180,   270,   0 };
```

### Smooth Movement
The code includes a `smoothMoveServo()` function for gradual movements:
```cpp
smoothMoveServo(servoIndex, targetPosition, steps);
```

### Debug Commands
Send these commands via Serial Monitor or Android app:
- `STATUS` - Get current servo positions
- `RESET` - Reset all servos to minimum positions
- `S0:90` - Move servo 0 to 90 degrees
