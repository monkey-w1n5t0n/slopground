"""
Calibration routine for improving eye tracking accuracy.
"""
import cv2
import numpy as np
from typing import List, Tuple
import time


class CalibrationPoint:
    """Represents a calibration point on the screen."""

    def __init__(self, screen_x: float, screen_y: float):
        """
        Args:
            screen_x: X position as ratio (0-1)
            screen_y: Y position as ratio (0-1)
        """
        self.screen_pos = (screen_x, screen_y)
        self.gaze_samples = []

    def add_sample(self, h_ratio: float, v_ratio: float):
        """Add a gaze measurement sample."""
        self.gaze_samples.append((h_ratio, v_ratio))

    def get_average_gaze(self) -> Tuple[float, float]:
        """Get average gaze position from all samples."""
        if not self.gaze_samples:
            return 0.5, 0.5
        avg_h = np.mean([s[0] for s in self.gaze_samples])
        avg_v = np.mean([s[1] for s in self.gaze_samples])
        return avg_h, avg_v


def run_calibration(eye_tracker, num_points: int = 9, samples_per_point: int = 30):
    """
    Run calibration routine to improve eye tracking accuracy.

    Args:
        eye_tracker: EyeTracker instance
        num_points: Number of calibration points (9 or 5 recommended)
        samples_per_point: Number of samples to collect per point

    Returns:
        Calibration data dictionary
    """
    print("\n=== Eye Tracker Calibration ===")
    print("Look at each green circle as it appears and keep your gaze steady.")
    print("Press SPACE to start calibration, ESC to cancel.\n")

    # Define calibration points (9-point calibration grid)
    if num_points == 9:
        positions = [
            (0.1, 0.1), (0.5, 0.1), (0.9, 0.1),  # Top row
            (0.1, 0.5), (0.5, 0.5), (0.9, 0.5),  # Middle row
            (0.1, 0.9), (0.5, 0.9), (0.9, 0.9),  # Bottom row
        ]
    elif num_points == 5:
        positions = [
            (0.5, 0.5),  # Center
            (0.1, 0.1), (0.9, 0.1),  # Top corners
            (0.1, 0.9), (0.9, 0.9),  # Bottom corners
        ]
    else:
        # Default to center + corners
        positions = [(0.5, 0.5), (0.1, 0.1), (0.9, 0.1), (0.1, 0.9), (0.9, 0.9)]

    calibration_points = [CalibrationPoint(x, y) for x, y in positions]

    # Start camera
    if not eye_tracker.start_camera():
        return None

    # Create calibration window
    screen_width = 800
    screen_height = 600

    # Wait for user to start
    waiting = True
    while waiting:
        ret, frame = eye_tracker.cap.read()
        if not ret:
            break

        # Show instructions
        display = np.zeros((screen_height, screen_width, 3), dtype=np.uint8)
        text_lines = [
            "Eye Tracker Calibration",
            "",
            "Instructions:",
            "1. Look at each green circle when it appears",
            "2. Keep your gaze steady on the circle",
            "3. The circle will disappear automatically",
            "",
            "Press SPACE to start",
            "Press ESC to cancel"
        ]

        y_offset = 150
        for i, line in enumerate(text_lines):
            font_scale = 1.0 if i == 0 else 0.6
            thickness = 2 if i == 0 else 1
            cv2.putText(display, line, (50, y_offset + i * 40),
                       cv2.FONT_HERSHEY_SIMPLEX, font_scale, (255, 255, 255), thickness)

        cv2.imshow('Calibration', display)

        key = cv2.waitKey(1) & 0xFF
        if key == ord(' '):
            waiting = False
        elif key == 27:  # ESC
            eye_tracker.stop_camera()
            cv2.destroyWindow('Calibration')
            return None

    # Run calibration for each point
    for idx, cal_point in enumerate(calibration_points):
        print(f"Calibration point {idx + 1}/{len(calibration_points)}")

        # Calculate screen position
        target_x = int(cal_point.screen_pos[0] * screen_width)
        target_y = int(cal_point.screen_pos[1] * screen_height)

        # Show countdown
        for countdown in range(3, 0, -1):
            display = np.zeros((screen_height, screen_width, 3), dtype=np.uint8)

            # Draw target point
            cv2.circle(display, (target_x, target_y), 30, (0, 255, 0), 2)
            cv2.circle(display, (target_x, target_y), 5, (0, 255, 0), -1)

            # Show countdown
            cv2.putText(display, f"Get ready: {countdown}", (screen_width // 2 - 100, 50),
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

            # Progress indicator
            cv2.putText(display, f"Point {idx + 1} of {len(calibration_points)}",
                       (10, screen_height - 20), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (200, 200, 200), 1)

            cv2.imshow('Calibration', display)
            cv2.waitKey(1000)

        # Collect samples
        sample_count = 0
        while sample_count < samples_per_point:
            ret, frame = eye_tracker.cap.read()
            if not ret:
                break

            # Process frame (without calibration)
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = eye_tracker.face_mesh.process(rgb_frame)

            if results.multi_face_landmarks:
                face_landmarks = results.multi_face_landmarks[0]
                left_eye_data, right_eye_data = eye_tracker.get_eye_landmarks(face_landmarks, frame.shape)

                if left_eye_data and right_eye_data:
                    left_iris_center, left_eye_coords = left_eye_data
                    right_iris_center, right_eye_coords = right_eye_data

                    # Calculate gaze ratios
                    left_gaze = eye_tracker.calculate_gaze_ratio(left_iris_center, left_eye_coords)
                    right_gaze = eye_tracker.calculate_gaze_ratio(right_iris_center, right_eye_coords)

                    # Average both eyes
                    avg_h = (left_gaze[0] + right_gaze[0]) / 2
                    avg_v = (left_gaze[1] + right_gaze[1]) / 2

                    cal_point.add_sample(avg_h, avg_v)
                    sample_count += 1

            # Show progress
            display = np.zeros((screen_height, screen_width, 3), dtype=np.uint8)

            # Draw target (filled circle for active sampling)
            cv2.circle(display, (target_x, target_y), 30, (0, 255, 0), -1)
            cv2.circle(display, (target_x, target_y), 5, (255, 255, 255), -1)

            # Progress bar
            progress = sample_count / samples_per_point
            bar_width = 300
            bar_x = (screen_width - bar_width) // 2
            bar_y = screen_height - 100
            cv2.rectangle(display, (bar_x, bar_y), (bar_x + bar_width, bar_y + 30), (100, 100, 100), 2)
            cv2.rectangle(display, (bar_x, bar_y), (bar_x + int(bar_width * progress), bar_y + 30), (0, 255, 0), -1)

            cv2.putText(display, "Look at the green circle", (screen_width // 2 - 150, 50),
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

            cv2.imshow('Calibration', display)
            cv2.waitKey(30)

        print(f"  Collected {sample_count} samples")

    # Calculate calibration parameters
    calibration_data = calculate_calibration_transform(calibration_points)

    # Save calibration
    eye_tracker.save_calibration(calibration_data)

    # Show completion message
    display = np.zeros((screen_height, screen_width, 3), dtype=np.uint8)
    cv2.putText(display, "Calibration Complete!", (screen_width // 2 - 200, screen_height // 2),
               cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)
    cv2.putText(display, "Press any key to continue", (screen_width // 2 - 150, screen_height // 2 + 60),
               cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
    cv2.imshow('Calibration', display)
    cv2.waitKey(2000)

    cv2.destroyWindow('Calibration')

    return calibration_data


def calculate_calibration_transform(calibration_points: List[CalibrationPoint]) -> dict:
    """
    Calculate calibration transformation from collected data.

    Args:
        calibration_points: List of CalibrationPoint objects with samples

    Returns:
        Dictionary with calibration parameters
    """
    # Collect all screen positions and corresponding gaze measurements
    screen_positions = []
    gaze_positions = []

    for point in calibration_points:
        avg_h, avg_v = point.get_average_gaze()
        screen_positions.append(point.screen_pos)
        gaze_positions.append((avg_h, avg_v))

    screen_positions = np.array(screen_positions)
    gaze_positions = np.array(gaze_positions)

    # Calculate simple linear transformation (offset and scale)
    # This is a basic calibration; more sophisticated methods could use polynomial regression

    # Calculate offset (difference from center)
    gaze_center = np.mean(gaze_positions, axis=0)
    screen_center = np.mean(screen_positions, axis=0)

    h_offset = gaze_center[0] - screen_center[0]
    v_offset = gaze_center[1] - screen_center[1]

    # Calculate scale (spread ratio)
    gaze_spread_h = np.std(gaze_positions[:, 0])
    gaze_spread_v = np.std(gaze_positions[:, 1])
    screen_spread_h = np.std(screen_positions[:, 0])
    screen_spread_v = np.std(screen_positions[:, 1])

    h_scale = screen_spread_h / gaze_spread_h if gaze_spread_h > 0 else 1.0
    v_scale = screen_spread_v / gaze_spread_v if gaze_spread_v > 0 else 1.0

    calibration_data = {
        'h_offset': float(h_offset),
        'v_offset': float(v_offset),
        'h_scale': float(h_scale),
        'v_scale': float(v_scale),
        'num_points': len(calibration_points),
        'timestamp': time.time()
    }

    print("\nCalibration parameters:")
    print(f"  Horizontal offset: {h_offset:.4f}, scale: {h_scale:.4f}")
    print(f"  Vertical offset: {v_offset:.4f}, scale: {v_scale:.4f}")

    return calibration_data
