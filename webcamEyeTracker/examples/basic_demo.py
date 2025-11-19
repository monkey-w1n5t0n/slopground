#!/usr/bin/env python3
"""
Basic demo of the webcam eye tracker.
Shows live gaze tracking with visualization.
"""
import sys
import os

# Add src directory to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from eye_tracker import EyeTracker


def main():
    """Run basic eye tracking demo."""
    print("=== Webcam Eye Tracker - Basic Demo ===")
    print("\nControls:")
    print("  'q' - Quit")
    print("  'c' - Run calibration routine")
    print("\nStarting eye tracker...\n")

    # Create eye tracker instance
    tracker = EyeTracker(camera_id=0)

    # Run live tracking
    tracker.run_live()

    print("\nDemo complete!")


if __name__ == "__main__":
    main()
