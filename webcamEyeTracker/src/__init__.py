"""
Webcam Eye Tracker - Simple eye-gaze detection using MediaPipe
"""

from .eye_tracker import EyeTracker
from .calibration import run_calibration

__version__ = '1.0.0'
__all__ = ['EyeTracker', 'run_calibration']
