#!/bin/bash

# Robotic Arm Controller - Android App Installation Script
# This script builds and installs the Android app on your connected device

echo "🤖 Robotic Arm Controller - Installation Script"
echo "================================================"

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "Setting up Android SDK path..."
    export ANDROID_HOME=~/Library/Android/sdk
    export PATH=$PATH:$ANDROID_HOME/platform-tools
fi

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please ensure Android SDK is installed and in your PATH."
    echo "   You can install Android Studio from: https://developer.android.com/studio"
    exit 1
fi

# Check if device is connected
echo "🔍 Checking for connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No Android devices found. Please:"
    echo "   1. Connect your Android device via USB"
    echo "   2. Enable USB Debugging in Developer Options"
    echo "   3. Allow USB Debugging when prompted on your device"
    exit 1
fi

echo "✅ Found $DEVICES connected device(s)"

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Gradle wrapper not found. Please run this script from the project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

echo "🔨 Building Android app..."
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Try to build the app
if ./gradlew assembleDebug; then
    echo "✅ App built successfully!"
    
    # Install the app
    echo "📱 Installing app on device..."
    if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
        echo "✅ App installed successfully!"
        echo ""
        echo "🎉 Installation complete!"
        echo ""
        echo "Next steps:"
        echo "1. Open the 'Robotic Arm Controller' app on your device"
        echo "2. Pair your Arduino with your Android device via Bluetooth"
        echo "3. Connect to your Arduino in the app"
        echo "4. Start controlling your robotic arm!"
        echo ""
        echo "For Arduino setup, see the README.md file."
    else
        echo "❌ Failed to install app. Please check device connection and try again."
        exit 1
    fi
else
    echo "❌ Failed to build app. Please check the error messages above."
    echo ""
    echo "Common solutions:"
    echo "1. Make sure you have the latest Android SDK installed"
    echo "2. Check that your device has USB debugging enabled"
    echo "3. Try opening the project in Android Studio for more detailed error messages"
    exit 1
fi
