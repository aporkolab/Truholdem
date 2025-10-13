export const RANKS = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'] as const;
export type Rank = typeof RANKS[number];


export type Suit = 'HEARTS' | 'DIAMONDS' | 'CLUBS' | 'SPADES';
export const SUITS: Suit[] = ['HEARTS', 'DIAMONDS', 'CLUBS', 'SPADES'];


export interface SelectedCard {
  rank: Rank;
  suit: Suit;
  display: string;
}


export type ComboType = 'POCKET_PAIR' | 'SUITED' | 'OFFSUIT' | 'ANY';


export interface HandCombo {
  highCard: Rank;
  lowCard: Rank;
  type: ComboType;
  notation: string;
}


export interface RangeCell {
  row: Rank;
  col: Rank;
  notation: string;
  isSuited: boolean;
  isPair: boolean;
  isOffsuit: boolean;
  selected: boolean;
  combos: number;
  color?: string;
}


export interface RangePreset {
  name: string;
  description: string;
  hands: string[];
  percentage: number;
}


export interface EquityResult {
  heroEquity: number;
  villainEquity: number;
  tieEquity: number;
  simulationCount: number;
  handTypeBreakdown?: Record<string, number>;
}


export interface EVResult {
  action: string;
  expectedValue: number;
  actualEquity: number;
  foldEquity?: number;
  potOdds?: number;
}


export interface HandRange {
  selectedCells: Set<string>;
  percentage: number;
  comboCount: number;
  notation: string;
}


export const TOTAL_COMBOS = 1326;

interface ComboCounts {
  pair: number;
  suited: number;
  offsuit: number;
}

export const COMBO_COUNTS: ComboCounts = {
  pair: 6,
  suited: 4,
  offsuit: 12
};


export const PRESET_RANGES: Record<string, RangePreset> = {
  premium: {
    name: 'Premium',
    description: 'Top premium hands (AA, KK, QQ, AKs)',
    hands: ['AA', 'KK', 'QQ', 'AKs'],
    percentage: 2.6
  },
  broadway: {
    name: 'Broadway',
    description: 'All broadway hands (T+ cards)',
    hands: [
      'AA', 'KK', 'QQ', 'JJ', 'TT',
      'AKs', 'AQs', 'AJs', 'ATs', 'KQs', 'KJs', 'KTs', 'QJs', 'QTs', 'JTs',
      'AKo', 'AQo', 'AJo', 'ATo', 'KQo', 'KJo', 'KTo', 'QJo', 'QTo', 'JTo'
    ],
    percentage: 14.3
  },
  pairs: {
    name: 'All Pairs',
    description: 'All pocket pairs (22-AA)',
    hands: ['AA', 'KK', 'QQ', 'JJ', 'TT', '99', '88', '77', '66', '55', '44', '33', '22'],
    percentage: 5.9
  },
  suitedConnectors: {
    name: 'Suited Connectors',
    description: 'Connected suited hands',
    hands: ['AKs', 'KQs', 'QJs', 'JTs', 'T9s', '98s', '87s', '76s', '65s', '54s', '43s', '32s'],
    percentage: 3.6
  },
  earlyPosition: {
    name: 'Early Position',
    description: 'Typical UTG/UTG+1 opening range (~12%)',
    hands: [
      'AA', 'KK', 'QQ', 'JJ', 'TT',
      'AKs', 'AQs', 'AJs', 'ATs', 'KQs',
      'AKo', 'AQo'
    ],
    percentage: 12.0
  },
  buttonOpen: {
    name: 'Button Open',
    description: 'Wide button opening range (~45%)',
    hands: [
      'AA', 'KK', 'QQ', 'JJ', 'TT', '99', '88', '77', '66', '55', '44', '33', '22',
      'AKs', 'AQs', 'AJs', 'ATs', 'A9s', 'A8s', 'A7s', 'A6s', 'A5s', 'A4s', 'A3s', 'A2s',
      'KQs', 'KJs', 'KTs', 'K9s', 'K8s', 'K7s', 'K6s', 'K5s',
      'QJs', 'QTs', 'Q9s', 'Q8s',
      'JTs', 'J9s', 'J8s',
      'T9s', 'T8s',
      '98s', '97s',
      '87s', '76s', '65s', '54s',
      'AKo', 'AQo', 'AJo', 'ATo', 'A9o',
      'KQo', 'KJo', 'KTo', 'K9o',
      'QJo', 'QTo',
      'JTo'
    ],
    percentage: 45.0
  }
};


export const SUIT_SYMBOLS: Record<Suit, string> = {
  HEARTS: '♥',
  DIAMONDS: '♦',
  CLUBS: '♣',
  SPADES: '♠'
};

export const SUIT_COLORS: Record<Suit, string> = {
  HEARTS: '#e53935',
  DIAMONDS: '#1e88e5',
  CLUBS: '#43a047',
  SPADES: '#424242'
};


export function getCellNotation(row: Rank, col: Rank): string {
  const rowIdx = RANKS.indexOf(row);
  const colIdx = RANKS.indexOf(col);

  if (rowIdx === colIdx) {
    return `${row}${row}`;
  } else if (rowIdx < colIdx) {
    return `${row}${col}s`;
  } else {
    return `${col}${row}o`;
  }
}

export function getComboCount(row: Rank, col: Rank): number {
  const rowIdx = RANKS.indexOf(row);
  const colIdx = RANKS.indexOf(col);

  if (rowIdx === colIdx) return COMBO_COUNTS.pair;
  if (rowIdx < colIdx) return COMBO_COUNTS.suited;
  return COMBO_COUNTS.offsuit;
}

export function parseNotation(notation: string): HandCombo | null {
  const clean = notation.trim().toUpperCase();
  if (clean.length < 2) return null;

  const high = clean[0] as Rank;
  const low = clean[1] as Rank;

  if (!RANKS.includes(high) || !RANKS.includes(low)) return null;

  let type: ComboType;
  if (high === low) {
    type = 'POCKET_PAIR';
  } else if (clean.endsWith('S')) {
    type = 'SUITED';
  } else if (clean.endsWith('O')) {
    type = 'OFFSUIT';
  } else {
    type = 'ANY';
  }

  return { highCard: high, lowCard: low, type, notation: clean };
}

export function notationToRange(notationStr: string): Set<string> {
  const result = new Set<string>();
  const parts = notationStr.split(',').map(p => p.trim().toUpperCase());

  for (const part of parts) {
    const combo = parseNotation(part);
    if (!combo) continue;

    if (combo.type === 'POCKET_PAIR') {
      result.add(`${combo.highCard}${combo.highCard}`);
    } else if (combo.type === 'SUITED') {
      result.add(`${combo.highCard}${combo.lowCard}s`);
    } else if (combo.type === 'OFFSUIT') {
      result.add(`${combo.highCard}${combo.lowCard}o`);
    } else {

      result.add(`${combo.highCard}${combo.lowCard}s`);
      result.add(`${combo.highCard}${combo.lowCard}o`);
    }
  }

  return result;
}

export function rangeToNotation(selected: Set<string>): string {
  return Array.from(selected).sort().join(',');
}

export function calculateRangePercentage(selected: Set<string>): number {
  let totalCombos = 0;

  for (const notation of selected) {
    const parsed = parseNotation(notation);
    if (!parsed) continue;

    if (parsed.type === 'POCKET_PAIR') {
      totalCombos += COMBO_COUNTS.pair;
    } else if (parsed.type === 'SUITED' || notation.endsWith('s')) {
      totalCombos += COMBO_COUNTS.suited;
    } else {
      totalCombos += COMBO_COUNTS.offsuit;
    }
  }

  return totalCombos / TOTAL_COMBOS;
}
