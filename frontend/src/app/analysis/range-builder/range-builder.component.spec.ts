import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RangeBuilderComponent } from './range-builder.component';
import { PRESET_RANGES } from '../models/analysis.models';

describe('RangeBuilderComponent', () => {
  let component: RangeBuilderComponent;
  let fixture: ComponentFixture<RangeBuilderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RangeBuilderComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RangeBuilderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should render builder header', () => {
      const header = fixture.nativeElement.querySelector('.builder-header h2');
      expect(header?.textContent).toContain('Range Builder');
    });

    it('should render subtitle', () => {
      const subtitle = fixture.nativeElement.querySelector('.subtitle');
      expect(subtitle?.textContent).toContain('hand ranges');
    });
  });

  describe('Layout Structure', () => {
    it('should have two-column layout', () => {
      const layout = fixture.nativeElement.querySelector('.builder-layout');
      expect(layout).toBeTruthy();
    });

    it('should contain range matrix', () => {
      const matrix = fixture.nativeElement.querySelector('app-range-matrix');
      expect(matrix).toBeTruthy();
    });

    it('should contain analysis panel', () => {
      const panel = fixture.nativeElement.querySelector('.analysis-panel');
      expect(panel).toBeTruthy();
    });
  });

  describe('Stats Section', () => {
    it('should display stats section', () => {
      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection).toBeTruthy();
      expect(statsSection?.textContent).toContain('Range Statistics');
    });

    it('should display total combos stat', () => {
      const statItems = fixture.nativeElement.querySelectorAll('.stat-item');
      const labels = Array.from<Element>(statItems).map((item) =>
        item.querySelector('.stat-label')?.textContent
      );
      expect(labels).toContain('Total Combos');
    });

    it('should display range percentage stat', () => {
      const statItems = fixture.nativeElement.querySelectorAll('.stat-item');
      const labels = Array.from<Element>(statItems).map((item) =>
        item.querySelector('.stat-label')?.textContent
      );
      expect(labels).toContain('Range %');
    });

    it('should display hands count stat', () => {
      const statItems = fixture.nativeElement.querySelectorAll('.stat-item');
      const labels = Array.from<Element>(statItems).map((item) =>
        item.querySelector('.stat-label')?.textContent
      );
      expect(labels).toContain('Hands');
    });

    it('should update stats when range changes', () => {
      
      const premiumBtn = fixture.nativeElement.querySelector('.preset-card');
      premiumBtn?.click();
      fixture.detectChanges();

      const comboValue = fixture.nativeElement.querySelector('.stat-item .stat-value');
      expect(parseInt(comboValue?.textContent || '0')).toBeGreaterThan(0);
    });
  });

  describe('Presets Section', () => {
    it('should display presets section', () => {
      const presetsSection = fixture.nativeElement.querySelector('.presets-section');
      expect(presetsSection).toBeTruthy();
      expect(presetsSection?.textContent).toContain('Position Presets');
    });

    it('should display all presets', () => {
      const presetCards = fixture.nativeElement.querySelectorAll('.preset-card');
      expect(presetCards.length).toBe(Object.keys(PRESET_RANGES).length);
    });

    it('should display preset names', () => {
      const presetNames = fixture.nativeElement.querySelectorAll('.preset-name');
      expect(presetNames.length).toBeGreaterThan(0);

      const names = Array.from<Element>(presetNames).map((el) => el.textContent);
      expect(names).toContain('Premium');
      expect(names).toContain('Broadway');
    });

    it('should display preset percentages', () => {
      const presetPcts = fixture.nativeElement.querySelectorAll('.preset-pct');
      expect(presetPcts.length).toBeGreaterThan(0);

      
      const pct = presetPcts[0]?.textContent;
      expect(pct).toContain('%');
    });

    it('should display preset descriptions', () => {
      const presetDescs = fixture.nativeElement.querySelectorAll('.preset-desc');
      expect(presetDescs.length).toBeGreaterThan(0);
    });

    it('should load preset on click', () => {
      const premiumCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Premium')) as HTMLElement;

      premiumCard?.click();
      fixture.detectChanges();


      const statsText = fixture.nativeElement.querySelector('.stats-section')?.textContent;
      expect(statsText).toBeTruthy();
    });

    it('should mark active preset', () => {
      const presetCard = fixture.nativeElement.querySelector('.preset-card');
      presetCard?.click();
      fixture.detectChanges();

      expect(presetCard?.classList.contains('active')).toBe(true);
    });
  });

  describe('Breakdown Section', () => {
    it('should display breakdown section', () => {
      const breakdownSection = fixture.nativeElement.querySelector('.breakdown-section');
      expect(breakdownSection).toBeTruthy();
      expect(breakdownSection?.textContent).toContain('Range Breakdown');
    });

    it('should display pairs breakdown', () => {
      const breakdownLabels = fixture.nativeElement.querySelectorAll('.breakdown-label');
      const labels = Array.from<Element>(breakdownLabels).map((el) => el.textContent);
      expect(labels).toContain('Pairs');
    });

    it('should display suited breakdown', () => {
      const breakdownLabels = fixture.nativeElement.querySelectorAll('.breakdown-label');
      const labels = Array.from<Element>(breakdownLabels).map((el) => el.textContent);
      expect(labels).toContain('Suited');
    });

    it('should display offsuit breakdown', () => {
      const breakdownLabels = fixture.nativeElement.querySelectorAll('.breakdown-label');
      const labels = Array.from<Element>(breakdownLabels).map((el) => el.textContent);
      expect(labels).toContain('Offsuit');
    });

    it('should render breakdown bars', () => {
      const bars = fixture.nativeElement.querySelectorAll('.bar-container .bar');
      expect(bars.length).toBe(3); 
    });

    it('should update bars when preset loaded', () => {

      const pairsCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('All Pairs')) as HTMLElement;

      pairsCard?.click();
      fixture.detectChanges();

      const pairsBar = fixture.nativeElement.querySelector('.bar.pairs');
      const width = pairsBar?.style.width;

      expect(parseFloat(width || '0')).toBeGreaterThan(50);
    });
  });

  describe('Export Section', () => {
    it('should display export section', () => {
      const exportSection = fixture.nativeElement.querySelector('.export-section');
      expect(exportSection).toBeTruthy();
      expect(exportSection?.textContent).toContain('Range Notation');
    });

    it('should have notation textarea', () => {
      const textarea = fixture.nativeElement.querySelector('.notation-display');
      expect(textarea).toBeTruthy();
      expect(textarea?.tagName.toLowerCase()).toBe('textarea');
      expect(textarea?.getAttribute('readonly')).not.toBeNull();
    });

    it('should have copy button', () => {
      const copyBtn = fixture.nativeElement.querySelector('.copy-btn');
      expect(copyBtn).toBeTruthy();
      expect(copyBtn?.textContent).toContain('Copy');
    });

    it('should display range notation when hands selected', () => {

      const premiumCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Premium')) as HTMLElement;

      premiumCard?.click();
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('.notation-display');
      expect(textarea?.value).toBeTruthy();
      expect(textarea?.value).toContain('AA');
    });

    it('should update copy button text after copying', async () => {
      // Use Jest fake timers
      jest.useFakeTimers();

      // Mock navigator.clipboard
      const mockClipboard = {
        writeText: jest.fn().mockResolvedValue(undefined)
      };
      Object.defineProperty(navigator, 'clipboard', {
        value: mockClipboard,
        writable: true,
        configurable: true
      });

      // Load a preset first
      const premiumCard = fixture.nativeElement.querySelector('.preset-card');
      premiumCard?.click();
      fixture.detectChanges();

      // Call the component method
      component.copyNotation();

      // Wait for the promise to resolve
      await Promise.resolve();
      fixture.detectChanges();

      const copyBtn = fixture.nativeElement.querySelector('.copy-btn');
      expect(copyBtn?.textContent).toContain('Copied');

      // Advance timers by 2000ms
      jest.advanceTimersByTime(2000);
      fixture.detectChanges();
      expect(copyBtn?.textContent).toContain('Copy');

      // Cleanup
      jest.useRealTimers();
    });
  });

  describe('Combo Count Calculation', () => {
    it('should calculate pair combos correctly (6 per pair)', () => {

      const premiumCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Premium')) as HTMLElement;

      premiumCard?.click();
      fixture.detectChanges();

      const comboValue = fixture.nativeElement.querySelector('.stat-item .stat-value');

      expect(comboValue?.textContent).toBe('22');
    });

    it('should calculate range percentage correctly', () => {


      const pairsCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('All Pairs')) as HTMLElement;

      pairsCard?.click();
      fixture.detectChanges();

      const pctValue = fixture.nativeElement.querySelectorAll('.stat-item')[1]
        ?.querySelector('.stat-value')?.textContent;
      expect(parseFloat(pctValue || '0')).toBeCloseTo(5.9, 0);
    });
  });

  describe('Range Statistics', () => {
    beforeEach(() => {

      const broadwayCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Broadway')) as HTMLElement;
      broadwayCard?.click();
      fixture.detectChanges();
    });

    it('should count pairs correctly', () => {
      
      const breakdownValues = fixture.nativeElement.querySelectorAll('.breakdown-value');
      const pairsValue = breakdownValues[0]?.textContent;
      expect(parseInt(pairsValue || '0')).toBe(5);
    });

    it('should count suited hands correctly', () => {
      
      const breakdownValues = fixture.nativeElement.querySelectorAll('.breakdown-value');
      const suitedValue = breakdownValues[1]?.textContent;
      expect(parseInt(suitedValue || '0')).toBe(10);
    });

    it('should count offsuit hands correctly', () => {
      
      const breakdownValues = fixture.nativeElement.querySelectorAll('.breakdown-value');
      const offsuitValue = breakdownValues[2]?.textContent;
      expect(parseInt(offsuitValue || '0')).toBe(10);
    });
  });

  describe('Range Change Handler', () => {
    it('should clear active preset when range manually changed', () => {
      
      const premiumCard = fixture.nativeElement.querySelector('.preset-card');
      premiumCard?.click();
      fixture.detectChanges();

      expect(premiumCard?.classList.contains('active')).toBe(true);

      
      component.onRangeChange(new Set(['AA']));
      fixture.detectChanges();

      expect(premiumCard?.classList.contains('active')).toBe(false);
    });

    it('should update range on onRangeChange', () => {
      const newRange = new Set(['AA', 'KK', 'QQ']);
      component.onRangeChange(newRange);
      fixture.detectChanges();

      const comboValue = fixture.nativeElement.querySelector('.stat-item .stat-value');
      
      expect(comboValue?.textContent).toBe('18');
    });
  });

  describe('Preset Selected Handler', () => {
    it('should set active preset on presetSelected event', () => {
      component.onPresetSelected('premium');
      fixture.detectChanges();

      const premiumCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Premium')) as HTMLElement;

      expect(premiumCard?.classList.contains('active')).toBe(true);
    });
  });

  describe('Load Preset', () => {
    it('should load preset hands into selected range', () => {
      const premiumCard = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-card')
      ).find((card) => card.textContent?.includes('Premium')) as HTMLElement;

      premiumCard?.click();
      fixture.detectChanges();

      const textarea = fixture.nativeElement.querySelector('.notation-display') as HTMLTextAreaElement;
      expect(textarea?.value).toContain('AA');
      expect(textarea?.value).toContain('KK');
      expect(textarea?.value).toContain('QQ');
      expect(textarea?.value).toContain('AKs');
    });

    it('should handle invalid preset key gracefully', () => {
      expect(() => {
        const invalidPreset = 'nonexistent';
        component.loadPreset(invalidPreset);
        fixture.detectChanges();
      }).not.toThrow();
    });
  });

  describe('Accessibility', () => {
    it('should have aria-label on notation textarea', () => {
      const textarea = fixture.nativeElement.querySelector('.notation-display');
      expect(textarea?.getAttribute('aria-label')).toBe('Range notation');
    });

    it('should have proper button elements for presets', () => {
      const presetCards = fixture.nativeElement.querySelectorAll('.preset-card');
      presetCards.forEach((card: HTMLElement) => {
        expect(card.tagName.toLowerCase()).toBe('button');
      });
    });

    it('should have proper section headings', () => {
      const headings = fixture.nativeElement.querySelectorAll('.analysis-panel h3');
      expect(headings.length).toBeGreaterThanOrEqual(4); 
    });
  });

  describe('PRESET_RANGES Integration', () => {
    it('should have correct premium preset', () => {
      expect(PRESET_RANGES['premium'].hands).toContain('AA');
      expect(PRESET_RANGES['premium'].hands).toContain('KK');
      expect(PRESET_RANGES['premium'].hands).toContain('QQ');
      expect(PRESET_RANGES['premium'].hands).toContain('AKs');
      expect(PRESET_RANGES['premium'].hands.length).toBe(4);
    });

    it('should have correct broadway preset', () => {
      expect(PRESET_RANGES['broadway'].hands.length).toBe(25);
    });

    it('should have correct pairs preset', () => {
      expect(PRESET_RANGES['pairs'].hands.length).toBe(13);
    });

    it('should have suited connectors preset', () => {
      expect(PRESET_RANGES['suitedConnectors']).toBeDefined();
      expect(PRESET_RANGES['suitedConnectors'].hands).toContain('AKs');
      expect(PRESET_RANGES['suitedConnectors'].hands).toContain('32s');
    });

    it('should have early position preset', () => {
      expect(PRESET_RANGES['earlyPosition']).toBeDefined();
      expect(PRESET_RANGES['earlyPosition'].percentage).toBe(12.0);
    });

    it('should have button open preset', () => {
      expect(PRESET_RANGES['buttonOpen']).toBeDefined();
      expect(PRESET_RANGES['buttonOpen'].percentage).toBe(45.0);
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty range', () => {
      component.onRangeChange(new Set());
      fixture.detectChanges();

      const comboValue = fixture.nativeElement.querySelector('.stat-item .stat-value');
      expect(comboValue?.textContent).toBe('0');
    });

    it('should prevent division by zero in percentages', () => {
      component.onRangeChange(new Set());
      fixture.detectChanges();

      
      const pctValue = fixture.nativeElement.querySelectorAll('.stat-item')[1]
        ?.querySelector('.stat-value')?.textContent;
      expect(pctValue).toBe('0.0%');
    });

    it('should handle all hands selected', () => {
      
      const allHands = new Set(PRESET_RANGES['buttonOpen'].hands);
      component.onRangeChange(allHands);
      fixture.detectChanges();

      
      const handCount = fixture.nativeElement.querySelectorAll('.stat-item')[2]
        ?.querySelector('.stat-value')?.textContent;
      expect(parseInt(handCount || '0')).toBeGreaterThan(0);
    });
  });
});
