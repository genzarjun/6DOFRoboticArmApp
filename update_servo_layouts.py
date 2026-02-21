#!/usr/bin/env python3
"""
Script to update all servo layouts to use vertical Min/Max buttons
"""

import re

# Read the layout file
with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
    content = f.read()

# Pattern to match servo controls (servos 1-5)
servo_pattern = r'(<!-- Servo \d+ -->\s*<LinearLayout[^>]*>\s*<TextView[^>]*android:text="@string/servo_\d+"[^>]*/>\s*)(<LinearLayout[^>]*android:orientation="horizontal"[^>]*>.*?</LinearLayout>)'

def replace_servo_controls(match):
    servo_header = match.group(1)
    servo_num = re.search(r'servo_(\d+)', servo_header).group(1)
    
    return servo_header + f'''            <!-- Controls row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp">

                <!-- Min/Max buttons stacked vertically -->
                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:layout_marginEnd="8dp">

                    <Button
                        android:id="@+id/btnServo{servo_num}Min"
                        android:layout_width="120dp"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_min"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/purple_500"
                        android:layout_marginBottom="4dp" />

                    <Button
                        android:id="@+id/btnServo{servo_num}Max"
                        android:layout_width="120dp"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_max"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/teal_700" />

                </LinearLayout>

                <!-- Custom position input and set button -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_weight="1">

                    <EditText
                        android:id="@+id/etServo{servo_num}Position"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:hint="@string/position_hint"
                        android:inputType="number"
                        android:layout_marginEnd="8dp" />

                    <Button
                        android:id="@+id/btnServo{servo_num}Set"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_custom"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/purple_700" />

                </LinearLayout>

            </LinearLayout>'''

# Apply the pattern to servos 1-5
for servo_num in range(1, 6):
    pattern = f'(<!-- Servo {servo_num} -->\\s*<LinearLayout[^>]*>\\s*<TextView[^>]*android:text="@string/servo_{servo_num}"[^>]*/>\\s*)(<LinearLayout[^>]*android:orientation="horizontal"[^>]*>.*?</LinearLayout>)'
    
    def replace_func(match):
        return match.group(1) + f'''            <!-- Controls row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="8dp">

                <!-- Min/Max buttons stacked vertically -->
                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:layout_marginEnd="8dp">

                    <Button
                        android:id="@+id/btnServo{servo_num}Min"
                        android:layout_width="120dp"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_min"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/purple_500"
                        android:layout_marginBottom="4dp" />

                    <Button
                        android:id="@+id/btnServo{servo_num}Max"
                        android:layout_width="120dp"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_max"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/teal_700" />

                </LinearLayout>

                <!-- Custom position input and set button -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_weight="1">

                    <EditText
                        android:id="@+id/etServo{servo_num}Position"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:hint="@string/position_hint"
                        android:inputType="number"
                        android:layout_marginEnd="8dp" />

                    <Button
                        android:id="@+id/btnServo{servo_num}Set"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/servo_custom"
                        android:textColor="@color/white"
                        android:backgroundTint="@color/purple_700" />

                </LinearLayout>

            </LinearLayout>'''
    
    content = re.sub(pattern, replace_func, content, flags=re.DOTALL)

# Write the updated content back
with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
    f.write(content)

print("Servo layouts updated successfully!")
