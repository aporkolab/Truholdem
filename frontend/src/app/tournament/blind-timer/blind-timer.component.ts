import {
  Component,
  input,
  ChangeDetectionStrategy,
  computed,
  signal,
  OnInit,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { timer, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { BlindLevel, formatTimeRemaining, calculateTimeRemaining } from '../../model/tournament';

@Component({
  selector: 'app-blind-timer',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="blind-timer"
      [class.warning]="isWarning()"
      [class.critical]="isCritical()"
      [class.break]="isOnBreak()"
      data-cy="blind-timer"
      role="timer"
      [attr.aria-label]="ariaLabel()"
    >
      <!-- Level Badge -->
      <div class="level-badge" data-cy="level-badge">
        Level {{ level() }}
      </div>

      <!-- Current Blinds -->
      <div class="current-blinds" data-cy="current-blinds">
        <span class="blinds-label">Blinds</span>
        <span class="blinds-value">
          {{ smallBlind() | number }}/{{ bigBlind() | number }}
        </span>
        @if (ante() > 0) {
          <span class="ante-value">Ante: {{ ante() | number }}</span>
        }
      </div>

      <!-- Timer Display -->
      <div class="timer-display" data-cy="timer-display">
        <svg class="timer-ring" viewBox="0 0 100 100">
          <circle
            class="timer-ring-bg"
            cx="50" cy="50" r="45"
          />
          <circle
            class="timer-ring-progress"
            cx="50" cy="50" r="45"
            [style.stroke-dasharray]="circumference"
            [style.stroke-dashoffset]="dashOffset()"
          />
        </svg>
        <div class="timer-text">
          <span class="time-remaining" data-cy="time-remaining">
            {{ formattedTime() }}
          </span>
          <span class="timer-label">remaining</span>
        </div>
      </div>

      <!-- Next Blinds Preview -->
      @if (hasNextLevel()) {
        <div class="next-blinds" data-cy="next-blinds">
          <span class="next-label">Next:</span>
          <span class="next-value">
            {{ nextSmallBlind() | number }}/{{ nextBigBlind() | number }}
          </span>
          @if (nextAnte() > 0) {
            <span class="next-ante">(Ante: {{ nextAnte() | number }})</span>
          }
        </div>
      }

      <!-- Break Indicator -->
      @if (isOnBreak()) {
        <div class="break-indicator" data-cy="break-indicator">
          ☕ Break Time!
        </div>
      }
    </div>
  `,
  styles: [`
    .blind-timer {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      padding: 1.25rem;
      background: rgba(0, 0, 0, 0.4);
      border: 2px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      min-width: 180px;
      transition: all 0.3s ease;
    }

    .blind-timer.warning {
      border-color: rgba(245, 158, 11, 0.5);
      background: rgba(245, 158, 11, 0.1);
    }

    .blind-timer.critical {
      border-color: rgba(239, 68, 68, 0.5);
      background: rgba(239, 68, 68, 0.15);
      animation: pulse-critical 1s ease-in-out infinite;
    }

    .blind-timer.break {
      border-color: rgba(34, 197, 94, 0.5);
      background: rgba(34, 197, 94, 0.1);
    }

    @keyframes pulse-critical {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.02); }
    }

    .level-badge {
      padding: 0.25rem 0.75rem;
      background: rgba(59, 130, 246, 0.3);
      color: #60a5fa;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .current-blinds {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
    }

    .blinds-label {
      font-size: 0.75rem;
      color: #94a3b8;
      text-transform: uppercase;
    }

    .blinds-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: #fff;
    }

    .ante-value {
      font-size: 0.875rem;
      color: #fbbf24;
    }

    .timer-display {
      position: relative;
      width: 120px;
      height: 120px;
    }

    .timer-ring {
      width: 100%;
      height: 100%;
      transform: rotate(-90deg);
    }

    .timer-ring-bg {
      fill: none;
      stroke: rgba(255, 255, 255, 0.1);
      stroke-width: 6;
    }

    .timer-ring-progress {
      fill: none;
      stroke: #3b82f6;
      stroke-width: 6;
      stroke-linecap: round;
      transition: stroke-dashoffset 1s linear;
    }

    .blind-timer.warning .timer-ring-progress {
      stroke: #f59e0b;
    }

    .blind-timer.critical .timer-ring-progress {
      stroke: #ef4444;
    }

    .timer-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .time-remaining {
      font-size: 1.5rem;
      font-weight: 700;
      font-family: 'Courier New', monospace;
      color: #fff;
    }

    .timer-label {
      font-size: 0.625rem;
      color: #94a3b8;
      text-transform: uppercase;
    }

    .next-blinds {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 0.75rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 0.5rem;
      font-size: 0.875rem;
    }

    .next-label {
      color: #94a3b8;
    }

    .next-value {
      color: #e2e8f0;
      font-weight: 600;
    }

    .next-ante {
      color: #fbbf24;
      font-size: 0.75rem;
    }

    .break-indicator {
      padding: 0.5rem 1rem;
      background: rgba(34, 197, 94, 0.2);
      color: #34d399;
      border-radius: 0.5rem;
      font-weight: 600;
      animation: pulse-break 2s ease-in-out infinite;
    }

    @keyframes pulse-break {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.7; }
    }
  `]
})
export class BlindTimerComponent implements OnInit, OnDestroy {

  readonly currentBlinds = input<BlindLevel | null>(null);
  readonly nextBlinds = input<BlindLevel | null>(null);
  readonly levelEndTime = input<number>(0);
  readonly levelDurationMinutes = input<number>(15);
  readonly isOnBreak = input<boolean>(false);

  private readonly destroy$ = new Subject<void>();


  private readonly timeRemaining$ = signal<number>(0);


  readonly level = computed(() => this.currentBlinds()?.level ?? 1);
  readonly smallBlind = computed(() => this.currentBlinds()?.smallBlind ?? 0);
  readonly bigBlind = computed(() => this.currentBlinds()?.bigBlind ?? 0);
  readonly ante = computed(() => this.currentBlinds()?.ante ?? 0);


  readonly nextSmallBlind = computed(() => this.nextBlinds()?.smallBlind ?? 0);
  readonly nextBigBlind = computed(() => this.nextBlinds()?.bigBlind ?? 0);
  readonly nextAnte = computed(() => this.nextBlinds()?.ante ?? 0);
  readonly hasNextLevel = computed(() => this.nextBlinds() !== null);


  readonly formattedTime = computed(() => {
    const time = this.timeRemaining$();
    return formatTimeRemaining(time);
  });

  readonly isWarning = computed(() => {
    const time = this.timeRemaining$();
    const warningThreshold = 60000;
    return time > 0 && time <= warningThreshold && time > 15000;
  });

  readonly isCritical = computed(() => {
    const time = this.timeRemaining$();
    return time > 0 && time <= 15000;
  });


  readonly circumference = 2 * Math.PI * 45;

  readonly dashOffset = computed(() => {
    const time = this.timeRemaining$();
    const totalMs = this.levelDurationMinutes() * 60 * 1000;
    const progress = time / totalMs;
    return this.circumference * (1 - progress);
  });

  readonly ariaLabel = computed(() => {
    const blinds = `${this.smallBlind()}/${this.bigBlind()}`;
    const time = this.formattedTime();
    return `Level ${this.level()}, Blinds ${blinds}, ${time} remaining`;
  });

  ngOnInit(): void {
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startTimer(): void {
    timer(0, 100).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const time = calculateTimeRemaining(this.levelEndTime());
      this.timeRemaining$.set(time);
    });
  }
}
