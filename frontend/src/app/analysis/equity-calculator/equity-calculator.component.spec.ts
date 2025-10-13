import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { EquityCalculatorComponent } from './equity-calculator.component';
import { AnalysisStore } from '../store/analysis.store';
import { AnalysisService } from '../services/analysis.service';

describe('EquityCalculatorComponent', () => {
  let component: EquityCalculatorComponent;
  let fixture: ComponentFixture<EquityCalculatorComponent>;
  let store: AnalysisStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EquityCalculatorComponent, HttpClientTestingModule],
      providers: [AnalysisService]
    }).compileComponents();

    fixture = TestBed.createComponent(EquityCalculatorComponent);
    component = fixture.componentInstance;
    // Get the store instance from the component's injector, not TestBed
    store = fixture.debugElement.injector.get(AnalysisStore);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display header', () => {
    const header = fixture.debugElement.query(By.css('.calc-header h2'));
    expect(header.nativeElement.textContent).toContain('Equity Calculator');
  });

  it('should have hero card selector', () => {
    const selector = fixture.debugElement.query(By.css('.hero-section app-card-selector'));
    expect(selector).toBeTruthy();
  });

  it('should have board card selector', () => {
    const selector = fixture.debugElement.query(By.css('.board-section app-card-selector'));
    expect(selector).toBeTruthy();
  });

  it('should have range matrix', () => {
    const matrix = fixture.debugElement.query(By.css('.range-section app-range-matrix'));
    expect(matrix).toBeTruthy();
  });

  it('should disable calculate button when cannot calculate', () => {
    const button = fixture.debugElement.query(By.css('.calculate-btn'));
    expect(button.nativeElement.disabled).toBe(true);
  });

  it('should enable calculate button with valid inputs', fakeAsync(() => {
    store.addHeroCard({ rank: 'A', suit: 'HEARTS', display: 'A♥' });
    store.addHeroCard({ rank: 'K', suit: 'HEARTS', display: 'K♥' });
    store.toggleRangeCell('AA');

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const button = fixture.debugElement.query(By.css('.calculate-btn'));
    expect(button.nativeElement.disabled).toBe(false);
  }));

  it('should show results section when equity result exists', fakeAsync(() => {
    store.patchState({
      heroHand: [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ],
      equityResult: {
        heroEquity: 0.65,
        villainEquity: 0.35,
        tieEquity: 0,
        simulationCount: 10000
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const results = fixture.debugElement.query(By.css('.results-section'));
    expect(results).toBeTruthy();
  }));

  it('should display equity bar with correct widths', fakeAsync(() => {
    store.patchState({
      equityResult: {
        heroEquity: 0.6,
        villainEquity: 0.4,
        tieEquity: 0,
        simulationCount: 10000
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const heroBar = fixture.debugElement.query(By.css('.hero-equity'));
    const villainBar = fixture.debugElement.query(By.css('.villain-equity'));

    expect(heroBar.nativeElement.style.width).toBe('60%');
    expect(villainBar.nativeElement.style.width).toBe('40%');
  }));

  it('should show tie equity when significant', fakeAsync(() => {
    store.patchState({
      equityResult: {
        heroEquity: 0.45,
        villainEquity: 0.45,
        tieEquity: 0.1,
        simulationCount: 10000
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const tieBar = fixture.debugElement.query(By.css('.tie-equity'));
    expect(tieBar).toBeTruthy();
  }));

  it('should not show tie equity when negligible', async () => {
    store.patchState({
      equityResult: {
        heroEquity: 0.5,
        villainEquity: 0.5,
        tieEquity: 0.0001,
        simulationCount: 10000
      }
    });
    
    fixture.detectChanges();
    
    const tieBar = fixture.debugElement.query(By.css('.tie-equity'));
    expect(tieBar).toBeFalsy();
  });

  it('should display simulation count', fakeAsync(() => {
    store.patchState({
      equityResult: {
        heroEquity: 0.5,
        villainEquity: 0.5,
        tieEquity: 0,
        simulationCount: 10000
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const simInfo = fixture.debugElement.query(By.css('.sim-info'));
    expect(simInfo.nativeElement.textContent).toContain('10,000');
  }));

  it('should show error message when error exists', fakeAsync(() => {
    store.setError('Test error');

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const error = fixture.debugElement.query(By.css('.error-message'));
    expect(error.nativeElement.textContent).toContain('Test error');
  }));

  it('should show spinner when calculating', fakeAsync(() => {
    store.setCalculating(true);

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const spinner = fixture.debugElement.query(By.css('.spinner'));
    expect(spinner).toBeTruthy();
  }));

  it('should display hand type breakdown when available', fakeAsync(() => {
    store.patchState({
      equityResult: {
        heroEquity: 0.5,
        villainEquity: 0.5,
        tieEquity: 0,
        simulationCount: 10000,
        handTypeBreakdown: {
          'PAIR': 0.4,
          'TWO_PAIR': 0.3,
          'HIGH_CARD': 0.3
        }
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const breakdown = fixture.debugElement.query(By.css('.breakdown-section'));
    expect(breakdown).toBeTruthy();

    const items = fixture.debugElement.queryAll(By.css('.breakdown-item'));
    expect(items.length).toBe(3);
  }));

  it('should format hand types correctly', () => {
    expect(component['formatHandType']('HIGH_CARD')).toBe('High Card');
    expect(component['formatHandType']('THREE_OF_A_KIND')).toBe('Three of a Kind');
    expect(component['formatHandType']('ROYAL_FLUSH')).toBe('Royal Flush');
  });

  it('should have accessible calculate button', () => {
    const button = fixture.debugElement.query(By.css('.calculate-btn'));
    expect(button.nativeElement.getAttribute('type')).toBe('button');
  });

  it('should have aria-live on results section', fakeAsync(() => {
    store.patchState({
      equityResult: {
        heroEquity: 0.5,
        villainEquity: 0.5,
        tieEquity: 0,
        simulationCount: 10000
      }
    });

    // Flush all pending asynchronous activities
    tick();
    fixture.detectChanges();
    tick();

    const results = fixture.debugElement.query(By.css('.results-section'));
    expect(results.nativeElement.getAttribute('aria-live')).toBe('polite');
  }));
});