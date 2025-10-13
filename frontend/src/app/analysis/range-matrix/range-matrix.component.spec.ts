import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RangeMatrixComponent } from './range-matrix.component';
import { RANKS, getCellNotation, getComboCount, PRESET_RANGES } from '../models/analysis.models';

describe('RangeMatrixComponent', () => {
  let component: RangeMatrixComponent;
  let fixture: ComponentFixture<RangeMatrixComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RangeMatrixComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RangeMatrixComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should render 13x13 grid (169 cells)', () => {
      const cells = fixture.nativeElement.querySelectorAll('.matrix-cell');
      expect(cells.length).toBe(169);
    });

    it('should display all ranks', () => {
      const cellTexts = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.cell-text')
      ).map((el) => el.textContent);


      expect(cellTexts).toContain('AA');
      expect(cellTexts).toContain('22');
      expect(cellTexts).toContain('AKs');
      expect(cellTexts).toContain('AKo');
    });
  });

  describe('Cell Notation', () => {
    it('should generate correct notation for pairs', () => {
      for (const rank of RANKS) {
        const notation = getCellNotation(rank, rank);
        expect(notation).toBe(`${rank}${rank}`);
      }
    });

    it('should generate correct notation for suited hands (above diagonal)', () => {

      expect(getCellNotation('A', 'K')).toBe('AKs');
      expect(getCellNotation('K', 'Q')).toBe('KQs');
      expect(getCellNotation('J', 'T')).toBe('JTs');
    });

    it('should generate correct notation for offsuit hands (below diagonal)', () => {

      expect(getCellNotation('K', 'A')).toBe('AKo');
      expect(getCellNotation('Q', 'K')).toBe('KQo');
    });
  });

  describe('Combo Counts', () => {
    it('should return 6 combos for pairs', () => {
      expect(getComboCount('A', 'A')).toBe(6);
      expect(getComboCount('K', 'K')).toBe(6);
      expect(getComboCount('2', '2')).toBe(6);
    });

    it('should return 4 combos for suited hands', () => {
      expect(getComboCount('A', 'K')).toBe(4);
      expect(getComboCount('Q', 'J')).toBe(4);
    });

    it('should return 12 combos for offsuit hands', () => {
      expect(getComboCount('K', 'A')).toBe(12);
      expect(getComboCount('J', 'Q')).toBe(12);
    });

    it('should sum to 1326 total combos', () => {
      let total = 0;
      for (const row of RANKS) {
        for (const col of RANKS) {
          total += getComboCount(row, col);
        }
      }
      expect(total).toBe(1326);
    });
  });

  describe('Cell Selection', () => {
    it('should emit rangeChange when cell is clicked', () => {
      const spy = jest.spyOn(component.rangeChange, 'emit');

      const cell = fixture.nativeElement.querySelector('.matrix-cell');
      cell.click();
      fixture.detectChanges();

      expect(spy).toHaveBeenCalled();
    });

    it('should toggle cell selection state', () => {
      const spy = jest.spyOn(component.rangeChange, 'emit');


      const cell = fixture.nativeElement.querySelector('.matrix-cell');
      cell.click();

      const emittedSet = spy.mock.calls[0][0] as Set<string>;
      expect(emittedSet.size).toBe(1);
    });

    it('should apply selected class to selected cells', () => {
      fixture.componentRef.setInput('selectedRange', new Set(['AA']));
      fixture.detectChanges();

      const selectedCells = fixture.nativeElement.querySelectorAll('.matrix-cell.selected');
      expect(selectedCells.length).toBe(1);
    });
  });

  describe('Preset Selection', () => {
    it('should load premium preset', () => {
      const spy = jest.spyOn(component.rangeChange, 'emit');

      const premiumBtn = Array.from<Element>(
        fixture.nativeElement.querySelectorAll('.preset-btn')
      ).find((btn) => btn.textContent?.includes('Premium')) as HTMLButtonElement;

      premiumBtn?.click();
      fixture.detectChanges();

      expect(spy).toHaveBeenCalled();
      const emittedSet = spy.mock.calls[0][0] as Set<string>;
      expect(emittedSet.has('AA')).toBe(true);
      expect(emittedSet.has('KK')).toBe(true);
    });

    it('should clear all selections', () => {
      fixture.componentRef.setInput('selectedRange', new Set(['AA', 'KK', 'QQ']));
      fixture.detectChanges();

      const spy = jest.spyOn(component.rangeChange, 'emit');
      const clearBtn = fixture.nativeElement.querySelector('.clear-btn');
      clearBtn.click();

      const emittedSet = spy.mock.calls[0][0] as Set<string>;
      expect(emittedSet.size).toBe(0);
    });
  });

  describe('Notation Input', () => {
    it('should parse notation input correctly', () => {
      const spy = jest.spyOn(component.rangeChange, 'emit');

      const input = fixture.nativeElement.querySelector('#range-notation');
      input.value = 'AA, KK, AKs';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(spy).toHaveBeenCalled();
    });
  });

  describe('Stats Display', () => {
    it('should display combo count and percentage', () => {
      fixture.componentRef.setInput('selectedRange', new Set(['AA', 'KK']));
      fixture.detectChanges();

      const statsText = fixture.nativeElement.querySelector('.range-stats')?.textContent;
      expect(statsText).toContain('12');
      expect(statsText).toContain('1326');
    });
  });

  describe('CSS Classes', () => {
    it('should apply suited class to suited cells', () => {
      const suitedCells = fixture.nativeElement.querySelectorAll('.matrix-cell.suited');

      expect(suitedCells.length).toBe(78);
    });

    it('should apply pair class to diagonal cells', () => {
      const pairCells = fixture.nativeElement.querySelectorAll('.matrix-cell.pair');
      expect(pairCells.length).toBe(13);
    });

    it('should apply offsuit class to offsuit cells', () => {
      const offsuitCells = fixture.nativeElement.querySelectorAll('.matrix-cell.offsuit');

      expect(offsuitCells.length).toBe(78);
    });
  });

  describe('Accessibility', () => {
    it('should have aria-label on grid', () => {
      const grid = fixture.nativeElement.querySelector('.matrix-grid');
      expect(grid?.getAttribute('role')).toBe('grid');
    });

    it('should have aria-label on cells', () => {
      const cell = fixture.nativeElement.querySelector('.matrix-cell');
      expect(cell?.getAttribute('aria-label')).toBeTruthy();
      expect(cell?.getAttribute('role')).toBe('gridcell');
    });

    it('should be keyboard navigable', () => {
      const cells = fixture.nativeElement.querySelectorAll('.matrix-cell');
      cells.forEach((cell: HTMLElement) => {
        expect(cell.getAttribute('tabindex')).toBe('0');
      });
    });
  });
});

describe('Analysis Models', () => {
  describe('getCellNotation', () => {
    it('should handle edge cases', () => {
      expect(getCellNotation('A', '2')).toBe('A2s');
      expect(getCellNotation('2', 'A')).toBe('A2o');
    });
  });

  describe('PRESET_RANGES', () => {
    it('should have valid presets', () => {
      expect(PRESET_RANGES['premium']).toBeDefined();
      expect(PRESET_RANGES['premium'].hands.length).toBeGreaterThan(0);
      expect(PRESET_RANGES['broadway'].hands.length).toBeGreaterThan(PRESET_RANGES['premium'].hands.length);
    });

    it('premium should contain AA, KK, QQ, AKs', () => {
      const premium = PRESET_RANGES['premium'].hands;
      expect(premium).toContain('AA');
      expect(premium).toContain('KK');
      expect(premium).toContain('QQ');
      expect(premium).toContain('AKs');
    });
  });
});
