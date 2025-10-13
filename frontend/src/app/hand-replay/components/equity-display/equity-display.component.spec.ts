import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { EquityDisplayComponent, EquityData } from './equity-display.component';

describe('EquityDisplayComponent', () => {
  let component: EquityDisplayComponent;
  let fixture: ComponentFixture<EquityDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EquityDisplayComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(EquityDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Equity Display', () => {
    const mockEquityData: EquityData = {
      winProbability: 0.65,
      tieProbability: 0.05,
      loseProbability: 0.30,
      confidenceInterval: { lower: 0.62, upper: 0.68 },
      handStrength: 'VALUE_HEAVY',
      outs: 9,
      potOdds: 0.25
    };

    it('should display equity correctly', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();


      expect(component.displayEquity()).toBeCloseTo(67.5, 0);
    });

    it('should calculate win width correctly', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.winWidth()).toBe(65);
    });

    it('should calculate tie width correctly', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.tieWidth()).toBe(5);
    });

    it('should calculate lose width correctly', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.loseWidth()).toBe(30);
    });

    it('should identify strong hands', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.isStrong()).toBe(true);
    });

    it('should identify weak hands', () => {
      component.equityData = {
        winProbability: 0.25,
        tieProbability: 0.05,
        loseProbability: 0.70
      };
      fixture.detectChanges();

      expect(component.isWeak()).toBe(true);
    });

    it('should detect tie presence', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.hasTie()).toBe(true);
    });

    it('should hide tie when negligible', () => {
      component.equityData = {
        winProbability: 0.50,
        tieProbability: 0.005,
        loseProbability: 0.495
      };
      fixture.detectChanges();

      expect(component.hasTie()).toBe(false);
    });

    it('should calculate outs equity using rule of 2', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.outsEquity()).toBe(18);
    });

    it('should determine if call is profitable', () => {
      component.equityData = mockEquityData;
      fixture.detectChanges();

      expect(component.isProfitable()).toBe(true);
    });

    it('should identify unprofitable calls', () => {
      component.equityData = {
        winProbability: 0.20,
        tieProbability: 0.02,
        loseProbability: 0.78,
        potOdds: 0.30
      };
      fixture.detectChanges();

      expect(component.isProfitable()).toBe(false);
    });
  });

  describe('Format Functions', () => {
    it('should format percentages correctly', () => {
      expect(component.formatPercent(0.6543)).toBe(65.4);
      expect(component.formatPercent(0.1)).toBe(10);
      expect(component.formatPercent(undefined)).toBe(0);
    });
  });

  describe('Hand Strength Classification', () => {
    it('should return correct strength class for value-heavy', () => {
      component.equityData = {
        winProbability: 0.7,
        tieProbability: 0,
        loseProbability: 0.3,
        handStrength: 'VALUE_HEAVY'
      };
      fixture.detectChanges();

      expect(component.getStrengthClass()).toBe('value-heavy');
    });

    it('should return correct strength class for drawing hands', () => {
      component.equityData = {
        winProbability: 0.35,
        tieProbability: 0,
        loseProbability: 0.65,
        handStrength: 'DRAWING'
      };
      fixture.detectChanges();

      expect(component.getStrengthClass()).toBe('drawing');
    });
  });

  describe('Quick Indicator', () => {
    it('should show AHEAD for strong equity', () => {
      component.equityData = {
        winProbability: 0.7,
        tieProbability: 0.05,
        loseProbability: 0.25
      };
      component.isCompact = true;
      fixture.detectChanges();

      expect(component.getQuickIndicator()).toBe('AHEAD');
      expect(component.getQuickIndicatorClass()).toBe('strong');
    });

    it('should show FLIP for medium equity', () => {
      component.equityData = {
        winProbability: 0.48,
        tieProbability: 0.04,
        loseProbability: 0.48
      };
      component.isCompact = true;
      fixture.detectChanges();

      expect(component.getQuickIndicator()).toBe('FLIP');
      expect(component.getQuickIndicatorClass()).toBe('medium');
    });

    it('should show BEHIND for weak equity', () => {
      component.equityData = {
        winProbability: 0.25,
        tieProbability: 0.05,
        loseProbability: 0.70
      };
      component.isCompact = true;
      fixture.detectChanges();

      expect(component.getQuickIndicator()).toBe('BEHIND');
      expect(component.getQuickIndicatorClass()).toBe('weak');
    });
  });

  describe('Compact Mode', () => {
    it('should apply compact class', () => {
      component.isCompact = true;
      fixture.detectChanges();

      const element = fixture.nativeElement.querySelector('.equity-display');
      expect(element.classList.contains('compact')).toBe(true);
    });

    it('should hide detailed info in compact mode', () => {
      component.equityData = {
        winProbability: 0.5,
        tieProbability: 0.05,
        loseProbability: 0.45,
        handStrength: 'MEDIUM_STRENGTH'
      };
      component.isCompact = true;
      fixture.detectChanges();

      const details = fixture.nativeElement.querySelector('.equity-details');
      expect(details).toBeFalsy();
    });
  });

  describe('Edge Cases', () => {
    it('should handle null equity data', () => {
      component.equityData = null;
      fixture.detectChanges();

      expect(component.displayEquity()).toBe(0);
      expect(component.winWidth()).toBe(0);
      expect(component.isStrong()).toBe(false);
    });

    it('should handle 100% equity', () => {
      component.equityData = {
        winProbability: 1.0,
        tieProbability: 0,
        loseProbability: 0
      };
      fixture.detectChanges();

      expect(component.displayEquity()).toBe(100);
      expect(component.isStrong()).toBe(true);
    });

    it('should handle 0% equity', () => {
      component.equityData = {
        winProbability: 0,
        tieProbability: 0,
        loseProbability: 1.0
      };
      fixture.detectChanges();

      expect(component.displayEquity()).toBe(0);
      expect(component.isWeak()).toBe(true);
    });

    it('should cap outs equity at 100%', () => {
      component.equityData = {
        winProbability: 0.5,
        tieProbability: 0,
        loseProbability: 0.5,
        outs: 60
      };
      fixture.detectChanges();

      expect(component.outsEquity()).toBe(100);
    });
  });

  describe('Rendering', () => {
    it('should render equity bar segments', () => {
      component.equityData = {
        winProbability: 0.5,
        tieProbability: 0.1,
        loseProbability: 0.4
      };
      fixture.detectChanges();

      const winSegment = fixture.nativeElement.querySelector('.equity-segment.win');
      const tieSegment = fixture.nativeElement.querySelector('.equity-segment.tie');
      const loseSegment = fixture.nativeElement.querySelector('.equity-segment.lose');

      expect(winSegment).toBeTruthy();
      expect(tieSegment).toBeTruthy();
      expect(loseSegment).toBeTruthy();
    });

    it('should render legend items', () => {
      component.equityData = {
        winProbability: 0.5,
        tieProbability: 0.1,
        loseProbability: 0.4
      };
      component.isCompact = false;
      fixture.detectChanges();

      const legendItems = fixture.nativeElement.querySelectorAll('.legend-item');
      expect(legendItems.length).toBeGreaterThan(0);
    });

    it('should render confidence interval when available', () => {
      component.equityData = {
        winProbability: 0.5,
        tieProbability: 0.05,
        loseProbability: 0.45,
        confidenceInterval: { lower: 0.47, upper: 0.53 }
      };
      component.isCompact = false;
      fixture.detectChanges();

      const ciElement = fixture.nativeElement.querySelector('.confidence-interval');
      expect(ciElement).toBeTruthy();
    });
  });
});
