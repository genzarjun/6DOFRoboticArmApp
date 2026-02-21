package com.roboticarm.controller;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int SCAN_PERIOD = 10000;
    
    // BLE Service and Characteristic UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABC");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-4321-4321-CBA987654321");
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private boolean isConnected = false;
    private boolean isScanning = false;
    private Handler scanHandler = new Handler();
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private static final String NOTIFICATION_CHANNEL_ID = "robotic_arm_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Xbox controller input throttling
    private long lastCommandTime = 0;
    private static final long COMMAND_DELAY_MS = 100; // Send commands max every 100ms
    
    // UI Components
    private TextView connectionStatus;
    private TextView servoStatusDisplay;
    private Button btnConnect, btnDisconnect;
    private List<Button> minButtons = new ArrayList<>();
    private List<Button> maxButtons = new ArrayList<>();
    private List<Button> setButtons = new ArrayList<>();
    private List<EditText> positionInputs = new ArrayList<>();
    
    // Servo tracking
    private int[] currentServoPositions = {90, 90, 90, 90, 90, 90}; // Track current positions
    private final String[] SERVO_NAMES = {"Base", "Shoulder", "Elbow", "Wrist", "Wrist Rot", "Gripper"};
    private final String[] PORT_MAPPING = {"Port 1", "Port 4", "Port 7", "Port 8", "Port 11", "Port 15"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Keep screen on while app is open
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Initialize wake lock to keep connection alive when screen is off
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoboticArm::ControllerWakeLock");
        
        // Request battery optimization exemption
        requestBatteryOptimizationExemption(powerManager);
        
        // Create notification channel for foreground service
        createNotificationChannel();
        
        initializeViews();
        setupBluetooth();
        setupServoControls();
        checkPermissions();
    }
    
    private void requestBatteryOptimizationExemption(PowerManager powerManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // Some devices don't support this
                    Toast.makeText(this, "Please disable battery optimization manually for best performance", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    private void createNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Robotic Arm Connection",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the robotic arm connection active");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void showConnectionNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Robotic Arm Connected")
                .setContentText("Controlling robotic arm - tap to open")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        }
        
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    private void hideConnectionNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
    
    private void initializeViews() {
        connectionStatus = findViewById(R.id.connectionStatus);
        servoStatusDisplay = findViewById(R.id.servoStatusDisplay);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        
        // Initialize servo control arrays
        int[] minButtonIds = {R.id.btnServo0Min, R.id.btnServo1Min, R.id.btnServo2Min, 
                             R.id.btnServo3Min, R.id.btnServo4Min, R.id.btnServo5Min};
        int[] maxButtonIds = {R.id.btnServo0Max, R.id.btnServo1Max, R.id.btnServo2Max, 
                             R.id.btnServo3Max, R.id.btnServo4Max, R.id.btnServo5Max};
        int[] setButtonIds = {R.id.btnServo0Set, R.id.btnServo1Set, R.id.btnServo2Set, 
                             R.id.btnServo3Set, R.id.btnServo4Set, R.id.btnServo5Set};
        int[] positionInputIds = {R.id.etServo0Position, R.id.etServo1Position, R.id.etServo2Position, 
                                 R.id.etServo3Position, R.id.etServo4Position, R.id.etServo5Position};
        
        for (int i = 0; i < 6; i++) {
            minButtons.add(findViewById(minButtonIds[i]));
            maxButtons.add(findViewById(maxButtonIds[i]));
            setButtons.add(findViewById(setButtonIds[i]));
            positionInputs.add(findViewById(positionInputIds[i]));
        }
        
        // Initialize servo status display
        updateServoStatusDisplay();
    }
    
    private void setupBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }
    
    private void setupServoControls() {
        btnConnect.setOnClickListener(v -> connectToArduino());
        btnDisconnect.setOnClickListener(v -> disconnectFromArduino());
        
        for (int i = 0; i < 6; i++) {
            final int servoIndex = i;
            
            // Min button sends 0 degrees
            minButtons.get(i).setOnClickListener(v -> {
                setServoPosition(servoIndex, 0);
                Toast.makeText(this, "Servo " + servoIndex + " (" + PORT_MAPPING[servoIndex] + ") → 0°", Toast.LENGTH_SHORT).show();
            });
            
            // Max button sends 180 degrees
            maxButtons.get(i).setOnClickListener(v -> {
                setServoPosition(servoIndex, 180);
                Toast.makeText(this, "Servo " + servoIndex + " (" + PORT_MAPPING[servoIndex] + ") → 180°", Toast.LENGTH_SHORT).show();
            });
            
            // Set button sends custom position
            setButtons.get(i).setOnClickListener(v -> {
                String positionText = positionInputs.get(servoIndex).getText().toString();
                if (!positionText.isEmpty()) {
                    try {
                        int position = Integer.parseInt(positionText);
                        if (position >= 0 && position <= 180) {
                            setServoPosition(servoIndex, position);
                            Toast.makeText(this, "Servo " + servoIndex + " (" + PORT_MAPPING[servoIndex] + ") → " + position + "°", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Position must be 0-180", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid position", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void connectToArduino() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        
        if (isScanning) {
            Toast.makeText(this, "Already scanning...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isConnected) {
            Toast.makeText(this, "Already connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        isScanning = true;
        btnConnect.setText("Scanning...");
        
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(SERVICE_UUID))
                .build();
        filters.add(filter);
        
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        
        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        
        scanHandler.postDelayed(this::stopScan, SCAN_PERIOD);
        
        Toast.makeText(this, "Scanning for Arduino...", Toast.LENGTH_SHORT).show();
    }
    
    private void stopScan() {
        if (isScanning && bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(scanCallback);
            }
            isScanning = false;
            btnConnect.setText("Connect to Arduino");
        }
    }
    
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            String deviceName = device.getName();
            
            if (deviceName != null && (deviceName.contains("Arduino") || deviceName.contains("Robotic"))) {
                stopScan();
                connectToDevice(device);
            }
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
                stopScan();
            });
        }
    };
    
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Toast.makeText(this, "Connecting to " + device.getName() + "...", Toast.LENGTH_SHORT).show();
    }
    
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    connectionStatus.setText("✓ Connected to Arduino");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    btnConnect.setEnabled(false);
                    btnDisconnect.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Connected! Phone can be locked now", Toast.LENGTH_LONG).show();
                    
                    // Acquire wake lock to keep connection alive
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();
                    }
                    
                    // Show persistent notification to keep connection alive
                    showConnectionNotification();
                });
                
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    isConnected = false;
                    connectionStatus.setText("Disconnected");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    btnConnect.setEnabled(true);
                    btnDisconnect.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                    
                    // Release wake lock when disconnected
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                    
                    // Hide notification
                    hideConnectionNotification();
                });
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (writeCharacteristic != null) {
                        runOnUiThread(() -> {
                            isConnected = true;
                            Toast.makeText(MainActivity.this, "Ready to calibrate!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Command failed", Toast.LENGTH_SHORT).show();
                });
            }
        }
    };
    
    private void disconnectFromArduino() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
        
        isConnected = false;
        connectionStatus.setText("Disconnected");
        connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        
        // Release wake lock and hide notification
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        hideConnectionNotification();
    }
    
    private void setServoPosition(int servoIndex, int position) {
        if (!isConnected) {
            Toast.makeText(this, "Not connected to Arduino", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update tracked position
        currentServoPositions[servoIndex] = position;
        updateServoStatusDisplay();
        
        // Send command to Arduino in format: S<servo>:<position>
        String command = "S" + servoIndex + ":" + position;
        sendCommand(command);
    }
    
    @Override
    public boolean onGenericMotionEvent(android.view.MotionEvent event) {
        // Analog controls removed - using button sequences only
        return super.onGenericMotionEvent(event);
    }
    
    private void sendCommand(String command) {
        if (bluetoothGatt != null && writeCharacteristic != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                writeCharacteristic.setValue(command.getBytes());
                bluetoothGatt.writeCharacteristic(writeCharacteristic);
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle Xbox controller input - SEQUENCE MODE (No manual controls)
        if (isConnected) {
            // A Button - Approach Sequence
            if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                sendCommand("BTN_A");
                Toast.makeText(this, "🎯 APPROACH Sequence Started", Toast.LENGTH_LONG).show();
                return true;
            }
            // B Button - Grab Sequence
            else if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                sendCommand("BTN_B");
                Toast.makeText(this, "✊ GRAB Sequence Started", Toast.LENGTH_LONG).show();
                return true;
            }
            // X Button - Drop Sequence
            else if (keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                sendCommand("BTN_X");
                Toast.makeText(this, "📍 DROP Sequence Started", Toast.LENGTH_LONG).show();
                return true;
            }
            // Y Button - Release Sequence
            else if (keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                sendCommand("BTN_Y");
                Toast.makeText(this, "👐 RELEASE Sequence Started", Toast.LENGTH_LONG).show();
                return true;
            }
            // START Button - Reset to Home
            else if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
                sendCommand("BTN_START");
                Toast.makeText(this, "🏠 RESET to Home Position", Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void updateServoStatusDisplay() {
        StringBuilder status = new StringBuilder();
        status.append("🎮 SERVO POSITIONS 🎮\n\n");
        for (int i = 0; i < 6; i++) {
            status.append(String.format("S%d %s (%s): %d°\n", 
                i, SERVO_NAMES[i], PORT_MAPPING[i], currentServoPositions[i]));
        }
        
        if (servoStatusDisplay != null) {
            runOnUiThread(() -> servoStatusDisplay.setText(status.toString()));
        }
    }
    
    private void sendIncrementCommand(int servoIndex, int increment) {
        if (!isConnected) {
            return;
        }
        
        // Send increment command to Arduino: S<servo>:<increment>
        String command = "S" + servoIndex + ":" + increment;
        sendCommand(command);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isScanning) {
            stopScan();
        }
        if (isConnected) {
            disconnectFromArduino();
        }
        // Release wake lock when app is destroyed
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Hide notification when app is destroyed
        hideConnectionNotification();
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Intercept DPAD events to prevent ScrollView from capturing them
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || 
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN || 
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT || 
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // Handle the key event ourselves
                return onKeyDown(keyCode, event);
            }
        }
        return super.dispatchKeyEvent(event);
    }
}