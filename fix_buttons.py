#!/usr/bin/env python3
"""
Script to fix button styling in the Android layout
"""

import re

# Read the layout file
with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
    content = f.read()

# Define button patterns and their replacements
button_patterns = [
    # Min buttons
    (r'(<Button\s+android:id="@\+id/btnServo\d+Min"[^>]*android:text="@string/servo_min"[^>]*)(>)', 
     r'\1\n                    android:textColor="@color/white"\n                    android:backgroundTint="@color/purple_500"\2'),
    
    # Max buttons
    (r'(<Button\s+android:id="@\+id/btnServo\d+Max"[^>]*android:text="@string/servo_max"[^>]*)(>)', 
     r'\1\n                    android:textColor="@color/white"\n                    android:backgroundTint="@color/teal_700"\2'),
    
    # Set buttons
    (r'(<Button\s+android:id="@\+id/btnServo\d+Set"[^>]*android:text="@string/servo_custom"[^>]*)(>)', 
     r'\1\n                    android:textColor="@color/white"\n                    android:backgroundTint="@color/purple_700"\2'),
]

# Apply the patterns
for pattern, replacement in button_patterns:
    content = re.sub(pattern, replacement, content)

# Write the updated content back
with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
    f.write(content)

print("Button styling updated successfully!")
