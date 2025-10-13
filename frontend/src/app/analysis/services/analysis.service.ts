import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { 
  EquityResult, 
  EVResult, 
  SelectedCard, 
  RANKS, 
  SUITS,
  Rank,
  Suit,
  SUIT_SYMBOLS,
  RangePreset
} from '../models/analysis.models';

interface BackendCard {
  suit: Suit;
  value: string;
}

interface EquityRequest {
  heroHand: BackendCard[];
  communityCards: BackendCard[];
  villainRange: string;
  iterations?: number;
}

interface BackendEquityResult {
  equity: number;
  tieEquity: number;
  loseEquity: number;
  simulationCount: number;
  handTypeBreakdown?: Record<string, number>;
}


export interface GTORecommendation {
  recommendedAction: string;
  confidence: number;
  reasoning: string;
  alternatives: ActionAlternative[];
  positionAdvice: string;
  handStrengthCategory: HandStrengthCategory;
  situationalFactors: string[];
}

export interface ActionAlternative {
  action: string;
  frequency: number;
  ev: number;
  reasoning: string;
}

export type HandStrengthCategory = 
  | 'PREMIUM' 
  | 'STRONG' 
  | 'MEDIUM' 
  | 'SPECULATIVE' 
  | 'MARGINAL' 
  | 'WEAK';


export interface HandAnalysis {
  handId: string;
  playerName: string;
  summary: string;
  heroHand: string[];
  communityCards: string[];
  finalPot: number;
  netResult: number;
  keyDecisions: DecisionAnalysis[];
  streetSummaries: Record<string, string>;
  overallAssessment: OverallAssessment;
  totalEVLost: number;
  mistakeCount: number;
  suggestions: string[];
  studyTopics: string[];
}

export interface DecisionAnalysis {
  phase: string;
  situation: string;
  actualAction: string;
  optimalAction: string;
  evDifference: number;
  equity: EquityResult;
  evOptions: EVResult[];
  gtoRecommendation: GTORecommendation;
  explanation: string;
}

export type OverallAssessment = 
  | 'OPTIMAL' 
  | 'GOOD' 
  | 'MIXED' 
  | 'POOR' 
  | 'COSTLY';


interface BackendRangePreset {
  id: string;
  name: string;
  description: string;
  hands: string[];
  percentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalysisService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/analysis`;

  
  calculateEquity(
    heroHand: SelectedCard[],
    villainRange: string,
    communityCards: SelectedCard[] = [],
    iterations = 10000
  ): Observable<EquityResult> {
    const request: EquityRequest = {
      heroHand: heroHand.map(c => this.toBackendCard(c)),
      communityCards: communityCards.map(c => this.toBackendCard(c)),
      villainRange,
      iterations
    };

    return this.http.post<BackendEquityResult>(`${this.apiUrl}/equity`, request).pipe(
      map(result => ({
        heroEquity: result.equity,
        villainEquity: result.loseEquity,
        tieEquity: result.tieEquity,
        simulationCount: result.simulationCount,
        handTypeBreakdown: result.handTypeBreakdown
      })),
      catchError(error => {
        console.error('Equity calculation failed:', error);
        
        return of({
          heroEquity: 0.5,
          villainEquity: 0.5,
          tieEquity: 0,
          simulationCount: 0
        });
      })
    );
  }

  
  calculateEquityQuick(
    heroHand: SelectedCard[],
    villainRange: string,
    communityCards: SelectedCard[] = []
  ): Observable<EquityResult> {
    return this.calculateEquity(heroHand, villainRange, communityCards, 2000);
  }

  
  calculateEquityPrecise(
    heroHand: SelectedCard[],
    villainRange: string,
    communityCards: SelectedCard[] = []
  ): Observable<EquityResult> {
    return this.calculateEquity(heroHand, villainRange, communityCards, 50000);
  }

  
  calculateEV(
    heroHand: SelectedCard[],
    communityCards: SelectedCard[],
    potSize: number,
    betToCall: number,
    villainRange: string
  ): Observable<EVResult[]> {
    return this.http.post<Record<string, EVResult>>(`${this.apiUrl}/ev`, {
      heroHand: this.cardsToString(heroHand),
      communityCards: communityCards.length > 0 ? this.cardsToString(communityCards) : null,
      potSize,
      betToCall,
      villainRange
    }).pipe(
      map(result => Object.entries(result).map(([action, ev]) => ({
        ...ev,
        action
      }))),
      catchError(error => {
        console.error('EV calculation failed:', error);
        return of([]);
      })
    );
  }

  
  getHandAnalysis(handId: string, playerName: string): Observable<HandAnalysis | null> {
    return this.http.get<HandAnalysis>(`${this.apiUrl}/hand/${handId}`, {
      params: { playerName }
    }).pipe(
      catchError(error => {
        console.error('Hand analysis failed:', error);
        return of(null);
      })
    );
  }

  
  getGTORecommendation(
    heroHand: SelectedCard[],
    communityCards: SelectedCard[],
    potSize: number,
    betToCall: number,
    position: string,
    villainRange: string,
    seatNumber = 5,
    dealerPosition = 0,
    totalPlayers = 6
  ): Observable<GTORecommendation | null> {
    return this.http.post<GTORecommendation>(`${this.apiUrl}/recommend`, {
      heroHand: this.cardsToString(heroHand),
      communityCards: communityCards.length > 0 ? this.cardsToString(communityCards) : null,
      potSize,
      betToCall,
      position,
      villainPosition: position,
      villainRange,
      seatNumber,
      dealerPosition,
      totalPlayers
    }).pipe(
      catchError(error => {
        console.error('GTO recommendation failed:', error);
        return of(null);
      })
    );
  }

  
  private rangePresetsCache$: Observable<RangePreset[]> | null = null;
  
  getRangePresets(): Observable<RangePreset[]> {
    if (!this.rangePresetsCache$) {
      this.rangePresetsCache$ = this.http.get<BackendRangePreset[]>(`${this.apiUrl}/ranges/presets`).pipe(
        map(presets => presets.map(p => ({
          name: p.name,
          description: p.description,
          hands: p.hands,
          percentage: p.percentage
        }))),
        shareReplay(1),
        catchError(error => {
          console.error('Failed to fetch range presets:', error);
          return of(this.getDefaultPresets());
        })
      );
    }
    return this.rangePresetsCache$;
  }

  
  private getDefaultPresets(): RangePreset[] {
    return [
      { name: 'Premium', description: 'Top premium hands', hands: ['AA', 'KK', 'QQ', 'AKs'], percentage: 2.6 },
      { name: 'Early Position', description: 'UTG opening range', hands: ['AA', 'KK', 'QQ', 'JJ', 'TT', 'AKs', 'AQs', 'AKo', 'AQo'], percentage: 12.0 },
      { name: 'Button Open', description: 'Wide button range', hands: ['AA', 'KK', 'QQ', 'JJ', 'TT', '99', '88', '77', '66', '55', '44', '33', '22', 'AKs', 'AQs', 'AJs', 'ATs', 'A9s', 'KQs', 'KJs', 'QJs', 'JTs', 'AKo', 'AQo', 'KQo'], percentage: 45.0 }
    ];
  }

  
  private cardsToString(cards: SelectedCard[]): string {
    return cards.map(c => `${c.rank}${c.suit.charAt(0)}`).join(',');
  }

  
  estimateEquityLocal(
    heroHand: SelectedCard[],
    villainRangeSize: number
  ): EquityResult {
    if (heroHand.length !== 2) {
      return { heroEquity: 0.5, villainEquity: 0.5, tieEquity: 0, simulationCount: 0 };
    }

    const card1 = heroHand[0];
    const card2 = heroHand[1];
    
    
    let strength = this.calculateHandStrength(card1, card2);
    
    
    
    const rangeAdjustment = (0.5 - villainRangeSize) * 0.2;
    strength = Math.max(0, Math.min(1, strength + rangeAdjustment));

    return {
      heroEquity: strength,
      villainEquity: 1 - strength,
      tieEquity: 0,
      simulationCount: 0
    };
  }

  
  getAvailableCards(deadCards: SelectedCard[] = []): SelectedCard[] {
    const available: SelectedCard[] = [];
    const deadSet = new Set(deadCards.map(c => `${c.rank}${c.suit}`));

    for (const rank of RANKS) {
      for (const suit of SUITS) {
        const key = `${rank}${suit}`;
        if (!deadSet.has(key)) {
          available.push({
            rank,
            suit,
            display: `${rank}${SUIT_SYMBOLS[suit]}`
          });
        }
      }
    }

    return available;
  }

  
  private rankToValue(rank: Rank): string {
    const valueMap: Record<Rank, string> = {
      'A': 'ACE',
      'K': 'KING',
      'Q': 'QUEEN',
      'J': 'JACK',
      'T': 'TEN',
      '9': 'NINE',
      '8': 'EIGHT',
      '7': 'SEVEN',
      '6': 'SIX',
      '5': 'FIVE',
      '4': 'FOUR',
      '3': 'THREE',
      '2': 'TWO'
    };
    return valueMap[rank];
  }

  
  private toBackendCard(card: SelectedCard): BackendCard {
    return {
      suit: card.suit,
      value: this.rankToValue(card.rank)
    };
  }

  
  private calculateHandStrength(card1: SelectedCard, card2: SelectedCard): number {
    const rankValues: Record<Rank, number> = {
      'A': 14, 'K': 13, 'Q': 12, 'J': 11, 'T': 10,
      '9': 9, '8': 8, '7': 7, '6': 6, '5': 5, '4': 4, '3': 3, '2': 2
    };

    const v1 = rankValues[card1.rank];
    const v2 = rankValues[card2.rank];
    const highCard = Math.max(v1, v2);
    const lowCard = Math.min(v1, v2);
    const isPair = v1 === v2;
    const isSuited = card1.suit === card2.suit;
    const gap = highCard - lowCard;
    const isConnected = gap === 1;

    let strength = 0;

    
    if (isPair) {
      strength = 0.5 + (highCard / 14) * 0.4; 
    } else {
      
      strength = (highCard / 14) * 0.3 + (lowCard / 14) * 0.15;
      
      
      if (isSuited) strength += 0.04;
      
      
      if (isConnected) strength += 0.02;
      if (gap <= 3) strength += 0.01;
      
      
      if (highCard >= 10 && lowCard >= 10) strength += 0.05;
    }

    return Math.min(0.95, Math.max(0.15, strength));
  }
}
