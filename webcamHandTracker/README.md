# Webcam Hand Tracker

Real-time hand tracking using Google's MediaPipe library for gesture-based code control and interactive applications.

## Features

- **Real-time Hand Tracking**: Tracks 21 hand landmarks including all fingers, wrist, palm, and forearm
- **Gesture Recognition**: Detects common hand gestures (open hand, fist, point, peace, thumbs up)
- **Calibration System**: Built-in calibration routine to define workspace boundaries
- **Visual Feedback**: Live visualization of hand landmarks and tracking data
- **MediaPipe Integration**: Uses Google's MediaPipe Hands solution for accurate tracking
- **Single-hand Tracking**: Optimized for one-hand gesture control

## Hand Landmarks Tracked

The system tracks all 21 hand landmarks defined by MediaPipe:

- **Wrist** (1 point)
- **Thumb** (4 joints: CMC, MCP, IP, Tip)
- **Index Finger** (4 joints: MCP, PIP, DIP, Tip)
- **Middle Finger** (4 joints: MCP, PIP, DIP, Tip)
- **Ring Finger** (4 joints: MCP, PIP, DIP, Tip)
- **Pinky Finger** (4 joints: MCP, PIP, DIP, Tip)
- **Palm Center** (calculated from MCP joints)

## Prerequisites

- Modern web browser with webcam support (Chrome, Firefox, Edge, Safari)
- Node.js and npm (for local development server)
- Webcam/camera access

## Installation

1. Navigate to the project directory:
   ```bash
   cd webcamHandTracker
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Usage

### Starting the Application

1. Start the development server:
   ```bash
   npm start
   ```

2. The application will automatically open in your default browser at `http://localhost:8080`

3. If it doesn't open automatically, navigate to `http://localhost:8080` manually

### Using the Hand Tracker

1. **Start Tracking**
   - Click the "Start Tracking" button
   - Allow camera access when prompted by your browser
   - Your webcam feed will appear with hand tracking visualization

2. **Calibration** (Recommended)
   - Click the "Calibrate" button after starting tracking
   - Follow the on-screen instructions:
     - Step 1: Hold your hand at the center of the screen
     - Step 2: Move your hand to the top-left corner
     - Step 3: Move your hand to the top-right corner
     - Step 4: Move your hand to the bottom-left corner
     - Step 5: Move your hand to the bottom-right corner
   - Hold your hand steady at each position for 3 seconds
   - Calibration defines your workspace boundaries for normalized position data

3. **Gesture Control**
   - After calibration, the system will detect gestures in real-time
   - See the "Detected Gestures" panel for gesture history
   - Current gesture is displayed in the status overlay

### Supported Gestures

| Gesture | Description | Use Case |
|---------|-------------|----------|
| **Open Hand** | All fingers extended | Release, Reset, Stop |
| **Fist** | All fingers curled | Select, Grab, Activate |
| **Point** | Index finger extended | Navigate, Point, Select |
| **Peace** | Index and middle fingers extended | Confirm, OK, Two |
| **Thumbs Up** | Only thumb extended | Approve, Like, Next |

## Project Structure

```
webcamHandTracker/
├── src/
│   ├── handTracker.js      # Main hand tracking class with MediaPipe
│   ├── calibrationUI.js    # Calibration user interface
│   └── app.js              # Main application logic
├── index.html              # Main HTML interface
├── styles.css              # Styling and layout
├── package.json            # Dependencies and scripts
└── README.md               # This file
```

## API Reference

### HandTracker Class

```javascript
import { HandTracker } from './src/handTracker.js';

const tracker = new HandTracker(videoElement, canvasElement, {
  maxNumHands: 1,
  modelComplexity: 1,
  minDetectionConfidence: 0.7,
  minTrackingConfidence: 0.5
});

// Setup callbacks
tracker.onHandDetected = (handData) => {
  console.log('Hand detected:', handData);
};

tracker.onGestureDetected = (gestureData) => {
  console.log('Gesture:', gestureData.gesture);
};

// Start tracking
await tracker.start();

// Start calibration
await tracker.startCalibration();

// Get normalized position (requires calibration)
const position = tracker.getNormalizedPosition();
```

### Hand Data Structure

```javascript
{
  timestamp: 1234567890,
  handedness: "Right",
  confidence: 0.95,
  landmarks: {
    wrist: { x: 0.5, y: 0.5, z: 0.0 },
    thumb_tip: { x: 0.6, y: 0.4, z: -0.02 },
    index_tip: { x: 0.65, y: 0.3, z: -0.01 },
    // ... all 21 landmarks
  },
  fingerStates: {
    thumb: true,
    index: true,
    middle: false,
    ring: false,
    pinky: false
  }
}
```

## Integration with Code Control

The hand tracker is designed to integrate with code editors and development tools. Here are some example use cases:

### Example: Gesture-based Code Navigation

```javascript
tracker.onGestureDetected = (gestureData) => {
  switch (gestureData.gesture) {
    case 'fist':
      // Select current line/block
      editor.selectCurrentBlock();
      break;

    case 'open_hand':
      // Clear selection
      editor.clearSelection();
      break;

    case 'point':
      const position = tracker.getNormalizedPosition();
      // Map hand position to code position
      editor.setCursor(position.y * totalLines);
      break;

    case 'peace':
      // Confirm action
      editor.executeCommand();
      break;

    case 'thumbs_up':
      // Next function/block
      editor.goToNextBlock();
      break;
  }
};
```

### Example: Custom Gesture Detection

```javascript
// Access hand data for custom gesture detection
tracker.onHandDetected = (handData) => {
  const thumb = handData.landmarks.thumb_tip;
  const index = handData.landmarks.index_tip;

  // Detect pinch gesture
  const distance = Math.sqrt(
    Math.pow(thumb.x - index.x, 2) +
    Math.pow(thumb.y - index.y, 2)
  );

  if (distance < 0.05) {
    console.log('Pinch gesture detected!');
    // Trigger zoom or precision mode
  }
};
```

## Configuration

Adjust tracking parameters in `src/app.js`:

```javascript
const tracker = new HandTracker(videoElement, canvasElement, {
  maxNumHands: 1,              // Number of hands to track (1-2)
  modelComplexity: 1,          // Model complexity (0, 1, or 2)
  minDetectionConfidence: 0.7, // Minimum confidence for detection (0-1)
  minTrackingConfidence: 0.5,  // Minimum confidence for tracking (0-1)
  showLandmarkIndices: false   // Show landmark numbers on visualization
});
```

## Browser Compatibility

- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support (iOS 14.3+)
- Opera: Full support

## Performance Tips

- Use `modelComplexity: 0` for faster tracking on lower-end devices
- Ensure good lighting for better detection accuracy
- Keep your hand within the camera frame
- Perform calibration in the same position you'll be using the application

## Troubleshooting

**Camera not working:**
- Check browser permissions for camera access
- Ensure no other application is using the camera
- Try refreshing the page

**Poor tracking accuracy:**
- Improve lighting conditions
- Reduce background motion
- Perform calibration routine
- Adjust `minDetectionConfidence` and `minTrackingConfidence`

**Gestures not detected:**
- Complete the calibration routine first
- Ensure gestures are clear and distinct
- Check that only one hand is visible

## Future Enhancements

- Two-hand tracking support
- Custom gesture creation and training
- Gesture macros and sequences
- Position-based cursor control
- Depth-based interactions using Z-axis data
- Recording and playback of gesture sequences

## Technical Details

- **MediaPipe Hands**: Google's ML solution for hand tracking
- **Hand Landmarks**: 21 3D landmarks per hand
- **Coordinate System**: Normalized to [0, 1] range
- **Z-axis**: Depth relative to wrist (smaller = closer to camera)

## License

MIT License - See LICENSE file in the root directory

## Contributing

Contributions are welcome! This is a subproject of the slopground repository.

## References

- [MediaPipe Hands Documentation](https://google.github.io/mediapipe/solutions/hands.html)
- [MediaPipe Hand Landmark Model](https://google.github.io/mediapipe/solutions/hands#hand-landmark-model)
