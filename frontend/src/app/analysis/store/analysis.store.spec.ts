import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalysisStore } from './analysis.store';
import { AnalysisService } from '../services/analysis.service';
import { SelectedCard } from '../models/analysis.models';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

describe('AnalysisStore', () => {
  let store: AnalysisStore;
  let httpMock: HttpTestingController;

  const mockCard = (rank: string, suit: string): SelectedCard => ({
    rank: rank as SelectedCard['rank'],
    suit: suit as SelectedCard['suit'],
    display: `${rank}${suit === 'HEARTS' ? '♥' : suit === 'SPADES' ? '♠' : suit === 'DIAMONDS' ? '♦' : '♣'}`
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalysisStore, AnalysisService]
    });

    store = TestBed.inject(AnalysisStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('initialization', () => {
    it('should create with initial state', async () => {
      const vm = await firstValueFrom(store.vm$);
      
      expect(vm.heroHand).toEqual([]);
      expect(vm.villainRange.size).toBe(0);
      expect(vm.communityCards).toEqual([]);
      expect(vm.equityResult).toBeNull();
      expect(vm.isCalculating).toBe(false);
      expect(vm.error).toBeNull();
    });
  });

  describe('hero hand management', () => {
    it('should add hero card', async () => {
      const card = mockCard('A', 'HEARTS');
      store.addHeroCard(card);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand.length).toBe(1);
      expect(vm.heroHand[0].rank).toBe('A');
    });

    it('should not add more than 2 hero cards', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      store.addHeroCard(mockCard('Q', 'HEARTS'));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand.length).toBe(2);
    });

    it('should not add duplicate hero card', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('A', 'HEARTS'));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand.length).toBe(1);
    });

    it('should remove hero card by index', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'SPADES'));
      store.removeHeroCard(0);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand.length).toBe(1);
      expect(vm.heroHand[0].rank).toBe('K');
    });

    it('should set complete hero hand', async () => {
      store.setHeroHand([mockCard('Q', 'CLUBS'), mockCard('J', 'CLUBS')]);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand.length).toBe(2);
      expect(vm.heroHand[0].rank).toBe('Q');
      expect(vm.heroHand[1].rank).toBe('J');
    });

    it('should clear equity result when hero hand changes', async () => {
      
      store.patchState({
        equityResult: { heroEquity: 0.6, villainEquity: 0.4, tieEquity: 0, simulationCount: 1000 }
      });
      
      store.addHeroCard(mockCard('A', 'HEARTS'));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.equityResult).toBeNull();
    });
  });

  describe('community cards management', () => {
    it('should add community card', async () => {
      store.addCommunityCard(mockCard('K', 'HEARTS'));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.communityCards.length).toBe(1);
    });

    it('should not add more than 5 community cards', async () => {
      for (let i = 0; i < 7; i++) {
        const ranks = ['A', 'K', 'Q', 'J', 'T', '9', '8'];
        store.addCommunityCard(mockCard(ranks[i], 'HEARTS'));
      }
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.communityCards.length).toBe(5);
    });

    it('should not add duplicate community card', async () => {
      store.addCommunityCard(mockCard('K', 'HEARTS'));
      store.addCommunityCard(mockCard('K', 'HEARTS'));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.communityCards.length).toBe(1);
    });

    it('should remove community card by index', async () => {
      store.addCommunityCard(mockCard('K', 'HEARTS'));
      store.addCommunityCard(mockCard('Q', 'HEARTS'));
      store.removeCommunityCard(0);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.communityCards.length).toBe(1);
      expect(vm.communityCards[0].rank).toBe('Q');
    });
  });

  describe('villain range management', () => {
    it('should toggle range cell on', async () => {
      store.toggleRangeCell('AA');
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AA')).toBe(true);
    });

    it('should toggle range cell off', async () => {
      store.toggleRangeCell('AA');
      store.toggleRangeCell('AA');
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AA')).toBe(false);
    });

    it('should set complete range', async () => {
      store.setVillainRange(new Set(['AA', 'KK', 'AKs']));
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.size).toBe(3);
      expect(vm.villainRange.has('AA')).toBe(true);
      expect(vm.villainRange.has('KK')).toBe(true);
      expect(vm.villainRange.has('AKs')).toBe(true);
    });

    it('should clear range', async () => {
      store.setVillainRange(new Set(['AA', 'KK', 'QQ']));
      store.clearRange();
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.size).toBe(0);
    });

    it('should apply preset range', async () => {
      store.setPresetRange('premium');
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.selectedPreset).toBe('premium');
      expect(vm.villainRange.size).toBeGreaterThan(0);
    });

    it('should parse notation string', async () => {
      store.parseRangeNotation('AA,KK,QQ,AKs');
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AA')).toBe(true);
      expect(vm.villainRange.has('KK')).toBe(true);
      expect(vm.villainRange.has('QQ')).toBe(true);
      expect(vm.villainRange.has('AKs')).toBe(true);
    });

    it('should clear selected preset when range manually modified', async () => {
      store.setPresetRange('premium');
      store.toggleRangeCell('72o');
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.selectedPreset).toBeNull();
    });
  });

  describe('derived selectors', () => {
    it('should calculate range percentage', async () => {
      
      store.setVillainRange(new Set(['AA', 'KK']));
      
      const percentage = await firstValueFrom(store.rangePercentage$);
      expect(percentage).toBeCloseTo(12 / 1326, 4);
    });

    it('should calculate combo count', async () => {
      
      store.setVillainRange(new Set(['AA', 'AKs', 'AKo']));
      
      const count = await firstValueFrom(store.selectedComboCount$);
      expect(count).toBe(22);
    });

    it('should determine canCalculate correctly', async () => {
      
      let canCalc = await firstValueFrom(store.canCalculate$);
      expect(canCalc).toBe(false);

      
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      canCalc = await firstValueFrom(store.canCalculate$);
      expect(canCalc).toBe(false); 

      
      store.toggleRangeCell('QQ');
      canCalc = await firstValueFrom(store.canCalculate$);
      expect(canCalc).toBe(true);
    });

    it('should track dead cards', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addCommunityCard(mockCard('K', 'DIAMONDS'));
      
      const deadCards = await firstValueFrom(store.deadCards$);
      expect(deadCards.length).toBe(2);
    });

    it('should generate range notation', async () => {
      store.setVillainRange(new Set(['AA', 'KK', 'AKs']));
      
      const notation = await firstValueFrom(store.rangeNotation$);
      expect(notation).toContain('AA');
      expect(notation).toContain('KK');
      expect(notation).toContain('AKs');
    });
  });

  describe('helper methods', () => {
    it('should check if cell is selected', () => {
      store.toggleRangeCell('AA');
      
      expect(store.isCellSelected('A', 'A')).toBe(true);
      expect(store.isCellSelected('K', 'K')).toBe(false);
    });

    it('should get cell stats for pairs', () => {
      const stats = store.getCellStats('A', 'A');
      
      expect(stats.notation).toBe('AA');
      expect(stats.combos).toBe(6);
      expect(stats.isPair).toBe(true);
      expect(stats.isSuited).toBe(false);
    });

    it('should get cell stats for suited hands', () => {
      const stats = store.getCellStats('A', 'K');
      
      expect(stats.notation).toBe('AKs');
      expect(stats.combos).toBe(4);
      expect(stats.isPair).toBe(false);
      expect(stats.isSuited).toBe(true);
    });

    it('should get cell stats for offsuit hands', () => {
      const stats = store.getCellStats('K', 'A');
      
      expect(stats.notation).toBe('AKo');
      expect(stats.combos).toBe(12);
      expect(stats.isPair).toBe(false);
      expect(stats.isSuited).toBe(false);
    });

    it('should apply preset additively', async () => {
      store.toggleRangeCell('72o');
      store.applyPreset('premium', true);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('72o')).toBe(true);
      expect(vm.villainRange.has('AA')).toBe(true);
    });

    it('should apply preset exclusively', async () => {
      store.toggleRangeCell('72o');
      store.applyPreset('premium', false);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('72o')).toBe(false);
      expect(vm.villainRange.has('AA')).toBe(true);
    });

    it('should select all pairs', async () => {
      store.selectHandType('pairs', true);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AA')).toBe(true);
      expect(vm.villainRange.has('KK')).toBe(true);
      expect(vm.villainRange.has('22')).toBe(true);
      expect(vm.villainRange.has('AKs')).toBe(false);
    });

    it('should select all suited hands', async () => {
      store.selectHandType('suited', true);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AKs')).toBe(true);
      expect(vm.villainRange.has('AA')).toBe(false);
      expect(vm.villainRange.has('AKo')).toBe(false);
    });

    it('should select all offsuit hands', async () => {
      store.selectHandType('offsuit', true);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AKo')).toBe(true);
      expect(vm.villainRange.has('AA')).toBe(false);
      expect(vm.villainRange.has('AKs')).toBe(false);
    });

    it('should deselect hand type', async () => {
      store.selectHandType('pairs', true);
      store.selectHandType('pairs', false);
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.villainRange.has('AA')).toBe(false);
      expect(vm.villainRange.has('KK')).toBe(false);
    });
  });

  describe('state management', () => {
    it('should reset to initial state', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      store.toggleRangeCell('AA');
      store.addCommunityCard(mockCard('Q', 'DIAMONDS'));
      
      store.reset();
      
      const vm = await firstValueFrom(store.vm$);
      expect(vm.heroHand).toEqual([]);
      expect(vm.villainRange.size).toBe(0);
      expect(vm.communityCards).toEqual([]);
    });

    it('should set and clear error', async () => {
      store.setError('Test error');
      
      let vm = await firstValueFrom(store.vm$);
      expect(vm.error).toBe('Test error');
      
      store.clearError();
      vm = await firstValueFrom(store.vm$);
      expect(vm.error).toBeNull();
    });

    it('should set calculating state', async () => {
      store.setCalculating(true);
      
      let vm = await firstValueFrom(store.vm$);
      expect(vm.isCalculating).toBe(true);
      
      store.setCalculating(false);
      vm = await firstValueFrom(store.vm$);
      expect(vm.isCalculating).toBe(false);
    });

    it('should set highlighted cell', async () => {
      store.setHighlightedCell('AA');
      
      const highlighted = await firstValueFrom(store.highlightedCell$);
      expect(highlighted).toBe('AA');
    });
  });

  describe('equity calculation effect', () => {
    it('should call service and update result', (done) => {
      
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      store.toggleRangeCell('QQ');

      
      store.calculateEquity();

      
      setTimeout(() => {
        const req = httpMock.expectOne(`${environment.apiUrl}/analysis/equity`);
        req.flush({
          equity: 0.65,
          loseEquity: 0.35,
          tieEquity: 0,
          simulationCount: 10000
        });

        firstValueFrom(store.vm$).then(vm => {
          expect(vm.equityResult?.heroEquity).toBe(0.65);
          expect(vm.isCalculating).toBe(false);
          done();
        });
      }, 0);
    });

    it('should not calculate without complete hero hand', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.toggleRangeCell('QQ');
      
      store.calculateEquity();
      
      
      httpMock.expectNone(`${environment.apiUrl}/analysis/equity`);
    });

    it('should not calculate without range', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      
      store.calculateEquity();
      
      
      httpMock.expectNone(`${environment.apiUrl}/analysis/equity`);
    });
  });

  describe('view model composition', () => {
    it('should compose complete view model', async () => {
      store.addHeroCard(mockCard('A', 'HEARTS'));
      store.addHeroCard(mockCard('K', 'HEARTS'));
      store.toggleRangeCell('AA');
      store.toggleRangeCell('KK');
      
      const vm = await firstValueFrom(store.vm$);
      
      expect(vm.heroHand.length).toBe(2);
      expect(vm.villainRange.size).toBe(2);
      expect(vm.selectedComboCount).toBe(12);
      expect(vm.rangePercentage).toBeCloseTo(0.009, 2);
      expect(vm.canCalculate).toBe(true);
      expect(vm.rangeNotation).toContain('AA');
    });
  });
});