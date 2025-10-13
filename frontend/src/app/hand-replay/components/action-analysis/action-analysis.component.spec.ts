import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActionAnalysisComponent, ActionAnalysisData, AlternativeAction } from './action-analysis.component';

describe('ActionAnalysisComponent', () => {
  let component: ActionAnalysisComponent;
  let fixture: ComponentFixture<ActionAnalysisComponent>;

  const mockAnalysisData: ActionAnalysisData = {
    action: 'CALL',
    assessment: 'GOOD',
    evActual: 15.50,
    evOptimal: 22.00,
    evLost: 6.50,
    optimalAction: 'RAISE',
    reasoning: 'Getting proper odds to continue but missing value by not raising.',
    alternatives: [
      { action: 'FOLD', ev: 0 },
      { action: 'CALL', ev: 15.50, frequency: 0.45 },
      { action: 'RAISE', ev: 22.00, frequency: 0.55 }
    ],
    gtoFrequency: {
      fold: 0.10,
      check: 0,
      call: 0.45,
      bet: 0,
      raise: 0.45
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActionAnalysisComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ActionAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Assessment Badge', () => {
    const assessments: ActionAnalysisData['assessment'][] = [
      'OPTIMAL', 'GOOD', 'ACCEPTABLE', 'QUESTIONABLE', 'MISTAKE', 'BLUNDER'
    ];

    assessments.forEach(assessment => {
      it(`should return correct class for ${assessment} assessment`, () => {
        component.analysisData = { ...mockAnalysisData, assessment };
        fixture.detectChanges();

        expect(component.getAssessmentClass()).toBe(assessment.toLowerCase());
      });
    });
  });

  describe('EV Calculations', () => {
    it('should identify positive actual EV', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      expect(component.isActualPositive()).toBe(true);
    });

    it('should identify negative actual EV', () => {
      component.analysisData = { ...mockAnalysisData, evActual: -10 };
      fixture.detectChanges();

      expect(component.isActualPositive()).toBe(false);
    });

    it('should detect EV loss', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      expect(component.hasEvLoss()).toBe(true);
    });

    it('should not detect EV loss when negligible', () => {
      component.analysisData = { ...mockAnalysisData, evLost: 0.005 };
      fixture.detectChanges();

      expect(component.hasEvLoss()).toBe(false);
    });
  });

  describe('Format Functions', () => {
    it('should format positive EV correctly', () => {
      expect(component.formatEV(15.5)).toBe('+$15.50');
    });

    it('should format negative EV correctly', () => {
      expect(component.formatEV(-10.25)).toBe('-$10.25');
    });

    it('should format zero EV correctly', () => {
      expect(component.formatEV(0)).toBe('+$0.00');
    });

    it('should handle undefined EV', () => {
      expect(component.formatEV(undefined)).toBe('$0');
    });

    it('should format percentages correctly', () => {
      expect(component.formatPercent(0.456)).toBe(45.6);
      expect(component.formatPercent(undefined)).toBe(0);
    });
  });

  describe('Optimal Action Display', () => {
    it('should show optimal action when different from actual', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      expect(component.showOptimalAction()).toBe(true);
    });

    it('should not show optimal action when already optimal', () => {
      component.analysisData = {
        ...mockAnalysisData,
        assessment: 'OPTIMAL',
        optimalAction: 'CALL'
      };
      fixture.detectChanges();

      expect(component.showOptimalAction()).toBe(false);
    });

    it('should not show optimal action when same as actual', () => {
      component.analysisData = {
        ...mockAnalysisData,
        assessment: 'GOOD',
        optimalAction: 'CALL'
      };
      fixture.detectChanges();

      expect(component.showOptimalAction()).toBe(false);
    });
  });

  describe('Alternatives', () => {
    it('should detect alternatives presence', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      expect(component.hasAlternatives()).toBe(true);
    });

    it('should not detect alternatives when empty', () => {
      component.analysisData = { ...mockAnalysisData, alternatives: [] };
      fixture.detectChanges();

      expect(component.hasAlternatives()).toBe(false);
    });

    it('should identify best alternative', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const raiseAlt = mockAnalysisData.alternatives.find(a => a.action === 'RAISE')!;
      const foldAlt = mockAnalysisData.alternatives.find(a => a.action === 'FOLD')!;

      expect(component.isBestAlternative(raiseAlt)).toBe(true);
      expect(component.isBestAlternative(foldAlt)).toBe(false);
    });
  });

  describe('Expand/Collapse', () => {
    it('should start collapsed', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      expect(component.expanded()).toBe(false);
    });

    it('should toggle expansion', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      component.toggleExpanded();
      expect(component.expanded()).toBe(true);

      component.toggleExpanded();
      expect(component.expanded()).toBe(false);
    });

    it('should show expanded content when expanded', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();
      component.toggleExpanded();
      fixture.detectChanges();

      const expandedContent = fixture.nativeElement.querySelector('.expanded-content');
      expect(expandedContent).toBeTruthy();
    });

    it('should hide expanded content when collapsed', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const expandedContent = fixture.nativeElement.querySelector('.expanded-content');
      expect(expandedContent).toBeFalsy();
    });
  });

  describe('GTO Frequencies', () => {
    it('should display frequency bars when gtoFrequency exists', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();
      component.toggleExpanded();
      fixture.detectChanges();

      const frequencyBars = fixture.nativeElement.querySelector('.gto-frequencies');
      expect(frequencyBars).toBeTruthy();
    });

    it('should hide frequency bars when gtoFrequency is undefined', () => {
      component.analysisData = { ...mockAnalysisData, gtoFrequency: undefined };
      fixture.detectChanges();
      component.toggleExpanded();
      fixture.detectChanges();

      const frequencyBars = fixture.nativeElement.querySelector('.gto-frequencies');
      expect(frequencyBars).toBeFalsy();
    });

    it('should only show frequency bars for non-zero frequencies', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();
      component.toggleExpanded();
      fixture.detectChanges();

      const freqRows = fixture.nativeElement.querySelectorAll('.freq-bar-row');
      expect(freqRows.length).toBe(3);
    });
  });

  describe('Rendering', () => {
    it('should render action text', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const actionValue = fixture.nativeElement.querySelector('.action-value');
      expect(actionValue.textContent).toContain('CALL');
    });

    it('should render assessment badge with correct class', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.assessment-badge');
      expect(badge).toBeTruthy();
      expect(badge.classList.contains('good')).toBe(true);
    });

    it('should render EV summary items', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const evItems = fixture.nativeElement.querySelectorAll('.ev-item');
      expect(evItems.length).toBeGreaterThanOrEqual(2);
    });

    it('should render reasoning when expanded', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();
      component.toggleExpanded();
      fixture.detectChanges();

      const reasoning = fixture.nativeElement.querySelector('.reasoning-text');
      expect(reasoning).toBeTruthy();
      expect(reasoning.textContent).toContain('Getting proper odds');
    });
  });

  describe('Edge Cases', () => {
    it('should handle null analysis data', () => {
      component.analysisData = null;
      fixture.detectChanges();

      expect(component.isActualPositive()).toBe(false);
      expect(component.hasEvLoss()).toBe(false);
      expect(component.showOptimalAction()).toBe(false);
      expect(component.hasAlternatives()).toBe(false);
    });

    it('should handle analysis with no alternatives', () => {
      component.analysisData = {
        ...mockAnalysisData,
        alternatives: []
      };
      fixture.detectChanges();

      expect(component.hasAlternatives()).toBe(false);
    });

    it('should handle analysis with missing optional fields', () => {
      const minimalData: ActionAnalysisData = {
        action: 'FOLD',
        assessment: 'ACCEPTABLE',
        evActual: 0,
        evOptimal: 0,
        evLost: 0,
        optimalAction: 'FOLD',
        reasoning: '',
        alternatives: []
      };
      
      component.analysisData = minimalData;
      fixture.detectChanges();

      expect(component.analysis()).toEqual(minimalData);
    });

    it('should identify best alternative when all have same EV', () => {
      const equalAlts: AlternativeAction[] = [
        { action: 'FOLD', ev: 5 },
        { action: 'CALL', ev: 5 },
        { action: 'RAISE', ev: 5 }
      ];

      component.analysisData = { ...mockAnalysisData, alternatives: equalAlts };
      fixture.detectChanges();

      expect(component.isBestAlternative(equalAlts[0])).toBe(true);
      expect(component.isBestAlternative(equalAlts[1])).toBe(true);
      expect(component.isBestAlternative(equalAlts[2])).toBe(true);
    });
  });

  describe('Accessibility', () => {
    it('should have aria-expanded on toggle button', () => {
      component.analysisData = mockAnalysisData;
      fixture.detectChanges();

      const toggle = fixture.nativeElement.querySelector('.expand-toggle');
      expect(toggle.getAttribute('aria-expanded')).toBe('false');

      component.toggleExpanded();
      fixture.detectChanges();

      expect(toggle.getAttribute('aria-expanded')).toBe('true');
    });
  });
});