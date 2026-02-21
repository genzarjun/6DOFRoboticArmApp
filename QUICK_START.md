# 🤖 Robotic Arm Controller - Quick Start Guide

## ✅ What's Been Created

I've successfully created a complete robotic arm control system for you with:

### 📱 Android App
- **Location**: `/Users/anurag/RoboticArmController/`
- **Status**: ✅ Built and installed on your device
- **Features**:
  - Bluetooth connectivity to Arduino
  - 6 servo controls (one for each DOF)
  - Min/Max position buttons for each servo
  - Custom position input for each servo
  - Real-time connection status
  - Input validation with your specified servo ranges

### 🔧 Arduino Code
- **Location**: `/Users/anurag/RoboticArmController/Arduino/RoboticArmController.ino`
- **Features**:
  - Controls 6 servos on pins 0-5
  - Bluetooth communication via SoftwareSerial
  - Command processing (S<servo>:<position>)
  - Servo range validation
  - Status reporting
  - Reset functionality

## 🚀 Next Steps

### 1. Arduino Setup
1. **Upload the code** to your Arduino Uno R4:
   - Open `Arduino/RoboticArmController.ino` in Arduino IDE
   - Connect your Arduino via USB
   - Upload the code

2. **Connect your servos**:
   - Servo 0 → Pin 0 (0°-150°)
   - Servo 1 → Pin 1 (0°-200°)
   - Servo 2 → Pin 2 (0°-270°)
   - Servo 3 → Pin 3 (0°-180°)
   - Servo 4 → Pin 4 (0°-270°)
   - Servo 5 → Pin 5 (180°-0°, inverted)

3. **Connect Bluetooth module**:
   - HC-05 VCC → Arduino 5V
   - HC-05 GND → Arduino GND
   - HC-05 TX → Arduino Pin 2
   - HC-05 RX → Arduino Pin 3

### 2. Android Setup
1. **Pair your Arduino**:
   - Go to Android Settings → Bluetooth
   - Turn on Bluetooth
   - Look for "HC-05" or your Arduino's Bluetooth name
   - Tap to pair (PIN is usually "1234" or "0000")

2. **Open the app**:
   - Find "Robotic Arm Controller" on your device
   - Tap to open

3. **Connect to Arduino**:
   - Tap "Connect to Arduino"
   - Select your paired Arduino device
   - You should see "Connected to Arduino"

### 3. Test the System
1. **Test individual servos**:
   - Use Min/Max buttons to test each servo's range
   - Use custom position input for precise control
   - Check that movements match your mechanical setup

2. **Verify ranges**:
   - Servo 0: 0°-150°
   - Servo 1: 0°-200°
   - Servo 2: 0°-270°
   - Servo 3: 0°-180°
   - Servo 4: 0°-270°
   - Servo 5: 180°-0° (inverted)

## 🔧 Troubleshooting

### If the app won't connect:
- Check that Arduino is powered on
- Verify Bluetooth module is connected properly
- Try unpairing and re-pairing the devices
- Check Serial Monitor on Arduino for error messages

### If servos don't move:
- Check power supply (servos need adequate current)
- Verify servo connections to Arduino pins
- Check that positions are within the defined ranges
- Use Arduino Serial Monitor to test commands manually

### If you need to modify servo ranges:
- Edit the `CFG_MIN_DEG` and `CFG_MAX_DEG` arrays in both:
  - Arduino code: `Arduino/RoboticArmController.ino`
  - Android app: `MainActivity.java`

## 📁 Project Structure
```
RoboticArmController/
├── app/                          # Android app
│   ├── src/main/
│   │   ├── java/com/roboticarm/controller/
│   │   │   └── MainActivity.java  # Main Android activity
│   │   ├── res/                   # Android resources
│   │   └── AndroidManifest.xml    # App permissions
│   └── build.gradle               # Android build config
├── Arduino/
│   ├── RoboticArmController.ino   # Arduino code
│   └── ARDUINO_SETUP.md          # Detailed Arduino setup
├── install_app.sh                # Installation script
├── README.md                     # Full documentation
└── QUICK_START.md               # This file
```

## 🎯 Commands Sent to Arduino

The Android app sends these commands:
- `S0:90` - Move servo 0 to 90°
- `S1:150` - Move servo 1 to 150°
- `STATUS` - Get current positions
- `RESET` - Reset all servos to minimum

## ⚠️ Safety Notes

- Always power off when making connections
- Start with small movements
- Ensure adequate space around the arm
- Be careful with servo power requirements
- Test each servo individually first

## 🆘 Need Help?

- Check the detailed `README.md` for comprehensive setup
- See `Arduino/ARDUINO_SETUP.md` for hardware details
- Use Arduino Serial Monitor for debugging
- Check Android app logs for connection issues

---

**🎉 Your robotic arm control system is ready to use!**
