/**
 * HandTracker - Real-time hand tracking using MediaPipe
 * Tracks 21 hand landmarks including fingers, wrist, and palm
 */

import { Hands } from '@mediapipe/hands';
import { Camera } from '@mediapipe/camera_utils';

export class HandTracker {
  constructor(videoElement, canvasElement, options = {}) {
    this.videoElement = videoElement;
    this.canvasElement = canvasElement;
    this.canvasCtx = canvasElement.getContext('2d');

    // Configuration
    this.config = {
      maxNumHands: options.maxNumHands || 1,
      modelComplexity: options.modelComplexity || 1,
      minDetectionConfidence: options.minDetectionConfidence || 0.7,
      minTrackingConfidence: options.minTrackingConfidence || 0.5,
      ...options
    };

    // Hand landmarks data
    this.currentHandData = null;
    this.previousHandData = null;
    this.isTracking = false;

    // Calibration data
    this.calibrationData = {
      isCalibrated: false,
      referencePoint: null, // Wrist reference position
      handSize: null, // Distance from wrist to middle finger tip
      maxReach: null, // Maximum reach in each direction
      workspaceCenter: null
    };

    // Event callbacks
    this.onHandDetected = null;
    this.onHandLost = null;
    this.onGestureDetected = null;
    this.onCalibrationComplete = null;

    // Initialize MediaPipe Hands
    this.hands = null;
    this.camera = null;

    this.initializeMediaPipe();
  }

  /**
   * Initialize MediaPipe Hands solution
   */
  initializeMediaPipe() {
    this.hands = new Hands({
      locateFile: (file) => {
        return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`;
      }
    });

    this.hands.setOptions({
      maxNumHands: this.config.maxNumHands,
      modelComplexity: this.config.modelComplexity,
      minDetectionConfidence: this.config.minDetectionConfidence,
      minTrackingConfidence: this.config.minTrackingConfidence,
    });

    this.hands.onResults((results) => this.onResults(results));
  }

  /**
   * Start tracking
   */
  async start() {
    if (this.isTracking) {
      console.warn('Tracking already started');
      return;
    }

    try {
      this.camera = new Camera(this.videoElement, {
        onFrame: async () => {
          await this.hands.send({ image: this.videoElement });
        },
        width: 1280,
        height: 720
      });

      await this.camera.start();
      this.isTracking = true;
      console.log('Hand tracking started');
    } catch (error) {
      console.error('Failed to start camera:', error);
      throw error;
    }
  }

  /**
   * Stop tracking
   */
  stop() {
    if (this.camera) {
      this.camera.stop();
    }
    this.isTracking = false;
    console.log('Hand tracking stopped');
  }

  /**
   * Process MediaPipe results
   */
  onResults(results) {
    // Clear canvas
    this.canvasCtx.save();
    this.canvasCtx.clearRect(0, 0, this.canvasElement.width, this.canvasElement.height);

    // Draw video frame
    this.canvasCtx.drawImage(results.image, 0, 0, this.canvasElement.width, this.canvasElement.height);

    if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
      // Store previous data
      this.previousHandData = this.currentHandData;

      // Process first hand only (can be extended for multiple hands)
      const handLandmarks = results.multiHandLandmarks[0];
      const handedness = results.multiHandedness[0];

      // Extract hand data
      this.currentHandData = this.extractHandData(handLandmarks, handedness);

      // Draw hand landmarks
      this.drawHandLandmarks(handLandmarks);

      // Trigger callback
      if (this.onHandDetected) {
        this.onHandDetected(this.currentHandData);
      }

      // Detect gestures if calibrated
      if (this.calibrationData.isCalibrated) {
        this.detectGestures();
      }
    } else {
      // No hand detected
      if (this.currentHandData !== null && this.onHandLost) {
        this.onHandLost();
      }
      this.currentHandData = null;
    }

    this.canvasCtx.restore();
  }

  /**
   * Extract structured hand data from landmarks
   */
  extractHandData(landmarks, handedness) {
    const data = {
      timestamp: Date.now(),
      handedness: handedness.label, // "Left" or "Right"
      confidence: handedness.score,
      landmarks: {},
      vectors: {}
    };

    // MediaPipe Hand Landmark indices
    const landmarkNames = [
      'wrist',           // 0
      'thumb_cmc',       // 1
      'thumb_mcp',       // 2
      'thumb_ip',        // 3
      'thumb_tip',       // 4
      'index_mcp',       // 5
      'index_pip',       // 6
      'index_dip',       // 7
      'index_tip',       // 8
      'middle_mcp',      // 9
      'middle_pip',      // 10
      'middle_dip',      // 11
      'middle_tip',      // 12
      'ring_mcp',        // 13
      'ring_pip',        // 14
      'ring_dip',        // 15
      'ring_tip',        // 16
      'pinky_mcp',       // 17
      'pinky_pip',       // 18
      'pinky_dip',       // 19
      'pinky_tip'        // 20
    ];

    // Store all landmarks
    landmarks.forEach((landmark, index) => {
      data.landmarks[landmarkNames[index]] = {
        x: landmark.x,
        y: landmark.y,
        z: landmark.z,
        visibility: landmark.visibility
      };
    });

    // Calculate useful vectors and measurements
    const wrist = data.landmarks.wrist;

    // Forearm direction (approximate, using wrist and middle finger MCP)
    const middleMcp = data.landmarks.middle_mcp;
    data.vectors.forearm = {
      x: wrist.x - middleMcp.x,
      y: wrist.y - middleMcp.y,
      z: wrist.z - middleMcp.z
    };

    // Palm center (average of MCP joints)
    data.landmarks.palm_center = {
      x: (data.landmarks.index_mcp.x + data.landmarks.middle_mcp.x +
          data.landmarks.ring_mcp.x + data.landmarks.pinky_mcp.x) / 4,
      y: (data.landmarks.index_mcp.y + data.landmarks.middle_mcp.y +
          data.landmarks.ring_mcp.y + data.landmarks.pinky_mcp.y) / 4,
      z: (data.landmarks.index_mcp.z + data.landmarks.middle_mcp.z +
          data.landmarks.ring_mcp.z + data.landmarks.pinky_mcp.z) / 4
    };

    // Finger states (extended or curled)
    data.fingerStates = this.analyzeFingerStates(data.landmarks);

    return data;
  }

  /**
   * Analyze finger states (extended or curled)
   */
  analyzeFingerStates(landmarks) {
    const fingers = {
      thumb: this.isFingerExtended('thumb', landmarks),
      index: this.isFingerExtended('index', landmarks),
      middle: this.isFingerExtended('middle', landmarks),
      ring: this.isFingerExtended('ring', landmarks),
      pinky: this.isFingerExtended('pinky', landmarks)
    };

    return fingers;
  }

  /**
   * Check if a finger is extended
   */
  isFingerExtended(fingerName, landmarks) {
    const wrist = landmarks.wrist;

    if (fingerName === 'thumb') {
      const tip = landmarks.thumb_tip;
      const ip = landmarks.thumb_ip;
      const mcp = landmarks.thumb_mcp;

      // For thumb, check if tip is further from wrist than MCP
      const tipDist = this.distance3D(wrist, tip);
      const mcpDist = this.distance3D(wrist, mcp);
      return tipDist > mcpDist * 1.2;
    } else {
      const tip = landmarks[`${fingerName}_tip`];
      const pip = landmarks[`${fingerName}_pip`];
      const mcp = landmarks[`${fingerName}_mcp`];

      // Check if tip is further from wrist than PIP
      const tipDist = this.distance3D(wrist, tip);
      const pipDist = this.distance3D(wrist, pip);
      return tipDist > pipDist * 1.1;
    }
  }

  /**
   * Calculate 3D distance between two points
   */
  distance3D(p1, p2) {
    return Math.sqrt(
      Math.pow(p2.x - p1.x, 2) +
      Math.pow(p2.y - p1.y, 2) +
      Math.pow(p2.z - p1.z, 2)
    );
  }

  /**
   * Draw hand landmarks on canvas
   */
  drawHandLandmarks(landmarks) {
    const ctx = this.canvasCtx;
    const width = this.canvasElement.width;
    const height = this.canvasElement.height;

    // Connection pairs for hand skeleton
    const connections = [
      [0, 1], [1, 2], [2, 3], [3, 4],           // Thumb
      [0, 5], [5, 6], [6, 7], [7, 8],           // Index
      [0, 9], [9, 10], [10, 11], [11, 12],      // Middle
      [0, 13], [13, 14], [14, 15], [15, 16],    // Ring
      [0, 17], [17, 18], [18, 19], [19, 20],    // Pinky
      [5, 9], [9, 13], [13, 17]                 // Palm
    ];

    // Draw connections
    ctx.strokeStyle = '#00FF00';
    ctx.lineWidth = 2;
    connections.forEach(([start, end]) => {
      const startPoint = landmarks[start];
      const endPoint = landmarks[end];

      ctx.beginPath();
      ctx.moveTo(startPoint.x * width, startPoint.y * height);
      ctx.lineTo(endPoint.x * width, endPoint.y * height);
      ctx.stroke();
    });

    // Draw landmarks
    landmarks.forEach((landmark, index) => {
      const x = landmark.x * width;
      const y = landmark.y * height;

      // Different colors for different parts
      if (index === 0) {
        ctx.fillStyle = '#FF0000'; // Wrist - Red
      } else if ([4, 8, 12, 16, 20].includes(index)) {
        ctx.fillStyle = '#0000FF'; // Fingertips - Blue
      } else {
        ctx.fillStyle = '#00FF00'; // Joints - Green
      }

      ctx.beginPath();
      ctx.arc(x, y, 5, 0, 2 * Math.PI);
      ctx.fill();

      // Draw landmark index for debugging
      if (this.config.showLandmarkIndices) {
        ctx.fillStyle = '#FFFFFF';
        ctx.font = '10px Arial';
        ctx.fillText(index, x + 7, y + 3);
      }
    });
  }

  /**
   * Start calibration routine
   */
  async startCalibration() {
    console.log('Starting calibration...');

    return new Promise((resolve, reject) => {
      const calibrationSteps = [
        'Hold your hand in a relaxed position at the center of the screen',
        'Move your hand to the top-left corner',
        'Move your hand to the top-right corner',
        'Move your hand to the bottom-left corner',
        'Move your hand to the bottom-right corner'
      ];

      let currentStep = 0;
      const measurements = [];

      const stepInterval = setInterval(() => {
        if (!this.currentHandData) {
          console.log('Waiting for hand detection...');
          return;
        }

        if (currentStep < calibrationSteps.length) {
          console.log(`Step ${currentStep + 1}/${calibrationSteps.length}: ${calibrationSteps[currentStep]}`);

          // Capture measurement after 2 seconds
          setTimeout(() => {
            if (this.currentHandData) {
              measurements.push({
                step: currentStep,
                wrist: { ...this.currentHandData.landmarks.wrist },
                handSize: this.distance3D(
                  this.currentHandData.landmarks.wrist,
                  this.currentHandData.landmarks.middle_tip
                )
              });
              currentStep++;
            }
          }, 2000);
        } else {
          clearInterval(stepInterval);

          // Calculate calibration data
          this.calibrationData = {
            isCalibrated: true,
            referencePoint: measurements[0].wrist,
            handSize: measurements[0].handSize,
            maxReach: {
              left: Math.min(...measurements.map(m => m.wrist.x)),
              right: Math.max(...measurements.map(m => m.wrist.x)),
              top: Math.min(...measurements.map(m => m.wrist.y)),
              bottom: Math.max(...measurements.map(m => m.wrist.y))
            },
            workspaceCenter: measurements[0].wrist
          };

          console.log('Calibration complete!', this.calibrationData);

          if (this.onCalibrationComplete) {
            this.onCalibrationComplete(this.calibrationData);
          }

          resolve(this.calibrationData);
        }
      }, 3000);
    });
  }

  /**
   * Detect gestures based on hand data
   */
  detectGestures() {
    if (!this.currentHandData) return;

    const fingers = this.currentHandData.fingerStates;
    let gesture = null;

    // Define common gestures
    if (fingers.thumb && fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = 'peace';
    } else if (!fingers.thumb && fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = 'point';
    } else if (fingers.thumb && fingers.index && fingers.middle && fingers.ring && fingers.pinky) {
      gesture = 'open_hand';
    } else if (!fingers.thumb && !fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = 'fist';
    } else if (fingers.thumb && !fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = 'thumbs_up';
    } else if (!fingers.thumb && fingers.index && fingers.middle && !fingers.ring && !fingers.pinky) {
      gesture = 'peace';
    }

    if (gesture && this.onGestureDetected) {
      this.onGestureDetected({
        gesture,
        handData: this.currentHandData,
        timestamp: Date.now()
      });
    }
  }

  /**
   * Get current hand position relative to calibrated workspace
   */
  getNormalizedPosition() {
    if (!this.calibrationData.isCalibrated || !this.currentHandData) {
      return null;
    }

    const wrist = this.currentHandData.landmarks.wrist;
    const cal = this.calibrationData;

    return {
      x: (wrist.x - cal.maxReach.left) / (cal.maxReach.right - cal.maxReach.left),
      y: (wrist.y - cal.maxReach.top) / (cal.maxReach.bottom - cal.maxReach.top),
      z: wrist.z
    };
  }

  /**
   * Get current tracking status
   */
  getStatus() {
    return {
      isTracking: this.isTracking,
      isCalibrated: this.calibrationData.isCalibrated,
      handDetected: this.currentHandData !== null,
      currentGesture: this.currentHandData ? this.detectCurrentGesture() : null
    };
  }

  detectCurrentGesture() {
    // Helper method to get current gesture without triggering callbacks
    if (!this.currentHandData) return null;

    const fingers = this.currentHandData.fingerStates;

    if (fingers.thumb && fingers.index && fingers.middle && fingers.ring && fingers.pinky) {
      return 'open_hand';
    } else if (!fingers.thumb && !fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      return 'fist';
    } else if (!fingers.thumb && fingers.index && !fingers.middle && !fingers.ring && !fingers.pinky) {
      return 'point';
    }

    return 'unknown';
  }
}
