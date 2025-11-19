# Webcam Eye Tracker

A modern, accurate eye-gaze detection system using MediaPipe Face Mesh with iris tracking. This project enables real-time eye tracking using only a standard webcam, with calibration support for improved accuracy.

## Features

- **Real-time Eye Tracking**: Track eye gaze position in real-time using webcam input
- **High Accuracy**: Uses Google's MediaPipe Face Mesh with iris landmark detection
- **Calibration System**: Built-in calibration routine to improve tracking accuracy
- **Easy to Use**: Simple API and ready-to-run examples
- **Visual Feedback**: Live visualization of eye landmarks and gaze position
- **Data Logging**: Export gaze data for analysis

## Technology Stack

- **MediaPipe**: State-of-the-art face mesh and iris tracking
- **OpenCV**: Webcam capture and image processing
- **NumPy**: Numerical computations
- **Python 3.7+**: Modern Python with type hints

## Installation

### Prerequisites

- Python 3.7 or higher
- Webcam/camera device
- pip package manager

### Setup

1. **Clone the repository** (if not already done):
   ```bash
   cd webcamEyeTracker
   ```

2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Verify installation**:
   ```bash
   python -c "import mediapipe, cv2; print('Installation successful!')"
   ```

## Quick Start

### Basic Usage

Run the basic demo to see eye tracking in action:

```bash
python examples/basic_demo.py
```

**Controls:**
- `q` - Quit the application
- `c` - Run calibration routine

### With Calibration

For improved accuracy, run the calibration demo first:

```bash
python examples/calibration_demo.py
```

This will:
1. Guide you through a 9-point calibration process
2. Save calibration data for future sessions
3. Start live tracking with improved accuracy

### Logging Gaze Data

To record gaze positions to a CSV file:

```bash
python examples/gaze_logger.py
```

This creates a timestamped CSV file with gaze coordinates for later analysis.

## Calibration

The calibration routine significantly improves tracking accuracy by mapping your eye movements to screen coordinates.

### How Calibration Works

1. **Look at the target**: A green circle appears at specific screen positions
2. **Hold your gaze**: Keep looking at the circle while it's filled (about 1 second)
3. **Repeat**: The process repeats for 9 calibration points
4. **Automatic calculation**: The system calculates offset and scale corrections

### When to Calibrate

- **First time use**: Always calibrate when using the tracker for the first time
- **New environment**: Recalibrate if lighting conditions change significantly
- **Different position**: Recalibrate if you change your seating position
- **Poor accuracy**: If tracking seems inaccurate, run calibration again

### Calibration Data

Calibration data is saved to `calibration_data/calibration.json` and automatically loaded in future sessions.

## Project Structure

```
webcamEyeTracker/
├── src/
│   ├── __init__.py           # Package initialization
│   ├── eye_tracker.py        # Main eye tracking class
│   └── calibration.py        # Calibration routine
├── examples/
│   ├── basic_demo.py         # Simple tracking demo
│   ├── calibration_demo.py   # Calibration + tracking demo
│   └── gaze_logger.py        # Gaze data logger
├── calibration_data/         # Stored calibration files
├── requirements.txt          # Python dependencies
└── README.md                 # This file
```

## API Reference

### EyeTracker Class

```python
from eye_tracker import EyeTracker

# Create tracker instance
tracker = EyeTracker(camera_id=0)

# Start camera
tracker.start_camera()

# Process single frame
gaze_position, annotated_frame = tracker.process_frame(frame)

# Run live tracking
tracker.run_live()

# Stop camera
tracker.stop_camera()
```

### Calibration Function

```python
from calibration import run_calibration

# Run calibration with custom parameters
calibration_data = run_calibration(
    tracker,
    num_points=9,           # 5 or 9 recommended
    samples_per_point=30    # More samples = better accuracy
)
```

## How It Works

### Eye Tracking Pipeline

1. **Face Detection**: MediaPipe detects face landmarks in each frame
2. **Iris Tracking**: Specialized iris landmarks provide precise eye position
3. **Gaze Calculation**: Iris position relative to eye boundaries determines gaze direction
4. **Calibration Mapping**: Calibration data transforms raw gaze to screen coordinates
5. **Visualization**: Results are drawn on the video frame for feedback

### Technical Details

- **Face Mesh**: 478 facial landmarks including detailed eye region
- **Iris Landmarks**: 5 landmarks per iris for sub-pixel accuracy
- **Refresh Rate**: Processes 30+ frames per second on most systems
- **Calibration**: Linear transformation (offset + scale) with optional polynomial fitting

## Troubleshooting

### Camera Not Found

```
Error: Could not open camera 0
```

**Solutions:**
- Check camera is connected and not used by another application
- Try different camera ID: `EyeTracker(camera_id=1)`
- On Linux, ensure user has camera permissions

### Poor Tracking Accuracy

**Solutions:**
- Run calibration: Press `c` during tracking
- Ensure good lighting (avoid backlighting)
- Keep face centered in frame
- Maintain consistent distance from camera (50-80cm recommended)

### Low Frame Rate

**Solutions:**
- Close other applications using camera
- Reduce video resolution in camera settings
- Use a more powerful computer
- Ensure GPU acceleration is available for MediaPipe

### Import Errors

```
ModuleNotFoundError: No module named 'mediapipe'
```

**Solution:**
```bash
pip install -r requirements.txt
```

## Advanced Usage

### Custom Screen Dimensions

```python
tracker = EyeTracker(camera_id=0)

# Process with custom screen size
gaze_pos, frame = tracker.process_frame(frame)
custom_pos = tracker.estimate_screen_position(
    left_gaze, right_gaze,
    screen_width=2560,
    screen_height=1440
)
```

### Integration Example

```python
import cv2
from eye_tracker import EyeTracker

tracker = EyeTracker()
tracker.start_camera()

while True:
    ret, frame = tracker.cap.read()
    if not ret:
        break

    gaze_position, annotated_frame = tracker.process_frame(frame)

    if gaze_position:
        x, y = gaze_position
        # Use gaze position in your application
        print(f"User is looking at: ({x}, {y})")

    cv2.imshow('Eye Tracking', annotated_frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

tracker.stop_camera()
```

## Performance

- **Latency**: <50ms typical end-to-end latency
- **Accuracy**: ±50-100 pixels typical (improves significantly with calibration)
- **Frame Rate**: 30+ FPS on modern hardware
- **CPU Usage**: ~15-30% on typical laptop CPU

## Limitations

- Requires visible eyes (won't work with sunglasses)
- Accuracy decreases at extreme viewing angles
- Lighting conditions affect performance
- Single face tracking only
- Not suitable for precise clinical/medical applications without validation

## Contributing

Contributions are welcome! Areas for improvement:

- Multi-monitor support
- Advanced calibration algorithms (polynomial regression)
- Head pose compensation
- Blink detection
- Attention monitoring
- Performance optimizations

## License

See the main repository LICENSE file for details.

## Acknowledgments

- **MediaPipe**: Google's excellent face mesh and iris tracking solution
- **OpenCV**: Computer vision library for camera and image processing
- Eye tracking research community for algorithms and best practices

## References

- [MediaPipe Face Mesh](https://google.github.io/mediapipe/solutions/face_mesh.html)
- [MediaPipe Iris](https://google.github.io/mediapipe/solutions/iris.html)
- [Eye Tracking Research](https://en.wikipedia.org/wiki/Eye_tracking)

## Support

For issues, questions, or contributions, please refer to the main repository.

---

**Happy Eye Tracking! 👁️**
