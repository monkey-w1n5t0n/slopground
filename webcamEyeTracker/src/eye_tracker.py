"""
Webcam Eye Tracker using MediaPipe Face Mesh with Iris Tracking
"""
import cv2
import mediapipe as mp
import numpy as np
from typing import Tuple, Optional, List
import json
import os


class EyeTracker:
    """
    Eye tracker using MediaPipe Face Mesh for accurate gaze detection.
    Supports calibration for improved accuracy.
    """

    # MediaPipe Face Mesh landmark indices for eyes
    LEFT_EYE_INDICES = [33, 133, 160, 159, 158, 144, 145, 153]
    RIGHT_EYE_INDICES = [362, 263, 387, 386, 385, 373, 374, 380]
    LEFT_IRIS_INDICES = [468, 469, 470, 471, 472]
    RIGHT_IRIS_INDICES = [473, 474, 475, 476, 477]

    def __init__(self, camera_id: int = 0, calibration_file: str = None):
        """
        Initialize the eye tracker.

        Args:
            camera_id: Camera device ID (default 0 for primary webcam)
            calibration_file: Path to calibration data file
        """
        self.camera_id = camera_id
        self.cap = None
        self.calibration_data = None
        self.calibration_file = calibration_file or "calibration_data/calibration.json"

        # Initialize MediaPipe Face Mesh with iris tracking
        self.mp_face_mesh = mp.solutions.face_mesh
        self.face_mesh = self.mp_face_mesh.FaceMesh(
            max_num_faces=1,
            refine_landmarks=True,  # Enable iris landmarks
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )

        # Load calibration if available
        self.load_calibration()

    def start_camera(self) -> bool:
        """Start the webcam capture."""
        self.cap = cv2.VideoCapture(self.camera_id)
        if not self.cap.isOpened():
            print(f"Error: Could not open camera {self.camera_id}")
            return False
        return True

    def stop_camera(self):
        """Stop the webcam capture and release resources."""
        if self.cap is not None:
            self.cap.release()
        cv2.destroyAllWindows()

    def get_eye_landmarks(self, face_landmarks, frame_shape) -> Tuple[Optional[np.ndarray], Optional[np.ndarray]]:
        """
        Extract eye and iris landmarks from face mesh.

        Args:
            face_landmarks: MediaPipe face landmarks
            frame_shape: Shape of the video frame (height, width, channels)

        Returns:
            Tuple of (left_eye_data, right_eye_data) where each contains iris center and eye boundaries
        """
        h, w = frame_shape[:2]

        def get_landmarks_coords(indices):
            coords = []
            for idx in indices:
                landmark = face_landmarks.landmark[idx]
                coords.append([landmark.x * w, landmark.y * h])
            return np.array(coords)

        # Get iris centers
        left_iris = get_landmarks_coords(self.LEFT_IRIS_INDICES)
        right_iris = get_landmarks_coords(self.RIGHT_IRIS_INDICES)

        left_iris_center = np.mean(left_iris, axis=0)
        right_iris_center = np.mean(right_iris, axis=0)

        # Get eye boundaries
        left_eye_coords = get_landmarks_coords(self.LEFT_EYE_INDICES)
        right_eye_coords = get_landmarks_coords(self.RIGHT_EYE_INDICES)

        return (left_iris_center, left_eye_coords), (right_iris_center, right_eye_coords)

    def calculate_gaze_ratio(self, iris_center: np.ndarray, eye_coords: np.ndarray) -> Tuple[float, float]:
        """
        Calculate gaze position within the eye region.

        Args:
            iris_center: Center point of the iris
            eye_coords: Boundary points of the eye

        Returns:
            Tuple of (horizontal_ratio, vertical_ratio) where 0.5 is center
        """
        # Calculate eye bounding box
        eye_min = np.min(eye_coords, axis=0)
        eye_max = np.max(eye_coords, axis=0)
        eye_width = eye_max[0] - eye_min[0]
        eye_height = eye_max[1] - eye_min[1]

        # Avoid division by zero
        if eye_width == 0 or eye_height == 0:
            return 0.5, 0.5

        # Calculate relative position (0 to 1)
        h_ratio = (iris_center[0] - eye_min[0]) / eye_width
        v_ratio = (iris_center[1] - eye_min[1]) / eye_height

        # Clamp values
        h_ratio = np.clip(h_ratio, 0.0, 1.0)
        v_ratio = np.clip(v_ratio, 0.0, 1.0)

        return h_ratio, v_ratio

    def estimate_screen_position(self, left_gaze: Tuple[float, float],
                                 right_gaze: Tuple[float, float],
                                 screen_width: int = 1920,
                                 screen_height: int = 1080) -> Tuple[int, int]:
        """
        Estimate screen position from gaze ratios.

        Args:
            left_gaze: (h_ratio, v_ratio) for left eye
            right_gaze: (h_ratio, v_ratio) for right eye
            screen_width: Screen width in pixels
            screen_height: Screen height in pixels

        Returns:
            Estimated (x, y) position on screen
        """
        # Average both eyes
        avg_h = (left_gaze[0] + right_gaze[0]) / 2
        avg_v = (left_gaze[1] + right_gaze[1]) / 2

        # Apply calibration if available
        if self.calibration_data:
            avg_h, avg_v = self.apply_calibration(avg_h, avg_v)

        # Map to screen coordinates
        # Note: horizontal is inverted because looking left moves iris right
        screen_x = int((1 - avg_h) * screen_width)
        screen_y = int(avg_v * screen_height)

        return screen_x, screen_y

    def apply_calibration(self, h_ratio: float, v_ratio: float) -> Tuple[float, float]:
        """
        Apply calibration transformation to gaze ratios.

        Args:
            h_ratio: Raw horizontal gaze ratio
            v_ratio: Raw vertical gaze ratio

        Returns:
            Calibrated (h_ratio, v_ratio)
        """
        if not self.calibration_data:
            return h_ratio, v_ratio

        # Apply affine transformation from calibration
        h_offset = self.calibration_data.get('h_offset', 0)
        v_offset = self.calibration_data.get('v_offset', 0)
        h_scale = self.calibration_data.get('h_scale', 1)
        v_scale = self.calibration_data.get('v_scale', 1)

        calibrated_h = (h_ratio - h_offset) * h_scale
        calibrated_v = (v_ratio - v_offset) * v_scale

        return np.clip(calibrated_h, 0.0, 1.0), np.clip(calibrated_v, 0.0, 1.0)

    def process_frame(self, frame: np.ndarray) -> Tuple[Optional[Tuple[int, int]], np.ndarray]:
        """
        Process a single frame to detect gaze position.

        Args:
            frame: Input frame from webcam

        Returns:
            Tuple of (estimated_screen_position, annotated_frame)
        """
        # Convert to RGB for MediaPipe
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = self.face_mesh.process(rgb_frame)

        gaze_position = None

        if results.multi_face_landmarks:
            face_landmarks = results.multi_face_landmarks[0]

            # Get eye landmarks
            left_eye_data, right_eye_data = self.get_eye_landmarks(face_landmarks, frame.shape)

            if left_eye_data and right_eye_data:
                left_iris_center, left_eye_coords = left_eye_data
                right_iris_center, right_eye_coords = right_eye_data

                # Calculate gaze ratios
                left_gaze = self.calculate_gaze_ratio(left_iris_center, left_eye_coords)
                right_gaze = self.calculate_gaze_ratio(right_iris_center, right_eye_coords)

                # Estimate screen position
                gaze_position = self.estimate_screen_position(left_gaze, right_gaze)

                # Draw visualization
                self.draw_eye_tracking(frame, left_iris_center, right_iris_center,
                                      left_eye_coords, right_eye_coords, gaze_position)

        return gaze_position, frame

    def draw_eye_tracking(self, frame: np.ndarray, left_iris: np.ndarray, right_iris: np.ndarray,
                         left_eye: np.ndarray, right_eye: np.ndarray, gaze_pos: Tuple[int, int]):
        """Draw eye tracking visualization on frame."""
        # Draw eye boundaries
        for coords in [left_eye, right_eye]:
            for i in range(len(coords)):
                pt1 = tuple(coords[i].astype(int))
                pt2 = tuple(coords[(i + 1) % len(coords)].astype(int))
                cv2.line(frame, pt1, pt2, (0, 255, 0), 1)

        # Draw iris centers
        cv2.circle(frame, tuple(left_iris.astype(int)), 3, (0, 0, 255), -1)
        cv2.circle(frame, tuple(right_iris.astype(int)), 3, (0, 0, 255), -1)

        # Display gaze position
        if gaze_pos:
            cv2.putText(frame, f"Gaze: ({gaze_pos[0]}, {gaze_pos[1]})",
                       (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    def load_calibration(self) -> bool:
        """Load calibration data from file."""
        if os.path.exists(self.calibration_file):
            try:
                with open(self.calibration_file, 'r') as f:
                    self.calibration_data = json.load(f)
                print(f"Loaded calibration from {self.calibration_file}")
                return True
            except Exception as e:
                print(f"Error loading calibration: {e}")
        return False

    def save_calibration(self, calibration_data: dict):
        """Save calibration data to file."""
        os.makedirs(os.path.dirname(self.calibration_file), exist_ok=True)
        try:
            with open(self.calibration_file, 'w') as f:
                json.dump(calibration_data, f, indent=2)
            self.calibration_data = calibration_data
            print(f"Saved calibration to {self.calibration_file}")
        except Exception as e:
            print(f"Error saving calibration: {e}")

    def run_live(self):
        """Run live eye tracking with visualization."""
        if not self.start_camera():
            return

        print("Eye tracker running. Press 'q' to quit, 'c' to calibrate.")

        try:
            while True:
                ret, frame = self.cap.read()
                if not ret:
                    print("Error reading frame")
                    break

                gaze_position, annotated_frame = self.process_frame(frame)

                cv2.imshow('Eye Tracker', annotated_frame)

                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    break
                elif key == ord('c'):
                    from calibration import run_calibration
                    self.stop_camera()
                    run_calibration(self)
                    self.start_camera()

        finally:
            self.stop_camera()
