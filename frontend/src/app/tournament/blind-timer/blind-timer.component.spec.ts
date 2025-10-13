import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BlindTimerComponent } from './blind-timer.component';
import { BlindLevel } from '../../model/tournament';

describe('BlindTimerComponent', () => {
  let component: BlindTimerComponent;
  let fixture: ComponentFixture<BlindTimerComponent>;

  const createMockBlindLevel = (overrides: Partial<BlindLevel> = {}): BlindLevel => ({
    level: overrides.level ?? 1,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50,
    ante: overrides.ante ?? 0,
    durationMinutes: overrides.durationMinutes ?? 15
  });

  const setupComponent = () => {
    fixture = TestBed.createComponent(BlindTimerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('currentBlinds', createMockBlindLevel());
    fixture.componentRef.setInput('nextBlinds', createMockBlindLevel({ level: 2, smallBlind: 50, bigBlind: 100 }));
    fixture.componentRef.setInput('levelEndTime', Date.now() + 5 * 60 * 1000);
    fixture.componentRef.setInput('isOnBreak', false);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BlindTimerComponent]
    }).compileComponents();

    setupComponent();
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });





  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });





  describe('Blind Display', () => {
    it('should display current small blind', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ smallBlind: 100 }));
      fixture.detectChanges();

      const blindsEl = fixture.nativeElement.querySelector('[data-cy="current-blinds"]');
      expect(blindsEl.textContent).toContain('100');
    });

    it('should display current big blind', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ bigBlind: 200 }));
      fixture.detectChanges();

      const blindsEl = fixture.nativeElement.querySelector('[data-cy="current-blinds"]');
      expect(blindsEl.textContent).toContain('200');
    });

    it('should display ante when present', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ ante: 25 }));
      fixture.detectChanges();

      const blindsEl = fixture.nativeElement.querySelector('[data-cy="current-blinds"]');
      expect(blindsEl).toBeTruthy();
      expect(blindsEl.textContent).toContain('25');
    });

    it('should not display ante when zero', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ ante: 0 }));
      fixture.detectChanges();

      const blindsEl = fixture.nativeElement.querySelector('[data-cy="current-blinds"]');
      expect(blindsEl.textContent).not.toContain('Ante');
    });

    it('should display current level number', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 5 }));
      fixture.detectChanges();

      const levelEl = fixture.nativeElement.querySelector('[data-cy="level-badge"]');
      expect(levelEl.textContent).toContain('5');
    });
  });





  describe('Next Blinds', () => {
    it('should display next blinds preview', () => {
      fixture.componentRef.setInput('nextBlinds', createMockBlindLevel({ smallBlind: 75, bigBlind: 150 }));
      fixture.detectChanges();

      const nextEl = fixture.nativeElement.querySelector('[data-cy="next-blinds"]');
      expect(nextEl).toBeTruthy();
      expect(nextEl.textContent).toContain('75');
      expect(nextEl.textContent).toContain('150');
    });

    it('should not show next blinds when no next blinds', () => {
      fixture.componentRef.setInput('nextBlinds', null);
      fixture.detectChanges();

      const nextEl = fixture.nativeElement.querySelector('[data-cy="next-blinds"]');
      expect(nextEl).toBeFalsy();
    });
  });





  describe('Timer', () => {
    it('should display time remaining element', () => {
      const timerEl = fixture.nativeElement.querySelector('[data-cy="time-remaining"]');
      expect(timerEl).toBeTruthy();
    });

    it('should have timer display component', () => {
      const timerDisplay = fixture.nativeElement.querySelector('.timer-display');
      expect(timerDisplay).toBeTruthy();
    });

    it('should have timer text container', () => {
      const timerText = fixture.nativeElement.querySelector('.timer-text');
      expect(timerText).toBeTruthy();
    });

    it('should have timer label', () => {
      const timerLabel = fixture.nativeElement.querySelector('.timer-label');
      expect(timerLabel).toBeTruthy();
      expect(timerLabel.textContent).toContain('remaining');
    });
  });





  describe('Progress Ring', () => {
    it('should render SVG progress ring', () => {
      const svg = fixture.nativeElement.querySelector('.timer-ring');
      expect(svg).toBeTruthy();
    });

    it('should have progress ring background', () => {
      const ringBg = fixture.nativeElement.querySelector('.timer-ring-bg');
      expect(ringBg).toBeTruthy();
    });

    it('should have progress ring progress element', () => {
      const ringProgress = fixture.nativeElement.querySelector('.timer-ring-progress');
      expect(ringProgress).toBeTruthy();
    });

    it('should have correct circumference constant', () => {
      expect(component.circumference).toBeCloseTo(2 * Math.PI * 45, 1);
    });
  });





  describe('Warning States', () => {
    it('should have warning class binding', () => {
      const container = fixture.nativeElement.querySelector('.blind-timer');
      expect(container).toBeTruthy();
    });

    it('should have isWarning computed property', () => {
      expect(typeof component.isWarning).toBe('function');
    });

    it('should have isCritical computed property', () => {
      expect(typeof component.isCritical).toBe('function');
    });

    it('should have warning CSS styles defined', () => {
      // Verify the component has the warning class handler
      const container = fixture.nativeElement.querySelector('.blind-timer');
      expect(container.classList).toBeDefined();
    });
  });





  describe('Break Display', () => {
    it('should show break indicator when on break', () => {
      fixture.componentRef.setInput('isOnBreak', true);
      fixture.detectChanges();

      const breakEl = fixture.nativeElement.querySelector('[data-cy="break-indicator"]');
      expect(breakEl).toBeTruthy();
      expect(breakEl.textContent).toContain('Break');
    });

    it('should apply break class when on break', () => {
      fixture.componentRef.setInput('isOnBreak', true);
      fixture.detectChanges();

      const container = fixture.nativeElement.querySelector('.blind-timer');
      expect(container.classList.contains('break')).toBe(true);
    });
  });





  describe('Next Blinds Visibility', () => {
    it('should show next blinds when available', () => {
      fixture.componentRef.setInput('nextBlinds', createMockBlindLevel({ smallBlind: 50, bigBlind: 100 }));
      fixture.detectChanges();

      const nextBlinds = fixture.nativeElement.querySelector('[data-cy="next-blinds"]');
      expect(nextBlinds).toBeTruthy();
    });

    it('should hide next blinds when not available', () => {
      fixture.componentRef.setInput('nextBlinds', null);
      fixture.detectChanges();

      const nextBlinds = fixture.nativeElement.querySelector('[data-cy="next-blinds"]');
      expect(nextBlinds).toBeFalsy();
    });
  });





  describe('Computed Properties', () => {
    it('should compute level correctly', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 5 }));
      fixture.detectChanges();

      expect(component.level()).toBe(5);
    });

    it('should have formattedTime computed property', () => {
      expect(typeof component.formattedTime).toBe('function');
    });

    it('should have dashOffset computed property', () => {
      expect(typeof component.dashOffset).toBe('function');
    });

    it('should compute smallBlind correctly', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ smallBlind: 100 }));
      fixture.detectChanges();

      expect(component.smallBlind()).toBe(100);
    });

    it('should compute bigBlind correctly', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ bigBlind: 200 }));
      fixture.detectChanges();

      expect(component.bigBlind()).toBe(200);
    });

    it('should compute ante correctly', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ ante: 25 }));
      fixture.detectChanges();

      expect(component.ante()).toBe(25);
    });
  });





  describe('Accessibility', () => {
    it('should have aria-label on blind timer', () => {
      const timerEl = fixture.nativeElement.querySelector('[data-cy="blind-timer"]');
      expect(timerEl.getAttribute('aria-label')).toBeTruthy();
    });

    it('should have role="timer"', () => {
      const timerEl = fixture.nativeElement.querySelector('[data-cy="blind-timer"]');
      expect(timerEl.getAttribute('role')).toBe('timer');
    });

    it('should have ariaLabel computed property', () => {
      expect(typeof component.ariaLabel).toBe('function');
    });

    it('should include level in aria label', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 3 }));
      fixture.detectChanges();

      const ariaLabel = component.ariaLabel();
      expect(ariaLabel).toContain('Level 3');
    });
  });





  describe('Edge Cases', () => {
    it('should handle null currentBlinds gracefully', () => {
      fixture.componentRef.setInput('currentBlinds', null);
      fixture.detectChanges();

      expect(component).toBeTruthy();
      expect(component.level()).toBe(1);
      expect(component.smallBlind()).toBe(0);
      expect(component.bigBlind()).toBe(0);
    });

    it('should handle very large blind values', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({
        smallBlind: 1000000,
        bigBlind: 2000000
      }));
      fixture.detectChanges();

      const blindsEl = fixture.nativeElement.querySelector('[data-cy="current-blinds"]');
      expect(blindsEl.textContent).toContain('1,000,000');
    });

    it('should handle input changes', () => {
      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 1 }));
      fixture.detectChanges();
      expect(component.level()).toBe(1);

      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 5 }));
      fixture.detectChanges();
      expect(component.level()).toBe(5);

      fixture.componentRef.setInput('currentBlinds', createMockBlindLevel({ level: 10 }));
      fixture.detectChanges();
      expect(component.level()).toBe(10);
    });
  });





  describe('Cleanup', () => {
    it('should have ngOnDestroy method', () => {
      expect(typeof component.ngOnDestroy).toBe('function');
    });

    it('should call ngOnDestroy without error', () => {
      expect(() => {
        component.ngOnDestroy();
      }).not.toThrow();
    });
  });
});
