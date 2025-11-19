#!/usr/bin/env python3
"""
Calibration demo for the webcam eye tracker.
Runs the calibration routine first, then shows tracking with improved accuracy.
"""
import sys
import os

# Add src directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from eye_tracker import EyeTracker
from calibration import run_calibration


def main():
    """Run calibration demo."""
    print("=== Webcam Eye Tracker - Calibration Demo ===\n")

    # Create eye tracker instance
    tracker = EyeTracker(camera_id=0)

    # Run calibration
    print("Starting calibration routine...")
    calibration_data = run_calibration(tracker, num_points=9, samples_per_point=30)

    if calibration_data is None:
        print("Calibration cancelled.")
        return

    print("\nCalibration complete! Starting live tracking with calibrated settings...")
    print("\nControls:")
    print("  'q' - Quit")
    print("  'c' - Re-run calibration")

    # Run live tracking with calibration
    tracker.run_live()

    print("\nDemo complete!")


if __name__ == "__main__":
    main()
