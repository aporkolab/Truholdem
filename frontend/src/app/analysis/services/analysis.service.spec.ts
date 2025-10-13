import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalysisService } from './analysis.service';
import { SelectedCard } from '../models/analysis.models';
import { environment } from '../../../environments/environment';

describe('AnalysisService', () => {
  let service: AnalysisService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/analysis`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalysisService]
    });
    service = TestBed.inject(AnalysisService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('calculateEquity', () => {
    it('should call equity endpoint with correct payload', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];
      const villainRange = 'AA,KK,QQ';
      const communityCards: SelectedCard[] = [];

      service.calculateEquity(heroHand, villainRange, communityCards, 10000).subscribe(result => {
        expect(result.heroEquity).toBe(0.65);
        expect(result.villainEquity).toBe(0.30);
        expect(result.tieEquity).toBe(0.05);
      });

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        heroHand: [
          { suit: 'HEARTS', value: 'ACE' },
          { suit: 'HEARTS', value: 'KING' }
        ],
        communityCards: [],
        villainRange: 'AA,KK,QQ',
        iterations: 10000
      });

      req.flush({
        equity: 0.65,
        loseEquity: 0.30,
        tieEquity: 0.05,
        simulationCount: 10000
      });
    });

    it('should include community cards in request', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'A', suit: 'CLUBS', display: 'A♣' }
      ];
      const communityCards: SelectedCard[] = [
        { rank: 'K', suit: 'HEARTS', display: 'K♥' },
        { rank: 'Q', suit: 'HEARTS', display: 'Q♥' },
        { rank: 'J', suit: 'HEARTS', display: 'J♥' }
      ];

      service.calculateEquity(heroHand, 'AA', communityCards).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      expect(req.request.body.communityCards).toEqual([
        { suit: 'HEARTS', value: 'KING' },
        { suit: 'HEARTS', value: 'QUEEN' },
        { suit: 'HEARTS', value: 'JACK' }
      ]);
      req.flush({ equity: 0.5, loseEquity: 0.5, tieEquity: 0, simulationCount: 10000 });
    });

    it('should return fallback result on error', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];

      service.calculateEquity(heroHand, 'AA').subscribe(result => {
        expect(result.heroEquity).toBe(0.5);
        expect(result.villainEquity).toBe(0.5);
        expect(result.tieEquity).toBe(0);
        expect(result.simulationCount).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should map hand type breakdown correctly', () => {
      const heroHand: SelectedCard[] = [
        { rank: '7', suit: 'DIAMONDS', display: '7♦' },
        { rank: '7', suit: 'CLUBS', display: '7♣' }
      ];

      service.calculateEquity(heroHand, 'QQ+').subscribe(result => {
        expect(result.handTypeBreakdown).toEqual({
          'PAIR': 0.6,
          'TWO_PAIR': 0.2,
          'THREE_OF_A_KIND': 0.1,
          'FULL_HOUSE': 0.1
        });
      });

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      req.flush({
        equity: 0.35,
        loseEquity: 0.65,
        tieEquity: 0,
        simulationCount: 10000,
        handTypeBreakdown: {
          'PAIR': 0.6,
          'TWO_PAIR': 0.2,
          'THREE_OF_A_KIND': 0.1,
          'FULL_HOUSE': 0.1
        }
      });
    });
  });

  describe('calculateEquityQuick', () => {
    it('should use 2000 iterations', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'Q', suit: 'HEARTS', display: 'Q♥' },
        { rank: 'Q', suit: 'SPADES', display: 'Q♠' }
      ];

      service.calculateEquityQuick(heroHand, 'AA,KK').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      expect(req.request.body.iterations).toBe(2000);
      req.flush({ equity: 0.5, loseEquity: 0.5, tieEquity: 0, simulationCount: 2000 });
    });
  });

  describe('calculateEquityPrecise', () => {
    it('should use 50000 iterations', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'J', suit: 'CLUBS', display: 'J♣' },
        { rank: 'T', suit: 'CLUBS', display: 'T♣' }
      ];

      service.calculateEquityPrecise(heroHand, 'TT+,AK').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/equity`);
      expect(req.request.body.iterations).toBe(50000);
      req.flush({ equity: 0.4, loseEquity: 0.6, tieEquity: 0, simulationCount: 50000 });
    });
  });

  describe('calculateEV', () => {
    it('should call EV endpoint with pot odds parameters', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];
      const communityCards: SelectedCard[] = [
        { rank: 'Q', suit: 'HEARTS', display: 'Q♥' },
        { rank: 'J', suit: 'DIAMONDS', display: 'J♦' },
        { rank: '2', suit: 'CLUBS', display: '2♣' }
      ];

      service.calculateEV(heroHand, communityCards, 100, 50, 'AA,KK,QQ').subscribe(results => {
        expect(results.length).toBe(3);
        expect(results[0].action).toBe('fold');
        expect(results[1].action).toBe('call');
        expect(results[2].action).toBe('raise');
      });

      const req = httpMock.expectOne(`${apiUrl}/ev`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        heroHand: 'AH,KH',
        communityCards: 'QH,JD,2C',
        potSize: 100,
        betToCall: 50,
        villainRange: 'AA,KK,QQ'
      });

      req.flush({
        fold: { ev: 0, recommended: false },
        call: { ev: 25, recommended: true },
        raise: { ev: 15, recommended: false }
      });
    });

    it('should return empty array on error', () => {
      const heroHand: SelectedCard[] = [
        { rank: '2', suit: 'HEARTS', display: '2♥' },
        { rank: '7', suit: 'SPADES', display: '7♠' }
      ];

      service.calculateEV(heroHand, [], 100, 50, 'AA').subscribe(results => {
        expect(results).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/ev`);
      req.error(new ErrorEvent('Server error'));
    });
  });

  describe('estimateEquityLocal', () => {
    it('should return neutral result for incomplete hand', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' }
      ];

      const result = service.estimateEquityLocal(heroHand, 0.1);

      expect(result.heroEquity).toBe(0.5);
      expect(result.villainEquity).toBe(0.5);
      expect(result.simulationCount).toBe(0);
    });

    it('should calculate high equity for premium pairs', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ];

      const result = service.estimateEquityLocal(heroHand, 0.2);

      expect(result.heroEquity).toBeGreaterThan(0.8);
    });

    it('should calculate lower equity for weak hands', () => {
      const heroHand: SelectedCard[] = [
        { rank: '7', suit: 'HEARTS', display: '7♥' },
        { rank: '2', suit: 'SPADES', display: '2♠' }
      ];

      const result = service.estimateEquityLocal(heroHand, 0.5);

      expect(result.heroEquity).toBeLessThan(0.4);
    });

    it('should give suited bonus', () => {
      const suitedHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];
      const offsuitHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'SPADES', display: 'K♠' }
      ];

      const suitedResult = service.estimateEquityLocal(suitedHand, 0.3);
      const offsuitResult = service.estimateEquityLocal(offsuitHand, 0.3);

      expect(suitedResult.heroEquity).toBeGreaterThan(offsuitResult.heroEquity);
    });

    it('should give connected bonus', () => {
      const connectedHand: SelectedCard[] = [
        { rank: 'J', suit: 'HEARTS', display: 'J♥' },
        { rank: 'T', suit: 'SPADES', display: 'T♠' }
      ];
      const gappedHand: SelectedCard[] = [
        { rank: 'J', suit: 'HEARTS', display: 'J♥' },
        { rank: '5', suit: 'SPADES', display: '5♠' }
      ];

      const connectedResult = service.estimateEquityLocal(connectedHand, 0.3);
      const gappedResult = service.estimateEquityLocal(gappedHand, 0.3);

      expect(connectedResult.heroEquity).toBeGreaterThan(gappedResult.heroEquity);
    });

    it('should adjust equity based on villain range size', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'A', suit: 'SPADES', display: 'A♠' }
      ];

      const narrowResult = service.estimateEquityLocal(heroHand, 0.1);
      const wideResult = service.estimateEquityLocal(heroHand, 0.5);

      expect(narrowResult.heroEquity).toBeGreaterThan(wideResult.heroEquity);
    });
  });

  describe('getAvailableCards', () => {
    it('should return 52 cards when no dead cards', () => {
      const available = service.getAvailableCards([]);
      expect(available.length).toBe(52);
    });

    it('should exclude dead cards', () => {
      const deadCards: SelectedCard[] = [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'SPADES', display: 'K♠' }
      ];

      const available = service.getAvailableCards(deadCards);

      expect(available.length).toBe(50);
      expect(available.find(c => c.rank === 'A' && c.suit === 'HEARTS')).toBeUndefined();
      expect(available.find(c => c.rank === 'K' && c.suit === 'SPADES')).toBeUndefined();
    });

    it('should include display property with suit symbol', () => {
      const available = service.getAvailableCards([]);

      const aceOfHearts = available.find(c => c.rank === 'A' && c.suit === 'HEARTS');
      expect(aceOfHearts?.display).toBe('A♥');

      const kingOfSpades = available.find(c => c.rank === 'K' && c.suit === 'SPADES');
      expect(kingOfSpades?.display).toBe('K♠');
    });

    it('should return cards for all ranks and suits', () => {
      const available = service.getAvailableCards([]);
      const ranks = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'];
      const suits = ['HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES'];

      for (const rank of ranks) {
        for (const suit of suits) {
          const card = available.find(c => c.rank === rank && c.suit === suit);
          expect(card).toBeDefined();
        }
      }
    });
  });

  describe('rank conversion', () => {
    it('should convert all ranks to backend format correctly', () => {
      const testCases = [
        { rank: 'A', expected: 'ACE' },
        { rank: 'K', expected: 'KING' },
        { rank: 'Q', expected: 'QUEEN' },
        { rank: 'J', expected: 'JACK' },
        { rank: 'T', expected: 'TEN' },
        { rank: '9', expected: 'NINE' },
        { rank: '2', expected: 'TWO' }
      ];

      for (const { rank, expected } of testCases) {
        const heroHand: SelectedCard[] = [
          { rank: rank as unknown as SelectedCard['rank'], suit: 'HEARTS', display: `${rank}♥` },
          { rank: 'A', suit: 'SPADES', display: 'A♠' }
        ];

        service.calculateEquity(heroHand, 'AA').subscribe();
        const req = httpMock.expectOne(`${apiUrl}/equity`);
        expect(req.request.body.heroHand[0].value).toBe(expected);
        req.flush({ equity: 0.5, loseEquity: 0.5, tieEquity: 0, simulationCount: 1000 });
      }
    });
  });

  

  describe('getHandAnalysis', () => {
    it('should call hand analysis endpoint with correct parameters', () => {
      const handId = '12345678-1234-1234-1234-123456789012';
      const playerName = 'TestPlayer';

      service.getHandAnalysis(handId, playerName).subscribe(result => {
        expect(result).toBeTruthy();
        expect(result?.handId).toBe(handId);
        expect(result?.playerName).toBe(playerName);
      });

      const req = httpMock.expectOne(
        req => req.url === `${apiUrl}/hand/${handId}` && 
               req.params.get('playerName') === playerName
      );
      expect(req.request.method).toBe('GET');

      req.flush({
        handId,
        playerName,
        summary: 'Test hand analysis',
        heroHand: ['AS', 'AH'],
        communityCards: ['KS', 'QS', 'JS'],
        finalPot: 200,
        netResult: 100,
        keyDecisions: [],
        streetSummaries: {},
        overallAssessment: 'GOOD',
        totalEVLost: 5,
        mistakeCount: 0,
        suggestions: ['Keep playing like this'],
        studyTopics: ['Advanced GTO concepts']
      });
    });

    it('should return null on error', () => {
      service.getHandAnalysis('invalid-id', 'TestPlayer').subscribe(result => {
        expect(result).toBeNull();
      });

      const req = httpMock.expectOne(req => req.url.includes('/hand/'));
      req.error(new ErrorEvent('Not found'));
    });
  });

  describe('getGTORecommendation', () => {
    it('should call recommend endpoint with all parameters', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'SPADES', display: 'K♠' }
      ];
      const communityCards: SelectedCard[] = [
        { rank: 'Q', suit: 'SPADES', display: 'Q♠' },
        { rank: 'J', suit: 'HEARTS', display: 'J♥' },
        { rank: '2', suit: 'CLUBS', display: '2♣' }
      ];

      service.getGTORecommendation(
        heroHand,
        communityCards,
        100,  
        50,   
        'DEALER',
        'AA,KK,QQ',
        5,    
        0,    
        6     
      ).subscribe(result => {
        expect(result).toBeTruthy();
        expect(result?.recommendedAction).toBe('RAISE');
        expect(result?.confidence).toBeGreaterThan(0.8);
      });

      const req = httpMock.expectOne(`${apiUrl}/recommend`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.potSize).toBe(100);
      expect(req.request.body.betToCall).toBe(50);
      expect(req.request.body.position).toBe('DEALER');

      req.flush({
        recommendedAction: 'RAISE',
        confidence: 0.85,
        reasoning: 'Strong draw with nut flush potential',
        alternatives: [
          { action: 'CALL', frequency: 0.15, ev: 25, reasoning: 'Slowplay option' }
        ],
        positionAdvice: 'In position, can be more aggressive',
        handStrengthCategory: 'STRONG',
        situationalFactors: ['Flush draw', 'Straight draw', 'Good position']
      });
    });

    it('should handle empty community cards', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'Q', suit: 'HEARTS', display: 'Q♥' },
        { rank: 'Q', suit: 'SPADES', display: 'Q♠' }
      ];

      service.getGTORecommendation(
        heroHand,
        [],  
        50,
        10,
        'MIDDLE',
        'AA,KK'
      ).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/recommend`);
      expect(req.request.body.communityCards).toBeNull();
      req.flush({
        recommendedAction: 'CALL',
        confidence: 0.6,
        reasoning: 'Set mining opportunity',
        alternatives: [],
        positionAdvice: 'Middle position, play cautiously',
        handStrengthCategory: 'MEDIUM',
        situationalFactors: []
      });
    });

    it('should return null on error', () => {
      const heroHand: SelectedCard[] = [
        { rank: '2', suit: 'HEARTS', display: '2♥' },
        { rank: '7', suit: 'SPADES', display: '7♠' }
      ];

      service.getGTORecommendation(heroHand, [], 100, 50, 'DEALER', 'AA').subscribe(result => {
        expect(result).toBeNull();
      });

      const req = httpMock.expectOne(`${apiUrl}/recommend`);
      req.error(new ErrorEvent('Server error'));
    });
  });

  describe('getRangePresets', () => {
    it('should fetch and cache range presets', () => {
      
      service.getRangePresets().subscribe(presets => {
        expect(presets.length).toBeGreaterThan(0);
        expect(presets[0].name).toBe('Premium');
        expect(presets[0].hands).toContain('AA');
      });

      const req = httpMock.expectOne(`${apiUrl}/ranges/presets`);
      expect(req.request.method).toBe('GET');

      req.flush([
        {
          id: 'premium',
          name: 'Premium',
          description: 'Top premium hands',
          hands: ['AA', 'KK', 'QQ', 'AKs'],
          percentage: 2.6
        },
        {
          id: 'buttonOpen',
          name: 'Button Open',
          description: 'Wide button range',
          hands: ['AA', 'KK', 'QQ', 'JJ', 'TT', 'AKs', 'AKo'],
          percentage: 45.0
        }
      ]);

      
      service.getRangePresets().subscribe(presets => {
        expect(presets.length).toBe(2);
      });

      
      httpMock.expectNone(`${apiUrl}/ranges/presets`);
    });

    it('should return default presets on error', () => {
      
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [AnalysisService]
      });
      service = TestBed.inject(AnalysisService);
      httpMock = TestBed.inject(HttpTestingController);

      service.getRangePresets().subscribe(presets => {
        expect(presets.length).toBeGreaterThan(0);
        
        expect(presets.some(p => p.name === 'Premium')).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/ranges/presets`);
      req.error(new ErrorEvent('Network error'));
    });

    it('should map backend format to frontend format', () => {
      service.getRangePresets().subscribe(presets => {
        const preset = presets[0];
        
        expect(preset).toHaveProperty('name');
        expect(preset).toHaveProperty('description');
        expect(preset).toHaveProperty('hands');
        expect(preset).toHaveProperty('percentage');
        
        expect(preset).not.toHaveProperty('id');
      });

      const req = httpMock.expectOne(`${apiUrl}/ranges/presets`);
      req.flush([
        {
          id: 'premium',
          name: 'Premium',
          description: 'Top hands',
          hands: ['AA'],
          percentage: 2.6
        }
      ]);
    });
  });

  describe('cardsToString helper', () => {
    it('should convert cards to comma-separated string for EV endpoint', () => {
      const heroHand: SelectedCard[] = [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ];

      service.calculateEV(heroHand, [], 100, 50, 'AA').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/ev`);
      expect(req.request.body.heroHand).toBe('AS,KH');
      req.flush({});
    });
  });
});