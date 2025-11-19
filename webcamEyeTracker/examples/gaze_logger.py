#!/usr/bin/env python3
"""
Gaze position logger demo.
Records gaze positions to a CSV file for analysis.
"""
import sys
import os
import csv
import time
from datetime import datetime

# Add src directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from eye_tracker import EyeTracker
import cv2


def main():
    """Run gaze logging demo."""
    print("=== Webcam Eye Tracker - Gaze Logger ===\n")

    # Create output filename with timestamp
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = f"gaze_log_{timestamp}.csv"

    print(f"Logging gaze data to: {output_file}")
    print("\nControls:")
    print("  'q' - Quit and save log")
    print("  'c' - Run calibration")
    print("\nStarting eye tracker...\n")

    # Create eye tracker instance
    tracker = EyeTracker(camera_id=0)

    if not tracker.start_camera():
        return

    # Open CSV file for writing
    with open(output_file, 'w', newline='') as csvfile:
        fieldnames = ['timestamp', 'gaze_x', 'gaze_y']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        start_time = time.time()
        frame_count = 0

        try:
            while True:
                ret, frame = tracker.cap.read()
                if not ret:
                    print("Error reading frame")
                    break

                # Process frame
                gaze_position, annotated_frame = tracker.process_frame(frame)

                # Log gaze position if detected
                if gaze_position:
                    current_time = time.time() - start_time
                    writer.writerow({
                        'timestamp': f"{current_time:.3f}",
                        'gaze_x': gaze_position[0],
                        'gaze_y': gaze_position[1]
                    })
                    frame_count += 1

                # Display frame
                cv2.imshow('Gaze Logger', annotated_frame)

                # Add logging info to display
                info_text = f"Logged: {frame_count} samples | Press 'q' to quit"
                cv2.putText(annotated_frame, info_text, (10, 60),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    break
                elif key == ord('c'):
                    from calibration import run_calibration
                    tracker.stop_camera()
                    run_calibration(tracker)
                    tracker.start_camera()

        finally:
            tracker.stop_camera()

    print(f"\nLogged {frame_count} gaze samples to {output_file}")
    print("Demo complete!")


if __name__ == "__main__":
    main()
