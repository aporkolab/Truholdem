import { Injectable, inject } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable } from 'rxjs';
import { switchMap, tap, debounceTime, withLatestFrom, filter } from 'rxjs/operators';
import { AnalysisService } from '../services/analysis.service';
import {
  SelectedCard,
  EquityResult,
  RANKS,
  Rank,
  getCellNotation,
  getComboCount,
  notationToRange,
  rangeToNotation,
  calculateRangePercentage,
  PRESET_RANGES
} from '../models/analysis.models';


export interface AnalysisState {
  
  heroHand: SelectedCard[];
  
  
  villainRange: Set<string>;
  
  
  communityCards: SelectedCard[];
  
  
  equityResult: EquityResult | null;
  
  
  isCalculating: boolean;
  error: string | null;
  selectedPreset: string | null;
  
  
  matrixMode: 'select' | 'view';
  highlightedCell: string | null;
}


const initialState: AnalysisState = {
  heroHand: [],
  villainRange: new Set<string>(),
  communityCards: [],
  equityResult: null,
  isCalculating: false,
  error: null,
  selectedPreset: null,
  matrixMode: 'select',
  highlightedCell: null
};


export interface AnalysisViewModel {
  heroHand: SelectedCard[];
  villainRange: Set<string>;
  communityCards: SelectedCard[];
  equityResult: EquityResult | null;
  isCalculating: boolean;
  error: string | null;
  rangePercentage: number;
  selectedComboCount: number;
  rangeNotation: string;
  canCalculate: boolean;
  selectedPreset: string | null;
}

@Injectable()
export class AnalysisStore extends ComponentStore<AnalysisState> {
  private readonly analysisService = inject(AnalysisService);

  constructor() {
    super(initialState);
  }

  

  readonly heroHand$ = this.select(state => state.heroHand);
  readonly villainRange$ = this.select(state => state.villainRange);
  readonly communityCards$ = this.select(state => state.communityCards);
  readonly equityResult$ = this.select(state => state.equityResult);
  readonly isCalculating$ = this.select(state => state.isCalculating);
  readonly error$ = this.select(state => state.error);
  readonly selectedPreset$ = this.select(state => state.selectedPreset);
  readonly highlightedCell$ = this.select(state => state.highlightedCell);

  
  readonly rangePercentage$ = this.select(
    this.villainRange$,
    range => calculateRangePercentage(range)
  );

  readonly selectedComboCount$ = this.select(
    this.villainRange$,
    range => {
      let count = 0;
      for (const notation of range) {
        const isPair = notation.length === 2 || 
          (notation.length === 3 && notation[0] === notation[1]);
        const isSuited = notation.endsWith('s');
        
        if (isPair || notation[0] === notation[1]) {
          count += 6;
        } else if (isSuited) {
          count += 4;
        } else {
          count += 12;
        }
      }
      return count;
    }
  );

  readonly rangeNotation$ = this.select(
    this.villainRange$,
    range => rangeToNotation(range)
  );

  readonly canCalculate$ = this.select(
    this.heroHand$,
    this.villainRange$,
    this.isCalculating$,
    (hero, range, calculating) => 
      hero.length === 2 && range.size > 0 && !calculating
  );

  readonly deadCards$ = this.select(
    this.heroHand$,
    this.communityCards$,
    (hero, community) => [...hero, ...community]
  );

  readonly availableCards$ = this.select(
    this.deadCards$,
    deadCards => this.analysisService.getAvailableCards(deadCards)
  );

  
  readonly vm$: Observable<AnalysisViewModel> = this.select(
    this.heroHand$,
    this.villainRange$,
    this.communityCards$,
    this.equityResult$,
    this.isCalculating$,
    this.error$,
    this.rangePercentage$,
    this.selectedComboCount$,
    this.rangeNotation$,
    this.canCalculate$,
    this.selectedPreset$,
    (
      heroHand, villainRange, communityCards, equityResult,
      isCalculating, error, rangePercentage, selectedComboCount,
      rangeNotation, canCalculate, selectedPreset
    ) => ({
      heroHand,
      villainRange,
      communityCards,
      equityResult,
      isCalculating,
      error,
      rangePercentage,
      selectedComboCount,
      rangeNotation,
      canCalculate,
      selectedPreset
    })
  );

  

  readonly setHeroHand = this.updater((state, heroHand: SelectedCard[]) => ({
    ...state,
    heroHand: heroHand.slice(0, 2),
    equityResult: null 
  }));

  readonly addHeroCard = this.updater((state, card: SelectedCard) => {
    if (state.heroHand.length >= 2) return state;
    if (state.heroHand.some(c => c.rank === card.rank && c.suit === card.suit)) {
      return state;
    }
    return {
      ...state,
      heroHand: [...state.heroHand, card],
      equityResult: null
    };
  });

  readonly removeHeroCard = this.updater((state, index: number) => ({
    ...state,
    heroHand: state.heroHand.filter((_, i) => i !== index),
    equityResult: null
  }));

  readonly setCommunityCards = this.updater((state, cards: SelectedCard[]) => ({
    ...state,
    communityCards: cards.slice(0, 5),
    equityResult: null
  }));

  readonly addCommunityCard = this.updater((state, card: SelectedCard) => {
    if (state.communityCards.length >= 5) return state;
    if (state.communityCards.some(c => c.rank === card.rank && c.suit === card.suit)) {
      return state;
    }
    return {
      ...state,
      communityCards: [...state.communityCards, card],
      equityResult: null
    };
  });

  readonly removeCommunityCard = this.updater((state, index: number) => ({
    ...state,
    communityCards: state.communityCards.filter((_, i) => i !== index),
    equityResult: null
  }));

  readonly setVillainRange = this.updater((state, range: Set<string>) => ({
    ...state,
    villainRange: new Set(range),
    equityResult: null,
    selectedPreset: null
  }));

  readonly toggleRangeCell = this.updater((state, notation: string) => {
    const newRange = new Set(state.villainRange);
    if (newRange.has(notation)) {
      newRange.delete(notation);
    } else {
      newRange.add(notation);
    }
    return {
      ...state,
      villainRange: newRange,
      equityResult: null,
      selectedPreset: null
    };
  });

  readonly clearRange = this.updater(state => ({
    ...state,
    villainRange: new Set<string>(),
    equityResult: null,
    selectedPreset: null
  }));

  readonly setPresetRange = this.updater((state, presetKey: string) => {
    const preset = PRESET_RANGES[presetKey];
    if (!preset) return state;
    
    return {
      ...state,
      villainRange: new Set(preset.hands),
      equityResult: null,
      selectedPreset: presetKey
    };
  });

  readonly parseRangeNotation = this.updater((state, notation: string) => ({
    ...state,
    villainRange: notationToRange(notation),
    equityResult: null,
    selectedPreset: null
  }));

  readonly setEquityResult = this.updater((state, result: EquityResult) => ({
    ...state,
    equityResult: result,
    isCalculating: false,
    error: null
  }));

  readonly setCalculating = this.updater((state, isCalculating: boolean) => ({
    ...state,
    isCalculating
  }));

  readonly setError = this.updater((state, error: string) => ({
    ...state,
    error,
    isCalculating: false
  }));

  readonly clearError = this.updater(state => ({
    ...state,
    error: null
  }));

  readonly setHighlightedCell = this.updater((state, cell: string | null) => ({
    ...state,
    highlightedCell: cell
  }));

  readonly reset = this.updater(() => initialState);

  

  readonly calculateEquity = this.effect<void>(trigger$ =>
    trigger$.pipe(
      tap(() => this.setCalculating(true)),
      withLatestFrom(
        this.heroHand$,
        this.villainRange$,
        this.communityCards$
      ),
      filter(([, hero, range]) => hero.length === 2 && range.size > 0),
      switchMap(([, heroHand, villainRange, communityCards]) => {
        const rangeNotation = rangeToNotation(villainRange);

        return this.analysisService.calculateEquity(
          heroHand,
          rangeNotation,
          communityCards
        ).pipe(
          tapResponse(
            result => this.setEquityResult(result),
            () => this.setError('Failed to calculate equity')
          )
        );
      })
    )
  );

  readonly calculateEquityQuick = this.effect<void>(trigger$ =>
    trigger$.pipe(
      tap(() => this.setCalculating(true)),
      withLatestFrom(
        this.heroHand$,
        this.villainRange$,
        this.communityCards$
      ),
      filter(([, hero, range]) => hero.length === 2 && range.size > 0),
      switchMap(([, heroHand, villainRange, communityCards]) => {
        const rangeNotation = rangeToNotation(villainRange);

        return this.analysisService.calculateEquityQuick(
          heroHand,
          rangeNotation,
          communityCards
        ).pipe(
          tapResponse(
            result => this.setEquityResult(result),
            () => this.setError('Failed to calculate equity')
          )
        );
      })
    )
  );

  
  readonly autoCalculate = this.effect<void>(trigger$ =>
    trigger$.pipe(
      debounceTime(500),
      withLatestFrom(this.canCalculate$),
      filter(([, canCalc]) => canCalc),
      tap(() => this.calculateEquityQuick())
    )
  );

  

  
  isCellSelected(row: Rank, col: Rank): boolean {
    const notation = getCellNotation(row, col);
    const range = this.get(state => state.villainRange);
    return range.has(notation);
  }

  
  getCellStats(row: Rank, col: Rank): { notation: string; combos: number; isPair: boolean; isSuited: boolean } {
    const notation = getCellNotation(row, col);
    const combos = getComboCount(row, col);
    const rowIdx = RANKS.indexOf(row);
    const colIdx = RANKS.indexOf(col);
    
    return {
      notation,
      combos,
      isPair: rowIdx === colIdx,
      isSuited: rowIdx < colIdx
    };
  }

  
  applyPreset(presetKey: string, additive = false): void {
    const preset = PRESET_RANGES[presetKey];
    if (!preset) return;

    if (additive) {
      const current = this.get(state => state.villainRange);
      const combined = new Set([...current, ...preset.hands]);
      this.setVillainRange(combined);
    } else {
      this.setPresetRange(presetKey);
    }
  }

  
  selectHandType(type: 'pairs' | 'suited' | 'offsuit', select: boolean): void {
    const current = this.get(state => state.villainRange);
    const newRange = new Set(current);

    for (let i = 0; i < RANKS.length; i++) {
      for (let j = 0; j < RANKS.length; j++) {
        const row = RANKS[i];
        const col = RANKS[j];
        const notation = getCellNotation(row, col);
        
        const isPair = i === j;
        const isSuited = i < j;
        const isOffsuit = i > j;

        let matches = false;
        if (type === 'pairs' && isPair) matches = true;
        if (type === 'suited' && isSuited) matches = true;
        if (type === 'offsuit' && isOffsuit) matches = true;

        if (matches) {
          if (select) {
            newRange.add(notation);
          } else {
            newRange.delete(notation);
          }
        }
      }
    }

    this.setVillainRange(newRange);
  }
}
