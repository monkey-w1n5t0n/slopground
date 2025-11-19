/**
 * Main application file for Webcam Hand Tracker
 */

import { HandTracker } from './handTracker.js';
import { CalibrationUI } from './calibrationUI.js';

class App {
  constructor() {
    this.tracker = null;
    this.calibrationUI = null;
    this.isInitialized = false;

    this.gestureLog = [];
    this.maxGestureLogSize = 50;

    this.init();
  }

  init() {
    // Wait for DOM to be ready
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => this.setupUI());
    } else {
      this.setupUI();
    }
  }

  setupUI() {
    // Get DOM elements
    this.videoElement = document.getElementById('video');
    this.canvasElement = document.getElementById('canvas');
    this.startBtn = document.getElementById('start-btn');
    this.stopBtn = document.getElementById('stop-btn');
    this.calibrateBtn = document.getElementById('calibrate-btn');

    // Set canvas dimensions
    this.canvasElement.width = 1280;
    this.canvasElement.height = 720;

    // Setup event listeners
    this.startBtn.addEventListener('click', () => this.startTracking());
    this.stopBtn.addEventListener('click', () => this.stopTracking());
    this.calibrateBtn.addEventListener('click', () => this.startCalibration());

    // Initialize calibration UI
    const calibrationContainer = document.getElementById('calibration-container');
    this.calibrationUI = new CalibrationUI(calibrationContainer);

    console.log('App initialized');
  }

  async startTracking() {
    if (this.isInitialized) {
      console.log('Tracker already initialized');
      return;
    }

    try {
      this.updateStatus('tracking-status', 'Starting...', '#f59e0b');

      // Create hand tracker instance
      this.tracker = new HandTracker(this.videoElement, this.canvasElement, {
        maxNumHands: 1,
        modelComplexity: 1,
        minDetectionConfidence: 0.7,
        minTrackingConfidence: 0.5,
        showLandmarkIndices: false
      });

      // Setup callbacks
      this.tracker.onHandDetected = (handData) => this.handleHandDetected(handData);
      this.tracker.onHandLost = () => this.handleHandLost();
      this.tracker.onGestureDetected = (gestureData) => this.handleGestureDetected(gestureData);
      this.tracker.onCalibrationComplete = (calibrationData) => this.handleCalibrationComplete(calibrationData);

      // Start tracking
      await this.tracker.start();

      this.isInitialized = true;

      // Update UI
      this.updateStatus('tracking-status', 'Active', '#10b981');
      this.startBtn.disabled = true;
      this.stopBtn.disabled = false;
      this.calibrateBtn.disabled = false;

      console.log('Tracking started successfully');
    } catch (error) {
      console.error('Failed to start tracking:', error);
      this.updateStatus('tracking-status', 'Error', '#ef4444');
      alert('Failed to start camera. Please ensure camera permissions are granted.');
    }
  }

  stopTracking() {
    if (!this.isInitialized) {
      return;
    }

    this.tracker.stop();
    this.isInitialized = false;

    // Update UI
    this.updateStatus('tracking-status', 'Stopped', '#6b7280');
    this.updateStatus('hand-status', 'No', '#ef4444');
    this.updateStatus('gesture-status', 'None', '#6b7280');

    this.startBtn.disabled = false;
    this.stopBtn.disabled = true;
    this.calibrateBtn.disabled = true;

    // Clear landmarks display
    const landmarksInfo = document.getElementById('landmarks-info');
    if (landmarksInfo) {
      landmarksInfo.innerHTML = '<p>Tracking stopped</p>';
    }

    console.log('Tracking stopped');
  }

  async startCalibration() {
    if (!this.isInitialized) {
      alert('Please start tracking first');
      return;
    }

    // Show calibration UI
    this.calibrationUI.show();

    // Start calibration process
    try {
      await this.tracker.startCalibration();
      this.updateStatus('calibration-status', 'Yes', '#10b981');
    } catch (error) {
      console.error('Calibration failed:', error);
      alert('Calibration failed. Please try again.');
      this.calibrationUI.hide();
    }
  }

  handleHandDetected(handData) {
    // Update status
    this.updateStatus('hand-status', 'Yes', '#10b981');

    // Update landmarks display
    this.updateLandmarksDisplay(handData);

    // Update gesture status
    const currentGesture = this.tracker.detectCurrentGesture();
    if (currentGesture && currentGesture !== 'unknown') {
      this.updateStatus('gesture-status', this.formatGestureName(currentGesture), '#10b981');
    } else {
      this.updateStatus('gesture-status', 'None', '#6b7280');
    }
  }

  handleHandLost() {
    this.updateStatus('hand-status', 'No', '#ef4444');
    this.updateStatus('gesture-status', 'None', '#6b7280');

    const landmarksInfo = document.getElementById('landmarks-info');
    if (landmarksInfo) {
      landmarksInfo.innerHTML = '<p>No hand detected</p>';
    }
  }

  handleGestureDetected(gestureData) {
    console.log('Gesture detected:', gestureData);

    // Add to gesture log
    this.addGestureToLog(gestureData);

    // You can add custom actions here based on detected gestures
    // For example, triggering specific code actions based on gestures
    switch (gestureData.gesture) {
      case 'fist':
        console.log('Fist gesture - could trigger: Select/Grab');
        break;
      case 'open_hand':
        console.log('Open hand gesture - could trigger: Release/Reset');
        break;
      case 'point':
        console.log('Point gesture - could trigger: Navigate/Select');
        break;
      case 'peace':
        console.log('Peace gesture - could trigger: Confirm/OK');
        break;
      case 'thumbs_up':
        console.log('Thumbs up gesture - could trigger: Approve/Next');
        break;
    }
  }

  handleCalibrationComplete(calibrationData) {
    console.log('Calibration complete:', calibrationData);
    this.updateStatus('calibration-status', 'Yes', '#10b981');

    // Hide calibration UI
    setTimeout(() => {
      if (this.calibrationUI) {
        this.calibrationUI.complete();
      }
    }, 500);
  }

  updateStatus(elementId, text, color) {
    const element = document.getElementById(elementId);
    if (element) {
      element.textContent = text;
      element.style.color = color;
    }
  }

  updateLandmarksDisplay(handData) {
    const landmarksInfo = document.getElementById('landmarks-info');
    if (!landmarksInfo) return;

    // Display key landmarks
    const keyLandmarks = [
      'wrist',
      'thumb_tip',
      'index_tip',
      'middle_tip',
      'ring_tip',
      'pinky_tip',
      'palm_center'
    ];

    let html = '';
    keyLandmarks.forEach(name => {
      const landmark = handData.landmarks[name];
      if (landmark) {
        html += `
          <div class="landmark-item">
            <span class="landmark-name">${this.formatLandmarkName(name)}</span>
            <div>X: ${landmark.x.toFixed(3)}</div>
            <div>Y: ${landmark.y.toFixed(3)}</div>
            <div>Z: ${landmark.z.toFixed(3)}</div>
          </div>
        `;
      }
    });

    // Add finger states
    if (handData.fingerStates) {
      html += `
        <div class="landmark-item" style="grid-column: 1 / -1;">
          <span class="landmark-name">Finger States</span>
          <div>Thumb: ${handData.fingerStates.thumb ? '✓' : '✗'}</div>
          <div>Index: ${handData.fingerStates.index ? '✓' : '✗'}</div>
          <div>Middle: ${handData.fingerStates.middle ? '✓' : '✗'}</div>
          <div>Ring: ${handData.fingerStates.ring ? '✓' : '✗'}</div>
          <div>Pinky: ${handData.fingerStates.pinky ? '✓' : '✗'}</div>
        </div>
      `;
    }

    landmarksInfo.innerHTML = html;
  }

  addGestureToLog(gestureData) {
    const gestureLog = document.getElementById('gesture-log');
    if (!gestureLog) return;

    // Add to internal log
    this.gestureLog.unshift(gestureData);
    if (this.gestureLog.length > this.maxGestureLogSize) {
      this.gestureLog.pop();
    }

    // Update display
    if (this.gestureLog.length === 0) {
      gestureLog.innerHTML = '<p>No gestures detected yet</p>';
      return;
    }

    let html = '';
    this.gestureLog.slice(0, 10).forEach(gesture => {
      const time = new Date(gesture.timestamp).toLocaleTimeString();
      html += `
        <div class="gesture-entry">
          <span class="time">${time}</span> -
          <span class="gesture">${this.formatGestureName(gesture.gesture)}</span>
        </div>
      `;
    });

    gestureLog.innerHTML = html;
  }

  formatLandmarkName(name) {
    return name
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  formatGestureName(gesture) {
    return gesture
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}

// Initialize app when page loads
new App();
