/**
 * CalibrationUI - User interface for hand tracking calibration
 */

export class CalibrationUI {
  constructor(containerElement) {
    this.container = containerElement;
    this.currentStep = 0;
    this.totalSteps = 5;
    this.isCalibrating = false;

    this.steps = [
      {
        title: 'Center Position',
        instruction: 'Hold your hand in a relaxed position at the center of the screen',
        position: 'center',
        duration: 3000
      },
      {
        title: 'Top-Left Corner',
        instruction: 'Move your hand to the top-left corner of the screen',
        position: 'top-left',
        duration: 3000
      },
      {
        title: 'Top-Right Corner',
        instruction: 'Move your hand to the top-right corner of the screen',
        position: 'top-right',
        duration: 3000
      },
      {
        title: 'Bottom-Left Corner',
        instruction: 'Move your hand to the bottom-left corner of the screen',
        position: 'bottom-left',
        duration: 3000
      },
      {
        title: 'Bottom-Right Corner',
        instruction: 'Move your hand to the bottom-right corner of the screen',
        position: 'bottom-right',
        duration: 3000
      }
    ];

    this.createUI();
  }

  createUI() {
    this.container.innerHTML = `
      <div id="calibration-overlay" style="display: none;">
        <div class="calibration-content">
          <h2 id="calibration-title">Hand Tracking Calibration</h2>
          <div id="calibration-progress">
            <div class="progress-bar">
              <div id="progress-fill" class="progress-fill"></div>
            </div>
            <div id="step-counter">Step 0 of ${this.totalSteps}</div>
          </div>
          <div id="calibration-instruction">
            Click "Start Calibration" to begin
          </div>
          <div id="position-indicator"></div>
          <div id="countdown"></div>
          <div class="calibration-buttons">
            <button id="start-calibration-btn" class="btn btn-primary">Start Calibration</button>
            <button id="cancel-calibration-btn" class="btn btn-secondary" style="display: none;">Cancel</button>
          </div>
        </div>
      </div>
    `;

    // Add event listeners
    const startBtn = document.getElementById('start-calibration-btn');
    const cancelBtn = document.getElementById('cancel-calibration-btn');

    if (startBtn) {
      startBtn.addEventListener('click', () => this.start());
    }

    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => this.cancel());
    }
  }

  show() {
    const overlay = document.getElementById('calibration-overlay');
    if (overlay) {
      overlay.style.display = 'flex';
    }
  }

  hide() {
    const overlay = document.getElementById('calibration-overlay');
    if (overlay) {
      overlay.style.display = 'none';
    }
    this.reset();
  }

  start() {
    this.isCalibrating = true;
    this.currentStep = 0;

    const startBtn = document.getElementById('start-calibration-btn');
    const cancelBtn = document.getElementById('cancel-calibration-btn');

    if (startBtn) startBtn.style.display = 'none';
    if (cancelBtn) cancelBtn.style.display = 'inline-block';

    this.nextStep();
  }

  nextStep() {
    if (this.currentStep >= this.totalSteps) {
      this.complete();
      return;
    }

    const step = this.steps[this.currentStep];

    // Update UI
    this.updateProgress();
    this.updateInstruction(step);
    this.showPositionIndicator(step.position);

    // Start countdown
    this.startCountdown(step.duration / 1000);
  }

  updateProgress() {
    const progressFill = document.getElementById('progress-fill');
    const stepCounter = document.getElementById('step-counter');

    const progressPercent = ((this.currentStep + 1) / this.totalSteps) * 100;

    if (progressFill) {
      progressFill.style.width = `${progressPercent}%`;
    }

    if (stepCounter) {
      stepCounter.textContent = `Step ${this.currentStep + 1} of ${this.totalSteps}`;
    }
  }

  updateInstruction(step) {
    const instructionEl = document.getElementById('calibration-instruction');
    if (instructionEl) {
      instructionEl.innerHTML = `
        <h3>${step.title}</h3>
        <p>${step.instruction}</p>
      `;
    }
  }

  showPositionIndicator(position) {
    const indicator = document.getElementById('position-indicator');
    if (!indicator) return;

    const positions = {
      'center': { top: '50%', left: '50%' },
      'top-left': { top: '10%', left: '10%' },
      'top-right': { top: '10%', left: '90%' },
      'bottom-left': { top: '90%', left: '10%' },
      'bottom-right': { top: '90%', left: '90%' }
    };

    const pos = positions[position];
    indicator.innerHTML = '<div class="position-marker"></div>';
    const marker = indicator.querySelector('.position-marker');

    if (marker && pos) {
      marker.style.top = pos.top;
      marker.style.left = pos.left;
      marker.style.display = 'block';
    }
  }

  startCountdown(seconds) {
    const countdownEl = document.getElementById('countdown');
    let remaining = seconds;

    const updateCountdown = () => {
      if (countdownEl) {
        countdownEl.textContent = `${Math.ceil(remaining)}s`;
      }
    };

    updateCountdown();

    const interval = setInterval(() => {
      remaining -= 0.1;
      updateCountdown();

      if (remaining <= 0) {
        clearInterval(interval);
        this.currentStep++;
        setTimeout(() => this.nextStep(), 500);
      }
    }, 100);

    this.countdownInterval = interval;
  }

  cancel() {
    this.isCalibrating = false;

    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }

    this.hide();
  }

  complete() {
    this.isCalibrating = false;

    const instructionEl = document.getElementById('calibration-instruction');
    const countdownEl = document.getElementById('countdown');

    if (instructionEl) {
      instructionEl.innerHTML = `
        <h3>Calibration Complete!</h3>
        <p>Your hand tracking workspace has been configured.</p>
      `;
    }

    if (countdownEl) {
      countdownEl.textContent = '';
    }

    setTimeout(() => {
      this.hide();
    }, 2000);
  }

  reset() {
    this.currentStep = 0;
    this.isCalibrating = false;

    const startBtn = document.getElementById('start-calibration-btn');
    const cancelBtn = document.getElementById('cancel-calibration-btn');
    const instructionEl = document.getElementById('calibration-instruction');

    if (startBtn) startBtn.style.display = 'inline-block';
    if (cancelBtn) cancelBtn.style.display = 'none';
    if (instructionEl) {
      instructionEl.textContent = 'Click "Start Calibration" to begin';
    }
  }
}
